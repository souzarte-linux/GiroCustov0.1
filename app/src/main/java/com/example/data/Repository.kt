package com.example.data

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class GiroCustoRepository(private val db: GiroCustoDatabase) {
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()

    val vehicleFlow: Flow<Vehicle?> = vehicleDao.getVehicleFlow()
    val allPartsFlow: Flow<List<VehiclePart>> = partDao.getAllPartsFlow()
    val allRecordsFlow: Flow<List<DailyRecord>> = recordDao.getAllRecordsFlow()

    suspend fun getVehicle(): Vehicle? = vehicleDao.getVehicle()
    suspend fun saveVehicle(vehicle: Vehicle) = vehicleDao.insertOrUpdateVehicle(vehicle)

    suspend fun savePart(part: VehiclePart) = partDao.insertPart(part)
    suspend fun updatePart(part: VehiclePart) = partDao.updatePart(part)
    suspend fun deletePart(part: VehiclePart) = partDao.deletePart(part)
    suspend fun resetPartWear(partId: Long) = partDao.resetPartWear(partId)

    suspend fun deleteRecord(record: DailyRecord) = recordDao.deleteRecordById(record.id)

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
            val vehicle = vehicleDao.getVehicle() ?: Vehicle()
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

            // 4. Atualizar hodômetro do veículo
            val updatedVehicle = vehicle.copy(currentOdometer = endOdometer)
            vehicleDao.insertOrUpdateVehicle(updatedVehicle)

            // 5. Adicionar quilometragem rodada para todas as peças de desgaste
            partDao.addKmToAllParts(kmRodados)
        }
    }

    // Atualiza um registro diário existente recalculando os custos operacionais
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
            val vehicle = vehicleDao.getVehicle() ?: Vehicle()
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
        }
    }

    // Método para sementes de dados ativas (Active Seed Data)
    suspend fun checkAndSeedData() {
        val existingVehicle = vehicleDao.getVehicle()
        if (existingVehicle == null) {
            Log.d("GiroCustoRepository", "Seeding initial data...")
            
            // 1. Inserir Veículo Padrão (Honda CG 160 Titan)
            val initialVehicle = Vehicle(
                id = 1,
                model = "Honda CG 160 Titan",
                averageConsumption = 40.0,
                fuelType = "Gasolina",
                monthlyFixedCosts = 180.0,
                plannedWorkDays = 22,
                currentOdometer = 15350.0 // O odômetro final do último dia de trabalho será 15.350
            )
            vehicleDao.insertOrUpdateVehicle(initialVehicle)

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
