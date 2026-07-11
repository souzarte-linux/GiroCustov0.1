package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Vehicle::class, VehiclePart::class, DailyRecord::class, UserProfile::class, Platform::class, FuelRefill::class],
    version = 7,
    exportSchema = true
)
abstract class GiroCustoDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehiclePartDao(): VehiclePartDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun platformDao(): PlatformDao
    abstract fun fuelRefillDao(): FuelRefillDao

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

        fun getDatabase(context: Context): GiroCustoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GiroCustoDatabase::class.java,
                    "giro_custo_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun setTestDatabase(db: GiroCustoDatabase) {
            INSTANCE = db
        }
    }
}
