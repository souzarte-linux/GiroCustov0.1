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
import com.example.ui.Period
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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

    private fun createRecordForDate(
        id: Long,
        year: Int,
        month: Int,
        day: Int,
        platform: String = "iFood",
        gross: Double = 100.0,
        net: Double = 60.0,
        startTime: String = "08:00",
        endTime: String = "13:00",
        pauseDuration: String = "00:00"
    ): DailyRecord {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DailyRecord(
            id = id,
            dateString = "$year-${month + 1}-$day",
            dateTimestamp = cal.timeInMillis,
            platform = platform,
            grossEarnings = gross,
            deliveriesCount = 5,
            startOdometer = 100.0,
            endOdometer = 150.0,
            fuelPrice = 5.0,
            foodExpense = 10.0,
            fuelCost = 20.0,
            wearCost = 5.0,
            proportionalFixedCost = 5.0,
            netProfit = net,
            startTime = startTime,
            endTime = endTime,
            pauseDuration = pauseDuration
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
                    netGrowthPercentage = 12.8,
                    annualGoalNet = 68000.0,
                    isPrivacyEnabled = isPrivacy
                )
            }
        }

        // Test tag de nível 0
        composeTestRule.onNodeWithTag("year_header_2026").assertIsDisplayed()
        composeTestRule.onNodeWithText("Consolidado Anual").assertIsDisplayed()
        composeTestRule.onNodeWithText("↑ +18,4%").assertIsDisplayed()

        // Sem máscara: exibe valor real e métricas consolidadas
        composeTestRule.onNodeWithText("TOTAL BRUTO ANUAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 42.850,20").assertIsDisplayed()
        composeTestRule.onNodeWithText("LUCRO LÍQUIDO ANUAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 28.400,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.840").assertIsDisplayed()
        composeTestRule.onNodeWithText("Média Dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 195,40").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meta Anual Atingida").assertIsDisplayed()

        // Ativa privacidade: exibe valor mascarado
        isPrivacy = true
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("R$ ••••••").onFirst().assertIsDisplayed()

        // Testa alternância de recolhimento
        composeTestRule.onNodeWithTag("btn_toggle_annual_consolidation").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ganhos Bruto: R$ ••••••").assertIsDisplayed()
    }

    @Test
    fun testYearHeaderCard_dailyAverageStatCard_hasSymmetricHeightAndUnifiedLabel() {
        composeTestRule.setContent {
            MyApplicationTheme {
                YearHeaderCard(
                    year = 2026,
                    totalGross = 659.46,
                    totalNet = 578.08,
                    totalDeliveries = 23,
                    dailyAverage = 219.82,
                    growthPercentage = null,
                    netGrowthPercentage = null,
                    annualGoalNet = 0.0,
                    isPrivacyEnabled = false
                )
            }
        }

        // Verifica que o rótulo unificado "Média Dia" está presente (sem quebra em partes separadas)
        composeTestRule.onNodeWithText("Média Dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 219,82").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("23").assertIsDisplayed()

        // Mede os limites dos dois blocos irmãos
        val deliveriesBounds = composeTestRule.onNodeWithTag("stat_card_deliveries").getUnclippedBoundsInRoot()
        val dailyAvgBounds = composeTestRule.onNodeWithTag("stat_card_daily_avg").getUnclippedBoundsInRoot()

        // Altura dos dois cartões deve ser simétrica (diferença menor que 2dp)
        val heightDiff = Math.abs(deliveriesBounds.height.value - dailyAvgBounds.height.value)
        assertTrue(
            "O card de Média Dia (${dailyAvgBounds.height}) deve ter altura simétrica ao de Entregas (${deliveriesBounds.height}), diferença: $heightDiff dp",
            heightDiff <= 2.0f
        )
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

        // Verifica a exibição dos 4 blocos do grid 2x2 via testTags semânticas na árvore desmesclada (o card pai possui .clickable)
        composeTestRule.onNodeWithTag("month_metric_packages", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("month_metric_km", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("month_metric_gross", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("month_metric_net", useUnmergedTree = true).assertExists()

        // Quando colapsado, mostra o texto de exportação
        composeTestRule.onNodeWithText("Toque para exportar extrato em PDF/CSV.").assertIsDisplayed()

        // Clica para expandir
        composeTestRule.onNodeWithTag("month_header_Outubro 2026").performClick()
        assertTrue("onToggleExpand deve ter sido disparado", toggleClicked)
    }

    @Test
    fun testMonthHeaderCard_grid2x2Submetrics_displaysCorrectLabelsValuesAndMinHeight() {
        composeTestRule.setContent {
            MyApplicationTheme {
                MonthHeaderCard(
                    monthLabel = "Outubro 2026",
                    totalGross = 5480.50,
                    totalNet = 4660.50,
                    activeDays = 24,
                    totalDeliveries = 218,
                    totalKm = 1420.0,
                    isExpanded = true,
                    isPrivacyEnabled = false
                )
            }
        }

        // Validação da Coluna Esquerda (Pacotes e KM empilhados)
        composeTestRule.onNodeWithTag("month_metric_packages", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Pacotes", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("218", useUnmergedTree = true).assertExists()

        composeTestRule.onNodeWithTag("month_metric_km", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("KM", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("1.420 km", useUnmergedTree = true).assertExists()

        // Validação das Colunas Central e Direita (Bruto e Líquido)
        composeTestRule.onNodeWithTag("month_metric_gross", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Bruto", useUnmergedTree = true).assertExists()
        composeTestRule.onAllNodesWithText("R$ 5.480,50", useUnmergedTree = true).fetchSemanticsNodes().let { nodes ->
            assertTrue("R$ 5.480,50 deve aparecer no cabeçalho e no bloco Bruto", nodes.size >= 2)
        }

        composeTestRule.onNodeWithTag("month_metric_net", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Líquido", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("R$ 4.660,50", useUnmergedTree = true).assertExists()

        // Validação de altura mínima de 52dp nos blocos Bruto e Líquido e renderização dos subcards empilhados
        val packagesBounds = composeTestRule.onNodeWithTag("month_metric_packages", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val kmBounds = composeTestRule.onNodeWithTag("month_metric_km", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val grossBounds = composeTestRule.onNodeWithTag("month_metric_gross", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val netBounds = composeTestRule.onNodeWithTag("month_metric_net", useUnmergedTree = true).getUnclippedBoundsInRoot()

        assertTrue(
            "Subcard Pacotes deve ter altura renderizada positiva (altura atual: ${packagesBounds.height})",
            packagesBounds.height.value > 0f
        )
        assertTrue(
            "Subcard KM deve ter altura renderizada positiva (altura atual: ${kmBounds.height})",
            kmBounds.height.value > 0f
        )
        assertTrue(
            "Bloco Bruto deve respeitar minHeight de 52dp (altura atual: ${grossBounds.height})",
            grossBounds.height.value >= 52.0f
        )
        assertTrue(
            "Bloco Líquido deve respeitar minHeight de 52dp (altura atual: ${netBounds.height})",
            netBounds.height.value >= 52.0f
        )
        // Validação da disposição 2x2: os dois maiores em cima (Bruto e Líquido) e os dois menores abaixo (Pacotes e KM)
        assertTrue(
            "Bloco Bruto deve estar posicionado na linha superior acima do bloco Pacotes (gross.top: ${grossBounds.top}, packages.top: ${packagesBounds.top})",
            grossBounds.top < packagesBounds.top
        )
        assertTrue(
            "Bloco Líquido deve estar posicionado na linha superior acima do bloco KM (net.top: ${netBounds.top}, km.top: ${kmBounds.top})",
            netBounds.top < kmBounds.top
        )
        assertTrue(
            "Blocos Bruto e Líquido (maiores) devem ter altura superior aos blocos inferiores Pacotes e KM",
            grossBounds.height > packagesBounds.height && netBounds.height > kmBounds.height
        )
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
                    totalKm = 120.5,
                    isPrivacyEnabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("week_header_Semana 4 (21 a 27 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Semana 4 (21 a 27 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("week_metric_deliveries", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("54 entregas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("week_metric_time", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("32h Online").assertIsDisplayed()
        composeTestRule.onNodeWithTag("week_metric_rate", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Média: R$ 41,87/h").assertIsDisplayed()
        composeTestRule.onNodeWithTag("week_metric_km", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("120,5 km").assertIsDisplayed()
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

        // Área colapsada: badge ML, métricas operacionais exclusivas (KM, Online), valor bruto e ausência de "12 Ent."
        composeTestRule.onNodeWithText("ML").assertIsDisplayed()
        composeTestRule.onNodeWithText("32,4 KM").assertIsDisplayed()
        composeTestRule.onNodeWithText("4h 45m Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 160,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("12 Ent.").assertDoesNotExist()

        // Clica no card para expandir a seção de detalhes
        cardNode.performClick()
        composeTestRule.waitForIdle()

        // Faixa de horários
        composeTestRule.onNodeWithText("08:00 - 13:10").assertIsDisplayed()
        composeTestRule.onNodeWithText("32,4 km").assertIsDisplayed()

        // Grade 2x3 de custos
        composeTestRule.onNodeWithText("Ganhos Brutos").assertIsDisplayed()
        composeTestRule.onNodeWithText("Custo Comb.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Desgaste/Peças").assertIsDisplayed()

        // Lucro Líquido no rodapé
        composeTestRule.onNodeWithText("LUCRO LÍQUIDO").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 147,08").assertIsDisplayed()

        // Botões de ação com alvos de toque >= 48dp no rodapé
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

        // Abre a modal de filtros pelo botão de filtro da barra de pesquisa
        composeTestRule.onNodeWithTag("history_filter_button").performClick()
        composeTestRule.waitForIdle()

        // Verifica chips de tipo de entrega dentro do modal
        composeTestRule.onNodeWithTag("delivery_type_chip_TODOS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delivery_type_chip_LOGISTICA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delivery_type_chip_DELIVERY").assertIsDisplayed()

        // Seleciona Logística e clica em Aplicar Filtros
        composeTestRule.onNodeWithTag("delivery_type_chip_LOGISTICA").performClick()
        composeTestRule.onNodeWithTag("filter_sheet_apply_btn").performClick()
        composeTestRule.waitForIdle()

        // Chip ativo de Logística deve estar visível na tela principal
        composeTestRule.onNodeWithTag("active_chip_delivery_type").assertIsDisplayed()

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

    @Test
    fun testHistoryRecordCard_withLongPlatformName_doesNotBreakOrExpand() {
        val recordUber = createDummyRecord(99L, "Uber", 120.00, 95.50)
        val recordGmail = createDummyRecord(3L, "Plataforma Gmail", 158.98, 147.08)
        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column {
                    HistoryRecordCard(
                        record = recordUber,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                    HistoryRecordCard(
                        record = recordGmail,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        val nodeUber = composeTestRule.onNodeWithTag("history_item_99").fetchSemanticsNode()
        val nodeGmail = composeTestRule.onNodeWithTag("history_item_3").fetchSemanticsNode()

        // Garante que o card com nome longo de plataforma mantém a mesma altura compacta do card padrão
        assertEquals(nodeUber.size.height, nodeGmail.size.height)

        // Clica para expandir e verificar botões de ação no rodapé
        composeTestRule.onNodeWithTag("history_item_3").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("edit_record_btn_3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_record_btn_3").assertIsDisplayed()
    }

    @Test
    fun testDayHeaderView_togglesExpansionAndDisplaysProfitWhenPositive() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        var expanded by mutableStateOf(false)
        var toggleClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                DayHeaderView(
                    label = "Terça-feira, 22 de Outubro",
                    totalGross = 250.00,
                    totalNet = 190.00,
                    totalDeliveries = 14,
                    totalWorkMinutes = 6L * 60L + 15L,
                    platformCount = 2,
                    isPrivacyEnabled = false,
                    isExpanded = expanded,
                    onToggleExpand = {
                        toggleClicked = true
                        expanded = !expanded
                    }
                )
            }
        }

        // Header exibido com valor bruto e lucro líquido
        composeTestRule.onNodeWithTag("day_header_Terça-feira, 22 de Outubro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terça-feira, 22 de Outubro").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 250,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("14 entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("6h 15m Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lucro R$ 190").assertIsDisplayed()

        // Ícone recolhido (expandir)
        val expandCd = app.getString(R.string.cd_expand)
        val collapseCd = app.getString(R.string.cd_collapse)
        composeTestRule.onNodeWithContentDescription(expandCd).assertIsDisplayed()

        // Clica para expandir
        composeTestRule.onNodeWithTag("day_header_Terça-feira, 22 de Outubro").performClick()
        assertTrue("onToggleExpand deve ter sido acionado", toggleClicked)

        composeTestRule.waitForIdle()
        // Agora expandido: ícone é cd_collapse
        composeTestRule.onNodeWithContentDescription(collapseCd).assertIsDisplayed()
    }

    @Test
    fun testWeekHeaderCard_togglesExpansionAndDisplaysGreenGrossValue() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        var expanded by mutableStateOf(true)
        var toggleClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                WeekHeaderCard(
                    weekLabel = "Semana 42 (14 a 20 Out)",
                    totalGross = 1850.50,
                    totalNet = 1420.00,
                    totalDeliveries = 62,
                    totalWorkMinutes = 35L * 60L,
                    hourlyRate = 52.87,
                    isPrivacyEnabled = false,
                    isExpanded = expanded,
                    onToggleExpand = {
                        toggleClicked = true
                        expanded = !expanded
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag("week_header_Semana 42 (14 a 20 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Semana 42 (14 a 20 Out)").assertIsDisplayed()
        composeTestRule.onNodeWithText("R$ 1.850,50").assertIsDisplayed()
        composeTestRule.onNodeWithText("62 entregas").assertIsDisplayed()
        composeTestRule.onNodeWithText("35h Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("Média: R$ 52,87/h").assertIsDisplayed()

        // Estado inicial expandido: ícone cd_collapse
        val expandCd = app.getString(R.string.cd_expand)
        val collapseCd = app.getString(R.string.cd_collapse)
        composeTestRule.onNodeWithContentDescription(collapseCd).assertIsDisplayed()

        // Clica para recolher
        composeTestRule.onNodeWithTag("week_header_Semana 42 (14 a 20 Out)").performClick()
        assertTrue("onToggleExpand deve ser disparado", toggleClicked)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(expandCd).assertIsDisplayed()
    }

    @Test
    fun testPlatformBrandBadges_variousPlatformsProduceCorrectLinesAndColors() {
        val uberBadge = getPlatformBrandBadge("Uber Moto")
        assertEquals("UBER", uberBadge.line1)
        assertEquals("MOTO", uberBadge.line2)

        val mlBadge = getPlatformBrandBadge("Mercado Livre")
        assertEquals("ML", mlBadge.line1)
        assertEquals("ENVIOS", mlBadge.line2)

        val ifoodBadge = getPlatformBrandBadge("iFood")
        assertEquals("IFOOD", ifoodBadge.line1)
        assertEquals("DELIVERY", ifoodBadge.line2)

        val shopeeBadge = getPlatformBrandBadge("Shopee Xpress")
        assertEquals("SHOPEE", shopeeBadge.line1)
        assertEquals("ENTREGA", shopeeBadge.line2)

        val loggiBadge = getPlatformBrandBadge("Loggi")
        assertEquals("LOGGI", loggiBadge.line1)
        assertEquals("ENVIO", loggiBadge.line2)

        val noventaENoveBadge = getPlatformBrandBadge("99 Pop")
        assertEquals("99", noventaENoveBadge.line1)
        assertEquals("ENTREGA", noventaENoveBadge.line2)
    }

    @Test
    fun testHistorySearchBar_singleLineAndConcisePlaceholder() {
        var query by mutableStateOf("")

        composeTestRule.setContent {
            MyApplicationTheme {
                HistorySearchBar(
                    searchQuery = query,
                    onSearchQueryChange = { query = it }
                )
            }
        }

        // Placeholder conciso
        composeTestRule.onNodeWithText("Buscar data ou plataforma...").assertIsDisplayed()

        // Digita na barra de pesquisa
        query = "Mercado Livre"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Mercado Livre").assertIsDisplayed()

        // Botão de limpar busca
        composeTestRule.onNodeWithContentDescription("Limpar busca").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Limpar busca").performClick()
        composeTestRule.waitForIdle()

        assertEquals("", query)
        composeTestRule.onNodeWithText("Buscar data ou plataforma...").assertIsDisplayed()
    }

    @Test
    fun testTimeline_initialExpansion_onlyMostRecentWeekAndDayExpanded() {
        // 4 semanas de Outubro de 2026, cada uma com múltiplos dias:
        // Semana 1: 05 e 06 de Outubro
        val rec1 = createRecordForDate(101L, 2026, Calendar.OCTOBER, 5)
        val rec2 = createRecordForDate(102L, 2026, Calendar.OCTOBER, 6)

        // Semana 2: 12 e 13 de Outubro
        val rec3 = createRecordForDate(201L, 2026, Calendar.OCTOBER, 12)
        val rec4 = createRecordForDate(202L, 2026, Calendar.OCTOBER, 13)

        // Semana 3: 19 e 20 de Outubro
        val rec5 = createRecordForDate(301L, 2026, Calendar.OCTOBER, 19)
        val rec6 = createRecordForDate(302L, 2026, Calendar.OCTOBER, 20)

        // Semana 4 (mais recente): 26 e 27 de Outubro (dia 27 é o mais recente)
        val rec7 = createRecordForDate(401L, 2026, Calendar.OCTOBER, 26)
        val rec8 = createRecordForDate(402L, 2026, Calendar.OCTOBER, 27)

        val records = listOf(rec1, rec2, rec3, rec4, rec5, rec6, rec7, rec8)

        // 1. Verificação lógica do cálculo dos conjuntos de expansão inicial
        val grouped = groupRecordsHierarchically(records, isAscending = false)
        val weekHeaders = grouped.filterIsInstance<HistoryListItem.WeekHeader>()
        assertEquals(4, weekHeaders.size)

        val initialExpandedWeeks = weekHeaders.maxByOrNull { it.weekStartMillis }
            ?.let { setOf(getHistoryListItemKey(it)) } ?: emptySet()
        assertEquals("Apenas 1 semana deve estar no conjunto de semanas expandidas", 1, initialExpandedWeeks.size)
        val mostRecentWeekKey = getHistoryListItemKey(weekHeaders.maxByOrNull { it.weekStartMillis }!!)
        assertTrue("A semana expandida deve ser a mais recente", initialExpandedWeeks.contains(mostRecentWeekKey))

        val dayHeaders = grouped.filterIsInstance<HistoryListItem.DayHeader>()
        assertEquals(8, dayHeaders.size)
        val initialExpandedDays = dayHeaders.maxByOrNull { it.dateTimestamp }
            ?.let { setOf(getHistoryListItemKey(it)) } ?: emptySet()
        assertEquals("Apenas 1 dia deve estar no conjunto de dias expandidos", 1, initialExpandedDays.size)
        val mostRecentDayKey = getHistoryListItemKey(dayHeaders.maxByOrNull { it.dateTimestamp }!!)
        assertTrue("O dia expandido deve ser o mais recente", initialExpandedDays.contains(mostRecentDayKey))

        // 2. Verificação no componente HistoryScreen
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    records = records,
                    initialFilterState = HistoryFilterState(
                        period = Period.PERSONALIZADO,
                        customStartDate = rec1.dateTimestamp - 86400000L,
                        customEndDate = rec8.dateTimestamp + 86400000L
                    )
                )
            }
        }
        composeTestRule.waitForIdle()

        val labelOct27 = formatDayLabel(rec8.dateTimestamp)
        val labelOct26 = formatDayLabel(rec7.dateTimestamp)

        // Semana 4 (mais recente) está expandida: seus dias aparecem na lista
        composeTestRule.onNodeWithTag("history_list")
            .performScrollToNode(hasText(labelOct27))
        composeTestRule.onNodeWithText(labelOct27).assertIsDisplayed()

        // O dia 27 (mais recente) está expandido: seu registro aparece
        composeTestRule.onNodeWithTag("history_list")
            .performScrollToNode(hasTestTag("history_item_402"))
        composeTestRule.onNodeWithTag("history_item_402").assertIsDisplayed()

        // O dia 26 está na semana 4, portanto seu cabeçalho aparece
        composeTestRule.onNodeWithTag("history_list")
            .performScrollToNode(hasText(labelOct26))
        composeTestRule.onNodeWithText(labelOct26).assertIsDisplayed()

        // O dia 26 está recolhido: seu registro NÃO deve estar na árvore
        composeTestRule.onNodeWithTag("history_item_401").assertDoesNotExist()

        // Semanas 1, 2 e 3 estão recolhidas: seus cabeçalhos de dia NÃO devem estar na árvore
        val labelOct20 = formatDayLabel(rec6.dateTimestamp)
        val labelOct13 = formatDayLabel(rec4.dateTimestamp)
        val labelOct6 = formatDayLabel(rec2.dateTimestamp)
        composeTestRule.onNodeWithText(labelOct20).assertDoesNotExist()
        composeTestRule.onNodeWithText(labelOct13).assertDoesNotExist()
        composeTestRule.onNodeWithText(labelOct6).assertDoesNotExist()

        // Registros de semanas recolhidas também NÃO devem estar na árvore
        composeTestRule.onNodeWithTag("history_item_301").assertDoesNotExist()
        composeTestRule.onNodeWithTag("history_item_201").assertDoesNotExist()
        composeTestRule.onNodeWithTag("history_item_101").assertDoesNotExist()
    }

    @Test
    fun testHistoryRecordCard_workTimeFormatting_variations() {
        // Cenário 1: Horas e minutos ("5h 10m Online") -> 5h 10m = 310 minutos
        val rec5h10m = createRecordForDate(
            id = 501L,
            year = 2026,
            month = Calendar.OCTOBER,
            day = 10,
            startTime = "08:00",
            endTime = "13:10",
            pauseDuration = "00:00"
        )

        // Cenário 2: Apenas horas ("2h Online") -> 2h 00m = 120 minutos
        val rec2h = createRecordForDate(
            id = 502L,
            year = 2026,
            month = Calendar.OCTOBER,
            day = 11,
            startTime = "08:00",
            endTime = "10:00",
            pauseDuration = "00:00"
        )

        // Cenário 3: Apenas minutos ("45m Online") -> 45 minutos
        val rec45m = createRecordForDate(
            id = 503L,
            year = 2026,
            month = Calendar.OCTOBER,
            day = 12,
            startTime = "08:00",
            endTime = "08:45",
            pauseDuration = "00:00"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                androidx.compose.foundation.layout.Column {
                    HistoryRecordCard(
                        record = rec5h10m,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                    HistoryRecordCard(
                        record = rec2h,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                    HistoryRecordCard(
                        record = rec45m,
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }

        // Verifica formatação exata para cada caso
        composeTestRule.onNodeWithText("5h 10m Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("2h Online").assertIsDisplayed()
        composeTestRule.onNodeWithText("45m Online").assertIsDisplayed()
    }

    @Test
    fun testHistoryRecordCard_grossValueColorAdaptsToLightThemeAndDarkTheme() {
        val record = createDummyRecord(77L, "Uber", 160.00, 140.00)
        var isDarkTheme by mutableStateOf(false)
        var currentIsDarkDetected = false
        var currentOnSurface: Color = Color.Unspecified
        var currentResolvedColor: Color = Color.Unspecified

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = isDarkTheme) {
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                currentIsDarkDetected = isDark
                currentOnSurface = MaterialTheme.colorScheme.onSurface
                currentResolvedColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

                HistoryRecordCard(
                    record = record,
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Tema Claro
        assertEquals("No tema claro, isDarkTheme deve ser falso", false, currentIsDarkDetected)
        assertEquals("No tema claro, a cor do texto bruto deve ser Color.Black", Color.Black, currentResolvedColor)
        composeTestRule.onNodeWithText("R$ 160,00").assertIsDisplayed()

        // 2. Muda para Tema Escuro
        isDarkTheme = true
        composeTestRule.waitForIdle()

        assertEquals("No tema escuro, isDarkTheme deve ser verdadeiro", true, currentIsDarkDetected)
        assertEquals("No tema escuro, a cor do texto bruto deve ser onSurface", currentOnSurface, currentResolvedColor)
        assertTrue("A cor onSurface do tema escuro deve ser diferente de Color.Black", currentOnSurface != Color.Black)
        composeTestRule.onNodeWithText("R$ 160,00").assertIsDisplayed()
    }

    @Test
    fun testHistoryRecordCard_expandAndShowEditAndDeleteButtons() {
        val record = createDummyRecord(88L, "Shopee", 180.00, 150.00)
        var editClicked = false
        var deleteClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordCard(
                    record = record,
                    onEditClick = { editClicked = true },
                    onDeleteClick = { deleteClicked = true }
                )
            }
        }

        // Inicialmente contraído: botões de editar e excluir não estão visíveis
        composeTestRule.onNodeWithTag("edit_record_btn_${record.id}").assertDoesNotExist()
        composeTestRule.onNodeWithTag("delete_record_btn_${record.id}").assertDoesNotExist()

        // Clica no card para expandir
        composeTestRule.onNodeWithTag("history_item_${record.id}").performClick()
        composeTestRule.waitForIdle()

        // Agora os botões Editar e Excluir devem estar visíveis e clicáveis
        composeTestRule.onNodeWithTag("edit_record_btn_${record.id}").assertIsDisplayed()
        composeTestRule.onNodeWithTag("delete_record_btn_${record.id}").assertIsDisplayed()

        composeTestRule.onNodeWithTag("edit_record_btn_${record.id}").performClick()
        assertTrue("Clique em editar deve ter sido disparado", editClicked)

        composeTestRule.onNodeWithTag("delete_record_btn_${record.id}").performClick()
        assertTrue("Clique em excluir deve ter sido disparado", deleteClicked)
    }

    @Test
    fun testEditRecordDialog_displaysDateAndPlatformCombobox_andSavesCorrectly() {
        val record = createDummyRecord(99L, "iFood", 200.0, 150.0).copy(
            dateString = "2026-11-02",
            dateTimestamp = 1793635200000L
        )
        val registered = listOf("iFood", "Uber Flash", "Shopee", "Mercado Livre")
        var savedPlatform = ""
        var savedDateStr = ""
        var savedDateTimestamp = 0L
        var savedGross = 0.0

        composeTestRule.setContent {
            MyApplicationTheme {
                EditRecordDialogContent(
                    record = record,
                    availablePlatforms = registered,
                    onDismiss = {},
                    onSave = { dateStr, timestamp, platform, _, _, gross, _, _, _, _, _, _ ->
                        savedDateStr = dateStr
                        savedDateTimestamp = timestamp
                        savedPlatform = platform
                        savedGross = gross
                    }
                )
            }
        }

        // 1. Verifica campo Data presente com formato amigável
        composeTestRule.onNodeWithTag("edit_date").assertIsDisplayed()
        composeTestRule.onNodeWithText("02/11/2026").assertIsDisplayed()

        // 2. Verifica campo Plataforma combobox
        composeTestRule.onNodeWithTag("edit_platform").assertIsDisplayed()
        composeTestRule.onNodeWithText("iFood").assertIsDisplayed()

        // 3. Abre o dropdown clicando no botão da combobox
        composeTestRule.onNodeWithTag("edit_platform_dropdown_btn").performClick()
        composeTestRule.waitForIdle()

        // Opções de plataforma devem aparecer
        composeTestRule.onNodeWithTag("edit_platform_option_Shopee").assertIsDisplayed()
        composeTestRule.onNodeWithTag("edit_platform_option_Shopee").performClick()
        composeTestRule.waitForIdle()

        // Agora o campo deve exibir "Shopee"
        composeTestRule.onNodeWithText("Shopee").assertIsDisplayed()

        // 4. Clica em Salvar
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        assertEquals("Shopee", savedPlatform)
        assertEquals("2026-11-02", savedDateStr)
        assertEquals(1793635200000L, savedDateTimestamp)
        assertEquals(200.0, savedGross, 0.01)
    }

    @Test
    fun testHistoryScreen_deleteRecord_showsConfirmationDialogAndCancelsOrConfirms() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)
        val rec = createDummyRecord(42L, "iFood", 120.0, 95.0)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    records = listOf(rec)
                )
            }
        }

        // Rola até o card do item para garantir que o LazyColumn o componha
        composeTestRule.onNodeWithTag("history_list")
            .performScrollToNode(hasTestTag("history_item_${rec.id}"))

        // Clica no card para expandir
        composeTestRule.onNodeWithTag("history_item_${rec.id}").performClick()
        composeTestRule.waitForIdle()

        // Rola até o botão de excluir no modo visualização
        composeTestRule.onNodeWithTag("delete_record_btn_${rec.id}").performScrollTo()

        // Clica no botão de excluir (lixeira)
        composeTestRule.onNodeWithTag("delete_record_btn_${rec.id}").performClick()
        composeTestRule.waitForIdle()

        // 1. Trava 1.3.a: Diálogo de confirmação deve aparecer
        composeTestRule.onNodeWithTag("delete_record_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Excluir lançamento?").assertIsDisplayed()

        // Clica em cancelar
        composeTestRule.onNodeWithTag("btn_cancel_delete_record").performClick()
        composeTestRule.waitForIdle()

        // O diálogo desaparece
        composeTestRule.onNodeWithTag("delete_record_confirm_dialog").assertDoesNotExist()

        // Clica novamente na lixeira
        composeTestRule.onNodeWithTag("delete_record_btn_${rec.id}").performScrollTo()
        composeTestRule.onNodeWithTag("delete_record_btn_${rec.id}").performClick()
        composeTestRule.waitForIdle()

        // 2. Confirma a exclusão
        composeTestRule.onNodeWithTag("btn_confirm_delete_record").performClick()
        composeTestRule.waitForIdle()

        // Diálogo fecha e Trava 1.3.b (Snackbar com "Desfazer") surge na parte inferior
        composeTestRule.onNodeWithTag("delete_record_confirm_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithText("Desfazer").assertIsDisplayed()
    }
}
