package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.GiroCustoDatabase
import com.example.data.GiroCustoRepository
import com.example.data.SharedPreferencesDataOwnerStorage
import com.example.network.supabase.SupabaseClient
import com.example.ui.GiroCustoViewModel
import com.example.ui.theme.AppTheme
import com.example.ui.theme.getThemeColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppThemeTest {

    private lateinit var app: Application
    private lateinit var inMemoryDb: GiroCustoDatabase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(app, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(inMemoryDb)

        val dataOwnerStorage = SharedPreferencesDataOwnerStorage(app)
        dataOwnerStorage.clearDataOwner()

        app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()

        SupabaseClient.resetForTesting(app).setAuthSession(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        inMemoryDb.close()
        GiroCustoDatabase.resetForTesting()
        app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()
        SupabaseClient.resetForTesting(app).setAuthSession(null)
    }

    private fun createViewModel(): GiroCustoViewModel {
        return GiroCustoViewModel(app)
    }

    @Test
    fun testAppTheme_defaultInitialSelectionIsEmerald() {
        val viewModel = createViewModel()
        assertEquals(AppTheme.EMERALD_GIROCUSTO, viewModel.currentTheme.value)
    }

    @Test
    fun testAppTheme_setThemePersistsToPreferencesAndReloads() {
        val viewModel1 = createViewModel()
        viewModel1.setAppTheme(AppTheme.TITAN_AMBER)

        assertEquals(AppTheme.TITAN_AMBER, viewModel1.currentTheme.value)

        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        assertEquals("TITAN_AMBER", appPrefs.getString("selected_app_theme", null))

        // Recriação de ViewModel deve preservar a escolha
        val viewModel2 = createViewModel()
        assertEquals(AppTheme.TITAN_AMBER, viewModel2.currentTheme.value)

        // Alterna para Orchid Glint
        viewModel2.setAppTheme(AppTheme.ORCHID_GLINT)
        assertEquals(AppTheme.ORCHID_GLINT, viewModel2.currentTheme.value)
        assertEquals("ORCHID_GLINT", appPrefs.getString("selected_app_theme", null))

        // Alterna para Electric Kinetic
        viewModel2.setAppTheme(AppTheme.ELECTRIC_KINETIC)
        assertEquals(AppTheme.ELECTRIC_KINETIC, viewModel2.currentTheme.value)
        assertEquals("ELECTRIC_KINETIC", appPrefs.getString("selected_app_theme", null))

        // Alterna para Solar & Náutico
        viewModel2.setAppTheme(AppTheme.SOLAR_NAUTICO)
        assertEquals(AppTheme.SOLAR_NAUTICO, viewModel2.currentTheme.value)
        assertEquals("SOLAR_NAUTICO", appPrefs.getString("selected_app_theme", null))

        // Alterna de volta para Esmeralda GiroCusto
        viewModel2.setAppTheme(AppTheme.EMERALD_GIROCUSTO)
        assertEquals(AppTheme.EMERALD_GIROCUSTO, viewModel2.currentTheme.value)
        assertEquals("EMERALD_GIROCUSTO", appPrefs.getString("selected_app_theme", null))
    }

    @Test
    fun testSetDarkTheme_persistsToPreferences() {
        val viewModel = createViewModel()
        viewModel.setDarkTheme(false)

        assertFalse(viewModel.isDarkTheme.value)
        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        assertFalse(appPrefs.getBoolean("is_dark_theme", true))

        viewModel.setDarkTheme(true)
        assertTrue(viewModel.isDarkTheme.value)
        assertTrue(appPrefs.getBoolean("is_dark_theme", false))
    }

    @Test
    fun testAppTheme_fromId_caseInsensitiveAndFallback() {
        assertEquals(AppTheme.EMERALD_GIROCUSTO, AppTheme.fromId("EMERALD_GIROCUSTO"))
        assertEquals(AppTheme.EMERALD_GIROCUSTO, AppTheme.fromId("emerald_girocusto"))
        assertEquals(AppTheme.ELECTRIC_KINETIC, AppTheme.fromId("ELECTRIC_KINETIC"))
        assertEquals(AppTheme.ELECTRIC_KINETIC, AppTheme.fromId("electric_kinetic"))
        assertEquals(AppTheme.TITAN_AMBER, AppTheme.fromId("TITAN_AMBER"))
        assertEquals(AppTheme.TITAN_AMBER, AppTheme.fromId("titan_amber"))
        assertEquals(AppTheme.ORCHID_GLINT, AppTheme.fromId("ORCHID_GLINT"))
        assertEquals(AppTheme.ORCHID_GLINT, AppTheme.fromId("orchid_glint"))
        assertEquals(AppTheme.SOLAR_NAUTICO, AppTheme.fromId("SOLAR_NAUTICO"))
        assertEquals(AppTheme.SOLAR_NAUTICO, AppTheme.fromId("solar_nautico"))

        // Fallbacks
        assertEquals(AppTheme.EMERALD_GIROCUSTO, AppTheme.fromId(null))
        assertEquals(AppTheme.EMERALD_GIROCUSTO, AppTheme.fromId(""))
        assertEquals(AppTheme.EMERALD_GIROCUSTO, AppTheme.fromId("DESCONHECIDO"))
    }

    @Test
    fun testGetThemeColorScheme_allThemesProvideValidDarkAndLightSchemes() {
        AppTheme.entries.forEach { theme ->
            val darkScheme = getThemeColorScheme(theme, isDark = true)
            val lightScheme = getThemeColorScheme(theme, isDark = false)

            assertNotNull(darkScheme)
            assertNotNull(lightScheme)
            assertNotNull(darkScheme.primary)
            assertNotNull(darkScheme.secondary)
            assertNotNull(darkScheme.tertiary)
            assertNotNull(lightScheme.primary)
            assertNotNull(lightScheme.secondary)
            assertNotNull(lightScheme.tertiary)
        }
    }
}
