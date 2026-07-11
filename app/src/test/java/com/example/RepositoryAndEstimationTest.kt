package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.test.advanceUntilIdle
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
        val activeVehicle = repository.getVehicle()!!

        // Setup Parts
        val standardPart = VehiclePart(name = "Pneu", price = 100.0, lifespanKm = 10000.0, runKmSinceChange = 0.0)
        repository.savePart(standardPart)

        // Instantiate actual ViewModel using Robolectric ViewModelProvider to allow clean lifecycle clearing
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModelStore = ViewModelStore()
        val viewModel = ViewModelProvider(
            viewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        )[GiroCustoViewModel::class.java]

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
        val vehicleConsumptionZero = activeVehicle.copy(averageConsumption = 0.0)
        repository.saveVehicle(vehicleConsumptionZero)
        db.invalidationTracker.refreshVersionsSync()
        
        // Wait for the ViewModel to receive the updated vehicle state
        viewModel.vehicle.first { it != null && it.averageConsumption == 0.0 }
        
        viewModel.startOdometer.value = "1000.0"
        viewModel.endOdometer.value = "1100.0"

        val estLimitConsumption = viewModel.realTimeEstimation.first { it.distance == 100.0 && it.fuelCost == 0.0 }
        assertEquals(0.0, estLimitConsumption.fuelCost, 0.001)

        // 4. Limit case: Planned work days == 0
        val vehiclePlannedWorkDaysZero = activeVehicle.copy(plannedWorkDays = 0)
        repository.saveVehicle(vehiclePlannedWorkDaysZero)
        db.invalidationTracker.refreshVersionsSync()
        
        // Wait for the ViewModel to receive the updated vehicle state
        viewModel.vehicle.first { it != null && it.plannedWorkDays == 0 }

        val estLimitWorkDays = viewModel.realTimeEstimation.first { it.fixedCost == 0.0 }
        assertEquals(0.0, estLimitWorkDays.fixedCost, 0.001)

        // 5. Limit case: Empty parts list
        val insertedPart = viewModel.parts.value.first()
        repository.deletePart(insertedPart)
        db.invalidationTracker.refreshVersionsSync()
        
        // Wait for the ViewModel to receive the empty parts state
        viewModel.parts.first { it.isEmpty() }
        
        repository.saveVehicle(activeVehicle) // restore average consumption and planned work days
        db.invalidationTracker.refreshVersionsSync()
        
        // Wait for the restored vehicle state
        viewModel.vehicle.first { it != null && it.averageConsumption == 40.0 && it.plannedWorkDays == 20 }

        val estLimitParts = viewModel.realTimeEstimation.first { it.wearCost == 0.0 && it.fixedCost == 10.0 }
        assertEquals(0.0, estLimitParts.wearCost, 0.001)

        // Clean up all active coroutines in the test scope and ViewModel scope to prevent UncompletedCoroutinesError
        viewModelStore.clear()
        advanceUntilIdle()
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

    @Test
    fun testFuelRefillCalculations() = runTest {
        // Prepare a vehicle
        val vehicle = Vehicle(id = 1001L, model = "Titan 160 Test", averageConsumption = 35.0, active = true)
        db.vehicleDao().insertVehicle(vehicle)

        // 1. Com apenas 1 abastecimento de tanque cheio (ou 0), o consumo médio real retorna null e vehicle.averageConsumption permanece inalterado
        val refill1 = FuelRefill(
            id = 0L,
            vehicleId = 1001L,
            dateTimestamp = 1000L,
            dateString = "10/07/2026",
            gasStation = "Posto A",
            fuelType = "Gasolina Comum",
            pricePerLiter = 5.0,
            liters = 10.0,
            totalPaid = 50.0,
            odometer = 10000.0,
            isFullTank = true,
            paymentMethod = "PIX"
        )
        repository.saveFuelRefill(refill1)

        val consumptionAfterOne = repository.calculateRealAverageConsumption(1001L)
        assertNull(consumptionAfterOne)

        val updatedVehicleAfterOne = db.vehicleDao().getVehicleById(1001L)
        assertNotNull(updatedVehicleAfterOne)
        assertEquals(35.0, updatedVehicleAfterOne!!.averageConsumption, 0.001)

        // 2. Inserção de dois abastecimentos de tanque cheio com odômetro e litros conhecidos, verificando que calculateRealAverageConsumption retorna o valor correto
        val refill2 = FuelRefill(
            id = 0L,
            vehicleId = 1001L,
            dateTimestamp = 2000L,
            dateString = "11/07/2026",
            gasStation = "Posto B",
            fuelType = "Gasolina Comum",
            pricePerLiter = 5.0,
            liters = 8.0,
            totalPaid = 40.0,
            odometer = 10320.0, // Distância do trecho = 10320 - 10000 = 320 km
            isFullTank = true,
            paymentMethod = "Cartão"
        )
        repository.saveFuelRefill(refill2)

        // Consumo médio real deve ser: 320 km / 8.0 L = 40.0 km/L
        val consumptionAfterTwo = repository.calculateRealAverageConsumption(1001L)
        assertNotNull(consumptionAfterTwo)
        assertEquals(40.0, consumptionAfterTwo!!, 0.001)

        // Que vehicle.averageConsumption é atualizado automaticamente após salvar um abastecimento que gera novo cálculo válido
        val updatedVehicleAfterTwo = db.vehicleDao().getVehicleById(1001L)
        assertNotNull(updatedVehicleAfterTwo)
        assertEquals(40.0, updatedVehicleAfterTwo!!.averageConsumption, 0.001)

        // 3. Adicione um teste específico simulando um parcial no meio de dois tanques cheios, confirmando que o litros do parcial entra na soma do trecho (e não é ignorado)
        db.vehicleDao().insertVehicle(vehicle.copy(id = 1002L, averageConsumption = 30.0))

        // Refill 1 (Full) - Odo 20000
        repository.saveFuelRefill(FuelRefill(
            id = 0L,
            vehicleId = 1002L,
            dateTimestamp = 1000L,
            dateString = "12/07/2026",
            gasStation = "Posto X",
            fuelType = "Gasolina",
            pricePerLiter = 6.0,
            liters = 5.0,
            totalPaid = 30.0,
            odometer = 20000.0,
            isFullTank = true,
            paymentMethod = "Dinheiro"
        ))

        // Refill 2 (Partial) - Odo 20150, 4 litros
        repository.saveFuelRefill(FuelRefill(
            id = 0L,
            vehicleId = 1002L,
            dateTimestamp = 2000L,
            dateString = "13/07/2026",
            gasStation = "Posto Y",
            fuelType = "Gasolina",
            pricePerLiter = 6.0,
            liters = 4.0,
            totalPaid = 24.0,
            odometer = 20150.0,
            isFullTank = false,
            paymentMethod = "PIX"
        ))

        // Refill 3 (Full) - Odo 20300, 6 litros
        repository.saveFuelRefill(FuelRefill(
            id = 0L,
            vehicleId = 1002L,
            dateTimestamp = 3000L,
            dateString = "14/07/2026",
            gasStation = "Posto Z",
            fuelType = "Gasolina",
            pricePerLiter = 6.0,
            liters = 6.0,
            totalPaid = 36.0,
            odometer = 20300.0,
            isFullTank = true,
            paymentMethod = "PIX"
        ))

        // Distância total = 20300 - 20000 = 300 km
        // Litros totais consumidos = 4.0 (do parcial) + 6.0 (do tanque cheio final) = 10.0 L
        // Consumo médio real = 300 / 10 = 30.0 km/L
        val consumptionWithPartial = repository.calculateRealAverageConsumption(1002L)
        assertNotNull(consumptionWithPartial)
        assertEquals(30.0, consumptionWithPartial!!, 0.001)

        val updatedVehicleWithPartial = db.vehicleDao().getVehicleById(1002L)
        assertNotNull(updatedVehicleWithPartial)
        assertEquals(30.0, updatedVehicleWithPartial!!.averageConsumption, 0.001)
    }

    @Test
    fun testMaintenanceRecordCrudAndFilteringAndStateIsolation() = runTest(testDispatcher) {
        // 1. Test CRUD of MaintenanceRecord via DAO and Repository
        val maintenance = MaintenanceRecord(
            id = 0L,
            vehicleId = 1L,
            dateTimestamp = 1000L,
            dateString = "10/07/2026",
            description = "Troca de óleo",
            location = "Oficina Central",
            value = 150.0,
            odometer = 10500.0
        )
        repository.saveMaintenanceRecord(maintenance)

        var allMaintenance = repository.maintenanceRecordsForVehicleFlow(1L).first()
        assertEquals(1, allMaintenance.size)
        assertEquals(150.0, allMaintenance[0].value, 0.001)

        // Update
        val updatedMaintenance = allMaintenance[0].copy(value = 180.0, description = "Troca de óleo e filtro")
        repository.saveMaintenanceRecord(updatedMaintenance)
        allMaintenance = repository.maintenanceRecordsForVehicleFlow(1L).first()
        assertEquals(1, allMaintenance.size)
        assertEquals(180.0, allMaintenance[0].value, 0.001)
        assertEquals("Troca de óleo e filtro", allMaintenance[0].description)

        // Delete
        repository.deleteMaintenanceRecord(allMaintenance[0])
        allMaintenance = repository.maintenanceRecordsForVehicleFlow(1L).first()
        assertTrue(allMaintenance.isEmpty())

        // 2. Test summation logic of corrective maintenance in a given period (filtering)
        val day1 = 1783641600000L // 10/07/2026
        val day2 = day1 + 24 * 60 * 60 * 1000L // 11/07/2026
        val day3 = day2 + 24 * 60 * 60 * 1000L // 12/07/2026

        val maintenance1 = MaintenanceRecord(id = 0L, vehicleId = 1L, dateTimestamp = day1, dateString = "10/07/2026", description = "M1", location = "", value = 100.0, odometer = 10.0)
        val maintenance2 = MaintenanceRecord(id = 0L, vehicleId = 1L, dateTimestamp = day2, dateString = "11/07/2026", description = "M2", location = "", value = 200.0, odometer = 20.0)
        val maintenance3 = MaintenanceRecord(id = 0L, vehicleId = 1L, dateTimestamp = day3, dateString = "12/07/2026", description = "M3", location = "", value = 300.0, odometer = 30.0)
        
        repository.saveMaintenanceRecord(maintenance1)
        repository.saveMaintenanceRecord(maintenance2)
        repository.saveMaintenanceRecord(maintenance3)

        val list = repository.maintenanceRecordsForVehicleFlow(1L).first()
        assertEquals(3, list.size)

        // Filter between day2 and day3: should include maintenance2 and maintenance3 (sum = 500.0)
        val filtered = com.example.ui.util.filterByPeriod(list, com.example.ui.Period.PERSONALIZADO, day2, day3) { it.dateTimestamp }
        assertEquals(2, filtered.size)
        assertEquals(500.0, filtered.sumOf { it.value }, 0.001)

        // 3. Test isolated behavior of the period filter in the ViewModel
        val viewModel = GiroCustoViewModel(ApplicationProvider.getApplicationContext() as Application)
        
        // Initial defaults
        assertEquals(com.example.ui.Period.SEMANA, viewModel.selectedPeriod.value)
        assertEquals(com.example.ui.Period.SEMANA, viewModel.reportsSelectedPeriod.value)

        // Change Dashboard (Painel) Period to MENSAL
        viewModel.setPeriod(com.example.ui.Period.MENSAL)
        assertEquals(com.example.ui.Period.MENSAL, viewModel.selectedPeriod.value)
        // Reports period should remain SEMANA
        assertEquals(com.example.ui.Period.SEMANA, viewModel.reportsSelectedPeriod.value)

        // Change Reports Period to QUINZENA
        viewModel.setReportsPeriod(com.example.ui.Period.QUINZENA)
        assertEquals(com.example.ui.Period.QUINZENA, viewModel.reportsSelectedPeriod.value)
        // Dashboard period should remain MENSAL
        assertEquals(com.example.ui.Period.MENSAL, viewModel.selectedPeriod.value)
    }
}
