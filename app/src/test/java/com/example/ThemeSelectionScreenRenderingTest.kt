package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GiroCustoDatabase
import com.example.data.GiroCustoRepository
import com.example.ui.GiroCustoViewModel
import com.example.ui.navigation.AppDrawer
import com.example.ui.navigation.DrawerDestination
import com.example.ui.screens.ThemeSelectionScreen
import com.example.ui.theme.AppTheme
import com.example.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [34])
class ThemeSelectionScreenRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: GiroCustoDatabase
    private lateinit var repository: GiroCustoRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
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
        db.close()
        GiroCustoDatabase.resetForTesting()
    }

    @Test
    fun testThemeSelectionScreen_rendersAllComponentsAndSelectsThemes() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        var backClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ThemeSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verifica existência dos elementos estruturais
        composeTestRule.onNodeWithTag("theme_preview_card").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_mode_toggle_switch").assertExists().assertIsDisplayed()

        // Verifica os 5 cartões de tema
        composeTestRule.onNodeWithTag("theme_card_EMERALD_GIROCUSTO").assertExists()
        composeTestRule.onNodeWithTag("theme_card_ELECTRIC_KINETIC").assertExists()
        composeTestRule.onNodeWithTag("theme_card_TITAN_AMBER").assertExists()
        composeTestRule.onNodeWithTag("theme_card_ORCHID_GLINT").assertExists()
        composeTestRule.onNodeWithTag("theme_card_SOLAR_NAUTICO").assertExists()

        // Esmeralda deve estar inicialmente selecionado
        composeTestRule.onNodeWithTag("theme_card_EMERALD_GIROCUSTO").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_EMERALD_GIROCUSTO", useUnmergedTree = true).assertIsSelected()

        // Clica no card Titan Amber
        composeTestRule.onNodeWithTag("theme_card_TITAN_AMBER").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppTheme.TITAN_AMBER, viewModel.currentTheme.value)
        composeTestRule.onNodeWithTag("theme_card_TITAN_AMBER").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_TITAN_AMBER", useUnmergedTree = true).assertIsSelected()

        // Clica no card Orchid Glint
        composeTestRule.onNodeWithTag("theme_card_ORCHID_GLINT").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppTheme.ORCHID_GLINT, viewModel.currentTheme.value)
        composeTestRule.onNodeWithTag("theme_card_ORCHID_GLINT").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_ORCHID_GLINT", useUnmergedTree = true).assertIsSelected()

        // Clica no card Solar & Náutico
        composeTestRule.onNodeWithTag("theme_card_SOLAR_NAUTICO").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppTheme.SOLAR_NAUTICO, viewModel.currentTheme.value)
        composeTestRule.onNodeWithTag("theme_card_SOLAR_NAUTICO").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_SOLAR_NAUTICO", useUnmergedTree = true).assertIsSelected()

        // Clica no card Electric Kinetic
        composeTestRule.onNodeWithTag("theme_card_ELECTRIC_KINETIC").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppTheme.ELECTRIC_KINETIC, viewModel.currentTheme.value)
        composeTestRule.onNodeWithTag("theme_card_ELECTRIC_KINETIC").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_ELECTRIC_KINETIC", useUnmergedTree = true).assertIsSelected()

        // Clica no card Esmeralda GiroCusto
        composeTestRule.onNodeWithTag("theme_card_EMERALD_GIROCUSTO").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals(AppTheme.EMERALD_GIROCUSTO, viewModel.currentTheme.value)
        composeTestRule.onNodeWithTag("theme_card_EMERALD_GIROCUSTO").assertIsSelected()
        composeTestRule.onNodeWithTag("theme_radio_EMERALD_GIROCUSTO", useUnmergedTree = true).assertIsSelected()
    }

    @Test
    fun testThemeSelectionScreen_backButtonInvokesCallback() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)
        var backClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                ThemeSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("theme_screen_back_btn").performClick()
        composeTestRule.waitForIdle()

        org.junit.Assert.assertTrue("Callback de voltar deve ser invocado ao clicar no botão de voltar", backClicked)
    }

    @Test
    fun testThemeSelectionScreen_darkModeToggleSwitch() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = GiroCustoViewModel(app)

        composeTestRule.setContent {
            MyApplicationTheme {
                ThemeSelectionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        val initialDarkMode = viewModel.isDarkTheme.value
        composeTestRule.onNodeWithTag("theme_mode_toggle_switch").performClick()
        composeTestRule.waitForIdle()

        assertEquals(!initialDarkMode, viewModel.isDarkTheme.value)
    }

    @Test
    fun testAppDrawer_rendersThemesDestinationItem() {
        var selectedDestination: DrawerDestination = DrawerDestination.NONE

        composeTestRule.setContent {
            MyApplicationTheme {
                val drawerState = androidx.compose.material3.rememberDrawerState(
                    initialValue = androidx.compose.material3.DrawerValue.Open
                )
                AppDrawer(
                    drawerState = drawerState,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { selectedDestination = it },
                    content = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verifica que o item Temas existe no drawer com a tag correta
        composeTestRule.onNodeWithTag("drawer_item_themes").assertExists()
        composeTestRule.onNodeWithTag("drawer_item_themes").performClick()

        assertEquals(DrawerDestination.TEMAS, selectedDestination)
    }

    @Test
    fun testAppDrawer_scrollableAndThemesItemDisplayed() {
        var selectedDestination: DrawerDestination = DrawerDestination.NONE

        composeTestRule.setContent {
            MyApplicationTheme {
                val drawerState = androidx.compose.material3.rememberDrawerState(
                    initialValue = androidx.compose.material3.DrawerValue.Open
                )
                AppDrawer(
                    drawerState = drawerState,
                    selectedDestination = selectedDestination,
                    onDestinationSelected = { selectedDestination = it },
                    content = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Valida que o container do Drawer é rolável (hasScrollAction)
        composeTestRule.onNode(hasScrollAction()).assertExists()

        // 2. Valida que o item de temas existe e está visível
        composeTestRule.onNodeWithTag("drawer_item_themes")
            .assertExists()
            .assertIsDisplayed()

        // 3. Valida que o item possui o rótulo claro e destacado "Temas e Aparência"
        composeTestRule.onNodeWithText("Temas e Aparência")
            .assertExists()
            .assertIsDisplayed()
    }
}
