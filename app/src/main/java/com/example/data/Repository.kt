package com.example.data

import android.util.Log
import androidx.room.withTransaction
import com.example.network.supabase.AuthSession
import com.example.network.supabase.SupabaseClient
import com.example.network.supabase.SupabaseSyncManager
import com.example.network.supabase.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

class GiroCustoRepository(private val db: GiroCustoDatabase) {
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()
    private val userProfileDao = db.userProfileDao()
    private val platformDao = db.platformDao()
    private val fuelRefillDao = db.fuelRefillDao()
    private val maintenanceRecordDao = db.maintenanceRecordDao()

    val vehicleFlow: Flow<Vehicle?> = vehicleDao.getActiveVehicleFlow()
    val allVehiclesFlow: Flow<List<Vehicle>> = vehicleDao.getAllVehiclesFlow()
    val allPartsFlow: Flow<List<VehiclePart>> = partDao.getAllPartsFlow()
    val allRecordsFlow: Flow<List<DailyRecord>> = recordDao.getAllRecordsFlow()
    val userProfileFlow: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val allPlatformsFlow: Flow<List<Platform>> = platformDao.getAllPlatformsFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val refillsForActiveVehicleFlow: Flow<List<FuelRefill>> = vehicleFlow.flatMapLatest { vehicle ->
        if (vehicle != null) {
            fuelRefillDao.getRefillsForVehicleFlow(vehicle.id)
        } else {
            flowOf(emptyList())
        }
    }

    suspend fun getVehicle(): Vehicle? = vehicleDao.getActiveVehicle()

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
        val status = if (isNew || part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = part.copy(
            remoteId = if (part.remoteId.isNotBlank()) part.remoteId else UUID.randomUUID().toString(),
            syncStatus = status,
            updatedAt = System.currentTimeMillis(),
            isDeleted = false
        )
        partDao.insertPart(toSave)
    }

