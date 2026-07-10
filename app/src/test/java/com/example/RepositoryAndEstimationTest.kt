package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.EstimationDetail
import com.example.ui.GiroCustoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryAndEstimationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)
        repository = GiroCustoRepository(db)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testInsertDailyRecordCalculationsAndOdometerAndWear() = runTest(testDispatcher) {
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

        // Verify vehicle current odometer is updated to endOdometer (since it's the only and most recent record)
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
    fun testUpdateDailyRecordAdjustsOdometerAndPartsCorrectly() = runTest(testDispatcher) {
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
    fun testDeleteRecordRevertsOdometerAndPartsWear() = runTest(testDispatcher) {
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

        // Verify parts wear is reverted: part runKm = 100.0
        assertEquals(100.0, repository.allPartsFlow.first()[0].runKmSinceChange, 0.001)
        assertEquals(0, repository.allRecordsFlow.first().size)

        // Verify vehicle odometer stays as is because there are no records left
        assertEquals(1050.0, repository.getVehicle()!!.currentOdometer, 0.001)
    }

    @Test
    fun testRetroactiveOdometerUpdate() = runTest(testDispatcher) {
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

        // 1. Insert older record (A): dateTimestamp = 10000, Start 1000.0, End 1050.0
        repository.insertDailyRecord(
            dateString = "2026-07-01",
            dateTimestamp = 10000L,
            platform = "iFood",
            grossEarnings = 80.0,
            deliveriesCount = 5,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        // Vehicle odometer should be 1050.0 (it is the most recent record right now)
        assertEquals(1050.0, repository.getVehicle()!!.currentOdometer, 0.001)

        // 2. Insert newer record (B): dateTimestamp = 20000 (most recent), Start 1050.0, End 1120.0
        repository.insertDailyRecord(
            dateString = "2026-07-02",
            dateTimestamp = 20000L,
            platform = "iFood",
            grossEarnings = 120.0,
            deliveriesCount = 8,
            startOdometer = 1050.0,
            endOdometer = 1120.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        // Vehicle odometer should be 1120.0 (it is the most recent record by timestamp)
        assertEquals(1120.0, repository.getVehicle()!!.currentOdometer, 0.001)

        // 3. Edit Record (A) (not the most recent, dateTimestamp = 10000L vs Record B's 20000L)
        val records = repository.allRecordsFlow.first()
        val recordA = records.find { it.dateTimestamp == 10000L }
        assertNotNull(recordA)

        // We edit its endOdometer to 1080.0 (from 1050.0)
        repository.updateDailyRecord(
            recordId = recordA!!.id,
            dateString = "2026-07-01",
            dateTimestamp = 10000L,
            platform = "iFood",
            grossEarnings = 80.0,
            deliveriesCount = 5,
            startOdometer = 1000.0,
            endOdometer = 1080.0,
            fuelPrice = 5.0,
            foodExpense = 15.0
        )

        // CRITICAL CHECK: vehicle odometer must remain 1120.0 (Record B's endOdometer), NOT change to 1080.0!
        assertEquals(1120.0, repository.getVehicle()!!.currentOdometer, 0.001)

        // 4. Delete Record (A)
        val updatedRecords = repository.allRecordsFlow.first()
        val updatedRecordA = updatedRecords.find { it.dateTimestamp == 10000L }
        assertNotNull(updatedRecordA)
        repository.deleteRecord(updatedRecordA!!)

        // Vehicle odometer must still remain 1120.0 (Record B's endOdometer)
        assertEquals(1120.0, repository.getVehicle()!!.currentOdometer, 0.001)
    }

    @Test
    fun testRealTimeEstimationAndLimitCases() = runTest(testDispatcher) {
        // Setup Vehicle
        val standardVehicle = Vehicle(
            averageConsumption = 40.0,
            monthlyFixedCosts = 200.0,
            plannedWorkDays = 20,
            currentOdometer = 1000.0
        )
        repository.saveVehicle(standardVehicle)

        // Setup Parts
        val standardPart = VehiclePart(name = "Pneu", price = 100.0, lifespanKm = 10000.0, runKmSinceChange = 0.0)
        repository.savePart(standardPart)

        // Instantiate actual ViewModel using Robolectric Application Provider
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        // Wait for asynchronous database flows to initialize and propagate standard values to ViewModel
        viewModel.vehicle.first { it != null && it.monthlyFixedCosts == 200.0 }
        viewModel.parts.first { it.isNotEmpty() && it.any { p -> p.name == "Pneu" } }
        runCurrent()

        // 1. Normal Case
        viewModel.startOdometer.value = "1000.0"
        viewModel.endOdometer.value = "1100.0"
        viewModel.grossEarnings.value = "150.0"
        viewModel.foodExpense.value = "15.0"
        viewModel.fuelPrice.value = "5.0"

        // Assert on the emitted state that satisfies the conditions
        val est1 = viewModel.realTimeEstimation.first { it.distance == 100.0 && it.fuelCost == 12.50 }
        assertEquals(100.0, est1.distance, 0.001)
        assertEquals(12.50, est1.fuelCost, 0.001)
        assertEquals(1.0, est1.wearCost, 0.001)
        assertEquals(10.0, est1.fixedCost, 0.001)
        assertEquals(38.50, est1.totalExpenses, 0.001)
        assertEquals(111.50, est1.netProfit, 0.001)

        // 2. Limit case: Odometer final < initial
        viewModel.startOdometer.value = "1100.0"
        viewModel.endOdometer.value = "1000.0"

        val estLimitOdo = viewModel.realTimeEstimation.first { it.distance == 0.0 && it.fixedCost == 10.0 }
        assertEquals(0.0, estLimitOdo.distance, 0.001)
        assertEquals(0.0, estLimitOdo.fuelCost, 0.001)
        assertEquals(0.0, estLimitOdo.wearCost, 0.001)
        assertEquals(10.0, estLimitOdo.fixedCost, 0.001)
        assertEquals(25.0, estLimitOdo.totalExpenses, 0.001)

        // 3. Limit case: Consumption == 0
        val vehicleConsumptionZero = standardVehicle.copy(averageConsumption = 0.0)
        repository.saveVehicle(vehicleConsumptionZero)
        
        // Wait for the ViewModel to receive the updated vehicle state
        viewModel.vehicle.first { it != null && it.averageConsumption == 0.0 }
        
        viewModel.startOdometer.value = "1000.0"
        viewModel.endOdometer.value = "1100.0"

        val estLimitConsumption = viewModel.realTimeEstimation.first { it.distance == 100.0 && it.fuelCost == 0.0 }
        assertEquals(0.0, estLimitConsumption.fuelCost, 0.001)

        // 4. Limit case: Planned work days == 0
        val vehiclePlannedWorkDaysZero = standardVehicle.copy(plannedWorkDays = 0)
        repository.saveVehicle(vehiclePlannedWorkDaysZero)
        
        // Wait for the ViewModel to receive the updated vehicle state
        viewModel.vehicle.first { it != null && it.plannedWorkDays == 0 }

        val estLimitWorkDays = viewModel.realTimeEstimation.first { it.fixedCost == 0.0 }
        assertEquals(0.0, estLimitWorkDays.fixedCost, 0.001)

        // 5. Limit case: Empty parts list
        val insertedPart = viewModel.parts.value.first()
        repository.deletePart(insertedPart)
        
        // Wait for the ViewModel to receive the empty parts state
        viewModel.parts.first { it.isEmpty() }
        
        repository.saveVehicle(standardVehicle) // restore average consumption and planned work days
        
        // Wait for the restored vehicle state
        viewModel.vehicle.first { it != null && it.averageConsumption == 40.0 && it.plannedWorkDays == 20 }

        val estLimitParts = viewModel.realTimeEstimation.first { it.wearCost == 0.0 && it.fixedCost == 10.0 }
        assertEquals(0.0, estLimitParts.wearCost, 0.001)

        // Clean up all active coroutines in the test scope to prevent UncompletedCoroutinesError
        coroutineContext[Job]?.cancelChildren()
    }

    @Test
    fun testUserProfileCreationAndUpdate() = runTest(testDispatcher) {
        val initialProfile = repository.userProfileFlow.first()
        assertNull(initialProfile)

        val profile = UserProfile(
            name = "John Doe",
            phone = "11999998888",
            city = "São Paulo"
        )
        repository.saveUserProfile(profile)

        val savedProfile = repository.userProfileFlow.first()
        assertNotNull(savedProfile)
        assertEquals("John Doe", savedProfile!!.name)
        assertEquals("11999998888", savedProfile.phone)
        assertEquals("São Paulo", savedProfile.city)

        val updatedProfile = savedProfile.copy(
            name = "Jane Doe"
        )
        repository.saveUserProfile(updatedProfile)

        val finalProfile = repository.getUserProfile()
        assertNotNull(finalProfile)
        assertEquals("Jane Doe", finalProfile!!.name)
        assertEquals("11999998888", finalProfile.phone)
    }

    @Test
    fun testPlatformCRUD() = runTest(testDispatcher) {
        val initialPlatforms = repository.allPlatformsFlow.first()
        assertTrue(initialPlatforms.isEmpty())

        val p = Platform(
            name = "iFood",
            segment = "delivery",
            paymentModel = "producao",
            cycle = "semanal",
            paymentDay = "QUA",
            fixedPayDelay = 7,
            cycleEntriesJson = "1:7,16:7",
            bankName = "Nubank",
            bankAgency = "0001",
            bankAccount = "123456-7",
            pixKeyType = "CPF",
            pixKey = "123.456.789-00",
            active = true
        )
        val id = repository.savePlatform(p)
        assertTrue(id > 0)

        val listAfterSave = repository.allPlatformsFlow.first()
        assertEquals(1, listAfterSave.size)
        val savedP = listAfterSave[0]
        assertEquals("iFood", savedP.name)
        assertEquals("delivery", savedP.segment)
        assertEquals("producao", savedP.paymentModel)
        assertEquals("semanal", savedP.cycle)
        assertEquals("QUA", savedP.paymentDay)
        assertEquals(7, savedP.fixedPayDelay)
        assertEquals("1:7,16:7", savedP.cycleEntriesJson)
        assertEquals("Nubank", savedP.bankName)
        assertEquals("0001", savedP.bankAgency)
        assertEquals("123456-7", savedP.bankAccount)
        assertEquals("CPF", savedP.pixKeyType)
        assertEquals("123.456.789-00", savedP.pixKey)
        assertTrue(savedP.active)

        val updatedP = savedP.copy(
            name = "Rappi",
            cycle = "misto"
        )
        repository.savePlatform(updatedP)

        val fetchedP = repository.getPlatformById(savedP.id)
        assertNotNull(fetchedP)
        assertEquals("Rappi", fetchedP!!.name)
        assertEquals("misto", fetchedP.cycle)

        repository.deletePlatform(fetchedP)
        val listAfterDelete = repository.allPlatformsFlow.first()
        assertTrue(listAfterDelete.isEmpty())
    }

    @Test
    fun testPlatformFilteringLogic() {
        val list = listOf(
            Platform(name = "iFood", segment = "delivery", paymentModel = "producao", cycle = "semanal", active = true),
            Platform(name = "Uber Eats", segment = "delivery", paymentModel = "producao", cycle = "semanal", active = true),
            Platform(name = "Rappi", segment = "delivery", paymentModel = "producao", cycle = "semanal", active = true)
        )

        // Filter by "ifo" (case insensitive)
        val filteredIfood = list.filter { it.name.contains("ifo", ignoreCase = true) }
        assertEquals(1, filteredIfood.size)
        assertEquals("iFood", filteredIfood[0].name)

        // Filter by "e"
        val filteredE = list.filter { it.name.contains("e", ignoreCase = true) }
        assertEquals(1, filteredE.size) // Only "Uber Eats" has 'e' (case-insensitive)

        // Filter by empty string should return all
        val filteredEmpty = list.filter { it.name.contains("", ignoreCase = true) }
        assertEquals(3, filteredEmpty.size)
    }
}
