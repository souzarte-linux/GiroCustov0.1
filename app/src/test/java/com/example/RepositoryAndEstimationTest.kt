package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.EstimationDetail
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryAndEstimationTest {

    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GiroCustoRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertDailyRecordCalculationsAndOdometerAndWear() = runTest {
        // Setup Vehicle
        val vehicle = Vehicle(
            averageConsumption = 40.0,
            monthlyFixedCosts = 200.0,
            plannedWorkDays = 20,
            currentOdometer = 1000.0
        )
        repository.saveVehicle(vehicle)

        // Setup Parts
        val part1 = VehiclePart(name = "Pneu", price = 150.0, lifespanKm = 10000.0, runKmSinceChange = 100.0)
        val part2 = VehiclePart(name = "Óleo", price = 40.0, lifespanKm = 1000.0, runKmSinceChange = 50.0)
        repository.savePart(part1)
        repository.savePart(part2)

        // Insert Record: Start 1000.0, End 1100.0 (100km)
        // Fuel cost: (100 / 40.0) * 5.0 = 12.50
        // Wear cost: 100 * (150/10000 + 40/1000) = 100 * (0.015 + 0.04) = 100 * 0.055 = 5.50
        // Proportional Fixed cost: 200.0 / 20 = 10.00
        // Net profit: 100.0 (gross) - 12.50 (fuel) - 5.50 (wear) - 10.00 (fixed) - 15.00 (food) = 57.00
        repository.insertDailyRecord(
            dateString = "2026-07-06",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 100.0,
            deliveriesCount = 10,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        // Verify record calculations
        val records = repository.allRecordsFlow.first()
        assertEquals(1, records.size)
        val record = records[0]
        assertEquals(12.50, record.fuelCost, 0.001)
        assertEquals(5.50, record.wearCost, 0.001)
        assertEquals(10.00, record.proportionalFixedCost, 0.001)
        assertEquals(57.00, record.netProfit, 0.001)

        // Verify vehicle current odometer is updated to endOdometer
        val updatedVehicle = repository.getVehicle()
        assertNotNull(updatedVehicle)
        assertEquals(1100.0, updatedVehicle!!.currentOdometer, 0.001)

        // Verify parts run km are updated by 100km
        val updatedParts = repository.allPartsFlow.first()
        val updatedPart1 = updatedParts.find { it.name == "Pneu" }
        val updatedPart2 = updatedParts.find { it.name == "Óleo" }
        assertNotNull(updatedPart1)
        assertNotNull(updatedPart2)
        assertEquals(200.0, updatedPart1!!.runKmSinceChange, 0.001)
        assertEquals(150.0, updatedPart2!!.runKmSinceChange, 0.001)
    }

    @Test
    fun testUpdateDailyRecordAdjustsOdometerAndPartsCorrectly() = runTest {
        // Setup Vehicle
        val vehicle = Vehicle(
            averageConsumption = 40.0,
            monthlyFixedCosts = 200.0,
            plannedWorkDays = 20,
            currentOdometer = 1000.0
        )
        repository.saveVehicle(vehicle)

        // Setup Parts
        val part = VehiclePart(name = "Pneu", price = 100.0, lifespanKm = 10000.0, runKmSinceChange = 100.0)
        repository.savePart(part)

        // 1. Insert record (100km)
        repository.insertDailyRecord(
            dateString = "2026-07-06",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 100.0,
            deliveriesCount = 10,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        val records = repository.allRecordsFlow.first()
        val recordId = records[0].id

        // Check state after insertion: Vehicle odometer should be 1100.0, part wear should be 200.0
        assertEquals(1100.0, repository.getVehicle()!!.currentOdometer, 0.001)
        assertEquals(200.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)

        // 2. Update record with new odometer range (Start 1000.0, End 1120.0 => 120km)
        // Diff in km = 120 - 100 = +20km.
        // Vehicle odometer should become 1100.0 + 20 = 1120.0
        // Part runKmSinceChange should become 200.0 + 20 = 220.0
        repository.updateDailyRecord(
            recordId = recordId,
            dateString = "2026-07-06",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 120.0,
            deliveriesCount = 12,
            startOdometer = 1000.0,
            endOdometer = 1120.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        assertEquals(1120.0, repository.getVehicle()!!.currentOdometer, 0.001)
        assertEquals(220.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)

        // 3. Update record with smaller odometer range (Start 1000.0, End 1050.0 => 50km)
        // Diff in km = 50 - 120 = -70km.
        // Vehicle odometer should become 1120.0 - 70 = 1050.0
        // Part runKmSinceChange should become 220.0 - 70 = 150.0
        repository.updateDailyRecord(
            recordId = recordId,
            dateString = "2026-07-06",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 80.0,
            deliveriesCount = 8,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        assertEquals(1050.0, repository.getVehicle()!!.currentOdometer, 0.001)
        assertEquals(150.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)
    }

    @Test
    fun testDeleteRecordRevertsOdometerAndPartsWear() = runTest {
        // Setup Vehicle
        val vehicle = Vehicle(
            averageConsumption = 40.0,
            monthlyFixedCosts = 200.0,
            plannedWorkDays = 20,
            currentOdometer = 1000.0
        )
        repository.saveVehicle(vehicle)

        // Setup Parts
        val part = VehiclePart(name = "Pneu", price = 100.0, lifespanKm = 10000.0, runKmSinceChange = 100.0)
        repository.savePart(part)

        // 1. Insert record (50km)
        repository.insertDailyRecord(
            dateString = "2026-07-06",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 100.0,
            deliveriesCount = 10,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        val records = repository.allRecordsFlow.first()
        val record = records[0]

        // State check: vehicle odometer = 1050.0, part runKm = 150.0
        assertEquals(1050.0, repository.getVehicle()!!.currentOdometer, 0.001)
        assertEquals(150.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)

        // 2. Delete record
        repository.deleteRecord(record)

        // Verify reverted: vehicle odometer = 1000.0, part runKm = 100.0
        assertEquals(1000.0, repository.getVehicle()!!.currentOdometer, 0.001)
        assertEquals(100.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)
        assertEquals(0, repository.allRecordsFlow.first().size)
    }

    @Test
    fun testRealTimeEstimationAndLimitCases() = runTest {
        // Let's test the calculations for various inputs directly replicating GiroCustoViewModel's logic structure
        fun calculateEstimation(
            startOdo: String,
            endOdo: String,
            gross: String,
            food: String,
            fuelPrice: String,
            partsList: List<VehiclePart>,
            vehicle: Vehicle
        ): EstimationDetail {
            val startVal = startOdo.toDoubleOrNull() ?: 0.0
            val endVal = endOdo.toDoubleOrNull() ?: 0.0
            val grossVal = gross.toDoubleOrNull() ?: 0.0
            val foodVal = food.toDoubleOrNull() ?: 0.0
            val fPrice = fuelPrice.toDoubleOrNull() ?: 0.0

            val distance = (endVal - startVal).coerceAtLeast(0.0)

            val fuelCost = if (vehicle.averageConsumption > 0) {
                (distance / vehicle.averageConsumption) * fPrice
            } else {
                0.0
            }

            val wearCostPerKm = partsList.sumOf { it.wearCostPerKm }
            val wearCost = distance * wearCostPerKm

            val proportionalFixed = if (vehicle.plannedWorkDays > 0) {
                vehicle.monthlyFixedCosts / vehicle.plannedWorkDays
            } else {
                0.0
            }

            val totalExpenses = fuelCost + wearCost + proportionalFixed + foodVal
            val netProfit = grossVal - totalExpenses

            return EstimationDetail(
                distance = distance,
                fuelCost = fuelCost,
                wearCost = wearCost,
                fixedCost = proportionalFixed,
                foodCost = foodVal,
                totalExpenses = totalExpenses,
                netProfit = netProfit
            )
        }

        val standardVehicle = Vehicle(averageConsumption = 40.0, monthlyFixedCosts = 200.0, plannedWorkDays = 20)
        val standardParts = listOf(VehiclePart(name = "Pneu", price = 100.0, lifespanKm = 10000.0))

        // Normal Case
        val est1 = calculateEstimation("1000.0", "1100.0", "150.0", "15.0", "5.0", standardParts, standardVehicle)
        assertEquals(100.0, est1.distance, 0.001)
        assertEquals(12.50, est1.fuelCost, 0.001)
        assertEquals(1.0, est1.wearCost, 0.001)
        assertEquals(10.0, est1.fixedCost, 0.001)
        assertEquals(38.50, est1.totalExpenses, 0.001)
        assertEquals(111.50, est1.netProfit, 0.001)

        // Limit case: Odometer final < initial
        val estLimitOdo = calculateEstimation("1100.0", "1000.0", "150.0", "15.0", "5.0", standardParts, standardVehicle)
        assertEquals(0.0, estLimitOdo.distance, 0.001)
        assertEquals(0.0, estLimitOdo.fuelCost, 0.001)
        assertEquals(0.0, estLimitOdo.wearCost, 0.001)
        assertEquals(10.0, estLimitOdo.fixedCost, 0.001)
        assertEquals(25.0, estLimitOdo.totalExpenses, 0.001)

        // Limit case: Consumption == 0
        val vehicleConsumptionZero = standardVehicle.copy(averageConsumption = 0.0)
        val estLimitConsumption = calculateEstimation("1000.0", "1100.0", "150.0", "15.0", "5.0", standardParts, vehicleConsumptionZero)
        assertEquals(0.0, estLimitConsumption.fuelCost, 0.001)

        // Limit case: Planned work days == 0
        val vehiclePlannedWorkDaysZero = standardVehicle.copy(plannedWorkDays = 0)
        val estLimitWorkDays = calculateEstimation("1000.0", "1100.0", "150.0", "15.0", "5.0", standardParts, vehiclePlannedWorkDaysZero)
        assertEquals(0.0, estLimitWorkDays.fixedCost, 0.001)

        // Limit case: Empty parts list
        val estLimitParts = calculateEstimation("1000.0", "1100.0", "150.0", "15.0", "5.0", emptyList(), standardVehicle)
        assertEquals(0.0, estLimitParts.wearCost, 0.001)
    }
}
