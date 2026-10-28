package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.util.CurrencyUtils
import com.example.network.supabase.AuthSession
import com.example.network.supabase.SupabaseClient
import com.example.ui.GiroCustoViewModel
import com.example.ui.screens.formatKmGoal
import com.example.ui.screens.formatMoneyGoal
import com.example.ui.screens.parsePtBrDouble
import com.example.ui.screens.parsePtBrInt
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserGoalsViewModelAndUiTest {

    private lateinit var app: Application
    private lateinit var inMemoryDb: GiroCustoDatabase
    private lateinit var userGoalsDao: UserGoalsDao
    private lateinit var dataOwnerStorage: DataOwnerStorage
    private lateinit var repository: GiroCustoRepository
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
        userGoalsDao = inMemoryDb.userGoalsDao()
        dataOwnerStorage = SharedPreferencesDataOwnerStorage(app)
        dataOwnerStorage.clearDataOwner()
        repository = GiroCustoRepository(inMemoryDb, dataOwnerStorage, testDispatcher)

        // Limpa arquivos de preferências
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
        return GiroCustoViewModel(app, dataOwnerStorage, repository)
    }

    // =========================================================================
    // 1. DESACOPLAMENTO DO TEMA ESCURO
    // =========================================================================

    @Test
    fun testInitialDarkTheme_readsFromDedicatedPreferences() {
        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        appPrefs.edit().putBoolean("is_dark_theme", false).commit()

        val viewModel = createViewModel()
        assertFalse("Tema deve ser lido de giro_custo_preferences", viewModel.isDarkTheme.value)
    }

    @Test
    fun testInitialDarkTheme_fallsBackToLegacyGoals_andMigratesToAppPrefs() {
        val legacyPrefs = app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        legacyPrefs.edit().putBoolean("is_dark_theme", false).commit()

        val viewModel = createViewModel()
        assertFalse("Tema deve ter feito fallback de giro_custo_goals", viewModel.isDarkTheme.value)

        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        assertTrue("giro_custo_preferences deve conter is_dark_theme após migração", appPrefs.contains("is_dark_theme"))
        assertFalse("is_dark_theme gravado em giro_custo_preferences deve ser false", appPrefs.getBoolean("is_dark_theme", true))
    }

    @Test
    fun testToggleTheme_persistsToDedicatedPreferences() {
        val viewModel = createViewModel()
        assertTrue("Padrão inicial é true", viewModel.isDarkTheme.value)

        viewModel.toggleTheme()
        assertFalse("isDarkTheme StateFlow deve ser false após toggle", viewModel.isDarkTheme.value)

        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        assertFalse("giro_custo_preferences deve persistir false", appPrefs.getBoolean("is_dark_theme", true))

        viewModel.toggleTheme()
        assertTrue("isDarkTheme StateFlow deve voltar a ser true", viewModel.isDarkTheme.value)
        assertTrue("giro_custo_preferences deve persistir true", appPrefs.getBoolean("is_dark_theme", false))
    }

    // =========================================================================
    // 2. INTEGRAÇÃO COM USER_GOALS FLOW E ATUALIZAÇÃO
    // =========================================================================

    @Test
    fun testGoalsCollection_updatesStateFlowsFromDatabaseFlow() = runTest(testDispatcher) {
        // Insere metas personalizadas no Room
        userGoalsDao.insertOrUpdateUserGoals(
            UserGoals(
                id = 1,
                dailyGoalGross = 350.0,
                dailyGoalNet = 220.0,
                dailyGoalKm = 150.0,
                dailyGoalDeliveries = 22,
                weeklyGoalGross = 2000.0,
                weeklyGoalNet = 1300.0,
                weeklyGoalKm = 900.0,
                weeklyGoalDeliveries = 130,
                monthlyGoalGross = 8000.0,
                monthlyGoalNet = 5200.0,
                monthlyGoalKm = 3500.0,
                monthlyGoalDeliveries = 500
            )
        )

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        assertEquals(350.0, viewModel.dailyGoalGross.value, 0.001)
        assertEquals(220.0, viewModel.dailyGoalNet.value, 0.001)
        assertEquals(150.0, viewModel.dailyGoalKm.value, 0.001)
        assertEquals(22, viewModel.dailyGoalDeliveries.value)

        assertEquals(2000.0, viewModel.weeklyGoalGross.value, 0.001)
        assertEquals(1300.0, viewModel.weeklyGoalNet.value, 0.001)
        assertEquals(900.0, viewModel.weeklyGoalKm.value, 0.001)
        assertEquals(130, viewModel.weeklyGoalDeliveries.value)

        assertEquals(8000.0, viewModel.monthlyGoalGross.value, 0.001)
        assertEquals(5200.0, viewModel.monthlyGoalNet.value, 0.001)
        assertEquals(3500.0, viewModel.monthlyGoalKm.value, 0.001)
        assertEquals(500, viewModel.monthlyGoalDeliveries.value)
    }

    @Test
    fun testUpdateGoals_updatesStateFlowsImmediatelyAndPersistsToRoom() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        viewModel.updateGoals(
            dailyG = 250.0, dailyN = 160.0, dailyK = 110.0, dailyD = 18,
            weeklyG = 1500.0, weeklyN = 950.0, weeklyK = 650.0, weeklyD = 105,
            monthlyG = 6000.0, monthlyN = 3800.0, monthlyK = 2800.0, monthlyD = 420
        )

        // Resposta imediata (0ms)
        assertEquals(250.0, viewModel.dailyGoalGross.value, 0.001)
        assertEquals(160.0, viewModel.dailyGoalNet.value, 0.001)
        assertEquals(110.0, viewModel.dailyGoalKm.value, 0.001)
        assertEquals(18, viewModel.dailyGoalDeliveries.value)

        // Aguarda execução da corrotina no Room
        testScheduler.advanceUntilIdle()

        val persisted = userGoalsDao.getUserGoals()
        assertNotNull("Metas devem ter sido persistidas no Room", persisted)
        assertEquals(250.0, CurrencyUtils.centsToDouble(persisted!!.dailyGoalGross), 0.001)
        assertEquals(160.0, CurrencyUtils.centsToDouble(persisted.dailyGoalNet), 0.001)
        assertEquals(110.0, persisted.dailyGoalKm, 0.001)
        assertEquals(18, persisted.dailyGoalDeliveries)
        assertEquals(SyncStatus.PENDING_UPDATE, persisted.syncStatus)
        assertTrue("updatedAt deve ser recente", persisted.updatedAt > 0)
    }

    // =========================================================================
    // 3. PARSER PT-BR COM VÍRGULA DECIMAL E NÚMEROS
    // =========================================================================

    @Test
    fun testPtBrCommaDecimalParsing_handlesCommasAndDefaults() {
        // Validação da função real de produção usada no EditGoalsDialog:
        assertEquals(250.0, parsePtBrDouble("250,00", 200.0), 0.001)
        assertEquals(250.5, parsePtBrDouble("250,5", 200.0), 0.001)
        assertEquals(250.5, parsePtBrDouble("250,50", 200.0), 0.001)
        assertEquals(250.0, parsePtBrDouble("250,", 200.0), 0.001)
        assertEquals(250.0, parsePtBrDouble("250, 00", 200.0), 0.001)
        assertEquals(160.0, parsePtBrDouble("160.00", 130.0), 0.001)
        assertEquals(120.0, parsePtBrDouble("  120,0  ", 100.0), 0.001)
        assertEquals(1250.5, parsePtBrDouble("1.250,50", 1200.0), 0.001)
        assertEquals(1200.0, parsePtBrDouble("1.200,00", 1200.0), 0.001)
        assertEquals(5000.0, parsePtBrDouble("5.000,00", 5000.0), 0.001)
        assertEquals(250.0, parsePtBrDouble("R$ 250,00", 200.0), 0.001)
        assertEquals(200.0, parsePtBrDouble("invalido", 200.0), 0.001)
        assertEquals(200.0, parsePtBrDouble("", 200.0), 0.001)

        assertEquals(18, parsePtBrInt("18", 15))
        assertEquals(25, parsePtBrInt(" 25 ", 15))
        assertEquals(15, parsePtBrInt("invalido", 15))
        assertEquals(15, parsePtBrInt("", 15))
    }

    @Test
    fun testPtBrFormatting_goalsDisplayAndInitialValuesPreserveDecimals() {
        // Validação da formatação dos valores iniciais no diálogo (com vírgula pt-BR)
        assertEquals("250,00", formatMoneyGoal(250.0))
        assertEquals("160,00", formatMoneyGoal(160.0))
        assertEquals("250,50", formatMoneyGoal(250.5))
        assertEquals("1200,00", formatMoneyGoal(1200.0))

        assertEquals("100", formatKmGoal(100.0))
        assertEquals("100,5", formatKmGoal(100.5))

        // Validação da formatação dos cards na Dashboard (R$ %,.2f com Locale.GERMAN)
        val grossFormat = "R$ %,.2f"
        assertEquals("R$ 250,00", String.format(Locale.GERMAN, grossFormat, 250.0))
        assertEquals("R$ 160,00", String.format(Locale.GERMAN, grossFormat, 160.0))
        assertEquals("R$ 1.200,00", String.format(Locale.GERMAN, grossFormat, 1200.0))
        assertEquals("R$ 5.000,00", String.format(Locale.GERMAN, grossFormat, 5000.0))
        assertEquals("R$ 250,50", String.format(Locale.GERMAN, grossFormat, 250.5))

        val kmFormat = "%,.1f km"
        assertEquals("100,0 km", String.format(Locale.GERMAN, kmFormat, 100.0))
        assertEquals("100,5 km", String.format(Locale.GERMAN, kmFormat, 100.5))
    }


    // =========================================================================
    // 4. RESET DE PREFERÊNCIAS NA TROCA DE CONTA NÃO AFETA TEMA
    // =========================================================================

    @Test
    fun testResetAccountSpecificPreferences_preservesTheme() = runTest(testDispatcher) {
        // Autentica usuário e define FakeRestApi para permitir confirmação de descarte de dados
        val client = SupabaseClient.getInstance(app)
        client.testRestApi = FakeSupabaseRestApi()
        client.setAuthSession(
            AuthSession("test-token", "test-refresh", "user-b", "userb@test.com")
        )

        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        appPrefs.edit().putBoolean("is_dark_theme", false).commit()

        val syncPrefs = app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit().putLong("last_sync_timestamp", 12345L).commit()

        val viewModel = createViewModel()
        testScheduler.advanceUntilIdle()

        // Executa confirmação de descarte de dados (troca de conta)
        viewModel.openDiscardConfirmation()
        var onCompleteCalled = false
        viewModel.confirmDiscardData { onCompleteCalled = true }
        testScheduler.advanceUntilIdle()

        assertTrue("onComplete deve ter sido chamado", onCompleteCalled)

        // Tema permanece intacto
        assertFalse("isDarkTheme deve continuar false", viewModel.isDarkTheme.value)
        assertFalse("giro_custo_preferences deve continuar com is_dark_theme = false", appPrefs.getBoolean("is_dark_theme", true))

        // last_sync_timestamp da conta anterior (12345L) foi limpo e substituído pelo sync da nova conta
        assertNotEquals(12345L, syncPrefs.getLong("last_sync_timestamp", 0L))
        assertTrue("Timestamp deve refletir nova sincronização da Conta B", syncPrefs.getLong("last_sync_timestamp", 0L) > 12345L)
    }
}
