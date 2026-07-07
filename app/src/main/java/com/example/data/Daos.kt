package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle WHERE id = 1 LIMIT 1")
    fun getVehicleFlow(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicle WHERE id = 1 LIMIT 1")
    suspend fun getVehicle(): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateVehicle(vehicle: Vehicle)
}

@Dao
interface VehiclePartDao {
    @Query("SELECT * FROM vehicle_parts ORDER BY id ASC")
    fun getAllPartsFlow(): Flow<List<VehiclePart>>

    @Query("SELECT * FROM vehicle_parts ORDER BY id ASC")
    suspend fun getAllParts(): List<VehiclePart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: VehiclePart)

    @Update
    suspend fun updatePart(part: VehiclePart)

    @Delete
    suspend fun deletePart(part: VehiclePart)

    @Query("UPDATE vehicle_parts SET runKmSinceChange = 0.0 WHERE id = :partId")
    suspend fun resetPartWear(partId: Long)

    @Query("UPDATE vehicle_parts SET runKmSinceChange = runKmSinceChange + :addedKm")
    suspend fun addKmToAllParts(addedKm: Double)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records ORDER BY dateTimestamp DESC")
    fun getAllRecordsFlow(): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records ORDER BY dateTimestamp DESC")
    suspend fun getAllRecords(): List<DailyRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Long)
}
