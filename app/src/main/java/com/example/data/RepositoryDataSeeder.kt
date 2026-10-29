package com.example.data

import androidx.room.withTransaction
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

object RepositoryDataSeeder {

    suspend fun seedJune2026Data(db: GiroCustoDatabase) {
        val vehicleDao = db.vehicleDao()
        val partDao = db.vehiclePartDao()
        val recordDao = db.dailyRecordDao()

        db.withTransaction {
            recordDao.deleteJune2026Records()

            var vehicle = vehicleDao.getActiveVehicle()
            if (vehicle == null) {
                vehicle = Vehicle(
                    id = 1L,
                    model = "Honda CG 160 Titan",
                    averageConsumption = 40.0,
                    fuelType = "Gasolina",
                    monthlyFixedCosts = 18000L,
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
                    VehiclePart(vehicleId = vehicle.id, name = "Óleo do Motor", price = 4500L, lifespanKm = 1500.0, runKmSinceChange = 1300.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Kit Relação (Coroa/Pinhão/Corrente)", price = 18000L, lifespanKm = 15000.0, runKmSinceChange = 4500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pneu Traseiro", price = 22000L, lifespanKm = 12000.0, runKmSinceChange = 11000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pneu Dianteiro", price = 18000L, lifespanKm = 15000.0, runKmSinceChange = 3000.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Pastilhas de Freio", price = 4000L, lifespanKm = 6000.0, runKmSinceChange = 1200.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT),
                    VehiclePart(vehicleId = vehicle.id, name = "Lona de Freio Traseira", price = 3500L, lifespanKm = 10000.0, runKmSinceChange = 2500.0, remoteId = UUID.randomUUID().toString(), syncStatus = SyncStatus.PENDING_INSERT)
                )
                for (part in initialParts) {
                    partDao.insertPart(part)
                }
                parts = partDao.getPartsForVehicle(vehicle.id)
            }

            val totalWearCostPerKm = parts.sumOf { it.wearCostPerKm }.takeIf { it > 0.0 } ?: 0.075
            val fixedCostPerDay = if (vehicle.plannedWorkDays > 0) CurrencyUtils.divideByInt(vehicle.monthlyFixedCosts, vehicle.plannedWorkDays) else (18000L / 22)
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

                val gross = CurrencyUtils.toCents(160.0 + ((index + 1) * 13) % 110)
                val deliveries = 10 + ((index + 1) * 3) % 12
                val food = CurrencyUtils.toCents(15.0 + ((index + 1) * 4) % 16)
                val fuelPriceVal = CurrencyUtils.toFuelPriceMillis(5.75 + ((index + 1) * 2) % 5 * 0.05)

                val fuelCost = CurrencyUtils.computeFuelTotalCents(km / consumption, fuelPriceVal, 0L)
                val wearCost = CurrencyUtils.toCents(km * totalWearCostPerKm)
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

            // Recalcula hodômetro do veículo ativo
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
    }
}
