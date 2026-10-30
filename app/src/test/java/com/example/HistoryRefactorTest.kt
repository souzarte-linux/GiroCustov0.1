package com.example

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.*
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class HistoryRefactorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createDummyRecord(id: Long, platform: String, gross: Double, net: Double): DailyRecord {
        val now = System.currentTimeMillis()
        return DailyRecord(
            id = id,
            dateString = "2026-10-23",
            dateTimestamp = now - 60000L,
            platform = platform,
            grossEarnings = gross,
            deliveriesCount = 12,
            startOdometer = 100.0,
            endOdometer = 132.4,
            fuelPrice = 5.5,
            foodExpense = 15.0,
            fuelCost = 12.0,
            wearCost = 6.0,
            proportionalFixedCost = 4.0,
            netProfit = net,
            startTime = "08:00",
            endTime = "13:10",
            pauseDuration = "00:25"
        )
    }

    @Test
    fun testYearHeaderCard_rendersCorrectlyWithAndWithoutPrivacyMask() {
        var isPrivacy by mutableStateOf(false)

        composeTestRule.setContent {
            MyApplicationTheme {
                YearHeaderCard(
                    year = 2026,
                    totalGross = 42850.20,
                    totalNet = 28400.00,
                    totalDeliveries = 1840,
                    dailyAverage = 195.40,
                    growthPercentage = 18.4,
                    isPrivacyEnabled = isPrivacy
                )
            }
        }

        // Test tag de nível 0
        composeTestRule.onNodeWithTag("year_header_2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("RESUMO CONSOLIDADO 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("+18,4% vs 2025").assertIsDisplayed()

        // Sem máscara: exibe valor real
        composeTestRule.onNodeWithText("R$ 42.850,20").assertIsDisplayed()

        // Ativa privacidade: exibe valor mascarado
        isPrivacy = true
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("R$ ••••••").onFirst().assertIsDisplayed()
    }

    @Test
    fun testMonthHeaderCard_togglesExpansionAndDisplaysSubmetrics() {
        var expanded by mutableStateOf(false)
        var toggleClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                MonthHeaderCard(
                    monthLabel = "Outubro 2026",
                    totalGross = 5480.50,
                    totalNet = 4660.50,
                    activeDays = 24,
                    totalDeliveries = 218,
                    totalKm = 1420.0,
                    isExpanded = expanded,
                    isPrivacyEnabled = false,
                    onToggleExpand = {
                        toggleClicked = true
                        expanded = !expanded
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("month_header_Outubro 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Outubro 2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("24 dias em rota ativa").assertIsDisplayed()

        // Quando colapsado, mostra o texto de exportação
        composeTestRule.onNodeWithText("Toque para exportar extrato em PDF/CSV.").assertIsDisplayed()

        // Clica para expandir
        composeTestRule.onNodeWithTag("month_header_Outubro 2026").performClick()
        assertTrue("onToggleExpand deve ter sido disparado", toggleClicked)
    }

    @Test
    fun testWeekHeaderCard_displaysWeekLabelAndHourlyStats() {
        composeTestRule.setContent {
            MyApplicationTheme {
                WeekHeaderCard(
                    weekLabel = "Semana 4 (21 a 27 Out)",
                    totalGross = 1340.00,
                    totalNet = 980.00,
                    totalDeliveries = 54,
                    totalWorkMinutes = 32L * 60L,
                    hourlyRate = 41.87,
                    isPrivacyEnabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("week_header_Semana 4 (21 a 27 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Semana 4 (21 a 27 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithText("54 entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("32h Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("Média: R$ 41,87/h").assertIsDisplayed()
    }

    @Test
    fun testDayHeaderView_displaysFullDayLabelAndGlanceSummary() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DayHeaderView(
                    label = "Quarta-feira, 23 de Outubro",
                    totalGross = 320.00,
                    totalDeliveries = 18,
                    totalWorkMinutes = 8L * 60L + 20L,
                    platformCount = 3,
                    isPrivacyEnabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("day_header_Quarta-feira, 23 de Outubro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quarta-feira, 23 de Outubro").assertIsDisplayed()
        composeTestRule.onNodeWithText("18 entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("8h 20m Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 plataformas").assertIsDisplayed()
    }

    @Test
    fun testHistoryRecordCard_expandDetailsAndPlatformColors() {
        val record = createDummyRecord(42L, "Mercado Livre", 160.00, 147.08)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordCard(
                    record = record,
                    onEditClick = {},
                    onDeleteClick = {},
                    isPrivacyEnabled = false
                )
            }
        }

        val cardNode = composeTestRule.onNodeWithTag("history_item_42")
        cardNode.assertIsDisplayed()

        // Top row
        composeTestRule.onNodeWithText("Mercado Livre").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 Entreg.").assertIsDisplayed()

        // Faixa de horários
        composeTestRule.onNodeWithText("08:00 - 13:10").assertIsDisplayed()
        composeTestRule.onNodeWithText("32,4 km").assertIsDisplayed()

        // Botões de ação com alvos de toque >= 48dp
        val editBtn = composeTestRule.onNodeWithTag("edit_record_btn_42")
        editBtn.assertIsDisplayed()
        val editBounds = editBtn.getUnclippedBoundsInRoot()
        assertTrue("Largura do botão editar deve ser >= 48dp", editBounds.width >= 47.9.dp)
        assertTrue("Altura do botão editar deve ser >= 48dp", editBounds.height >= 47.9.dp)

        val deleteBtn = composeTestRule.onNodeWithTag("delete_record_btn_42")
        deleteBtn.assertIsDisplayed()
        val deleteBounds = deleteBtn.getUnclippedBoundsInRoot()
        assertTrue("Largura do botão excluir deve ser >= 48dp", deleteBounds.width >= 47.9.dp)
        assertTrue("Altura do botão excluir deve ser >= 48dp", deleteBounds.height >= 47.9.dp)

        // Lucro Líquido no rodapé
        composeTestRule.onNodeWithText("LUCRO LÍQUIDO").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 147,08").assertIsDisplayed()

        // Clica no card para expandir a grade 2x3
        cardNode.performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Ganhos Brutos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custo Combustível").assertIsDisplayed()
        composeTestRule.onNodeWithText("Desgaste/Peças").assertIsDisplayed()
    }

    @Test
    fun testHistoryScreen_privacyToggleAndFiltering() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)
        val rec1 = createDummyRecord(1L, "Mercado Livre", 160.0, 140.0)
        val rec2 = createDummyRecord(2L, "iFood", 80.0, 65.0)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    records = listOf(rec1, rec2)
                )
            }
        }

        // Verifica o botão de privacidade
        val privacyBtn = composeTestRule.onNodeWithTag("privacy_toggle_btn_history")
        privacyBtn.assertIsDisplayed()
        val pBounds = privacyBtn.getUnclippedBoundsInRoot()
        assertTrue("Botão privacidade largura >= 48dp", pBounds.width >= 47.9.dp)
        assertTrue("Botão privacidade altura >= 48dp", pBounds.height >= 47.9.dp)

        // Alterna privacidade
        privacyBtn.performClick()
        composeTestRule.waitForIdle()

        // Verifica chips de filtro rápido
        composeTestRule.onNodeWithText("Todos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Logística (ML/Shopee)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delivery/Apps (iFood/Uber)").assertIsDisplayed()

        // Clica no chip Logística
        composeTestRule.onNodeWithText("Logística (ML/Shopee)").performClick()
        composeTestRule.waitForIdle()

        // Lançamento Mercado Livre deve estar presente
        composeTestRule.onNodeWithTag("history_list")
            .performScrollToNode(hasTestTag("history_item_1"))
        composeTestRule.onNodeWithTag("history_item_1").assertIsDisplayed()
    }

    @Test
    fun testHistoryCardsAndComponents_renderInAllThemesHarmoniously() {
        val record = createDummyRecord(99L, "Uber", 120.00, 95.50)
        val themeVariations = AppTheme.entries.flatMap { listOf(it to true, it to false) }

        composeTestRule.setContent {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                for ((theme, isDark) in themeVariations) {
                    val mode = if (isDark) "dark" else "light"
                    MyApplicationTheme(appTheme = theme, darkTheme = isDark) {
                        androidx.compose.foundation.layout.Column(
                            modifier = androidx.compose.ui.Modifier.testTag("theme_container_${theme.name}_$mode")
                        ) {
                            YearHeaderCard(
                                year = 2026,
                                totalGross = 10000.0,
                                totalNet = 8000.0,
                                totalDeliveries = 500,
                                dailyAverage = 200.0
                            )
                            MonthHeaderCard(
                                monthLabel = "Outubro 2026",
                                totalGross = 2500.0,
                                totalNet = 2000.0,
                                activeDays = 15,
                                totalDeliveries = 120,
                                totalKm = 450.0
                            )
                            WeekHeaderCard(
                                weekLabel = "Semana 42",
                                totalGross = 600.0,
                                totalNet = 480.0,
                                totalDeliveries = 30,
                                totalWorkMinutes = 1200L,
                                hourlyRate = 24.0
                            )
                            DayHeaderView(
                                label = "23 de Outubro",
                                totalGross = 120.0,
                                totalDeliveries = 6,
                                totalWorkMinutes = 240L,
                                platformCount = 1
                            )
                            HistoryRecordCard(
                                record = record,
                                onEditClick = {},
                                onDeleteClick = {}
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        for ((theme, isDark) in themeVariations) {
            val mode = if (isDark) "dark" else "light"
            composeTestRule.onNodeWithTag("theme_container_${theme.name}_$mode", useUnmergedTree = true).assertExists()
        }
    }
}
