package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.network.supabase.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import com.example.util.CurrencyUtils
import java.util.*

class GiroCustoRepository(
    private val db: GiroCustoDatabase,
    val dataOwnerStorage: DataOwnerStorage? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()
    private val userProfileDao = db.userProfileDao()
    private val platformDao = db.platformDao()
    private val fuelRefillDao = db.fuelRefillDao()
    private val maintenanceRecordDao = db.maintenanceRecordDao()
    private val userGoalsDao = db.userGoalsDao()

    private val syncDelegate = RepositorySyncDelegate(db, dataOwnerStorage, ioDispatcher)

    val vehicleFlow: Flow<Vehicle?> = vehicleDao.getActiveVehicleFlow()
    val allVehiclesFlow: Flow<List<Vehicle>> = vehicleDao.getAllVehiclesFlow()
    val allPartsFlow: Flow<List<VehiclePart>> = partDao.getAllPartsFlow()
    val allRecordsFlow: Flow<List<DailyRecord>> = recordDao.getAllRecordsFlow()
    val userProfileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val allPlatformsFlow: Flow<List<Platform>> = platformDao.getAllPlatformsFlow()
    val userGoalsFlow: Flow<UserGoals?> = userGoalsDao.getUserGoalsFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val refillsForActiveVehicleFlow: Flow<List<FuelRefill>> = vehicleFlow.flatMapLatest { vehicle ->
        if (vehicle != null) {
            fuelRefillDao.getRefillsForVehicleFlow(vehicle.id)
        } else {
            flowOf(emptyList())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val latestRefillForActiveVehicleFlow: Flow<FuelRefill?> = vehicleFlow.flatMapLatest { vehicle ->
        if (vehicle != null) {
            fuelRefillDao.getLatestRefillForVehicleFlow(vehicle.id)
        } else {
            flowOf(null)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val partsForActiveVehicleFlow: Flow<List<VehiclePart>> = vehicleFlow.flatMapLatest { vehicle ->
        if (vehicle != null) {
            partDao.getPartsForVehicleFlow(vehicle.id)
        } else {
            partDao.getAllPartsFlow()
        }
    }

    suspend fun getVehicle(): Vehicle? = vehicleDao.getActiveVehicle()
    suspend fun getAllVehicles(): List<Vehicle> = vehicleDao.getAllVehicles()
    suspend fun getAllRecords(): List<DailyRecord> = recordDao.getAllRecords()

    suspend fun saveVehicle(vehicle: Vehicle) {
        if (vehicle.id == 0L) {
            addVehicle(vehicle)
        } else {
            val status = if (vehicle.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
            vehicleDao.updateVehicle(
                vehicle.copy(
                    remoteId = if (vehicle.remoteId.isNotBlank()) vehicle.remoteId else UUID.randomUUID().toString(),
                    syncStatus = status,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun addVehicle(vehicle: Vehicle): Long {
        val activeVehicle = vehicleDao.getActiveVehicle()
        val assignedRemoteId = if (vehicle.remoteId.isNotBlank()) vehicle.remoteId else UUID.randomUUID().toString()
        val toInsert = vehicle.copy(
            active = activeVehicle == null || vehicle.active,
            remoteId = assignedRemoteId,
            syncStatus = SyncStatus.PENDING_INSERT,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        return vehicleDao.insertVehicle(toInsert)
    }

    suspend fun setActiveVehicle(vehicleId: Long) {
        vehicleDao.setActiveVehicle(vehicleId)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        val toDelete = vehicle.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = System.currentTimeMillis()
        )
        vehicleDao.updateVehicle(toDelete)
    }

    suspend fun restoreVehicle(vehicle: Vehicle) {
        val toRestore = vehicle.copy(
            isDeleted = false,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = System.currentTimeMillis()
        )
        vehicleDao.updateVehicle(toRestore)
    }

    suspend fun getUserProfile(): UserProfile? = userProfileDao.getUserProfile()

    suspend fun saveUserProfile(userProfile: UserProfile) {
        val status = if (userProfile.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = userProfile.copy(
            remoteId = if (userProfile.remoteId.isNotBlank()) userProfile.remoteId else UUID.randomUUID().toString(),
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        userProfileDao.insertOrUpdateUserProfile(toSave)
    }

    suspend fun getUserGoals(): UserGoals? = userGoalsDao.getUserGoals()

    suspend fun updateUserGoals(goals: UserGoals) {
        val existing = userGoalsDao.getUserGoals()
        val remoteId = when {
            goals.remoteId.isNotBlank() -> goals.remoteId
            existing?.remoteId?.isNotBlank() == true -> existing.remoteId
            else -> UUID.randomUUID().toString()
        }
        val toSave = goals.copy(
            id = 1,
            remoteId = remoteId,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        userGoalsDao.insertOrUpdateUserGoals(toSave)
    }

    suspend fun insertInitialUserGoals(goals: UserGoals) {
        userGoalsDao.insertOrUpdateUserGoals(goals.copy(id = 1))
    }

    suspend fun migrateLegacyGoalsIfNeeded(context: Context) {
        LegacyGoalsMigrator.migrate(db, context, ioDispatcher)
    }

    suspend fun getPlatformById(id: Long): Platform? = platformDao.getPlatformById(id)

    suspend fun savePlatform(platform: Platform): Long {
        val isNew = platform.id == 0L
        val status = if (isNew || platform.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = platform.copy(
            remoteId = if (platform.remoteId.isNotBlank()) platform.remoteId else UUID.randomUUID().toString(),
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        return platformDao.insertOrUpdatePlatform(toSave)
    }

    suspend fun deletePlatform(platform: Platform) {
        val toDelete = platform.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = System.currentTimeMillis()
        )
        platformDao.updatePlatform(toDelete)
    }

    suspend fun savePart(part: VehiclePart) {
        val isNew = part.id == 0L
        val activeVehicleId = if (part.vehicleId == 0L) {
            vehicleDao.getActiveVehicle()?.id ?: 0L
        } else {
            part.vehicleId
        }
        val status = if (isNew || part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = part.copy(
            vehicleId = activeVehicleId,
            remoteId = if (part.remoteId.isNotBlank()) part.remoteId else UUID.randomUUID().toString(),
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        partDao.insertPart(toSave)
    }

    suspend fun addPart(part: VehiclePart) {
        savePart(part)
    }

    suspend fun updatePart(part: VehiclePart) {
        val activeVehicleId = if (part.vehicleId == 0L) {
            partDao.getPartById(part.id)?.vehicleId?.takeIf { it != 0L }
                ?: vehicleDao.getActiveVehicle()?.id
                ?: 0L
        } else {
            part.vehicleId
        }
        val status = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = part.copy(
            vehicleId = activeVehicleId,
            remoteId = if (part.remoteId.isNotBlank()) part.remoteId else UUID.randomUUID().toString(),
            syncStatus = status,
            updatedAt = System.currentTimeMillis()
        )
        partDao.updatePart(toSave)
    }

    suspend fun deletePart(part: VehiclePart) {
        val toDelete = part.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = System.currentTimeMillis()
        )
        partDao.updatePart(toDelete)
    }

    suspend fun restorePart(part: VehiclePart) {
        val toRestore = part.copy(
            isDeleted = false,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = System.currentTimeMillis()
        )
        partDao.updatePart(toRestore)
    }

    suspend fun resetPartWear(partId: Long) {
        val part = partDao.getPartById(partId)
        if (part != null) {
            val status = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
            partDao.updatePart(
                part.copy(
                    runKmSinceChange = 0.0,
                    syncStatus = status,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            partDao.resetPartWear(partId)
        }
    }

    private suspend fun recalculateVehicleOdometer() {
        val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()
        val mostRecentRecord = recordDao.getMostRecentRecord()
        if (mostRecentRecord != null) {
            val vStatus = if (vehicle.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
            val updatedVehicle = vehicle.copy(
                currentOdometer = mostRecentRecord.endOdometer,
                syncStatus = vStatus,
                updatedAt = System.currentTimeMillis()
            )
            vehicleDao.updateVehicle(updatedVehicle)
        }
    }

    suspend fun deleteRecord(record: DailyRecord) {
        db.withTransaction {
            val oldKmRodados = (record.endOdometer - record.startOdometer).coerceAtLeast(0.0)

            // Reverte desgaste de peças do veículo ativo
            val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()
            val parts = partDao.getPartsForVehicle(vehicle.id)
            for (part in parts) {
                val newRunKm = (part.runKmSinceChange - oldKmRodados).coerceAtLeast(0.0)
                val pStatus = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
                partDao.updatePart(
                    part.copy(
                        runKmSinceChange = newRunKm,
                        syncStatus = pStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Soft-delete do registro com marcação pendente para sincronização
            val toDelete = record.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = System.currentTimeMillis()
            )
            recordDao.updateRecord(toDelete)

            // Recalcula hodômetro do veículo baseado no mais recente após a exclusão
            recalculateVehicleOdometer()
        }
    }

    suspend fun insertDailyRecord(
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Long,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Long,
        foodExpense: Long,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = ""
    ) {
        db.withTransaction {
            val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()
            val kmRodados = (endOdometer - startOdometer).coerceAtLeast(0.0)

            // 1. Obter peças para calcular o custo de desgaste acumulado do veículo ativo
            val parts = partDao.getPartsForVehicle(vehicle.id)
            val totalWearCostPerKm = parts.sumOf { it.wearCostPerKm }
            val wearCost = CurrencyUtils.toCents(kmRodados * totalWearCostPerKm)

            // 2. Calcular custos restantes
            val realAvgConsumption = calculateRealAverageConsumption(vehicle.id)
            val effectiveConsumption = if (realAvgConsumption != null && realAvgConsumption > 0) {
                realAvgConsumption
            } else {
                vehicle.averageConsumption
            }
            val fuelCost = if (effectiveConsumption > 0) {
                CurrencyUtils.computeFuelTotalCents(kmRodados / effectiveConsumption, fuelPrice, 0L)
            } else {
                0L
            }
            val proportionalFixedCost = if (vehicle.plannedWorkDays > 0) {
                CurrencyUtils.divideByInt(vehicle.monthlyFixedCosts, vehicle.plannedWorkDays)
            } else {
                0L
            }
            val netProfit = grossEarnings - fuelCost - wearCost - proportionalFixedCost - foodExpense

            val record = DailyRecord(
                remoteId = UUID.randomUUID().toString(),
                syncStatus = SyncStatus.PENDING_INSERT,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                dateString = dateString,
                dateTimestamp = dateTimestamp,
                platform = platform,
                grossEarnings = grossEarnings,
                deliveriesCount = deliveriesCount,
                startOdometer = startOdometer,
                endOdometer = endOdometer,
                fuelPrice = fuelPrice,
                foodExpense = foodExpense,
                fuelCost = fuelCost,
                wearCost = wearCost,
                proportionalFixedCost = proportionalFixedCost,
                netProfit = netProfit,
                startTime = startTime,
                endTime = endTime,
                pauseDuration = pauseDuration
            )

            // 3. Salvar registro
            recordDao.insertRecord(record)

            // 4. Recalcula hodômetro do veículo baseado no mais recente após a inserção
            recalculateVehicleOdometer()

            // 5. Adicionar quilometragem rodada para todas as peças de desgaste
            for (part in parts) {
                val newRunKm = part.runKmSinceChange + kmRodados
                val pStatus = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
                partDao.updatePart(
                    part.copy(
                        runKmSinceChange = newRunKm,
                        syncStatus = pStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun insertDailyRecord(
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Double,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Double,
        foodExpense: Double,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = ""
    ) = insertDailyRecord(
        dateString = dateString,
        dateTimestamp = dateTimestamp,
        platform = platform,
        grossEarnings = CurrencyUtils.toCents(grossEarnings),
        deliveriesCount = deliveriesCount,
        startOdometer = startOdometer,
        endOdometer = endOdometer,
        fuelPrice = CurrencyUtils.toFuelPriceMillis(fuelPrice),
        foodExpense = CurrencyUtils.toCents(foodExpense),
        startTime = startTime,
        endTime = endTime,
        pauseDuration = pauseDuration
    )

    suspend fun updateDailyRecord(
        recordId: Long,
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Long,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Long,
        foodExpense: Long,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = ""
    ) {
        db.withTransaction {
            val oldRecord = recordDao.getRecordById(recordId)
            val oldKmRodados = oldRecord?.let { (it.endOdometer - it.startOdometer).coerceAtLeast(0.0) } ?: 0.0
            val kmRodados = (endOdometer - startOdometer).coerceAtLeast(0.0)
            val diffKm = kmRodados - oldKmRodados

            val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()

            // 1. Obter peças para calcular o custo de desgaste acumulado do veículo ativo
            val parts = partDao.getPartsForVehicle(vehicle.id)
            val totalWearCostPerKm = parts.sumOf { it.wearCostPerKm }
            val wearCost = CurrencyUtils.toCents(kmRodados * totalWearCostPerKm)

            // 2. Calcular custos restantes
            val realAvgConsumption = calculateRealAverageConsumption(vehicle.id)
            val effectiveConsumption = if (realAvgConsumption != null && realAvgConsumption > 0) {
                realAvgConsumption
            } else {
                vehicle.averageConsumption
            }
            val fuelCost = if (effectiveConsumption > 0) {
                CurrencyUtils.computeFuelTotalCents(kmRodados / effectiveConsumption, fuelPrice, 0L)
            } else {
                0L
            }
            val proportionalFixedCost = if (vehicle.plannedWorkDays > 0) {
                CurrencyUtils.divideByInt(vehicle.monthlyFixedCosts, vehicle.plannedWorkDays)
            } else {
                0L
            }
            val netProfit = grossEarnings - fuelCost - wearCost - proportionalFixedCost - foodExpense

            val rStatus = if (oldRecord?.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
            val remId = oldRecord?.remoteId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()

            val record = DailyRecord(
                id = recordId,
                remoteId = remId,
                syncStatus = rStatus,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                dateString = dateString,
                dateTimestamp = dateTimestamp,
                platform = platform,
                grossEarnings = grossEarnings,
                deliveriesCount = deliveriesCount,
                startOdometer = startOdometer,
                endOdometer = endOdometer,
                fuelPrice = fuelPrice,
                foodExpense = foodExpense,
                fuelCost = fuelCost,
                wearCost = wearCost,
                proportionalFixedCost = proportionalFixedCost,
                netProfit = netProfit,
                startTime = startTime,
                endTime = endTime,
                pauseDuration = pauseDuration
            )

            // Salvar registro atualizado substituindo o anterior
            recordDao.insertRecord(record)

            // Recalcula hodômetro do veículo baseado no mais recente após a atualização
            recalculateVehicleOdometer()

            // Atualizar quilometragem rodada para todas as peças de desgaste considerando a diferença
            for (part in parts) {
                val newRunKm = (part.runKmSinceChange + diffKm).coerceAtLeast(0.0)
                val pStatus = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
                partDao.updatePart(
                    part.copy(
                        runKmSinceChange = newRunKm,
                        syncStatus = pStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun updateDailyRecord(
        recordId: Long,
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Double,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Double,
        foodExpense: Double,
        startTime: String = "",
        endTime: String = "",
        pauseDuration: String = ""
    ) = updateDailyRecord(
        recordId = recordId,
        dateString = dateString,
        dateTimestamp = dateTimestamp,
        platform = platform,
        grossEarnings = CurrencyUtils.toCents(grossEarnings),
        deliveriesCount = deliveriesCount,
        startOdometer = startOdometer,
        endOdometer = endOdometer,
        fuelPrice = CurrencyUtils.toFuelPriceMillis(fuelPrice),
        foodExpense = CurrencyUtils.toCents(foodExpense),
        startTime = startTime,
        endTime = endTime,
        pauseDuration = pauseDuration
    )

    suspend fun checkAndSeedData() {
        // Banco de dados deve permanecer limpo quando vazio.
        // Nenhuma semeadura automática de dados fictícios ou registros diários.
    }

    suspend fun clearAllData() {
        withContext(ioDispatcher) {
            db.clearAllTables()
        }
    }

    suspend fun calculateRealAverageConsumption(vehicleId: Long): Double? =
        FuelCalculationsHelper.calculateRealAverageConsumption(fuelRefillDao, vehicleId)

    suspend fun calculateLatestRealConsumption(vehicleId: Long): Double? =
        FuelCalculationsHelper.calculateLatestRealConsumption(fuelRefillDao, vehicleId)

    suspend fun getLatestFuelRefill(vehicleId: Long): FuelRefill? =
        fuelRefillDao.getLatestRefillForVehicle(vehicleId)

    fun getLatestRefillForVehicleFlow(vehicleId: Long): Flow<FuelRefill?> =
        fuelRefillDao.getLatestRefillForVehicleFlow(vehicleId)

    suspend fun saveFuelRefill(refill: FuelRefill) {
        val isNew = refill.id == 0L
        val remId = if (refill.remoteId.isNotBlank()) refill.remoteId else UUID.randomUUID().toString()
        val status = if (isNew || refill.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = refill.copy(
            remoteId = remId,
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        if (isNew) {
            fuelRefillDao.insertRefill(toSave)
        } else {
            fuelRefillDao.updateRefill(toSave)
        }
    }

    suspend fun deleteFuelRefill(refill: FuelRefill) {
        val toDelete = refill.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = System.currentTimeMillis()
        )
        fuelRefillDao.updateRefill(toDelete)
    }

    suspend fun averageFuelPrice(vehicleId: Long): Double =
        FuelCalculationsHelper.averageFuelPrice(fuelRefillDao, vehicleId)

    suspend fun averageLitersPerRefill(vehicleId: Long): Double =
        FuelCalculationsHelper.averageLitersPerRefill(fuelRefillDao, vehicleId)

    fun maintenanceRecordsForVehicleFlow(vehicleId: Long): Flow<List<MaintenanceRecord>> =
        maintenanceRecordDao.getRecordsForVehicleFlow(vehicleId)

    suspend fun saveMaintenanceRecord(record: MaintenanceRecord) {
        val isNew = record.id == 0L
        val remId = if (record.remoteId.isNotBlank()) record.remoteId else UUID.randomUUID().toString()
        val status = if (isNew || record.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = record.copy(
            remoteId = remId,
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        if (isNew) {
            maintenanceRecordDao.insertRecord(toSave)
        } else {
            maintenanceRecordDao.updateRecord(toSave)
        }
    }

    suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        val toDelete = record.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = System.currentTimeMillis()
        )
        maintenanceRecordDao.updateRecord(toDelete)
    }

    suspend fun renameGasStation(oldName: String, newName: String) {
        fuelRefillDao.renameGasStation(oldName, newName)
    }

    suspend fun seedJune2026Data() {
        RepositoryDataSeeder.seedJune2026Data(db)
    }

    // =========================================================================
    // MÉTODOS DE SINCRONIZAÇÃO COM SUPABASE
    // =========================================================================

    suspend fun clearAllLocalData() = withContext(ioDispatcher) {
        db.clearAllTables()
    }

    suspend fun hasPendingSyncChanges(): Boolean =
        syncDelegate.hasPendingSyncChanges()

    suspend fun hasAnyLocalData(): Boolean =
        syncDelegate.hasAnyLocalData()

    suspend fun countPendingChanges(): Int =
        syncDelegate.countPendingChanges()

    suspend fun syncWithSupabase(userId: String, authToken: String): SyncResult =
        syncDelegate.syncWithSupabase(userId, authToken)

    suspend fun syncWithSupabase(userId: String): SyncResult =
        syncDelegate.syncWithSupabase(userId)

    suspend fun syncCurrentSession(): SyncResult =
        syncDelegate.syncCurrentSession()

    suspend fun exportAllUserDataAsJson(): String = withContext(ioDispatcher) {
        com.example.util.UserDataExportHelper.export(db)
    }
}
