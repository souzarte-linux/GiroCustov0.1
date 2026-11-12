package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.ui.screens.history.HistoryRecordDetailScreen
import com.example.ui.screens.history.HistoryRecordShareUtils
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class HistoryRecordDetailTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleRecord(
        id: Long = 1L,
        platform: String = "iFood",
        gross: Double = 189.80,
        net: Double = 145.20,
        startOdo: Double = 15350.0,
        endOdo: Double = 15382.4
    ): DailyRecord {
        return DailyRecord(
            id = id,
            dateString = "2026-10-30",
            dateTimestamp = 1793318400000L, // 30/10/2026
            platform = platform,
            grossEarnings = gross,
            deliveriesCount = 5,
            startOdometer = startOdo,
            endOdometer = endOdo,
            fuelPrice = 5.89,
            foodExpense = 15.00,
            fuelCost = 18.50,
            wearCost = 6.10,
            proportionalFixedCost = 5.00,
            netProfit = net,
            startTime = "18:00",
            endTime = "22:45",
            pauseDuration = "00:30"
        )
    }

    @Test
    fun testDetailScreen_topBar_displaysCompanyBadgeInRectangle_routeAndActionButtonsWithoutText() {
        var backClicked = false
        var editClicked = false
        var deleteClicked = false

        val record = sampleRecord(platform = "Mercado Livre")

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = { backClicked = true },
                    onEdit = { editClicked = true },
                    onDelete = { deleteClicked = true },
                    routeName = "Origem: Galpão Cajamar ➔ Destino: Zona Sul SP"
                )
            }
        }

        // Topbar
        composeTestRule.onNodeWithTag("detail_screen_topbar").assertIsDisplayed()

        // 1ª linha: Nome da empresa centralizado envolto em retângulo
        composeTestRule.onNodeWithTag("detail_company_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("MERCADO LIVRE").assertIsDisplayed()

        // 2ª linha: Nome da rota (origem e destino)
        composeTestRule.onNodeWithTag("detail_route_name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Origem: Galpão Cajamar ➔ Destino: Zona Sul SP").assertIsDisplayed()

        // Botões de ação no topo sem texto
        composeTestRule.onNodeWithTag("detail_share_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_edit_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detail_back_btn").assertIsDisplayed()

        // Clique no botão de editar
        composeTestRule.onNodeWithTag("detail_edit_btn").performClick()
        assertTrue("O callback onEdit deve ser acionado", editClicked)

        // Clique no botão de voltar
        composeTestRule.onNodeWithTag("detail_back_btn").performClick()
        assertTrue("O callback onBack deve ser acionado", backClicked)
    }

    @Test
    fun testDetailScreen_cardValorTotalDaRota_standardLayout() {
        val record = sampleRecord(gross = 210.50)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        // Card E: VALOR TOTAL DA ROTA
        composeTestRule.onNodeWithTag("card_total_route_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("VALOR TOTAL DA ROTA").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_total_gross_value").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_total_gross_value").assertTextContains("210,50", substring = true)
    }

    @Test
    fun testDetailScreen_cardValorTotalDaRota_privacyModeMasksValue() {
        val record = sampleRecord(gross = 210.50)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = {},
                    onEdit = {},
                    onDelete = {},
                    isPrivacyEnabled = true
                )
            }
        }

        composeTestRule.onNodeWithTag("route_total_gross_value").assertTextContains("••••••", substring = true)
    }

    @Test
    fun testDetailScreen_cardDistanciaPercorrida_strictOrderOfThreeLines() {
        // startOdo = 15350.0, endOdo = 15382.4, dist = 32.4
        val record = sampleRecord(startOdo = 15350.0, endOdo = 15382.4)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("card_distance_traveled").assertIsDisplayed()
        composeTestRule.onNodeWithText("DISTÂNCIA PERCORRIDA").assertIsDisplayed()

        // I) Linha 1: ODÔMETRO INICIAL: XX.XXX "KM"
        composeTestRule.onNodeWithTag("odo_line_initial").assertIsDisplayed()
        composeTestRule.onNodeWithTag("odo_line_initial").assertTextContains("ODÔMETRO INICIAL: 15.350 KM", substring = true)

        // II) Linha 2: ODÔMETRO FINAL: XX.XXX "KM"
        composeTestRule.onNodeWithTag("odo_line_final").assertIsDisplayed()
        composeTestRule.onNodeWithTag("odo_line_final").assertTextContains("ODÔMETRO FINAL: 15.382,4 KM", substring = true)

        // III) Linha 3: DIST. PERCORRIDO: XX.XXX "KM"
        composeTestRule.onNodeWithTag("odo_line_traveled").assertIsDisplayed()
        composeTestRule.onNodeWithTag("odo_line_traveled").assertTextContains("DIST. PERCORRIDO: 32,4 KM", substring = true)
    }

    @Test
    fun testDetailScreen_subcards_journey_productivity_costs_netProfit() {
        val record = sampleRecord(net = 145.20)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        // Demais cards da sequência mantidos
        composeTestRule.onNodeWithTag("card_shift_time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("HORÁRIO DA JORNADA").assertIsDisplayed()

        composeTestRule.onNodeWithTag("card_deliveries_prod").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("ENTREGAS & PRODUTIVIDADE").assertIsDisplayed()

        composeTestRule.onNodeWithTag("card_operating_costs").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("CUSTOS OPERACIONAIS").assertIsDisplayed()

        composeTestRule.onNodeWithTag("card_net_result").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("LUCRO LÍQUIDO").assertIsDisplayed()
        composeTestRule.onNodeWithTag("route_net_value").assertTextContains("145,20", substring = true)
    }

    @Test
    fun testDetailScreen_shareDialog_opensAndOffersImageAndPdfOptions() {
        val record = sampleRecord()

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordDetailScreen(
                    record = record,
                    onBack = {},
                    onEdit = {},
                    onDelete = {}
                )
            }
        }

        // Toca no botão compartilhar
        composeTestRule.onNodeWithTag("detail_share_btn").performClick()

        // O modal de escolha surge
        composeTestRule.onNodeWithTag("share_format_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_option_image").assertIsDisplayed()
        composeTestRule.onNodeWithTag("share_option_pdf").assertIsDisplayed()
    }

    @Test
    fun testShareUtils_formatOdometer_handlesDecimalsAndIntegersCorrectly() {
        assertEquals("15.350", HistoryRecordShareUtils.formatOdometer(15350.0))
        assertEquals("15.382,4", HistoryRecordShareUtils.formatOdometer(15382.4))
        assertEquals("0", HistoryRecordShareUtils.formatOdometer(0.0))
        assertEquals("1.250.000", HistoryRecordShareUtils.formatOdometer(1250000.0))
    }

    @Test
    fun testShareUtils_generatesPdfAndImageFiles() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val record = sampleRecord(id = 99L, platform = "Shopee")

        val pdfFile = HistoryRecordShareUtils.generateRecordPdf(
            context = context,
            record = record,
            routeName = "Origem: Hub Shopee ➔ Destino: Bairro Centro"
        )
        assertNotNull(pdfFile)
        assertTrue("Arquivo PDF deve existir", pdfFile.exists())
        assertTrue("Arquivo PDF não deve ser vazio", pdfFile.length() > 0)
        assertTrue("Arquivo deve ter extensão .pdf", pdfFile.name.endsWith(".pdf"))

        val imageFile = HistoryRecordShareUtils.generateRecordImage(
            context = context,
            record = record,
            routeName = "Origem: Hub Shopee ➔ Destino: Bairro Centro"
        )
        assertNotNull(imageFile)
        assertTrue("Arquivo de imagem deve existir", imageFile.exists())
        assertTrue("Arquivo de imagem não deve ser vazio", imageFile.length() > 0)
        assertTrue("Arquivo deve ter extensão .png", imageFile.name.endsWith(".png"))
    }
}
