package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle WHERE active = 1 AND isDeleted = 0 LIMIT 1")
    fun getActiveVehicleFlow(): Flow<Vehicle?>

    @Query("SELECT * FROM vehicle WHERE active = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getActiveVehicle(): Vehicle?

    @Query("SELECT * FROM vehicle WHERE id = :vehicleId LIMIT 1")
    suspend fun getVehicleById(vehicleId: Long): Vehicle?

    @Query("SELECT * FROM vehicle WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getVehicleByRemoteId(remoteId: String): Vehicle?

    @Query("SELECT * FROM vehicle WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllVehiclesFlow(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicle WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllVehicles(): List<Vehicle>

    @Query("SELECT * FROM vehicle WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingVehicles(): List<Vehicle>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Query("UPDATE vehicle SET active = 0")
    suspend fun clearAllActive()

    @Query("UPDATE vehicle SET active = 1 WHERE id = :vehicleId")
    suspend fun setSingleActive(vehicleId: Long)

    @Transaction
    suspend fun setActiveVehicle(vehicleId: Long) {
        clearAllActive()
        setSingleActive(vehicleId)
    }

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)

    @Query("DELETE FROM vehicle WHERE id = :id")
    suspend fun hardDeleteVehicle(id: Long)
}

@Dao
interface VehiclePartDao {
    @Query("SELECT * FROM vehicle_parts WHERE isDeleted = 0 ORDER BY id ASC")
    fun getAllPartsFlow(): Flow<List<VehiclePart>>

    @Query("SELECT * FROM vehicle_parts WHERE (vehicleId = :vehicleId OR vehicleId = 0) AND isDeleted = 0 ORDER BY id ASC")
    fun getPartsForVehicleFlow(vehicleId: Long): Flow<List<VehiclePart>>

    @Query("SELECT * FROM vehicle_parts WHERE isDeleted = 0 ORDER BY id ASC")
    suspend fun getAllParts(): List<VehiclePart>

    @Query("SELECT * FROM vehicle_parts WHERE (vehicleId = :vehicleId OR vehicleId = 0) AND isDeleted = 0 ORDER BY id ASC")
    suspend fun getPartsForVehicle(vehicleId: Long): List<VehiclePart>

    @Query("SELECT * FROM vehicle_parts WHERE id = :partId LIMIT 1")
    suspend fun getPartById(partId: Long): VehiclePart?

    @Query("SELECT * FROM vehicle_parts WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getPartByRemoteId(remoteId: String): VehiclePart?

    @Query("SELECT * FROM vehicle_parts WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingParts(): List<VehiclePart>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: VehiclePart)

    @Update
    suspend fun updatePart(part: VehiclePart)

    @Delete
    suspend fun deletePart(part: VehiclePart)

    @Query("DELETE FROM vehicle_parts WHERE id = :id")
    suspend fun hardDeletePart(id: Long)

    @Query("UPDATE vehicle_parts SET runKmSinceChange = 0.0 WHERE id = :partId")
    suspend fun resetPartWear(partId: Long)

    @Query("UPDATE vehicle_parts SET runKmSinceChange = runKmSinceChange + :addedKm")
    suspend fun addKmToAllParts(addedKm: Double)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE isDeleted = 0 ORDER BY dateTimestamp DESC")
    fun getAllRecordsFlow(): Flow<List<DailyRecord>>

    @Query("SELECT * FROM daily_records WHERE isDeleted = 0 ORDER BY dateTimestamp DESC")
    suspend fun getAllRecords(): List<DailyRecord>

    @Query("SELECT * FROM daily_records WHERE id = :recordId LIMIT 1")
    suspend fun getRecordById(recordId: Long): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRecordByRemoteId(remoteId: String): DailyRecord?

    @Query("SELECT * FROM daily_records WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingRecords(): List<DailyRecord>

    @Query("SELECT * FROM daily_records WHERE isDeleted = 0 ORDER BY dateTimestamp DESC LIMIT 1")
    suspend fun getMostRecentRecord(): DailyRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DailyRecord): Long

    @Update
    suspend fun updateRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE id = :recordId")
    suspend fun deleteRecordById(recordId: Long)

    @Query("DELETE FROM daily_records WHERE dateString LIKE '2026-06%'")
    suspend fun deleteJune2026Records()

    @Delete
    suspend fun deleteRecord(record: DailyRecord)

    @Query("DELETE FROM daily_records WHERE id = :id")
    suspend fun hardDeleteRecord(id: Long)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 AND isDeleted = 0 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Query("SELECT * FROM user_profile WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getUserProfileByRemoteId(remoteId: String): UserProfile?

    @Query("SELECT * FROM user_profile WHERE syncStatus != 'SYNCED' LIMIT 1")
    suspend fun getPendingUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(userProfile: UserProfile)
}

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platforms WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllPlatformsFlow(): Flow<List<Platform>>

    @Query("SELECT * FROM platforms WHERE isDeleted = 0 ORDER BY name ASC")
    suspend fun getAllPlatforms(): List<Platform>

    @Query("SELECT * FROM platforms WHERE id = :platformId LIMIT 1")
    suspend fun getPlatformById(platformId: Long): Platform?

    @Query("SELECT * FROM platforms WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getPlatformByRemoteId(remoteId: String): Platform?

    @Query("SELECT * FROM platforms WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingPlatforms(): List<Platform>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlatform(platform: Platform): Long

    @Update
    suspend fun updatePlatform(platform: Platform)

    @Delete
    suspend fun deletePlatform(platform: Platform)

    @Query("DELETE FROM platforms WHERE id = :id")
    suspend fun hardDeletePlatform(id: Long)
}

@Dao
interface FuelRefillDao {
    @Query("SELECT * FROM fuel_refills WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY dateTimestamp DESC")
    fun getRefillsForVehicleFlow(vehicleId: Long): Flow<List<FuelRefill>>

    @Query("SELECT * FROM fuel_refills WHERE vehicleId = :vehicleId AND isFullTank = 1 AND isDeleted = 0 ORDER BY odometer ASC")
    suspend fun getFullTankRefillsOrderedByOdometer(vehicleId: Long): List<FuelRefill>

    @Query("SELECT * FROM fuel_refills WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY odometer ASC")
    suspend fun getAllRefillsOrderedByOdometer(vehicleId: Long): List<FuelRefill>

    @Query("SELECT * FROM fuel_refills WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY odometer DESC, dateTimestamp DESC LIMIT 1")
    suspend fun getLatestRefillForVehicle(vehicleId: Long): FuelRefill?

    @Query("SELECT * FROM fuel_refills WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY odometer DESC, dateTimestamp DESC LIMIT 1")
    fun getLatestRefillForVehicleFlow(vehicleId: Long): Flow<FuelRefill?>

    @Query("SELECT * FROM fuel_refills WHERE id = :id LIMIT 1")
    suspend fun getRefillById(id: Long): FuelRefill?

    @Query("SELECT * FROM fuel_refills WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRefillByRemoteId(remoteId: String): FuelRefill?

    @Query("SELECT * FROM fuel_refills WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingRefills(): List<FuelRefill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefill(refill: FuelRefill): Long

    @Update
    suspend fun updateRefill(refill: FuelRefill)

    @Delete
    suspend fun deleteRefill(refill: FuelRefill)

    @Query("DELETE FROM fuel_refills WHERE id = :id")
    suspend fun hardDeleteRefill(id: Long)

    @Query("UPDATE fuel_refills SET gasStation = :newName WHERE gasStation = :oldName")
    suspend fun renameGasStation(oldName: String, newName: String)
}

@Dao
interface MaintenanceRecordDao {
    @Query("SELECT * FROM maintenance_records WHERE vehicleId = :vehicleId AND isDeleted = 0 ORDER BY dateTimestamp DESC")
    fun getRecordsForVehicleFlow(vehicleId: Long): Flow<List<MaintenanceRecord>>

    @Query("SELECT * FROM maintenance_records WHERE id = :id LIMIT 1")
    suspend fun getRecordById(id: Long): MaintenanceRecord?

    @Query("SELECT * FROM maintenance_records WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getRecordByRemoteId(remoteId: String): MaintenanceRecord?

    @Query("SELECT * FROM maintenance_records WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingRecords(): List<MaintenanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MaintenanceRecord): Long

    @Update
    suspend fun updateRecord(record: MaintenanceRecord)

    @Delete
    suspend fun deleteRecord(record: MaintenanceRecord)

    @Query("DELETE FROM maintenance_records WHERE id = :id")
    suspend fun hardDeleteRecord(id: Long)
}
