package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.network.supabase.AuthSession
import com.example.network.supabase.SupabaseClient
import com.example.network.supabase.SupabaseSyncManager
import com.example.network.supabase.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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

    suspend fun migrateLegacyGoalsIfNeeded(context: Context) = withContext(ioDispatcher) {
        if (userGoalsDao.getUserGoals() != null) return@withContext
        val legacyPrefs = context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        val hasLegacyGoals = legacyPrefs.contains("daily_goal_gross")
        val goals = if (hasLegacyGoals) {
            UserGoals(
                id = 1,
                dailyGoalGross = legacyPrefs.getFloat("daily_goal_gross", 200.0f).toDouble(),
                dailyGoalNet = legacyPrefs.getFloat("daily_goal_net", 130.0f).toDouble(),
                dailyGoalKm = legacyPrefs.getFloat("daily_goal_km", 100.0f).toDouble(),
                dailyGoalDeliveries = legacyPrefs.getInt("daily_goal_deliveries", 15),
                weeklyGoalGross = legacyPrefs.getFloat("weekly_goal_gross", 1200.0f).toDouble(),
                weeklyGoalNet = legacyPrefs.getFloat("weekly_goal_net", 800.0f).toDouble(),
                weeklyGoalKm = legacyPrefs.getFloat("weekly_goal_km", 600.0f).toDouble(),
                weeklyGoalDeliveries = legacyPrefs.getInt("weekly_goal_deliveries", 90),
                monthlyGoalGross = legacyPrefs.getFloat("monthly_goal_gross", 5000.0f).toDouble(),
                monthlyGoalNet = legacyPrefs.getFloat("monthly_goal_net", 3200.0f).toDouble(),
                monthlyGoalKm = legacyPrefs.getFloat("monthly_goal_km", 2500.0f).toDouble(),
                monthlyGoalDeliveries = legacyPrefs.getInt("monthly_goal_deliveries", 400),
                syncStatus = SyncStatus.PENDING_UPDATE,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            UserGoals(
                id = 1,
                syncStatus = SyncStatus.SYNCED
            )
        }
        userGoalsDao.insertOrUpdateUserGoals(goals)

        if (legacyPrefs.contains("is_dark_theme")) {
            val isDark = legacyPrefs.getBoolean("is_dark_theme", true)
            context.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
                .edit().putBoolean("is_dark_theme", isDark).apply()
            context.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("is_dark_theme", isDark).apply()
        }
        legacyPrefs.edit().clear().apply()
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
        grossEarnings: Double,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Double,
        foodExpense: Double,
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
            val wearCost = kmRodados * totalWearCostPerKm

            // 2. Calcular custos restantes
            val realAvgConsumption = calculateRealAverageConsumption(vehicle.id)
            val effectiveConsumption = if (realAvgConsumption != null && realAvgConsumption > 0) {
                realAvgConsumption
            } else {
                vehicle.averageConsumption
            }
            val fuelCost = if (effectiveConsumption > 0) {
                (kmRodados / effectiveConsumption) * fuelPrice
            } else {
                0.0
            }
            val proportionalFixedCost = if (vehicle.plannedWorkDays > 0) {
                vehicle.monthlyFixedCosts / vehicle.plannedWorkDays
            } else {
                0.0
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
            val wearCost = kmRodados * totalWearCostPerKm

            // 2. Calcular custos restantes
            val realAvgConsumption = calculateRealAverageConsumption(vehicle.id)
            val effectiveConsumption = if (realAvgConsumption != null && realAvgConsumption > 0) {
                realAvgConsumption
            } else {
                vehicle.averageConsumption
            }
            val fuelCost = if (effectiveConsumption > 0) {
                (kmRodados / effectiveConsumption) * fuelPrice
            } else {
                0.0
            }
            val proportionalFixedCost = if (vehicle.plannedWorkDays > 0) {
                vehicle.monthlyFixedCosts / vehicle.plannedWorkDays
            } else {
                0.0
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

    suspend fun checkAndSeedData() {
        // Banco de dados deve permanecer limpo quando vazio.
        // Nenhuma semeadura automática de dados fictícios ou registros diários.
    }

    suspend fun clearAllData() {
        withContext(ioDispatcher) {
            db.clearAllTables()
        }
    }

    suspend fun calculateRealAverageConsumption(vehicleId: Long): Double? {
        val allRefills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        val fullTankIndices = allRefills.indices.filter { allRefills[it].isFullTank }
        if (fullTankIndices.size < 2) return null

        val segmentConsumptions = mutableListOf<Double>()
        for (i in 0 until fullTankIndices.size - 1) {
            val prevFullIdx = fullTankIndices[i]
            val currFullIdx = fullTankIndices[i + 1]

            val prevFullRefill = allRefills[prevFullIdx]
            val currFullRefill = allRefills[currFullIdx]

            val distance = currFullRefill.odometer - prevFullRefill.odometer
            if (distance <= 0) continue

            var litersSum = 0.0
            for (j in (prevFullIdx + 1)..currFullIdx) {
                litersSum += allRefills[j].liters
            }

            if (litersSum > 0) {
                val segmentConsumption = distance / litersSum
                segmentConsumptions.add(segmentConsumption)
            }
        }

        return if (segmentConsumptions.isEmpty()) null else segmentConsumptions.average()
    }

    suspend fun calculateLatestRealConsumption(vehicleId: Long): Double? {
        val allRefills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        val fullTankIndices = allRefills.indices.filter { allRefills[it].isFullTank }
        if (fullTankIndices.size < 2) return null

        val prevFullIdx = fullTankIndices[fullTankIndices.size - 2]
        val currFullIdx = fullTankIndices.last()

        val prevFullRefill = allRefills[prevFullIdx]
        val currFullRefill = allRefills[currFullIdx]

        val distance = currFullRefill.odometer - prevFullRefill.odometer
        if (distance <= 0) return null

        var litersSum = 0.0
        for (j in (prevFullIdx + 1)..currFullIdx) {
            litersSum += allRefills[j].liters
        }

        if (litersSum <= 0) return null
        return distance / litersSum
    }

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

    suspend fun averageFuelPrice(vehicleId: Long): Double {
        val refills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        if (refills.isEmpty()) return 0.0
        return refills.map { it.pricePerLiter }.average()
    }

    suspend fun averageLitersPerRefill(vehicleId: Long): Double {
        val refills = fuelRefillDao.getAllRefillsOrderedByOdometer(vehicleId)
        if (refills.isEmpty()) return 0.0
        return refills.map { it.liters }.average()
    }

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
        db.withTransaction {
            recordDao.deleteJune2026Records()

            var vehicle = vehicleDao.getActiveVehicle()
            if (vehicle == null) {
                vehicle = Vehicle(
                    id = 1L,
                    model = "Honda CG 160 Titan",
                    averageConsumption = 40.0,
                    fuelType = "Gasolina",
                    monthlyFixedCosts = 180.0,
                    plannedWorkDays = 22,
                    currentOdometer = 15350.0,
                    active = true,
                    remoteId = UUID.randomUUID().toString(),
                    syncStatus = SyncStatus.PENDING_INSERT,
                    updatedAt = System.currentTimeMillis()
                )
                val insertedId = vehicleDao.insertVehicle(vehicle)
                vehicle = vehicle.copy(id = insertedId)
            }

            var parts = partDao.getPartsForVehicle(vehicle.id)
            if (parts.isEmpty()) {
                val initialParts = listOf(
                    VehiclePart(vehicleId = vehicle.id, name = "Óleo do Motor", price = 45.0, lifespanKm = 1500.0, runKmSinceChange = 1300.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 4500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pneu Traseiro", price = 220.0, lifespanKm = 12000.0, runKmSinceChange = 11000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pneu Dianteiro", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 3000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pastilhas de Freio", price = 40.0, lifespanKm = 6000.0, runKmSinceChange = 1200.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Lona de Freio Traseira", price = 35.0, lifespanKm = 10000.0, runKmSinceChange = 2500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT)
                )
                for (part in initialParts) {
                    partDao.insertPart(part)
                }
                parts = partDao.getPartsForVehicle(vehicle.id)
            }

            val totalWearCostPerKm = parts.sumOf { it.price / it.lifespanKm }.takeIf { it > 0 } ?: 0.075
            val fixedCostPerDay = if (vehicle.plannedWorkDays > 0) vehicle.monthlyFixedCosts / vehicle.plannedWorkDays else (180.0 / 22.0)
            val consumption = if (vehicle.averageConsumption > 0) vehicle.averageConsumption else 40.0

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val platforms = listOf("iFood", "Uber Flash", "Rappi", "Loggi")

            val workDaysList = mutableListOf<Calendar>()
            for (dayOfMonth in 1..30) {
                if (workDaysList.size >= 25) break

                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, Calendar.JUNE)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    workDaysList.add(cal)
                }
            }

            val kmPerDay = List(workDaysList.size) { index ->
                70.0 + ((index + 1) * 7) % 45
            }
            val totalKm = kmPerDay.sum()
            var startOdo = 14580.0 - totalKm

            workDaysList.forEachIndexed { index, cal ->
                val km = kmPerDay[index]
                val endOdo = startOdo + km
                val dateStr = dateFormat.format(cal.time)
                val timestamp = cal.timeInMillis

                val gross = 160.0 + ((index + 1) * 13) % 110
                val deliveries = 10 + ((index + 1) * 3) % 12
                val food = 15.0 + ((index + 1) * 4) % 16
                val fuelPriceVal = 5.75 + ((index + 1) * 2) % 5 * 0.05

                val fuelCost = (km / consumption) * fuelPriceVal
                val wearCost = km * totalWearCostPerKm
                val net = gross - fuelCost - wearCost - fixedCostPerDay - food
                val selectedPlatform = platforms[index % platforms.size]

                val record = DailyRecord(
                    remoteId = UUID.randomUUID().toString(),
                    syncStatus = SyncStatus.PENDING_INSERT,
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = false,
                    dateString = dateStr,
                    dateTimestamp = timestamp,
                    platform = selectedPlatform,
                    grossEarnings = gross,
                    deliveriesCount = deliveries,
                    startOdometer = startOdo,
                    endOdometer = endOdo,
                    fuelPrice = fuelPriceVal,
                    foodExpense = food,
                    fuelCost = fuelCost,
                    wearCost = wearCost,
                    proportionalFixedCost = fixedCostPerDay,
                    netProfit = net,
                    startTime = "08:00",
                    endTime = "17:30",
                    pauseDuration = "01:00"
                )
                recordDao.insertRecord(record)

                for (part in parts) {
                    val updatedPart = part.copy(
                        runKmSinceChange = (part.runKmSinceChange + km).coerceAtMost(part.lifespanKm),
                        syncStatus = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE,
                        updatedAt = System.currentTimeMillis()
                    )
                    partDao.updatePart(updatedPart)
                }

                startOdo = endOdo
            }

            recalculateVehicleOdometer()
        }
    }

    // =========================================================================
    // MÉTODOS DE SINCRONIZAÇÃO COM SUPABASE
    // =========================================================================

    suspend fun clearAllLocalData() = withContext(ioDispatcher) {
        db.clearAllTables()
    }

    suspend fun hasPendingSyncChanges(): Boolean = withContext(ioDispatcher) {
        vehicleDao.getPendingVehicles().isNotEmpty() ||
        partDao.getPendingParts().isNotEmpty() ||
        recordDao.getPendingRecords().isNotEmpty() ||
        fuelRefillDao.getPendingRefills().isNotEmpty() ||
        maintenanceRecordDao.getPendingRecords().isNotEmpty() ||
        platformDao.getPendingPlatforms().isNotEmpty() ||
        userProfileDao.getPendingUserProfile() != null ||
        userGoalsDao.getPendingUserGoals() != null
    }

    suspend fun hasAnyLocalData(): Boolean = withContext(ioDispatcher) {
        vehicleDao.getAllVehicles().isNotEmpty() ||
        vehicleDao.getPendingVehicles().isNotEmpty() ||
        partDao.getAllParts().isNotEmpty() ||
        recordDao.getAllRecords().isNotEmpty() ||
        platformDao.getAllPlatforms().isNotEmpty() ||
        userProfileDao.getUserProfile() != null ||
        userGoalsDao.getUserGoals() != null
    }

    suspend fun countPendingChanges(): Int = withContext(ioDispatcher) {
        vehicleDao.getPendingVehicles().size +
        partDao.getPendingParts().size +
        recordDao.getPendingRecords().size +
        fuelRefillDao.getPendingRefills().size +
        maintenanceRecordDao.getPendingRecords().size +
        platformDao.getPendingPlatforms().size +
        (if (userProfileDao.getPendingUserProfile() != null) 1 else 0) +
        (if (userGoalsDao.getPendingUserGoals() != null) 1 else 0)
    }

    suspend fun syncWithSupabase(userId: String, authToken: String): SyncResult {
        val client = SupabaseClient.getInstance()
        if (authToken.isNotBlank()) {
            client.setAuthSession(
                AuthSession(
                    accessToken = authToken,
                    refreshToken = "",
                    userId = userId,
                    userEmail = null
                )
            )
        }
        val storage = dataOwnerStorage ?: return SyncResult.Error(IllegalStateException("DataOwnerStorage não configurado no repositório"))
        val syncManager = SupabaseSyncManager(db, client, storage, ioDispatcher)
        return syncManager.sync(userId)
    }

    suspend fun syncWithSupabase(userId: String): SyncResult {
        val client = SupabaseClient.getInstance()
        val storage = dataOwnerStorage ?: return SyncResult.Error(IllegalStateException("DataOwnerStorage não configurado no repositório"))
        val syncManager = SupabaseSyncManager(db, client, storage, ioDispatcher)
        return syncManager.sync(userId)
    }

    suspend fun syncCurrentSession(): SyncResult {
        val client = SupabaseClient.getInstance()
        val userId = client.currentUserId ?: return SyncResult.Error(IllegalStateException("Usuário não autenticado no Supabase"))
        return syncWithSupabase(userId)
    }
}
