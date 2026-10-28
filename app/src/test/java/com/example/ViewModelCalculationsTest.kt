package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.GiroCustoViewModel
import com.example.ui.LaunchDistanceState
import com.example.ui.Period
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ViewModelCalculationsTest {

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

    // =========================================================================
    // 1. Hodômetro e Distância Diária (LaunchScreen)
    // =========================================================================

    @Test
    fun testCalculateDailyDistance_validPositiveDistance() {
        val result = viewModel.calculateDailyDistance("1000.0", "1050.0")
        assertTrue(result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(50.0, valid.distanceKm, 0.001)
        assertTrue(valid.formattedKm.contains("50"))
    }

    @Test
    fun testCalculateDailyDistance_commaDecimalFormat() {
        val result = viewModel.calculateDailyDistance("1000,5", "1050,7")
        assertTrue(result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(50.2, valid.distanceKm, 0.001)
    }

    @Test
    fun testCalculateDailyDistance_zeroDistance() {
        val result = viewModel.calculateDailyDistance("1200", "1200")
        assertTrue(result is LaunchDistanceState.Valid)
        val valid = result as LaunchDistanceState.Valid
        assertEquals(0.0, valid.distanceKm, 0.001)
    }

    @Test
    fun testCalculateDailyDistance_negativeOdometerError() {
        val result = viewModel.calculateDailyDistance("1100", "1050")
        assertEquals(LaunchDistanceState.NegativeError, result)
    }

    @Test
    fun testCalculateDailyDistance_blankOrInvalidInputs() {
        assertEquals(LaunchDistanceState.None, viewModel.calculateDailyDistance("", "1050"))
        assertEquals(LaunchDistanceState.None, viewModel.calculateDailyDistance("1000", ""))
        assertEquals(LaunchDistanceState.None, viewModel.calculateDailyDistance("abc", "1050"))
        assertEquals(LaunchDistanceState.None, viewModel.calculateDailyDistance("1000", "xyz"))
        assertEquals(LaunchDistanceState.None, viewModel.calculateDailyDistance("   ", "   "))
    }

    @Test
    fun testLaunchCalculatedDistance_reactiveFlowUpdates() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.launchCalculatedDistance.collect()
        }

        viewModel.startOdometer.value = "2000"
        viewModel.endOdometer.value = "2080"
        val state = viewModel.launchCalculatedDistance.value
        assertTrue("Expected Valid state for 2000 -> 2080, got $state", state is LaunchDistanceState.Valid)
        assertEquals(80.0, (state as LaunchDistanceState.Valid).distanceKm, 0.001)

        // Invert odometers -> should reactively emit NegativeError
        viewModel.endOdometer.value = "1950"
        val errState = viewModel.launchCalculatedDistance.value
        assertEquals(LaunchDistanceState.NegativeError, errState)

        // Clear -> should emit None
        viewModel.startOdometer.value = ""
        val noneState = viewModel.launchCalculatedDistance.value
        assertEquals(LaunchDistanceState.None, noneState)

        job.cancel()
    }

    // =========================================================================
    // 2. Abastecimentos e Cálculo Multidirecional (FuelRefillsScreen)
    // =========================================================================

    @Test
    fun testRefillMultiDirectional_twoFields_priceAndLiters_calculatesTotal() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "5.00",
            litersStr = "10.00",
            discountStr = "2.00",
            totalPaidStr = "",
            focusedField = ""
        )
        // Expected total = (5.00 * 10.00) - 2.00 = 48.00
        assertNotNull(result.updatedTotalPaid)
        assertEquals("48.00", result.updatedTotalPaid)
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedLiters)
    }

    @Test
    fun testRefillMultiDirectional_twoFields_litersAndTotal_calculatesPrice() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "",
            litersStr = "10.00",
            discountStr = "0.00",
            totalPaidStr = "50.00",
            focusedField = ""
        )
        // Expected price = 50.00 / 10.00 = 5.00
        assertNotNull(result.updatedPricePerLiter)
        assertEquals("5.00", result.updatedPricePerLiter)
        assertNull(result.updatedLiters)
        assertNull(result.updatedTotalPaid)
    }

    @Test
    fun testRefillMultiDirectional_twoFields_priceAndTotal_calculatesLiters() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "5.00",
            litersStr = "",
            discountStr = "0.00",
            totalPaidStr = "50.00",
            focusedField = ""
        )
        // Expected liters = 50.00 / 5.00 = 10.00
        assertNotNull(result.updatedLiters)
        assertEquals("10.00", result.updatedLiters)
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedTotalPaid)
    }

    @Test
    fun testRefillMultiDirectional_threeFields_focusTotal_recalculatesLiters() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "5.00",
            litersStr = "10.00",
            discountStr = "0.00",
            totalPaidStr = "75.00",
            focusedField = "total"
        )
        // Expected liters = 75.00 / 5.00 = 15.00
        assertEquals("15.00", result.updatedLiters)
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedTotalPaid)
    }

    @Test
    fun testRefillMultiDirectional_threeFields_focusPrice_recalculatesTotal() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "6.00",
            litersStr = "10.00",
            discountStr = "0.00",
            totalPaidStr = "50.00",
            focusedField = "price"
        )
        // Expected total = 6.00 * 10.00 = 60.00
        assertEquals("60.00", result.updatedTotalPaid)
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedLiters)
    }

    @Test
    fun testRefillMultiDirectional_threeFields_focusDiscount_recalculatesTotal() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "5.00",
            litersStr = "10.00",
            discountStr = "5.00",
            totalPaidStr = "50.00",
            focusedField = "discount"
        )
        // Expected total = (5.00 * 10.00) - 5.00 = 45.00
        assertEquals("45.00", result.updatedTotalPaid)
    }

    @Test
    fun testRefillMultiDirectional_invalidOrZeroInputs_returnsEmptyResult() {
        val result = viewModel.calculateRefillMultiDirectional(
            pricePerLiterStr = "0.00",
            litersStr = "",
            discountStr = "0.00",
            totalPaidStr = "",
            focusedField = ""
        )
        assertNull(result.updatedPricePerLiter)
        assertNull(result.updatedLiters)
        assertNull(result.updatedTotalPaid)
    }

    // =========================================================================
    // 3. Capacidade de Tanque e Total de Abastecimento (FuelRefillsScreen)
    // =========================================================================

    @Test
    fun testCheckExceedsTankCapacity_exceedsCapacity_returnsTrue() {
        assertTrue(viewModel.checkExceedsTankCapacity("55.0", 50.0))
        assertTrue(viewModel.checkExceedsTankCapacity("50,5", 50.0))
    }

    @Test
    fun testCheckExceedsTankCapacity_withinOrEqualCapacity_returnsFalse() {
        assertFalse(viewModel.checkExceedsTankCapacity("50.0", 50.0))
        assertFalse(viewModel.checkExceedsTankCapacity("45.0", 50.0))
    }

    @Test
    fun testCheckExceedsTankCapacity_zeroOrInvalidCapacity_returnsFalse() {
        assertFalse(viewModel.checkExceedsTankCapacity("55.0", 0.0))
        assertFalse(viewModel.checkExceedsTankCapacity("", 50.0))
        assertFalse(viewModel.checkExceedsTankCapacity("invalid", 50.0))
    }

    @Test
    fun testComputeRefillTotal_withExplicitTotal_returnsParsedTotal() {
        val total = viewModel.computeRefillTotal("5.00", "10.00", "0.00", "48.50")
        assertEquals(48.50, total, 0.001)
    }

    @Test
    fun testComputeRefillTotal_withBlankTotal_computesPriceTimesLitersMinusDiscount() {
        val total = viewModel.computeRefillTotal("5.00", "10.00", "2.00", "")
        // (5.00 * 10.00) - 2.00 = 48.00
        assertEquals(48.00, total, 0.001)
    }

    @Test
    fun testComputeRefillTotal_negativeResult_coercedToZero() {
        val total = viewModel.computeRefillTotal("5.00", "10.00", "100.00", "")
        assertEquals(0.0, total, 0.001)
    }

    // =========================================================================
    // 4. Métricas e Projeções de Relatórios (ReportsScreen)
    // =========================================================================

    @Test
    fun testCalculateReportMetrics_emptyRecords_returnsZeroedState() {
        val metrics = viewModel.calculateReportMetrics(
            records = emptyList(),
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.SEMANA,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 22,
            monthlyGoalNet = 3000.0
        )

        assertEquals(0, metrics.recordsCount)
        assertEquals(0.0, metrics.totalEarnings, 0.001)
        assertEquals(0.0, metrics.netProfit, 0.001)
        assertEquals(0.0, metrics.totalExpenses, 0.001)
        assertEquals(0.0, metrics.totalKm, 0.001)
        assertNull(metrics.bestDay)
        assertNull(metrics.worstDay)
        assertEquals("0h 00m", metrics.formattedTotalWork)
        assertEquals("0h 00m", metrics.formattedTotalPause)
        assertEquals(0.0, metrics.grossPerHour, 0.001)
        assertEquals(0.0, metrics.netPerHour, 0.001)
        assertEquals(0.0, metrics.projectedMonthlyProfit, 0.001)
        assertEquals(0f, metrics.pctFuel, 0.001f)
    }

    @Test
    fun testCalculateReportMetrics_singleDayFinancialCalculations() {
        val now = System.currentTimeMillis()
        val record = DailyRecord(
            id = 1L,
            dateString = "2026-05-10",
            dateTimestamp = now,
            platform = "iFood",
            grossEarnings = 200.0,
            deliveriesCount = 8,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5.0,
            foodExpense = 15.0,
            fuelCost = 30.0,
            wearCost = 10.0,
            proportionalFixedCost = 5.0,
            netProfit = 140.0,
            startTime = "08:00",
            endTime = "17:00",
            pauseDuration = "01:00"
        )

        val metrics = viewModel.calculateReportMetrics(
            records = listOf(record),
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.SEMANA,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 20,
            monthlyGoalNet = 2800.0
        )

        assertEquals(1, metrics.recordsCount)
        assertEquals(200.0, metrics.totalEarnings, 0.001)
        assertEquals(60.0, metrics.totalExpenses, 0.001) // 30 + 10 + 5 + 15
        assertEquals(140.0, metrics.netProfit, 0.001)
        assertEquals(140.0, metrics.realNetProfit, 0.001)
        assertEquals(100.0, metrics.totalKm, 0.001)
        assertEquals(8, metrics.totalDeliveries)

        // Best and Worst day
        assertEquals(1L, metrics.bestDay?.id)
        assertEquals(1L, metrics.worstDay?.id)

        // Averages per day
        assertEquals(200.0, metrics.avgEarningsPerDay, 0.001)
        assertEquals(140.0, metrics.avgProfitPerDay, 0.001)
        assertEquals(100.0, metrics.avgKmPerDay, 0.001)

        // Work and pause hours
        assertEquals(8.0, metrics.totalWorkHours, 0.001)
        assertEquals(25.0, metrics.grossPerHour, 0.001) // 200 / 8.0
        assertEquals(17.5, metrics.netPerHour, 0.001)   // 140 / 8.0
        assertEquals("8h 00m", metrics.formattedTotalWork)
        assertEquals("1h 00m", metrics.formattedTotalPause)

        // Projection: 140 * 20 days = 2800.0
        assertEquals(2800.0, metrics.projectedMonthlyProfit, 0.001)
        assertEquals(4000.0, metrics.projectedMonthlyEarnings, 0.001) // 200 * 20
        assertEquals(1.0, metrics.pctOfGoal, 0.001) // 2800 / 2800 = 100%

        // Percentage breakdown: total expenses = 60.0
        // Fuel = 30/60 = 50%, Food = 15/60 = 25%, Wear = 10/60 = 16.67%, Fixed = 5/60 = 8.33%
        assertEquals(0.50f, metrics.pctFuel, 0.01f)
        assertEquals(0.25f, metrics.pctFood, 0.01f)
        assertEquals(0.1667f, metrics.pctWear, 0.01f)
        assertEquals(0.0833f, metrics.pctFixed, 0.01f)
        assertEquals(0.0f, metrics.pctMaintenance, 0.001f)
    }

    @Test
    fun testCalculateReportMetrics_multipleDaysBestAndWorst() {
        val now = System.currentTimeMillis()
        val day1 = DailyRecord(
            id = 1L, dateString = "2026-05-10", dateTimestamp = now - 1000,
            platform = "iFood", grossEarnings = 150.0, deliveriesCount = 5,
            startOdometer = 100.0, endOdometer = 150.0, fuelPrice = 5.0,
            foodExpense = 10.0, fuelCost = 20.0, wearCost = 5.0,
            proportionalFixedCost = 5.0, netProfit = 110.0
        )
        val day2 = DailyRecord(
            id = 2L, dateString = "2026-05-11", dateTimestamp = now - 500,
            platform = "iFood", grossEarnings = 350.0, deliveriesCount = 12,
            startOdometer = 150.0, endOdometer = 250.0, fuelPrice = 5.0,
            foodExpense = 15.0, fuelCost = 35.0, wearCost = 10.0,
            proportionalFixedCost = 5.0, netProfit = 285.0
        )
        val day3 = DailyRecord(
            id = 3L, dateString = "2026-05-12", dateTimestamp = now,
            platform = "Uber", grossEarnings = 90.0, deliveriesCount = 3,
            startOdometer = 250.0, endOdometer = 300.0, fuelPrice = 5.0,
            foodExpense = 10.0, fuelCost = 15.0, wearCost = 5.0,
            proportionalFixedCost = 5.0, netProfit = 55.0
        )

        val metrics = viewModel.calculateReportMetrics(
            records = listOf(day1, day2, day3),
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.SEMANA,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 22,
            monthlyGoalNet = 3000.0
        )

        assertEquals(3, metrics.recordsCount)
        assertEquals(2L, metrics.bestDay?.id) // day2 has highest net profit (285.0)
        assertEquals(3L, metrics.worstDay?.id) // day3 has lowest net profit (55.0)
    }

    @Test
    fun testCalculateReportMetrics_withMaintenanceRecords() {
        val now = System.currentTimeMillis()
        val record = DailyRecord(
            id = 1L, dateString = "2026-05-10", dateTimestamp = now,
            platform = "iFood", grossEarnings = 200.0, deliveriesCount = 8,
            startOdometer = 1000.0, endOdometer = 1100.0, fuelPrice = 5.0,
            foodExpense = 10.0, fuelCost = 30.0, wearCost = 10.0,
            proportionalFixedCost = 10.0, netProfit = 140.0
        )
        val maintenance = MaintenanceRecord(
            id = 10L,
            vehicleId = 1L,
            dateTimestamp = now,
            dateString = "2026-05-10",
            description = "Troca de Óleo",
            location = "Oficina Central",
            value = 50.0,
            odometer = 1050.0
        )

        val metrics = viewModel.calculateReportMetrics(
            records = listOf(record),
            maintenanceRecords = listOf(maintenance),
            selectedPeriod = Period.SEMANA,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 20,
            monthlyGoalNet = 2000.0
        )

        assertEquals(50.0, metrics.totalMaintenanceCost, 0.001)
        // totalExpenses = 30 + 10 + 10 + 10 = 60.0
        assertEquals(60.0, metrics.totalExpenses, 0.001)
        // realExpenses = totalExpenses + maintenance = 110.0
        assertEquals(110.0, metrics.realExpenses, 0.001)
        // netProfit = 140.0
        assertEquals(140.0, metrics.netProfit, 0.001)
        // realNetProfit = 200 - 110 = 90.0
        assertEquals(90.0, metrics.realNetProfit, 0.001)
    }

    @Test
    fun testCalculateReportMetrics_periodFiltering_SEMANA_and_PERSONALIZADO() {
        val now = System.currentTimeMillis()
        val eightDaysAgo = now - (8L * 24 * 3600 * 1000)
        val twoDaysAgo = now - (2L * 24 * 3600 * 1000)

        val oldRecord = DailyRecord(
            id = 1L, dateString = "old", dateTimestamp = eightDaysAgo,
            platform = "iFood", grossEarnings = 100.0, deliveriesCount = 3,
            startOdometer = 100.0, endOdometer = 150.0, fuelPrice = 5.0,
            foodExpense = 10.0, fuelCost = 20.0, wearCost = 5.0,
            proportionalFixedCost = 5.0, netProfit = 60.0
        )
        val recentRecord = DailyRecord(
            id = 2L, dateString = "recent", dateTimestamp = twoDaysAgo,
            platform = "iFood", grossEarnings = 200.0, deliveriesCount = 6,
            startOdometer = 150.0, endOdometer = 220.0, fuelPrice = 5.0,
            foodExpense = 10.0, fuelCost = 30.0, wearCost = 5.0,
            proportionalFixedCost = 5.0, netProfit = 150.0
        )

        // In SEMANA filter (last 7 days), oldRecord should be filtered out
        val semanaMetrics = viewModel.calculateReportMetrics(
            records = listOf(oldRecord, recentRecord),
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.SEMANA,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 22,
            monthlyGoalNet = 3000.0
        )
        assertEquals(1, semanaMetrics.recordsCount)
        assertEquals(2L, semanaMetrics.filteredRecords.first().id)

        // In PERSONALIZADO filter covering both dates, both should be present
        val customMetrics = viewModel.calculateReportMetrics(
            records = listOf(oldRecord, recentRecord),
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.PERSONALIZADO,
            customStart = eightDaysAgo - 10000,
            customEnd = now + 10000,
            plannedWorkDays = 22,
            monthlyGoalNet = 3000.0
        )
        assertEquals(2, customMetrics.recordsCount)
    }

    @Test
    fun testSaveDailyRecord_withCommaSeparatedInputs_savesSuccessfully() = runTest {
        // Reproduz exatamente o cenário do usuário:
        // Faturamento: "150,50", Combustível: "5,899", Alimentação: "25,00", Hodômetros: "10050" / "10099", Entregas: "10"
        viewModel.startOdometer.value = "10050"
        viewModel.endOdometer.value = "10099"
        viewModel.grossEarnings.value = "150,50"
        viewModel.deliveriesCount.value = "10"
        viewModel.fuelPrice.value = "5,899"
        viewModel.foodExpense.value = "25,00"
        viewModel.platform.value = "iFood"

        var successCalled = false
        var errorMessage: String? = null

        viewModel.saveDailyRecord(
            onSuccess = { successCalled = true },
            onError = { err -> errorMessage = err }
        )

        assertTrue("Esperava salvar com sucesso, mas falhou com: $errorMessage", successCalled)
        assertNull(errorMessage)
    }
}
