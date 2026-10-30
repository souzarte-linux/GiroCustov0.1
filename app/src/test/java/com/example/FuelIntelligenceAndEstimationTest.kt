package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.util.CurrencyUtils
import com.example.ui.ConsumptionAlertLevel
import com.example.ui.FuelConsumptionComparisonState
import com.example.ui.FuelCostEstimationInfo
import com.example.ui.GiroCustoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FuelIntelligenceAndEstimationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository
    private lateinit var context: Context
    private lateinit var viewModel: GiroCustoViewModel

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
        viewModel = GiroCustoViewModel(context as Application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testConsumptionAlertLevelAndModelsInstantiations() {
        // 1. Testa todos os valores do enum e propriedades
        val levels = ConsumptionAlertLevel.values()
        assertEquals(4, levels.size)
        assertTrue(levels.contains(ConsumptionAlertLevel.GREEN))
        assertTrue(levels.contains(ConsumptionAlertLevel.AMBER))
        assertTrue(levels.contains(ConsumptionAlertLevel.RED))
        assertTrue(levels.contains(ConsumptionAlertLevel.GRAY))

        assertTrue(ConsumptionAlertLevel.GREEN.isEfficient)
        assertFalse(ConsumptionAlertLevel.AMBER.isEfficient)
        assertFalse(ConsumptionAlertLevel.RED.isEfficient)
        assertFalse(ConsumptionAlertLevel.GRAY.isEfficient)

        // 2. Testa modelo FuelConsumptionComparisonState
        val comparisonState = FuelConsumptionComparisonState(
            theoreticalConsumption = 30.0,
            realConsumption = 35.0,
            differencePercentage = 16.67,
            alertLevel = ConsumptionAlertLevel.GREEN,
            message = "Excelente",
            hasSufficientData = true
        )
        assertEquals(30.0, comparisonState.theoreticalConsumption, 0.001)
        assertEquals(35.0, comparisonState.realConsumption!!, 0.001)
        assertEquals(16.67, comparisonState.differencePercentage!!, 0.01)
        assertEquals(ConsumptionAlertLevel.GREEN, comparisonState.alertLevel)
        assertTrue(comparisonState.hasSufficientData)

        // 3. Testa modelo FuelCostEstimationInfo
        val estimationInfo = FuelCostEstimationInfo(
            vehicleModel = "Honda CG 160",
            distanceKm = 100.0,
            effectiveConsumption = 40.0,
            isUsingRealConsumption = true,
            estimatedFuelCost = 15.0,
            fuelPriceUsed = 6.00,
            displayText = "Custo estimado para 100 km"
        )
        assertEquals("Honda CG 160", estimationInfo.vehicleModel)
        assertEquals(100.0, estimationInfo.distanceKm, 0.001)
        assertEquals(40.0, estimationInfo.effectiveConsumption, 0.001)
        assertTrue(estimationInfo.isUsingRealConsumption)
        assertEquals(15.0, estimationInfo.estimatedFuelCost, 0.001)
        assertEquals(6.00, estimationInfo.fuelPriceUsed, 0.001)
    }

    @Test
    fun testFuelComparisonState_levelsAndTransitions() = runTest(testDispatcher) {
        val testVehicleId = 7001L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Yamaha Fazer 250",
            averageConsumption = 30.0, // Consumo cadastrado (teórico)
            tankCapacity = 14.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // Cenário 1: Sem abastecimentos -> Nível GRAY (dados insuficientes)
        var comparison = viewModel.fuelComparisonState.first()
        assertEquals(30.0, comparison.theoreticalConsumption, 0.001)
        assertNull(comparison.realConsumption)
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
        assertFalse(comparison.hasSufficientData)

        // Cenário 2: Apenas 1 tanque cheio -> Ainda GRAY
        val refill1 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 1000L,
            dateString = "01/08/2026",
            gasStation = "Posto Shell",
            fuelType = "Gasolina",
            pricePerLiter = 5.80,
            liters = 10.0,
            totalPaid = 58.0,
            odometer = 10000.0,
            isFullTank = true,
            paymentMethod = "PIX"
        )
        repository.saveFuelRefill(refill1)
        comparison = viewModel.fuelComparisonState.first()
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
        assertFalse(comparison.hasSufficientData)

        // Cenário 3: Segundo tanque cheio gerando consumo Excelente (35 km/L >= 30 * 0.95) -> GREEN
        // 350 km com 10 litros = 35.0 km/L (+16.6% melhor que o teórico)
        val refill2 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 2000L,
            dateString = "05/08/2026",
            gasStation = "Posto Ipiranga",
            fuelType = "Gasolina",
            pricePerLiter = 5.90,
            liters = 10.0,
            totalPaid = 59.0,
            odometer = 10350.0,
            isFullTank = true,
            paymentMethod = "PIX"
        )
        repository.saveFuelRefill(refill2)
        comparison = viewModel.fuelComparisonState.first()
        assertTrue(comparison.hasSufficientData)
        assertNotNull(comparison.realConsumption)
        assertEquals(35.0, comparison.realConsumption!!, 0.001)
        assertEquals(ConsumptionAlertLevel.GREEN, comparison.alertLevel)
        assertTrue(comparison.differencePercentage!! > 0)

        // Cenário 4: Atualiza o veículo para teórico = 40.0 km/L (35.0 / 40.0 = 87.5% -> entre 80% e 95%) -> AMBER
        val updatedVehicle = vehicle.copy(averageConsumption = 40.0)
        db.vehicleDao().updateVehicle(updatedVehicle)
        comparison = viewModel.fuelComparisonState.first()
        assertEquals(40.0, comparison.theoreticalConsumption, 0.001)
        assertEquals(35.0, comparison.realConsumption!!, 0.001)
        assertEquals(ConsumptionAlertLevel.AMBER, comparison.alertLevel)

        // Cenário 5: Atualiza o veículo para teórico = 50.0 km/L (35.0 / 50.0 = 70.0% -> < 80%) -> RED
        val updatedVehicle2 = vehicle.copy(averageConsumption = 50.0)
        db.vehicleDao().updateVehicle(updatedVehicle2)
        comparison = viewModel.fuelComparisonState.first()
        assertEquals(50.0, comparison.theoreticalConsumption, 0.001)
        assertEquals(35.0, comparison.realConsumption!!, 0.001)
        assertEquals(ConsumptionAlertLevel.RED, comparison.alertLevel)
    }

    @Test
    fun testFuelCostEstimationInfo_realVsCadastradoPriorityAndText() = runTest(testDispatcher) {
        val testVehicleId = 8001L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Fazer 250",
            averageConsumption = 30.0, // Consumo cadastrado (teórico)
            tankCapacity = 14.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // Configurar formulário
        viewModel.startOdometer.value = "10000.0"
        viewModel.endOdometer.value = "10150.0" // 150 km
        viewModel.fuelPrice.value = "6.00"

        // Cenário 1: Sem abastecimentos reais -> Usa o consumo cadastrado (30.0 km/L)
        // Custo = 150 / 30.0 * 6.00 = 30.00
        var info = viewModel.fuelCostEstimationInfo.first()
        assertEquals(150.0, info.distanceKm, 0.001)
        assertEquals(30.0, info.effectiveConsumption, 0.001)
        assertFalse(info.isUsingRealConsumption)
        assertEquals(30.00, info.estimatedFuelCost, 0.001)
        assertTrue(info.displayText.contains("Fazer 250"))
        assertTrue(info.displayText.contains("150"))

        // Cenário 2: Adiciona 2 abastecimentos cheios gerando consumo real de 50 km/L
        val r1 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 1000L,
            dateString = "01/08/2026",
            gasStation = "Shell",
            fuelType = "Gasolina",
            pricePerLiter = 6.00,
            liters = 5.0,
            totalPaid = 30.0,
            odometer = 5000.0,
            isFullTank = true,
            paymentMethod = "PIX"
        )
        val r2 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 2000L,
            dateString = "03/08/2026",
            gasStation = "Shell",
            fuelType = "Gasolina",
            pricePerLiter = 6.00,
            liters = 5.0,
            totalPaid = 30.0,
            odometer = 5250.0, // 250 km / 5 L = 50.0 km/L
            isFullTank = true,
            paymentMethod = "PIX"
        )
        repository.saveFuelRefill(r1)
        repository.saveFuelRefill(r2)

        // Agora com consumo real disponível (50.0 km/L):
        // Custo = 150 / 50.0 * 6.00 = 18.00
        info = viewModel.fuelCostEstimationInfo.first()
        assertEquals(150.0, info.distanceKm, 0.001)
        assertEquals(50.0, info.effectiveConsumption, 0.001)
        assertTrue(info.isUsingRealConsumption)
        assertEquals(18.00, info.estimatedFuelCost, 0.001)
    }

    @Test
    fun testRealTimeEstimation_priorityForRealConsumption() = runTest(testDispatcher) {
        val testVehicleId = 9001L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "CG 160",
            averageConsumption = 35.0, // Teórico
            monthlyFixedCosts = 200.0,
            plannedWorkDays = 20,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        viewModel.startOdometer.value = "1000.0"
        viewModel.endOdometer.value = "1105.0" // 105 km
        viewModel.grossEarnings.value = "150.0"
        viewModel.foodExpense.value = "10.0"
        viewModel.fuelPrice.value = "5.00"

        // 1. Sem consumo real -> Usa 35.0 km/L
        // 105 / 35.0 = 3 L * 5.00 = 15.00
        var estimation = viewModel.realTimeEstimation.first()
        assertEquals(105.0, estimation.distance, 0.001)
        assertEquals(15.00, estimation.fuelCost, 0.001)

        // 2. Adiciona abastecimentos para consumo real de 42.0 km/L
        val r1 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 1000L,
            dateString = "01/08/2026",
            gasStation = "Posto BR",
            fuelType = "Gasolina",
            pricePerLiter = 5.00,
            liters = 5.0,
            totalPaid = 25.0,
            odometer = 1000.0,
            isFullTank = true,
            paymentMethod = "PIX"
        )
        val r2 = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 2000L,
            dateString = "04/08/2026",
            gasStation = "Posto BR",
            fuelType = "Gasolina",
            pricePerLiter = 5.00,
            liters = 5.0,
            totalPaid = 25.0,
            odometer = 1210.0, // 210 km / 5 L = 42.0 km/L
            isFullTank = true,
            paymentMethod = "PIX"
        )
        repository.saveFuelRefill(r1)
        repository.saveFuelRefill(r2)

        // 105 / 42.0 = 2.5 L * 5.00 = 12.50
        estimation = viewModel.realTimeEstimation.first()
        assertEquals(105.0, estimation.distance, 0.001)
        assertEquals(12.50, estimation.fuelCost, 0.001)
    }

    @Test
    fun testPrefillLaunchFields_fuelPriceSuggestionPriority() = runTest(testDispatcher) {
        val testVehicleId = 9501L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Mobi",
            averageConsumption = 14.0,
            currentOdometer = 30000.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // 1. Sem registros e sem abastecimentos: fuelPrice vazio, startOdometer sem .0
        viewModel.prefillLaunchFields()
        assertEquals("", viewModel.fuelPrice.value)
        assertEquals("30000", viewModel.startOdometer.value)

        // 2. Insere um DailyRecord com fuelPrice = 6.20 e endOdometer = 30100.0
        repository.insertDailyRecord(
            dateString = "2026-08-01",
            dateTimestamp = 1000L,
            platform = "Uber",
            grossEarnings = 100.0,
            deliveriesCount = 5,
            startOdometer = 30000.0,
            endOdometer = 30100.0,
            fuelPrice = 6.20,
            foodExpense = 0.0
        )
        viewModel.prefillLaunchFields()
        // fuelPrice permanece vazio por especificação da tela de lançamento, startOdo atualiza
        assertEquals("", viewModel.fuelPrice.value)
        assertEquals("30100", viewModel.startOdometer.value)

        // 3. Insere um abastecimento para o veículo ativo com pricePerLiter = 5.49
        val refill = FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 2000L,
            dateString = "02/08/2026",
            gasStation = "Posto Shell",
            fuelType = "Gasolina",
            pricePerLiter = 5.49,
            liters = 20.0,
            totalPaid = 109.80,
            odometer = 30120.0,
            isFullTank = true,
            paymentMethod = "Cartão"
        )
        repository.saveFuelRefill(refill)

        viewModel.prefillLaunchFields()
        // fuelPrice permanece vazio conforme requisito de lançamento limpo
        assertEquals("", viewModel.fuelPrice.value)
    }

    @Test
    fun testTankCapacityValidationCondition() {
        val vehicle = Vehicle(
            id = 1L,
            model = "Titan 160",
            tankCapacity = 16.0
        )
        val validLiters = 15.5
        val excessiveLiters = 18.0

        assertTrue("15.5 L não excede 16.0 L", validLiters <= vehicle.tankCapacity)
        assertTrue("18.0 L excede 16.0 L", excessiveLiters > vehicle.tankCapacity)
    }

    @Test
    fun testMultiplePartialsBetweenFullTanks_consumptionCalculation() = runTest(testDispatcher) {
        val testVehicleId = 6001L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Honda CG 160",
            averageConsumption = 35.0,
            tankCapacity = 16.1,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // Trecho 1: Full (10000) -> Partial 1 (10120, 3.5L) -> Partial 2 (10250, 4.0L) -> Partial 3 (10380, 3.5L) -> Full (10500, 5.0L)
        // Distância: 10500 - 10000 = 500 km
        // Litros consumidos: 3.5 + 4.0 + 3.5 + 5.0 = 16.0 L
        // Consumo Trecho 1: 500 / 16.0 = 31.25 km/L
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 1000L, dateString = "01/08/2026", gasStation = "Posto A", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 10.0, totalPaid = 60.0, odometer = 10000.0, isFullTank = true, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "Posto B", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 3.5, totalPaid = 21.0, odometer = 10120.0, isFullTank = false, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 3000L, dateString = "03/08/2026", gasStation = "Posto C", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 4.0, totalPaid = 24.0, odometer = 10250.0, isFullTank = false, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 4000L, dateString = "04/08/2026", gasStation = "Posto D", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 3.5, totalPaid = 21.0, odometer = 10380.0, isFullTank = false, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 5000L, dateString = "05/08/2026", gasStation = "Posto E", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 5.0, totalPaid = 30.0, odometer = 10500.0, isFullTank = true, paymentMethod = "PIX"))

        val realAvg1 = repository.calculateRealAverageConsumption(testVehicleId)
        val latestReal1 = repository.calculateLatestRealConsumption(testVehicleId)
        assertNotNull(realAvg1)
        assertNotNull(latestReal1)
        assertEquals(31.25, realAvg1!!, 0.001)
        assertEquals(31.25, latestReal1!!, 0.001)

        val comparison1 = viewModel.fuelComparisonState.first()
        assertTrue(comparison1.hasSufficientData)
        assertEquals(31.25, comparison1.realConsumption!!, 0.001)

        // Trecho 2: Partial 4 (10650, 4.0L) -> Full (10800, 6.0L)
        // Distância: 10800 - 10500 = 300 km
        // Litros consumidos: 4.0 + 6.0 = 10.0 L
        // Consumo Trecho 2: 300 / 10.0 = 30.00 km/L
        // Média Geral: (31.25 + 30.00) / 2 = 30.625 km/L
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 6000L, dateString = "06/08/2026", gasStation = "Posto F", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 4.0, totalPaid = 24.0, odometer = 10650.0, isFullTank = false, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 7000L, dateString = "07/08/2026", gasStation = "Posto G", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 6.0, totalPaid = 36.0, odometer = 10800.0, isFullTank = true, paymentMethod = "PIX"))

        val realAvg2 = repository.calculateRealAverageConsumption(testVehicleId)
        val latestReal2 = repository.calculateLatestRealConsumption(testVehicleId)
        assertNotNull(realAvg2)
        assertNotNull(latestReal2)
        assertEquals(30.625, realAvg2!!, 0.001)
        assertEquals(30.00, latestReal2!!, 0.001)
    }

    @Test
    fun testZeroOrInvertedOdometerRefills_handledGracefullyWithoutDivisionByZero() = runTest(testDispatcher) {
        val testVehicleId = 6002L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Yamaha Factor 150",
            averageConsumption = 38.0,
            tankCapacity = 15.7,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // 1. Dois abastecimentos cheios com odômetro IDÊNTICO (distância = 0)
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 1000L, dateString = "01/08/2026", gasStation = "Posto A", fuelType = "Gasolina", pricePerLiter = 5.50, liters = 10.0, totalPaid = 55.0, odometer = 12000.0, isFullTank = true, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "Posto B", fuelType = "Gasolina", pricePerLiter = 5.50, liters = 10.0, totalPaid = 55.0, odometer = 12000.0, isFullTank = true, paymentMethod = "PIX"))

        // Não deve quebrar com divisão por zero nem gerar infinito; distance <= 0 é ignorada
        assertNull(repository.calculateRealAverageConsumption(testVehicleId))
        assertNull(repository.calculateLatestRealConsumption(testVehicleId))

        var comparison = viewModel.fuelComparisonState.first()
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
        assertFalse(comparison.hasSufficientData)

        // 2. Terceiro abastecimento com odômetro menor (ex: erro de digitação do usuário: 11500)
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 3000L, dateString = "03/08/2026", gasStation = "Posto C", fuelType = "Gasolina", pricePerLiter = 5.50, liters = 10.0, totalPaid = 55.0, odometer = 11500.0, isFullTank = true, paymentMethod = "PIX"))

        // O DAO ordena por odometer ASC: 11500, 12000, 12000
        // Trecho 1: 11500 -> 12000 = 500 km / 10 L = 50 km/L
        // Trecho 2: 12000 -> 12000 = 0 km (ignorado)
        // calculateRealAverageConsumption deve retornar 50.0 km/L com sucesso sem crash
        val calculatedAvg = repository.calculateRealAverageConsumption(testVehicleId)
        assertNotNull(calculatedAvg)
        assertEquals(50.0, calculatedAvg!!, 0.001)

        // 3. Adiciona abastecimento válido subsequente
        // 12000 -> 12350 = 350 km / 10 L = 35 km/L
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 4000L, dateString = "04/08/2026", gasStation = "Posto D", fuelType = "Gasolina", pricePerLiter = 5.50, liters = 10.0, totalPaid = 55.0, odometer = 12350.0, isFullTank = true, paymentMethod = "PIX"))

        val latestReal = repository.calculateLatestRealConsumption(testVehicleId)
        assertNotNull(latestReal)
        assertEquals(35.0, latestReal!!, 0.001)
    }

    @Test
    fun testZeroLitersRefills_handledGracefullyWithoutDivisionByZero() = runTest(testDispatcher) {
        val testVehicleId = 6003L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Mobi Easy",
            averageConsumption = 15.0,
            tankCapacity = 47.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // Dois tanques cheios porém com 0.0 litros
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 1000L, dateString = "01/08/2026", gasStation = "Posto A", fuelType = "Etanol", pricePerLiter = 4.00, liters = 0.0, totalPaid = 0.0, odometer = 50000.0, isFullTank = true, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "Posto B", fuelType = "Etanol", pricePerLiter = 4.00, liters = 0.0, totalPaid = 0.0, odometer = 50200.0, isFullTank = true, paymentMethod = "PIX"))

        assertNull(repository.calculateRealAverageConsumption(testVehicleId))
        assertNull(repository.calculateLatestRealConsumption(testVehicleId))
        val comparison = viewModel.fuelComparisonState.first()
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
    }

    @Test
    fun testOnlyPartialsOrSingleFullRefill_alwaysProducesGrayAlert() = runTest(testDispatcher) {
        val testVehicleId = 6004L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "HB20",
            averageConsumption = 13.0,
            tankCapacity = 50.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // 1. Múltiplos parciais sem nenhum tanque cheio
        for (i in 1..4) {
            repository.saveFuelRefill(FuelRefill(
                id = 0L,
                vehicleId = testVehicleId,
                dateTimestamp = 1000L * i,
                dateString = "0$i/08/2026",
                gasStation = "Posto $i",
                fuelType = "Gasolina",
                pricePerLiter = 5.80,
                liters = 10.0,
                totalPaid = 58.0,
                odometer = 10000.0 + (i * 100.0),
                isFullTank = false,
                paymentMethod = "PIX"
            ))
        }

        assertNull(repository.calculateRealAverageConsumption(testVehicleId))
        assertNull(repository.calculateLatestRealConsumption(testVehicleId))
        var comparison = viewModel.fuelComparisonState.first()
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
        assertFalse(comparison.hasSufficientData)

        // 2. Adiciona 1 tanque cheio no meio
        repository.saveFuelRefill(FuelRefill(
            id = 0L,
            vehicleId = testVehicleId,
            dateTimestamp = 5000L,
            dateString = "05/08/2026",
            gasStation = "Posto Cheio",
            fuelType = "Gasolina",
            pricePerLiter = 5.80,
            liters = 20.0,
            totalPaid = 116.0,
            odometer = 10500.0,
            isFullTank = true,
            paymentMethod = "PIX"
        ))

        // Permanece GRAY pois precisa de pelo menos 2 cheios
        assertNull(repository.calculateRealAverageConsumption(testVehicleId))
        assertNull(repository.calculateLatestRealConsumption(testVehicleId))
        comparison = viewModel.fuelComparisonState.first()
        assertEquals(ConsumptionAlertLevel.GRAY, comparison.alertLevel)
        assertFalse(comparison.hasSufficientData)
    }

    @Test
    fun testConsumptionAlertLevel_ExactBoundaryThresholds() = runTest(testDispatcher) {
        val testVehicleId = 6005L
        // Teórico = 40.0 km/L
        // 95% = 38.0 km/L
        // 80% = 32.0 km/L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Biz 125",
            averageConsumption = 40.0,
            tankCapacity = 5.1,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // Primeiro tanque cheio base em 1000 km
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 1000L, dateString = "01/08/2026", gasStation = "P1", fuelType = "Gasolina", pricePerLiter = 5.0, liters = 5.0, totalPaid = 25.0, odometer = 1000.0, isFullTank = true, paymentMethod = "PIX"))

        // Limite Exato 1: GREEN (real = 38.0 -> 380 km com 10 L => odo = 1380.0)
        val refillGreen = FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "P2", fuelType = "Gasolina", pricePerLiter = 5.0, liters = 10.0, totalPaid = 50.0, odometer = 1380.0, isFullTank = true, paymentMethod = "PIX")
        repository.saveFuelRefill(refillGreen)

        var comparison = viewModel.fuelComparisonState.first()
        assertEquals(38.0, comparison.realConsumption!!, 0.001)
        assertEquals(ConsumptionAlertLevel.GREEN, comparison.alertLevel)
        assertTrue(comparison.message.contains("Excelente"))

        // Limite Exato 2: AMBER (real = 37.99 -> atualizamos refillGreen para 1379.9 km => 379.9 / 10 = 37.99 km/L)
        val savedRefillGreen = repository.getLatestFuelRefill(testVehicleId)!!
        repository.saveFuelRefill(savedRefillGreen.copy(odometer = 1379.9))

        comparison = viewModel.fuelComparisonState.first()
        assertEquals(37.99, comparison.realConsumption!!, 0.01)
        assertEquals(ConsumptionAlertLevel.AMBER, comparison.alertLevel)
        assertTrue(comparison.message.contains("Atenção: Consumo moderado"))

        // Limite Exato 3: AMBER exato no limiar inferior (real = 32.0 -> 320 km com 10 L => odo = 1320.0)
        repository.saveFuelRefill(savedRefillGreen.copy(odometer = 1320.0))

        comparison = viewModel.fuelComparisonState.first()
        assertEquals(32.0, comparison.realConsumption!!, 0.001)
        assertEquals(ConsumptionAlertLevel.AMBER, comparison.alertLevel)

        // Limite Exato 4: RED (real = 31.99 -> 319.9 km com 10 L => odo = 1319.9)
        repository.saveFuelRefill(savedRefillGreen.copy(odometer = 1319.9))

        comparison = viewModel.fuelComparisonState.first()
        assertEquals(31.99, comparison.realConsumption!!, 0.01)
        assertEquals(ConsumptionAlertLevel.RED, comparison.alertLevel)
        assertTrue(comparison.message.contains("Alerta: Consumo elevado"))
    }

    @Test
    fun testPrefillLaunchFields_VehicleIsolationAndEdgeCases() = runTest(testDispatcher) {
        val vehicleInactiveId = 7010L
        val vehicleActiveId = 7020L

        // Inactive Vehicle
        db.vehicleDao().insertVehicle(Vehicle(id = vehicleInactiveId, model = "Mobi Inativo", currentOdometer = 80000.0, active = false))
        // Refill no veículo inativo com preço barato
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = vehicleInactiveId, dateTimestamp = 1000L, dateString = "01/08/2026", gasStation = "Posto Barato", fuelType = "Gasolina", pricePerLiter = 4.29, liters = 30.0, totalPaid = 128.70, odometer = 80000.0, isFullTank = true, paymentMethod = "PIX"))

        // Active Vehicle sem abastecimentos e sem registros
        db.vehicleDao().insertVehicle(Vehicle(id = vehicleActiveId, model = "Titan Ativa", currentOdometer = 15000.0, active = true))
        repository.setActiveVehicle(vehicleActiveId)

        // Caso 1: Veículo ativo com fuelPrice vazio e odometer inteiro sem .0
        viewModel.prefillLaunchFields()
        assertEquals("15000", viewModel.startOdometer.value)
        assertEquals("", viewModel.fuelPrice.value)

        // Caso 2: Veículo ativo com DailyRecord anterior com fuelPrice = 6.30
        repository.insertDailyRecord(
            dateString = "2026-08-01",
            dateTimestamp = 1000L,
            platform = "iFood",
            grossEarnings = 120.0,
            deliveriesCount = 8,
            startOdometer = 15000.0,
            endOdometer = 15080.0,
            fuelPrice = 6.30,
            foodExpense = 0.0
        )
        viewModel.prefillLaunchFields()
        assertEquals("15080", viewModel.startOdometer.value)
        assertEquals("", viewModel.fuelPrice.value)

        // Caso 3: Abastecimento no veículo ativo porém com preço 0.0
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = vehicleActiveId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "Posto Zero", fuelType = "Gasolina", pricePerLiter = 0.0, liters = 10.0, totalPaid = 0.0, odometer = 15090.0, isFullTank = true, paymentMethod = "PIX"))
        viewModel.prefillLaunchFields()
        assertEquals("", viewModel.fuelPrice.value)

        // Caso 4: Abastecimento válido no veículo ativo com preço 5.65 (fuelPrice deve continuar iniciando vazio)
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = vehicleActiveId, dateTimestamp = 3000L, dateString = "03/08/2026", gasStation = "Posto Shell", fuelType = "Gasolina", pricePerLiter = 5.65, liters = 10.0, totalPaid = 56.50, odometer = 15100.0, isFullTank = true, paymentMethod = "PIX"))
        viewModel.prefillLaunchFields()
        assertEquals("", viewModel.fuelPrice.value)
    }

    @Test
    fun testTankCapacityAndAutonomyCalculations() {
        // Validações de capacidade e autonomia teórica vs real
        val moto = Vehicle(
            id = 1L,
            model = "Titan 160",
            vehicleType = "MOTO",
            tankCapacity = 16.1,
            averageConsumption = 35.0
        )
        val carro = Vehicle(
            id = 2L,
            model = "Onix",
            vehicleType = "CARRO",
            tankCapacity = 44.0,
            averageConsumption = 14.0
        )

        // Capacidade
        assertEquals(16.1, moto.tankCapacity, 0.001)
        assertEquals(44.0, carro.tankCapacity, 0.001)

        // Condições de limite para exceder capacidade
        assertFalse(16.1 > moto.tankCapacity)
        assertTrue(16.11 > moto.tankCapacity)
        assertFalse(44.0 > carro.tankCapacity)
        assertTrue(44.1 > carro.tankCapacity)

        // Autonomia Teórica (Tanque Cheio)
        val motoTheoreticalAutonomy = moto.tankCapacity * moto.averageConsumption
        assertEquals(563.5, motoTheoreticalAutonomy, 0.001)

        val carroTheoreticalAutonomy = carro.tankCapacity * carro.averageConsumption
        assertEquals(616.0, carroTheoreticalAutonomy, 0.001)

        // Autonomia com Consumo Real apurado
        val realMotoConsumption = 42.0 // Eficiência real
        val motoRealAutonomy = moto.tankCapacity * realMotoConsumption
        assertEquals(676.2, motoRealAutonomy, 0.001)
        val additionalRangeKm = motoRealAutonomy - motoTheoreticalAutonomy
        assertEquals(112.7, additionalRangeKm, 0.001)
    }

    @Test
    fun testDailyRecordFuelCostPriorityAndOdometerInversionHandled() = runTest(testDispatcher) {
        val testVehicleId = 8801L
        val vehicle = Vehicle(
            id = testVehicleId,
            model = "Kwid",
            averageConsumption = 15.0,
            active = true
        )
        db.vehicleDao().insertVehicle(vehicle)
        repository.setActiveVehicle(testVehicleId)

        // 1. Inserção com odômetro invertido no DailyRecord (start 10000 > end 9950):
        // kmRodados coerceAtLeast(0.0) -> fuelCost = 0.0
        repository.insertDailyRecord(
            dateString = "2026-08-01",
            dateTimestamp = 1000L,
            platform = "Uber",
            grossEarnings = 100.0,
            deliveriesCount = 5,
            startOdometer = 10000.0,
            endOdometer = 9950.0,
            fuelPrice = 5.00,
            foodExpense = 0.0
        )
        val records = repository.allRecordsFlow.first()
        val recordInverted = records.first()
        assertEquals(0.0, CurrencyUtils.centsToDouble(recordInverted.fuelCost), 0.001)
        assertEquals(0.0, CurrencyUtils.centsToDouble(recordInverted.wearCost), 0.001)

        // 2. Atualização corrigindo odômetro para 100 km rodados:
        // Sem abastecimentos reais -> Usa o consumo cadastrado (15.0 km/L)
        // 100 / 15.0 * 6.00 = 40.00
        repository.updateDailyRecord(
            recordId = recordInverted.id,
            dateString = "2026-08-01",
            dateTimestamp = 1000L,
            platform = "Uber",
            grossEarnings = 150.0,
            deliveriesCount = 8,
            startOdometer = 10000.0,
            endOdometer = 10100.0, // 100 km
            fuelPrice = 6.00,
            foodExpense = 10.0
        )
        val updatedRecord1 = db.dailyRecordDao().getRecordById(recordInverted.id)!!
        assertEquals(40.00, CurrencyUtils.centsToDouble(updatedRecord1.fuelCost), 0.001)

        // 3. Adiciona dois abastecimentos cheios gerando consumo real de 20.0 km/L
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 2000L, dateString = "02/08/2026", gasStation = "P1", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 10.0, totalPaid = 60.0, odometer = 10100.0, isFullTank = true, paymentMethod = "PIX"))
        repository.saveFuelRefill(FuelRefill(id = 0L, vehicleId = testVehicleId, dateTimestamp = 3000L, dateString = "03/08/2026", gasStation = "P2", fuelType = "Gasolina", pricePerLiter = 6.00, liters = 10.0, totalPaid = 60.0, odometer = 10300.0, isFullTank = true, paymentMethod = "PIX"))

        // Novo DailyRecord com 100 km rodados:
        // Deve usar consumo real de 20.0 km/L com prioridade!
        // 100 / 20.0 * 6.00 = 30.00 (em vez de 40.00 do teórico)
        repository.insertDailyRecord(
            dateString = "2026-08-04",
            dateTimestamp = 4000L,
            platform = "Uber",
            grossEarnings = 200.0,
            deliveriesCount = 10,
            startOdometer = 10300.0,
            endOdometer = 10400.0,
            fuelPrice = 6.00,
            foodExpense = 0.0
        )
        val recordWithReal = repository.allRecordsFlow.first().first { it.dateTimestamp == 4000L }
        assertEquals(30.00, CurrencyUtils.centsToDouble(recordWithReal.fuelCost), 0.001)
    }
}

