package com.example.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Vehicle::class, VehiclePart::class, DailyRecord::class, UserProfile::class, Platform::class, FuelRefill::class, MaintenanceRecord::class],
    version = 13,
    exportSchema = true
)
abstract class GiroCustoDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehiclePartDao(): VehiclePartDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun platformDao(): PlatformDao
    abstract fun fuelRefillDao(): FuelRefillDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: GiroCustoDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_records ADD COLUMN platform TEXT NOT NULL DEFAULT 'Geral'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`phone` TEXT NOT NULL, " +
                    "`city` TEXT NOT NULL, " +
                    "`platforms` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `platforms` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`segment` TEXT NOT NULL, " +
                    "`paymentModel` TEXT NOT NULL, " +
                    "`cycle` TEXT NOT NULL, " +
                    "`paymentDay` TEXT NOT NULL, " +
                    "`fixedPayDelay` INTEGER NOT NULL, " +
                    "`cycleEntriesJson` TEXT NOT NULL, " +
                    "`bankName` TEXT NOT NULL, " +
                    "`bankAgency` TEXT NOT NULL, " +
                    "`bankAccount` TEXT NOT NULL, " +
                    "`pixKeyType` TEXT NOT NULL, " +
                    "`pixKey` TEXT NOT NULL, " +
                    "`active` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_profile_new` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`phone` TEXT NOT NULL, " +
                    "`city` TEXT NOT NULL, " +
                    "PRIMARY KEY(`id`))"
                )
                db.execSQL("INSERT INTO `user_profile_new` (`id`, `name`, `phone`, `city`) SELECT `id`, `name`, `phone`, `city` FROM `user_profile`")
                db.execSQL("DROP TABLE `user_profile`")
                db.execSQL("ALTER TABLE `user_profile_new` RENAME TO `user_profile`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vehicle ADD COLUMN active INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fuel_refills` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, " +
                    "`dateTimestamp` INTEGER NOT NULL, " +
                    "`dateString` TEXT NOT NULL, " +
                    "`gasStation` TEXT NOT NULL, " +
                    "`fuelType` TEXT NOT NULL, " +
                    "`pricePerLiter` REAL NOT NULL, " +
                    "`liters` REAL NOT NULL, " +
                    "`discount` REAL NOT NULL, " +
                    "`totalPaid` REAL NOT NULL, " +
                    "`odometer` REAL NOT NULL, " +
                    "`isFullTank` INTEGER NOT NULL, " +
                    "`paymentMethod` TEXT NOT NULL, " +
                    "`isInstallment` INTEGER NOT NULL, " +
                    "`installmentsCount` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_records` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`vehicleId` INTEGER NOT NULL, " +
                    "`dateTimestamp` INTEGER NOT NULL, " +
                    "`dateString` TEXT NOT NULL, " +
                    "`description` TEXT NOT NULL, " +
                    "`location` TEXT NOT NULL, " +
                    "`value` REAL NOT NULL, " +
                    "`odometer` REAL NOT NULL)"
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_records ADD COLUMN startTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE daily_records ADD COLUMN endTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE daily_records ADD COLUMN pauseDuration TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf(
                    "vehicle",
                    "vehicle_parts",
                    "daily_records",
                    "user_profile",
                    "platforms",
                    "fuel_refills",
                    "maintenance_records"
                )
                for (table in tables) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `remoteId` TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // user_profile
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `isWhatsapp` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `cep` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `street` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `addressNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `neighborhood` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `stateUf` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `onboardingCompleted` INTEGER NOT NULL DEFAULT 0")

                // vehicle
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `vehicleType` TEXT NOT NULL DEFAULT 'MOTO'")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `brand` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `manufacturingYear` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `tankCapacity` REAL NOT NULL DEFAULT 16.0")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `hasTopBox` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `hasDeliveryBag` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `vehicle` ADD COLUMN `carBodyType` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `vehicle_parts` ADD COLUMN `vehicleId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `vehicle_parts` SET `vehicleId` = (SELECT id FROM vehicle WHERE active = 1 LIMIT 1) WHERE `vehicleId` = 0 AND (SELECT id FROM vehicle WHERE active = 1 LIMIT 1) IS NOT NULL")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // vehicle
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_active_isDeleted` ON `vehicle` (`active`, `isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_remoteId` ON `vehicle` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_syncStatus` ON `vehicle` (`syncStatus`)")

                // vehicle_parts
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_vehicleId` ON `vehicle_parts` (`vehicleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_remoteId` ON `vehicle_parts` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vehicle_parts_syncStatus` ON `vehicle_parts` (`syncStatus`)")

                // daily_records
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_isDeleted_dateTimestamp` ON `daily_records` (`isDeleted`, `dateTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_remoteId` ON `daily_records` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_records_syncStatus` ON `daily_records` (`syncStatus`)")

                // user_profile
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_remoteId` ON `user_profile` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_syncStatus` ON `user_profile` (`syncStatus`)")

                // platforms
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_platforms_remoteId` ON `platforms` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_platforms_syncStatus` ON `platforms` (`syncStatus`)")

                // fuel_refills
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_vehicleId_dateTimestamp` ON `fuel_refills` (`vehicleId`, `dateTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_vehicleId_odometer` ON `fuel_refills` (`vehicleId`, `odometer`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_remoteId` ON `fuel_refills` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fuel_refills_syncStatus` ON `fuel_refills` (`syncStatus`)")

                // maintenance_records
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_vehicleId_dateTimestamp` ON `maintenance_records` (`vehicleId`, `dateTimestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_remoteId` ON `maintenance_records` (`remoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_syncStatus` ON `maintenance_records` (`syncStatus`)")
            }
        }

        fun getDatabase(context: Context): GiroCustoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GiroCustoDatabase::class.java,
                    "giro_custo_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build()
                INSTANCE = instance
                instance
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setTestDatabase(db: GiroCustoDatabase?) {
            synchronized(this) {
                INSTANCE = db
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun resetForTesting() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}
