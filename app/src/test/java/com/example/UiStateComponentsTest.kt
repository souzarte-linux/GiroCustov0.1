package com.example

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.example.ui.GiroCustoViewModel
import com.example.ui.SyncState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorNotificationCard
import com.example.ui.components.OfflineNoticeBanner
import com.example.ui.components.SyncLoadingBanner
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ReportsScreen
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
 * Suíte de testes automatizados para os Estados de Interface (T-12 [UX-02]).
 * Valida Empty States com ações rápidas, Banners de Modo Offline, Notificações de Erro com "Tentar novamente"
 * e Indicadores de Carregamento nas telas principais (DashboardScreen, ReportsScreen e HistoryScreen).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class UiStateComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testUiStateStrings_allKeysExistAndAreNonEmptyInPortuguese() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        val stringKeys = listOf(
            R.string.offline_banner_title to "Modo Offline",
            R.string.error_sync_title to "Não foi possível sincronizar",
            R.string.error_retry_btn to "Tentar novamente",
            R.string.error_dismiss_btn to "Dispensar",
            R.string.empty_dashboard_no_records_title to "Nenhum turno lançado ainda",
            R.string.empty_dashboard_filter_title to "Nenhum turno neste período",
            R.string.empty_action_launch to "Lançar Turno",
            R.string.empty_action_refill to "Novo Abastecimento",
            R.string.empty_reports_title to "Sem dados para o relatório",
            R.string.empty_history_title to "Histórico Vazio",
            R.string.empty_history_filter_title to "Nenhum lançamento encontrado",
            R.string.sync_loading_indicator_title to "Sincronizando com a nuvem…"
        )

        for ((resId, expectedVal) in stringKeys) {
            val actualVal = app.getString(resId)
            assertTrue("String resource $resId não pode estar vazia", actualVal.isNotBlank())
            assertEquals("String resource $resId deve coincidir com o texto em português", expectedVal, actualVal)
        }
    }

    @Test
    fun testEmptyStateCard_rendersContentAndHandlesActionsWithMinimumTouchTarget() {
        var primaryClicked = false
        var secondaryClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                EmptyStateCard(
                    title = "Nenhum turno lançado ainda",
                    subtitle = "Comece registrando o seu primeiro turno.",
                    icon = Icons.Default.DirectionsCar,
                    primaryActionText = "Lançar Turno",
                    onPrimaryAction = { primaryClicked = true },
                    secondaryActionText = "Novo Abastecimento",
                    onSecondaryAction = { secondaryClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_state_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nenhum turno lançado ainda").assertIsDisplayed()
        composeTestRule.onNodeWithText("Comece registrando o seu primeiro turno.").assertIsDisplayed()

        // Verificar botão primário e dimensão mínima de toque de 48dp
        val primaryBtn = composeTestRule.onNodeWithTag("empty_state_primary_btn")
        primaryBtn.assertIsDisplayed()
        val primaryBounds = primaryBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Botão primário deve ter altura mínima de 48dp (obtido: ${primaryBounds.height})",
            primaryBounds.height >= 48.dp
        )
        primaryBtn.performClick()
        assertTrue("Callback primário deve ter sido acionado", primaryClicked)

        // Verificar botão secundário e dimensão mínima de toque de 48dp
        val secondaryBtn = composeTestRule.onNodeWithTag("empty_state_secondary_btn")
        secondaryBtn.assertIsDisplayed()
        val secondaryBounds = secondaryBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Botão secundário deve ter altura mínima de 48dp (obtido: ${secondaryBounds.height})",
            secondaryBounds.height >= 48.dp
        )
        secondaryBtn.performClick()
        assertTrue("Callback secundário deve ter sido acionado", secondaryClicked)
    }

    @Test
    fun testOfflineNoticeBanner_rendersWithCorrectTextAndIcon() {
        composeTestRule.setContent {
            MyApplicationTheme {
                OfflineNoticeBanner()
            }
        }

        composeTestRule.onNodeWithTag("offline_notice_banner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Modo Offline").assertIsDisplayed()
    }

    @Test
    fun testErrorNotificationCard_rendersAndTriggersRetryAndDismiss() {
        var retryClicked = false
        var dismissClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ErrorNotificationCard(
                    errorMessage = "Falha ao conectar ao servidor Supabase",
                    onRetry = { retryClicked = true },
                    onDismiss = { dismissClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("error_notification_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Falha ao conectar ao servidor Supabase").assertIsDisplayed()

        val retryBtn = composeTestRule.onNodeWithTag("retry_sync_btn")
        retryBtn.assertIsDisplayed()
        val retryBounds = retryBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Botão de tentar novamente deve ter altura mínima de 48dp (obtido: ${retryBounds.height})",
            retryBounds.height >= 48.dp
        )
        retryBtn.performClick()
        assertTrue("Callback de retry deve ter sido acionado", retryClicked)

        val dismissBtn = composeTestRule.onNodeWithTag("dismiss_error_btn")
        dismissBtn.assertIsDisplayed()
        val dismissBounds = dismissBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Botão de dispensar deve ter área de toque mínima de 48dp (obtido: ${dismissBounds.height})",
            dismissBounds.height >= 48.dp
        )
        dismissBtn.performClick()
        assertTrue("Callback de dismiss deve ter sido acionado", dismissClicked)
    }

    @Test
    fun testSyncLoadingBanner_rendersProgressIndicator() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SyncLoadingBanner()
            }
        }

        composeTestRule.onNodeWithTag("sync_loading_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun testDashboardScreen_rendersEmptyStateWhenRecordsEmpty_andHandlesNavigation() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        var launchNavigated = false
        var refillNavigated = false

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = emptyList(),
                    maintenanceRecords = emptyList(),
                    onNavigateToLaunch = { launchNavigated = true },
                    onNavigateToRefill = { refillNavigated = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Nenhum turno lançado ainda").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_primary_btn").performClick()
        assertTrue("onNavigateToLaunch deve ter sido disparado no Dashboard", launchNavigated)

        composeTestRule.onNodeWithTag("empty_state_secondary_btn").performClick()
        assertTrue("onNavigateToRefill deve ter sido disparado no Dashboard", refillNavigated)
    }

    @Test
    fun testReportsScreen_rendersEmptyStateWhenRecordsEmpty() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        var launchNavigated = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ReportsScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    parts = emptyList(),
                    records = emptyList(),
                    maintenanceRecords = emptyList(),
                    onNavigateToLaunch = { launchNavigated = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Sem dados para o relatório").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_primary_btn").performClick()
        assertTrue("onNavigateToLaunch deve ter sido disparado em ReportsScreen", launchNavigated)
    }

    @Test
    fun testHistoryScreen_rendersEmptyStateWhenRecordsEmpty() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        var launchNavigated = false

        composeTestRule.setContent {
            MyApplicationTheme {
                HistoryScreen(
                    viewModel = viewModel,
                    vehicle = null,
                    records = emptyList(),
                    onNavigateToLaunch = { launchNavigated = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Histórico Vazio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_state_primary_btn").performClick()
        assertTrue("onNavigateToLaunch deve ter sido disparado em HistoryScreen", launchNavigated)
    }
}
