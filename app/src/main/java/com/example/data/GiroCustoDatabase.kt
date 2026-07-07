package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Vehicle::class, VehiclePart::class, DailyRecord::class],
    version = 1,
    exportSchema = false
)
abstract class GiroCustoDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun vehiclePartDao(): VehiclePartDao
    abstract fun dailyRecordDao(): DailyRecordDao

    companion object {
        @Volatile
        private var INSTANCE: GiroCustoDatabase? = null

        fun getDatabase(context: Context): GiroCustoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GiroCustoDatabase::class.java,
                    "giro_custo_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
