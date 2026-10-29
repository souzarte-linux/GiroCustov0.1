package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.data.Vehicle
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.ReportsScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
