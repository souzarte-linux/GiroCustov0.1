package com.example.data

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
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
            vehicleDao.updateVehicle(vehicle)
        }
    }

    suspend fun addVehicle(vehicle: Vehicle): Long {
        val activeVehicle = vehicleDao.getActiveVehicle()
        val toInsert = if (activeVehicle == null) {
            vehicle.copy(active = true)
        } else {
            vehicle
        }
        return vehicleDao.insertVehicle(toInsert)
    }

    suspend fun setActiveVehicle(vehicleId: Long) {
        vehicleDao.setActiveVehicle(vehicleId)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) {
        vehicleDao.deleteVehicle(vehicle)
    }

    suspend fun getUserProfile(): UserProfile? = userProfileDao.getUserProfile()
    suspend fun saveUserProfile(userProfile: UserProfile) = userProfileDao.insertOrUpdateUserProfile(userProfile)

    suspend fun getPlatformById(id: Long): Platform? = platformDao.getPlatformById(id)
    suspend fun savePlatform(platform: Platform): Long = platformDao.insertOrUpdatePlatform(platform)
    suspend fun deletePlatform(platform: Platform) = platformDao.deletePlatform(platform)

    suspend fun savePart(part: VehiclePart) = partDao.insertPart(part)
    suspend fun updatePart(part: VehiclePart) = partDao.updatePart(part)
    suspend fun deletePart(part: VehiclePart) = partDao.deletePart(part)
    suspend fun resetPartWear(partId: Long) = partDao.resetPartWear(partId)

    private suspend fun recalculateVehicleOdometer() {
        val vehicle = vehicleDao.getActiveVehicle() ?: Vehicle()
        val mostRecentRecord = recordDao.getMostRecentRecord()
        if (mostRecentRecord != null) {
            val updatedVehicle = vehicle.copy(currentOdometer = mostRecentRecord.endOdometer)
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
                partDao.updatePart(part.copy(runKmSinceChange = newRunKm))
            }

            // Exclui o registro
            recordDao.deleteRecordById(record.id)

            // Recalcula hodômetro do veículo baseado no mais recente após a exclusão
            recalculateVehicleOdometer()
        }
    }

    // Insere registro diário e atualiza o hodômetro do veículo e o desgaste das peças em uma transação
    suspend fun insertDailyRecord(
        dateString: String,
        dateTimestamp: Long,
        platform: String,
        grossEarnings: Double,
        deliveriesCount: Int,
        startOdometer: Double,
        endOdometer: Double,
        fuelPrice: Double,
        foodExpense: Double
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
                netProfit = netProfit
            )

            // 3. Salvar registro
            recordDao.insertRecord(record)

            // 4. Recalcula hodômetro do veículo baseado no mais recente após a inserção
            recalculateVehicleOdometer()

            // 5. Adicionar quilometragem rodada para todas as peças de desgaste
            partDao.addKmToAllParts(kmRodados)
        }
    }

    // Atualiza um registro diário existente recalculando os custos operacionais e ajustando odômetro e peças de desgaste
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
        foodExpense: Double
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

            val record = DailyRecord(
                id = recordId,
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
                netProfit = netProfit
            )

            // Salvar registro atualizado substituindo o anterior
            recordDao.insertRecord(record)

            // Recalcula hodômetro do veículo baseado no mais recente após a atualização
            recalculateVehicleOdometer()

            // Atualizar quilometragem rodada para todas as peças de desgaste considerando a diferença
            for (part in parts) {
                val newRunKm = (part.runKmSinceChange + diffKm).coerceAtLeast(0.0)
                partDao.updatePart(part.copy(runKmSinceChange = newRunKm))
            }
        }
    }

    // Método para sementes de dados ativas (Active Seed Data)
    suspend fun checkAndSeedData() {
        val existingVehicle = vehicleDao.getActiveVehicle()
        if (existingVehicle == null) {
            Log.d("GiroCustoRepository", "Seeding initial data...")
            
            // 1. Inserir Veículo Padrão (Honda CG 160 Titan)
            val initialVehicle = Vehicle(
                id = 1L,
                model = "Honda CG 160 Titan",
                averageConsumption = 40.0,
                fuelType = "Gasolina",
                monthlyFixedCosts = 180.0,
                plannedWorkDays = 22,
                currentOdometer = 15350.0, // O odômetro final do último dia de trabalho será 15.350
                active = true
            )
            vehicleDao.insertVehicle(initialVehicle)

            // 2. Inserir 6 Peças de Desgaste Monitoradas com uso parcial pré-carregado
            val initialParts = listOf(
                VehiclePart(name = "Óleo do Motor", price = 45.0, lifespanKm = 1500.0, runKmSinceChange = 1300.0), // 86% de desgaste (Banner de Atenção)
                VehiclePart(name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 4500.0), // 30%
                VehiclePart(name = "Pneu Traseiro", price = 220.0, lifespanKm = 12000.0, runKmSinceChange = 11000.0), // 91% de desgaste (Banner Crítico)
                VehiclePart(name = "Pneu Dianteiro", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 3000.0), // 20%
                VehiclePart(name = "Pastilhas de Freio", price = 40.0, lifespanKm = 6000.0, runKmSinceChange = 1200.0), // 20%
                VehiclePart(name = "Lona de Freio Traseira", price = 35.0, lifespanKm = 10000.0, runKmSinceChange = 2500.0) // 25%
            )
            for (part in initialParts) {
                partDao.insertPart(part)
            }

            // 3. Inserir Histórico de Entregas dos Últimos 7 Dias (Retroativo)
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Vamos inserir registros diários realistas do dia 6 para trás (July 5th, 2026 backwards)
            // Dados calculados para que o hodômetro termine em 15.350 km.
            val history = listOf(
                // Dia 7 (Hoje): julho 5
                SeededDay(offset = 0, gross = 220.0, deliveries = 18, startOdo = 15240.0, endOdo = 15350.0, food = 25.0, fuelPrice = 5.80),
                // Dia 6 (Ontem): julho 4
                SeededDay(offset = -1, gross = 240.0, deliveries = 20, startOdo = 15110.0, endOdo = 15240.0, food = 20.0, fuelPrice = 5.80),
                // Dia 5: julho 3
                SeededDay(offset = -2, gross = 190.0, deliveries = 15, startOdo = 15015.0, endOdo = 15110.0, food = 22.0, fuelPrice = 5.80),
                // Dia 4: julho 2
                SeededDay(offset = -3, gross = 260.0, deliveries = 22, startOdo = 14880.0, endOdo = 15015.0, food = 28.0, fuelPrice = 5.85),
                // Dia 3: julho 1
                SeededDay(offset = -4, gross = 180.0, deliveries = 14, startOdo = 14795.0, endOdo = 14880.0, food = 15.0, fuelPrice = 5.85),
                // Dia 2: junho 30
                SeededDay(offset = -5, gross = 210.0, deliveries = 17, startOdo = 14685.0, endOdo = 14795.0, food = 20.0, fuelPrice = 5.80),
                // Dia 1: junho 29
                SeededDay(offset = -6, gross = 205.0, deliveries = 16, startOdo = 14580.0, endOdo = 14685.0, food = 18.0, fuelPrice = 5.80)
            )

            val totalWearCostPerKm = initialParts.sumOf { it.price / it.lifespanKm } // Custo fixado para os históricos
            val fixedCostPerDay = 180.0 / 22.0

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
                    netProfit = net
                )
                recordDao.insertRecord(record)
            }
        }

        // Programmatically seed June 2026 data if not already present
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

            // Soma de litros de todos os abastecimentos (parciais inclusive) ocorridos após o prevFullRefill até o currFullRefill
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
            if (refill.id == 0L) {
                fuelRefillDao.insertRefill(refill)
            } else {
                fuelRefillDao.updateRefill(refill)
            }
            val realConsumption = calculateRealAverageConsumption(refill.vehicleId)
            if (realConsumption != null) {
                val vehicle = vehicleDao.getVehicleById(refill.vehicleId)
                if (vehicle != null) {
                    vehicleDao.updateVehicle(vehicle.copy(averageConsumption = realConsumption))
                }
            }
        }
    }

    suspend fun deleteFuelRefill(refill: FuelRefill) {
        db.withTransaction {
            fuelRefillDao.deleteRefill(refill)
            val realConsumption = calculateRealAverageConsumption(refill.vehicleId)
            if (realConsumption != null) {
                val vehicle = vehicleDao.getVehicleById(refill.vehicleId)
                if (vehicle != null) {
                    vehicleDao.updateVehicle(vehicle.copy(averageConsumption = realConsumption))
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
        if (record.id == 0L) {
            maintenanceRecordDao.insertRecord(record)
        } else {
            maintenanceRecordDao.updateRecord(record)
        }
    }

    suspend fun deleteMaintenanceRecord(record: MaintenanceRecord) {
        maintenanceRecordDao.deleteRecord(record)
    }

    suspend fun renameGasStation(oldName: String, newName: String) {
        fuelRefillDao.renameGasStation(oldName, newName)
    }

    suspend fun seedJune2026Data() {
        db.withTransaction {
            // 1. Delete existing June 2026 records to prevent duplication
            recordDao.deleteJune2026Records()

            // 2. Ensure an active vehicle exists
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
                    active = true
                )
                vehicleDao.insertVehicle(vehicle)
            }

            // 3. Ensure parts exist to compute wear costs
            var parts = partDao.getAllParts()
            if (parts.isEmpty()) {
                val initialParts = listOf(
                    VehiclePart(name = "Óleo do Motor", price = 45.0, lifespanKm = 1500.0, runKmSinceChange = 1300.0),
                    VehiclePart(name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 4500.0),
                    VehiclePart(name = "Pneu Traseiro", price = 220.0, lifespanKm = 12000.0, runKmSinceChange = 11000.0),
                    VehiclePart(name = "Pneu Dianteiro", price = 180.0, lifespanKm = 15000.0, runKmSinceChange = 3000.0),
                    VehiclePart(name = "Pastilhas de Freio", price = 40.0, lifespanKm = 6000.0, runKmSinceChange = 1200.0),
                    VehiclePart(name = "Lona de Freio Traseira", price = 35.0, lifespanKm = 10000.0, runKmSinceChange = 2500.0)
                )
                for (part in initialParts) {
                    partDao.insertPart(part)
                }
                parts = partDao.getAllParts()
            }

            val totalWearCostPerKm = parts.sumOf { it.price / it.lifespanKm }.takeIf { it > 0 } ?: 0.075
            val fixedCostPerDay = if (vehicle.plannedWorkDays > 0) vehicle.monthlyFixedCosts / vehicle.plannedWorkDays else (180.0 / 22.0)
            val consumption = if (vehicle.averageConsumption > 0) vehicle.averageConsumption else 40.0

            // 4. Generate the 25 working days list
            val calendar = Calendar.getInstance()
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

            // Let's pre-calculate total km so we end exactly at 14580.0 (the base end/start odometer for June 29)
            // or close to it.
            val kmPerDay = List(workDaysList.size) { index ->
                70.0 + ((index + 1) * 7) % 45 // 70 to 110 km
            }
            val totalKm = kmPerDay.sum()
            var startOdo = 14580.0 - totalKm

            workDaysList.forEachIndexed { index, cal ->
                val km = kmPerDay[index]
                val endOdo = startOdo + km
                val dateStr = dateFormat.format(cal.time)
                val timestamp = cal.timeInMillis

                val gross = 160.0 + ((index + 1) * 13) % 110 // R$ 160.0 to R$ 270.0
                val deliveries = 10 + ((index + 1) * 3) % 12 // 10 to 22 deliveries
                val food = 15.0 + ((index + 1) * 4) % 16 // R$ 15.0 to R$ 31.0
                val fuelPriceVal = 5.75 + ((index + 1) * 2) % 5 * 0.05 // 5.75 to 5.95

                val fuelCost = (km / consumption) * fuelPriceVal
                val wearCost = km * totalWearCostPerKm
                val net = gross - fuelCost - wearCost - fixedCostPerDay - food
                val selectedPlatform = platforms[index % platforms.size]

                val record = DailyRecord(
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
                    netProfit = net
                )
                recordDao.insertRecord(record)

                // Accumulate km on parts
                for (part in parts) {
                    val updatedPart = part.copy(runKmSinceChange = (part.runKmSinceChange + km).coerceAtMost(part.lifespanKm))
                    partDao.updatePart(updatedPart)
                }

                // Setup next day's start odo
                startOdo = endOdo
            }

            recalculateVehicleOdometer()
        }
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