    suspend fun updatePart(part: VehiclePart) {
        val status = if (part.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
        val toSave = part.copy(
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

            // Reverte desgaste de peças
            val parts = partDao.getAllParts()
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

            // 1. Obter peças para calcular o custo de desgaste acumulado
            val parts = partDao.getAllParts()
            val totalWearCostPerKm = parts.sumOf { it.wearCostPerKm }
            val wearCost = kmRodados * totalWearCostPerKm

            // 2. Calcular custos restantes
            val fuelCost = if (vehicle.averageConsumption > 0) {
                (kmRodados / vehicle.averageConsumption) * fuelPrice
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

            // 1. Obter peças para calcular o custo de desgaste acumulado
            val parts = partDao.getAllParts()
            val totalWearCostPerKm = parts.sumOf { it.wearCostPerKm }
            val wearCost = kmRodados * totalWearCostPerKm

            // 2. Calcular custos restantes
            val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()
            val fuelCost = if (vehicle.averageConsumption > 0) {
                (kmRodados / vehicle.averageConsumption) * fuelPrice
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
        val existingVehicle = vehicleDao.getActiveVehicle()
        if (existingVehicle == null) {
            Log.d("GiroCustoRepository", "Seeding initial data...")
            
            val initialVehicle = Vehicle(
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
            vehicleDao.insertVehicle(initialVehicle)

            val initialParts = listOf(
                VehiclePart(name = "Óleo do Motor", price = 45.0, lifespanKm = 1500.0, runKmSinceChange = 1300.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                VehiclePart(name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 4500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                VehiclePart(name = "Pneu Traseiro", price = 220.0, lifespanKm = 12000.0, runKmSinceChange = 11000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                VehiclePart(name = "Pneu Dianteiro", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 3000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                VehiclePart(name = "Pastilhas de Freio", price = 40.0, lifespanKm = 6000.0, runKmSinceChange = 1200.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                VehiclePart(name = "Lona de Freio Traseira", price = 35.0, lifespanKm = 10000.0, runKmSinceChange = 2500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT)
            )
            for (part in initialParts) {
                partDao.insertPart(part)
            }

            val history = listOf(
                SeededDay(offset = 0, gross = 220.0, deliveries = 18, startOdo = 15240.0, endOdo = 15350.0, food = 25.0, fuelPrice = 5.80),
                SeededDay(offset = -1, gross = 240.0, deliveries = 20, startOdo = 15110.0, endOdo = 15240.0, food = 20.0, fuelPrice = 5.80),
                SeededDay(offset = -2, gross = 190.0, deliveries = 15, startOdo = 15015.0, endOdo = 15110.0, food = 22.0, fuelPrice = 5.80),
                SeededDay(offset = -3, gross = 260.0, deliveries = 22, startOdo = 14880.0, endOdo = 15015.0, food = 28.0, fuelPrice = 5.85),
                SeededDay(offset = -4, gross = 180.0, deliveries = 14, startOdo = 14795.0, endOdo = 14880.0, food = 15.0, fuelPrice = 5.85),
                SeededDay(offset = -5, gross = 210.0, deliveries = 17, startOdo = 14685.0, endOdo = 14795.0, food = 20.0, fuelPrice = 5.80),
                SeededDay(offset = -6, gross = 205.0, deliveries = 16, startOdo = 14580.0, endOdo = 14685.0, food = 18.0, fuelPrice = 5.80)
            )

            val totalWearCostPerKm = initialParts.sumOf { it.price / it.lifespanKm }
            val fixedCostPerDay = 180.0 / 22.0
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val platforms = listOf("iFood", "Uber Flash", "Rappi", "iFood", "Uber Flash", "Loggi", "iFood")
            var idx = 0
            for (day in history) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, day.offset)
                val dateStr = dateFormat.format(cal.time)
                val timestamp = cal.timeInMillis

                val km = day.endOdo - day.startOdo
                val fuelCost = (km / 40.0) * day.fuelPrice
                val wearCost = km * totalWearCostPerKm
                val net = day.gross - fuelCost - wearCost - fixedCostPerDay - day.food
                val selectedPlatform = platforms.getOrElse(idx % platforms.size) { "iFood" }
                idx++

                val record = DailyRecord(
                    remoteId = UUID.randomUUID().toString(),
                    syncStatus = SyncStatus.PENDING_INSERT,
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = false,
                    dateString = dateStr,
                    dateTimestamp = timestamp,
                    platform = selectedPlatform,
                    grossEarnings = day.gross,
                    deliveriesCount = day.deliveries,
                    startOdometer = day.startOdo,
                    endOdometer = day.endOdo,
                    fuelPrice = day.fuelPrice,
                    foodExpense = day.food,
                    fuelCost = fuelCost,
                    wearCost = wearCost,
                    proportionalFixedCost = fixedCostPerDay,
                    netProfit = net,
                    startTime = "08:00",
                    endTime = "17:30",
                    pauseDuration = "01:00"
                )
                recordDao.insertRecord(record)
            }
        }

        val allRecords = recordDao.getAllRecords()
        val hasJune2026 = allRecords.any { it.dateString.startsWith("2026-06") }
        if (!hasJune2026) {
            Log.d("GiroCustoRepository", "Seeding June 2026 data programmatically...")
            seedJune2026Data()
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

    suspend fun saveFuelRefill(refill: FuelRefill) {
        db.withTransaction {
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
            val realConsumption = calculateRealAverageConsumption(refill.vehicleId)
            if (realConsumption != null) {
                val vehicle = vehicleDao.getVehicleById(refill.vehicleId)
                if (vehicle != null) {
                    val vStatus = if (vehicle.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
                    vehicleDao.updateVehicle(
                        vehicle.copy(
                            averageConsumption = realConsumption,
                            syncStatus = vStatus,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    suspend fun deleteFuelRefill(refill: FuelRefill) {
        db.withTransaction {
            val toDelete = refill.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = System.currentTimeMillis()
            )
            fuelRefillDao.updateRefill(toDelete)
            val realConsumption = calculateRealAverageConsumption(refill.vehicleId)
            if (realConsumption != null) {
                val vehicle = vehicleDao.getVehicleById(refill.vehicleId)
                if (vehicle != null) {
                    val vStatus = if (vehicle.syncStatus == SyncStatus.PENDING_INSERT) SyncStatus.PENDING_INSERT else SyncStatus.PENDING_UPDATE
                    vehicleDao.updateVehicle(
                        vehicle.copy(
                            averageConsumption = realConsumption,
                            syncStatus = vStatus,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
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
                vehicleDao.insertVehicle(vehicle)
            }

            var parts = partDao.getAllParts()
            if (parts.isEmpty()) {
                val initialParts = listOf(
                    VehiclePart(name = "Óleo do Motor", price = 45.0, lifespanKm = 1500.0, runKmSinceChange = 1300.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 4500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(name = "Pneu Traseiro", price = 220.0, lifespanKm = 12000.0, runKmSinceChange = 11000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(name = "Pneu Dianteiro", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 3000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(name = "Pastilhas de Freio", price = 40.0, lifespanKm = 6000.0, runKmSinceChange = 1200.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(name = "Lona de Freio Traseira", price = 35.0, lifespanKm = 10000.0, runKmSinceChange = 2500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT)
                )
                for (part in initialParts) {
                    partDao.insertPart(part)
                }
                parts = partDao.getAllParts()
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
        val syncManager = SupabaseSyncManager(db, client)
        return syncManager.sync(userId)
    }

    suspend fun syncWithSupabase(userId: String): SyncResult {
        val client = SupabaseClient.getInstance()
        val syncManager = SupabaseSyncManager(db, client)
        return syncManager.sync(userId)
    }

    suspend fun syncCurrentSession(): SyncResult {
        val client = SupabaseClient.getInstance()
        val userId = client.currentUserId ?: return SyncResult.Error(IllegalStateException("Usuário não autenticado no Supabase"))
        return syncWithSupabase(userId)
    }

    private data class SeededDay(
        val offset: Int,
        val gross: Double,
        val deliveries: Int,
        val startOdo: Double,
        val endOdo: Double,
        val food: Double,
        val fuelPrice: Double
    )
}
