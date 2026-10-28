package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.core.app.ApplicationProvider
import com.example.data.DailyRecord
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryRecordCard
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Suíte de testes automatizados para auditoria de Acessibilidade e contentDescription (T-15 [ACC-01]).
 * Valida a existência e integridade de strings de acessibilidade em português,
 * presença de contentDescription nos botões e ícones interativos e áreas mínimas de toque de 48dp x 48dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class AccessibilityContentDescriptionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAccessibilityStrings_allKeysExistAndAreNonEmptyInPortuguese() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val stringKeys = listOf(
            R.string.cd_theme_toggle to "Alternar entre modo claro e escuro",
            R.string.cd_select_period to "Selecionar período de análise",
            R.string.cd_change_custom_period to "Alterar período personalizado",
            R.string.cd_filter_platform to "Filtrar por plataforma",
            R.string.cd_sort_by to "Selecionar ordenação dos lançamentos",
            R.string.cd_edit_record to "Editar lançamento",
            R.string.cd_delete_record to "Excluir lançamento",
            R.string.cd_select_start_time to "Selecionar hora de início",
            R.string.cd_select_end_time to "Selecionar hora de término",
            R.string.cd_select_pause_duration to "Selecionar tempo de pausa",
            R.string.cd_back to "Voltar à tela anterior",
            R.string.cd_expand_options to "Expandir opções de postos",
            R.string.cd_edit_gas_station to "Editar dados do posto",
            R.string.cd_add_new_gas_station to "Cadastrar novo posto",
            R.string.cd_search_nearby_stations to "Buscar postos próximos por GPS",
            R.string.cd_close_station_search to "Fechar busca de postos por GPS",
            R.string.cd_select_brand to "Selecionar bandeira do posto",
            R.string.cd_search_cep to "Buscar endereço pelo CEP",
            R.string.cd_edit_vehicle to "Editar veículo",
            R.string.cd_add_part to "Adicionar nova peça para monitoramento",
            R.string.cd_edit_part to "Editar peça",
            R.string.cd_reset_part_wear to "Zerar desgaste da peça",
            R.string.cd_edit_refill to "Editar abastecimento"
        )

        for ((resId, expectedVal) in stringKeys) {
            val actualVal = app.getString(resId)
            assertTrue("String resource $resId não pode estar vazia", actualVal.isNotBlank())
            assertEquals("String resource $resId deve coincidir com o texto em português", expectedVal, actualVal)
        }
    }

    @Test
    fun testDashboardThemeToggle_hasAccessibilityDescriptionAndMinimumTouchTarget() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = emptyList(),
                    maintenanceRecords = emptyList()
                )
            }
        }

        val themeButton = composeTestRule.onNodeWithTag("theme_toggle_btn")
        themeButton.assertIsDisplayed()

        // Valida que o contentDescription do ícone do botão foi associado com a string correta
        val expectedCd = app.getString(R.string.cd_theme_toggle)
        composeTestRule.onNodeWithContentDescription(expectedCd).assertIsDisplayed()

        // Valida dimensões mínimas de acessibilidade de 48dp x 48dp (com tolerância de arredondamento float)
        val bounds = themeButton.getUnclippedBoundsInRoot()
        assertTrue("Largura do botão de tema deve ser >= 48dp (obtido: ${bounds.width})", bounds.width >= 47.9.dp)
        assertTrue("Altura do botão de tema deve ser >= 48dp (obtido: ${bounds.height})", bounds.height >= 47.9.dp)
    }

    @Test
    fun testHistoryThemeToggleAndFilters_haveAccessibilityDescriptionsAndMinimumTouchTarget() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    records = emptyList()
                )
            }
        }

        val themeButton = composeTestRule.onNodeWithTag("theme_toggle_btn_history")
        themeButton.assertIsDisplayed()

        val expectedThemeCd = app.getString(R.string.cd_theme_toggle)
        composeTestRule.onNodeWithContentDescription(expectedThemeCd).assertIsDisplayed()

        val bounds = themeButton.getUnclippedBoundsInRoot()
        assertTrue("Largura do botão de tema em histórico deve ser >= 48dp (obtido: ${bounds.width})", bounds.width >= 47.9.dp)
        assertTrue("Altura do botão de tema em histórico deve ser >= 48dp (obtido: ${bounds.height})", bounds.height >= 47.9.dp)

        // Valida os contentDescriptions dos dropdowns
        composeTestRule.onNodeWithContentDescription(app.getString(R.string.cd_select_period)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(app.getString(R.string.cd_filter_platform)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(app.getString(R.string.cd_sort_by)).assertIsDisplayed()
    }

    @Test
    fun testHistoryRecordCard_editAndDeleteButtonsHaveCorrectAccessibilityAndTouchBounds() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val dummyRecord = DailyRecord(
            id = 42L,
            dateString = "27/10/2026",
            dateTimestamp = System.currentTimeMillis(),
            platform = "iFood",
            grossEarnings = 150.0,
            deliveriesCount = 8,
            startOdometer = 1000.0,
            endOdometer = 1050.0,
            fuelPrice = 5.89,
            foodExpense = 20.0,
            fuelCost = 25.0,
            wearCost = 10.0,
            proportionalFixedCost = 5.0,
            netProfit = 90.0
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryRecordCard(
                    record = dummyRecord,
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }

        val editBtn = composeTestRule.onNodeWithTag("edit_record_btn_42")
        val deleteBtn = composeTestRule.onNodeWithTag("delete_record_btn_42")

        editBtn.assertIsDisplayed()
        deleteBtn.assertIsDisplayed()

        val editCd = app.getString(R.string.cd_edit_record)
        val deleteCd = app.getString(R.string.cd_delete_record)

        composeTestRule.onNodeWithContentDescription(editCd).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(deleteCd).assertIsDisplayed()

        val editBounds = editBtn.getUnclippedBoundsInRoot()
        val deleteBounds = deleteBtn.getUnclippedBoundsInRoot()

        assertTrue("Botão de editar histórico deve ter largura >= 48dp (obtido: ${editBounds.width})", editBounds.width >= 47.9.dp)
        assertTrue("Botão de editar histórico deve ter altura >= 48dp (obtido: ${editBounds.height})", editBounds.height >= 47.9.dp)

        assertTrue("Botão de excluir histórico deve ter largura >= 48dp (obtido: ${deleteBounds.width})", deleteBounds.width >= 47.9.dp)
        assertTrue("Botão de excluir histórico deve ter altura >= 48dp (obtido: ${deleteBounds.height})", deleteBounds.height >= 47.9.dp)
    }

    @Test
    fun testProfileScreen_actionButtonsHaveAccessibilityDescriptionsAndTouchBounds() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                ProfileScreen(viewModel = viewModel)
            }
        }

        val themeBtn = composeTestRule.onNodeWithTag("theme_toggle_btn_profile")
        themeBtn.assertIsDisplayed()

        val themeBounds = themeBtn.getUnclippedBoundsInRoot()
        assertTrue("Botão de tema em perfil deve ter largura >= 48dp (obtido: ${themeBounds.width})", themeBounds.width >= 47.9.dp)
        assertTrue("Botão de tema em perfil deve ter altura >= 48dp (obtido: ${themeBounds.height})", themeBounds.height >= 47.9.dp)

        val addPartBtn = composeTestRule.onNodeWithTag("profile_add_part_btn")
        addPartBtn.performScrollTo()
        addPartBtn.assertIsDisplayed()

        val addPartBounds = addPartBtn.getUnclippedBoundsInRoot()
        assertTrue("Botão adicionar peça deve ter largura >= 48dp (obtido: ${addPartBounds.width})", addPartBounds.width >= 47.9.dp)
        assertTrue("Botão adicionar peça deve ter altura >= 48dp (obtido: ${addPartBounds.height})", addPartBounds.height >= 47.9.dp)

        val searchCepBtn = composeTestRule.onNodeWithContentDescription(app.getString(R.string.cd_search_cep))
        searchCepBtn.performScrollTo()
        searchCepBtn.assertIsDisplayed()
    }
}
