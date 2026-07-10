package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Vehicle::class, VehiclePart::class, DailyRecord::class, UserProfile::class, Platform::class],
    version = 4,
    exportSchema = true
)
abstract class GiroCustoDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehiclePartDao(): VehiclePartDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun platformDao(): PlatformDao

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

        fun getDatabase(context: Context): GiroCustoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GiroCustoDatabase::class.java,
                    "giro_custo_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
