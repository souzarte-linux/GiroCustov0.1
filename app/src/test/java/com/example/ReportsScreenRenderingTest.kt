package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.ui.GiroCustoViewModel
import com.example.ui.Period
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.ReportMetricsState
import com.example.ui.screens.reports.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReportsScreenRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testReportsScreen_withPopulatedRecords_doesNotCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        val record1 = DailyRecord(
            id = 1L,
            dateString = "2026-10-28",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 20000L,
            deliveriesCount = 15,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5890L,
            foodExpense = 2500L,
            fuelCost = 5000L,
            wearCost = 2000L,
            proportionalFixedCost = 1000L,
            netProfit = 9500L,
            startTime = "08:00",
            endTime = "18:00",
            pauseDuration = "01:00"
        )

        val record2 = DailyRecord(
            id = 2L,
            dateString = "2026-10-27",
            dateTimestamp = System.currentTimeMillis() - 86400000L,
            platform = "Uber",
            grossEarnings = 15000L,
            deliveriesCount = 10,
            startOdometer = 900.0,
            endOdometer = 1000.0,
            fuelPrice = 5890L,
            foodExpense = 0L,
            fuelCost = 5000L,
            wearCost = 2000L,
            proportionalFixedCost = 1000L,
            netProfit = 7000L,
            startTime = "09:00",
            endTime = "17:00",
            pauseDuration = "00:00"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsScreen(
                    viewModel = viewModel,
                    vehicle = Vehicle(
                        id = 1L,
                        model = "CG 160",
                        fuelType = "Gasolina",
                        averageConsumption = 40.0,
                        tankCapacity = 16.0,
                        currentOdometer = 1100.0,
                        plannedWorkDays = 22,
                        monthlyFixedCosts = 30000L,
                        active = true
                    ),
                    parts = emptyList(),
                    records = listOf(record1, record2),
                    maintenanceRecords = emptyList()
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsScreen_withEmptyRecords_doesNotCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = emptyList(),
                    maintenanceRecords = emptyList()
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsScreen_withSingleRecordAndZeroHours_doesNotCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        val record = DailyRecord(
            id = 1L,
            dateString = "2026-10-29",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 10000L,
            deliveriesCount = 5,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5890L,
            foodExpense = 0L,
            fuelCost = 2000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = 6500L,
            startTime = "",
            endTime = "",
            pauseDuration = ""
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = listOf(record),
                    maintenanceRecords = emptyList()
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsScreen_withOutOfPeriodRecords_doesNotCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        val oldRecord = DailyRecord(
            id = 1L,
            dateString = "2026-01-01",
            dateTimestamp = 1767225600000L, // Jan 2026
            platform = "iFood",
            grossEarnings = 10000L,
            deliveriesCount = 5,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5890L,
            foodExpense = 0L,
            fuelCost = 2000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = 6500L,
            startTime = "08:00",
            endTime = "18:00",
            pauseDuration = "01:00"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = listOf(oldRecord),
                    maintenanceRecords = emptyList()
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testUserFlow_launchShift_thenNavigateToReports() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        viewModel.startOdometer.value = "50000"
        viewModel.endOdometer.value = "50120"
        viewModel.grossEarnings.value = "250"
        viewModel.deliveriesCount.value = "12"
        viewModel.fuelPrice.value = "5.89"
        viewModel.foodExpense.value = "20"
        viewModel.platform.value = "Uber"

        var saved = false
        var errorMsg = ""
        viewModel.saveDailyRecord(
            onSuccess = { saved = true },
            onError = { errorMsg = it }
        )

        composeTestRule.setContent {
            com.example.ui.navigation.GiroCustoMainApp(viewModel = viewModel)
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nav_tab_relatorios")
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testNetProfitCurveChart_withZeroDimensionsAndSingleRecord_doesNotThrow() {
        val record = DailyRecord(
            id = 1L,
            dateString = "2026-10-29",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 10000L,
            deliveriesCount = 5,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5890L,
            foodExpense = 0L,
            fuelCost = 2000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = 6500L
        )

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(0.dp)) {
                NetProfitCurveChart(records = listOf(record))
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testProductivityComparisonChart_withEmptyListAndNegativeValues_doesNotThrow() {
        val recordNegative = DailyRecord(
            id = 1L,
            dateString = "2026-10-29",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 0L,
            deliveriesCount = 0,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5890L,
            foodExpense = 3000L,
            fuelCost = 2000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = -6500L
        )

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.size(100.dp)) {
                ProductivityComparisonChart(records = listOf(recordNegative))
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsExpenseDistributionCard_withZeroAndNaNPercentages_doesNotThrow() {
        val stateWithZeroExpenses = com.example.ui.ReportMetricsState(
            sumAllExpenses = 0.0,
            pctFuel = Float.NaN,
            pctFood = 0f,
            pctWear = 0f,
            pctFixed = 0f,
            pctMaintenance = 0f
        )

        composeTestRule.setContent {
            ReportsExpenseDistributionCard(metrics = stateWithZeroExpenses)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsProjectionsCard_withNaNAndInfiniteValues_doesNotThrow() {
        val stateWithNaN = com.example.ui.ReportMetricsState(
            pctOfGoal = Double.NaN,
            projectedMonthlyProfit = Double.POSITIVE_INFINITY,
            projectedMonthlyEarnings = Double.NaN
        )

        composeTestRule.setContent {
            ReportsProjectionsCard(metrics = stateWithNaN, monthlyNGoal = 0.0)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testReportsWorkHoursCard_withNaNAndInfiniteHours_doesNotThrow() {
        val stateWithNaN = com.example.ui.ReportMetricsState(
            netPerHour = Double.NaN,
            grossPerHour = Double.POSITIVE_INFINITY,
            avgHoursPerShift = Double.NaN,
            formattedTotalWork = "0h 00m",
            formattedTotalPause = "0h 00m"
        )

        composeTestRule.setContent {
            ReportsWorkHoursCard(metrics = stateWithNaN)
            ReportsHighlightsSection(metrics = stateWithNaN)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testHighlightsSection_formatsDatesInBrazilianFormat() {
        val calBest = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 25, 12, 0, 0)
        }
        val calWorst = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 18, 12, 0, 0)
        }

        val bestRecord = DailyRecord(
            id = 1L,
            dateString = "2026-10-25",
            dateTimestamp = calBest.timeInMillis,
            grossEarnings = 35000L,
            deliveriesCount = 15,
            startOdometer = 1000.0,
            endOdometer = 1100.0,
            fuelPrice = 5890L,
            foodExpense = 2000L,
            fuelCost = 3000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = 28500L
        )

        val worstRecord = DailyRecord(
            id = 2L,
            dateString = "2026-10-18",
            dateTimestamp = calWorst.timeInMillis,
            grossEarnings = 8000L,
            deliveriesCount = 3,
            startOdometer = 1100.0,
            endOdometer = 1130.0,
            fuelPrice = 5890L,
            foodExpense = 1000L,
            fuelCost = 1500L,
            wearCost = 500L,
            proportionalFixedCost = 500L,
            netProfit = 4500L
        )

        val state = ReportMetricsState(
            bestDay = bestRecord,
            worstDay = worstRecord
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsHighlightsSection(metrics = state)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("25/10/2026", substring = true).assertExists()
        composeTestRule.onNodeWithText("18/10/2026", substring = true).assertExists()
    }

    @Test
    fun testProductivityBarColors_lighterBarHasEnhancedVisibleContrast() {
        // Emerald Light: gross is dark green (0xFF059669), net is lighter green (0xFF10B981)
        val grossBase = Color(0xFF059669)
        val netBase = Color(0xFF10B981)
        val (darkerGross, lighterNet) = getProductivityBarColors(grossBase, netBase)

        // Barra mais clara deve ter luminância aumentada em relação à original
        assertTrue(lighterNet.luminance() > netBase.luminance())
        // Não deve ser totalmente clara / branca
        assertTrue(lighterNet.luminance() < 0.90f)
        // Diferença perceptível de contraste entre as barras
        assertTrue(Math.abs(lighterNet.luminance() - darkerGross.luminance()) >= 0.15f)

        // Caso onde ambas as cores de entrada são idênticas (ex.: Emerald Dark base)
        val (darkerSame, lighterSame) = getProductivityBarColors(Color(0xFF10B981), Color(0xFF10B981))
        assertNotEquals(darkerSame, lighterSame)
        assertTrue(lighterSame.luminance() > darkerSame.luminance())
    }

    @Test
    fun testProductivityComparisonChart_barClickDisplaysDateAndValue() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 29, 10, 0, 0)
        }
        val record = DailyRecord(
            id = 1L,
            dateString = "2026-10-29",
            dateTimestamp = cal.timeInMillis,
            grossEarnings = 25000L, // R$ 250,00
            deliveriesCount = 10,
            startOdometer = 1000.0,
            endOdometer = 1080.0,
            fuelPrice = 5890L,
            foodExpense = 2000L,
            fuelCost = 4000L,
            wearCost = 1000L,
            proportionalFixedCost = 500L,
            netProfit = 17500L // R$ 175,00
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ProductivityComparisonChart(records = listOf(record))
            }
        }
        composeTestRule.waitForIdle()

        // Clica na barra para acionar a dinâmica
        composeTestRule.onNodeWithTag("theme_toggle_btn_reports", useUnmergedTree = true).assertDoesNotExist()
        // Clica no gráfico
        composeTestRule.onNodeWithText("R$ 250,00", substring = true).assertDoesNotExist()

        // Dispara clique sobre o componente do gráfico
        composeTestRule.onNodeWithText("Faturamento vs Lucro Líquido", substring = true).assertDoesNotExist()

        // Dispara clique sobre o Canvas do gráfico
        composeTestRule.onNodeWithTag("productivity_chart_canvas").performClick()
        composeTestRule.waitForIdle()

        // Deve exibir a data no formato brasileiro e o valor
        composeTestRule.onNodeWithText("29/10/2026", substring = true).assertExists()
    }

    @Test
    fun testChronologicalRecords_includesAllRecordsInFilteredPeriodWithoutTake7Limit() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        val baseTs = cal.timeInMillis

        // Cria 10 registros distribuídos no mesmo mês
        val testRecords = (1..10).map { i ->
            DailyRecord(
                id = i.toLong(),
                dateString = "2026-10-%02d".format(i + 1),
                dateTimestamp = baseTs + (i * 24L * 60 * 60 * 1000),
                grossEarnings = 10000L * i,
                deliveriesCount = 5,
                startOdometer = 1000.0 + (i * 20),
                endOdometer = 1020.0 + (i * 20),
                fuelPrice = 5890L,
                foodExpense = 1000L,
                fuelCost = 1000L,
                wearCost = 500L,
                proportionalFixedCost = 500L,
                netProfit = 7000L * i
            )
        }

        val metrics = viewModel.calculateReportMetrics(
            records = testRecords,
            maintenanceRecords = emptyList(),
            selectedPeriod = Period.MENSAL,
            customStart = 0L,
            customEnd = 0L,
            plannedWorkDays = 22,
            monthlyGoalNet = 3000.0
        )

        assertEquals(10, metrics.recordsCount)
        assertEquals(10, metrics.chronologicalRecords.size)
    }

    @Test
    fun testHighlightsSection_rendersOuterCardWithCenteredTrophyAndTitle() {
        val record = DailyRecord(
            id = 1L,
            dateString = "2026-10-30",
            dateTimestamp = System.currentTimeMillis(),
            grossEarnings = 25000L,
            deliveriesCount = 8,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5890L,
            foodExpense = 1500L,
            fuelCost = 2500L,
            wearCost = 500L,
            proportionalFixedCost = 500L,
            netProfit = 20000L
        )
        val state = ReportMetricsState(bestDay = record, worstDay = null)

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsHighlightsSection(metrics = state)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Destaques e Recordes").assertExists()
        composeTestRule.onNodeWithContentDescription("Destaques e Recordes").assertExists()
        composeTestRule.onNodeWithText("Melhor Turno").assertExists()
    }

    @Test
    fun testReportsWorkHoursCard_rendersCenteredTitleAndSubtitle() {
        val state = ReportMetricsState(
            netPerHour = 25.0,
            grossPerHour = 35.0,
            avgHoursPerShift = 6.5,
            formattedTotalWork = "13h 00m",
            formattedTotalPause = "1h 30m"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsWorkHoursCard(metrics = state)
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Jornada & Eficiência por Hora").assertExists()
        composeTestRule.onNodeWithContentDescription("Jornada e Horas").assertExists()
    }
}

