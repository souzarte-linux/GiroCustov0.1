package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.supabase.*
import com.example.ui.GiroCustoViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Testes de integração HTTP com MockWebServer e Retrofit real para o sincronismo de Metas (UserGoals),
 * cobrindo PUSH, PULL com reconciliação Last-Write-Wins (LWW) e isolamento multi-conta na troca de usuário.
 *
 * Em estrita conformidade com:
 * - docs/design-metas-por-conta.md (Seção 9.1)
 * - .agents/rules/integridade-de-testes.md
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserGoalsSyncTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var app: Application
    private lateinit var inMemoryDb: GiroCustoDatabase
    private lateinit var userGoalsDao: UserGoalsDao
    private lateinit var dataOwnerStorage: DataOwnerStorage
    private lateinit var repository: GiroCustoRepository
    private lateinit var restApi: SupabaseRestApi
    private lateinit var syncManager: SupabaseSyncManager
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun setMockServerGoalsDispatcher(goalsResponseBody: String = "[]") {
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/rest/v1/user_goals") -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody(goalsResponseBody)
                    }
                    else -> {
                        MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
                            .setBody("[]")
                    }
                }
            }
        }
    }

    private fun findRecordedRequest(pathPrefix: String, maxRequests: Int = 10): RecordedRequest? {
        for (i in 0 until maxRequests) {
            val req = mockWebServer.takeRequest(2, TimeUnit.SECONDS) ?: break
            if (req.path?.startsWith(pathPrefix) == true) {
                return req
            }
        }
        return null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        // 1. Inicializa MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // 2. Configura Moshi e Retrofit com MockWebServer
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        restApi = retrofit.create(SupabaseRestApi::class.java)

        // 3. Inicializa banco de dados Room em memória
        inMemoryDb = Room.inMemoryDatabaseBuilder(app, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(inMemoryDb)
        userGoalsDao = inMemoryDb.userGoalsDao()

        // 4. Configura DataOwnerStorage e Repositório
        dataOwnerStorage = SharedPreferencesDataOwnerStorage(app)
        dataOwnerStorage.clearDataOwner()

        repository = GiroCustoRepository(inMemoryDb, dataOwnerStorage, testDispatcher)
        syncManager = SupabaseSyncManager(inMemoryDb, restApi, dataOwnerStorage, testDispatcher)

        // 5. Configura SupabaseClient para testes
        val supabaseClient = SupabaseClient.resetForTesting(app)
        supabaseClient.testRestApi = restApi
        supabaseClient.setAuthSession(null)

        // 6. Limpa arquivos de preferências
        app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        inMemoryDb.close()
        GiroCustoDatabase.resetForTesting()
        dataOwnerStorage.clearDataOwner()
        mockWebServer.shutdown()

        SupabaseClient.resetForTesting(app).setAuthSession(null)

        app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // =========================================================================
    // TESTE 1: PUSH COM MOCKWEBSERVER
    // =========================================================================
    @Test
    fun testPushUserGoals_pendingSerializedToJson_postRequestSent_roomTransitionsToSynced() = runTest(testDispatcher) {
        val userId = "user-qa-push-123"
        dataOwnerStorage.setDataOwnerId(userId)

        // 1. Registro local pendente de envio (syncStatus = PENDING_UPDATE)
        val pendingGoal = UserGoals(
            id = 1,
            dailyGoalGross = 250.0,
            dailyGoalNet = 160.0,
            dailyGoalKm = 120.0,
            dailyGoalDeliveries = 18,
            weeklyGoalGross = 1500.0,
            weeklyGoalNet = 950.0,
            weeklyGoalKm = 700.0,
            weeklyGoalDeliveries = 100,
            monthlyGoalGross = 6000.0,
            monthlyGoalNet = 4000.0,
            monthlyGoalKm = 3000.0,
            monthlyGoalDeliveries = 450,
            remoteId = "goal-uuid-push-1",
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 1761050000000L,
            isDeleted = false
        )
        userGoalsDao.insertOrUpdateUserGoals(pendingGoal)

        // 2. MockWebServer responde HTTP 201 Created com a representação do DTO salvo
        val responseJson = """
            [
              {
                "id": "goal-uuid-push-1",
                "user_id": "$userId",
                "daily_goal_gross": 250.0,
                "daily_goal_net": 160.0,
                "daily_goal_km": 120.0,
                "daily_goal_deliveries": 18,
                "weekly_goal_gross": 1500.0,
                "weekly_goal_net": 950.0,
                "weekly_goal_km": 700.0,
                "weekly_goal_deliveries": 100,
                "monthly_goal_gross": 6000.0,
                "monthly_goal_net": 4000.0,
                "monthly_goal_km": 3000.0,
                "monthly_goal_deliveries": 450,
                "created_at": "2026-10-26T20:00:00Z",
                "updated_at": "2026-10-26T20:00:00Z",
                "deleted_at": null
              }
            ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(responseJson)
        )

        // 3. Executa PUSH
        syncManager.pushLocalChanges(userId)

        // 4. Confirmação no Room: o status transitou de PENDING_UPDATE para SYNCED
        val syncedGoal = userGoalsDao.getUserGoals()
        assertNotNull("Meta deve existir no Room após o PUSH", syncedGoal)
        assertEquals(SyncStatus.SYNCED, syncedGoal?.syncStatus)
        assertEquals("goal-uuid-push-1", syncedGoal?.remoteId)
        assertEquals(250.0, syncedGoal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(160.0, syncedGoal?.dailyGoalNet ?: 0.0, 0.001)

        // 5. Validação detalhada no MockWebServer: takeRequest()
        val recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull("Requisição HTTP deve ter sido recebida pelo MockWebServer", recordedRequest)
        assertEquals("POST", recordedRequest!!.method)
        assertEquals("/rest/v1/user_goals", recordedRequest.path)
        assertEquals(
            "resolution=merge-duplicates,return=representation",
            recordedRequest.getHeader("Prefer")
        )

        // Validação da serialização JSON do payload enviado
        val requestBody = recordedRequest.body.readUtf8()
        val jsonArray = JSONArray(requestBody)
        assertEquals(1, jsonArray.length())
        val jsonGoal = jsonArray.getJSONObject(0)
        assertEquals("goal-uuid-push-1", jsonGoal.getString("id"))
        assertEquals(userId, jsonGoal.getString("user_id"))
        assertEquals(250.0, jsonGoal.getDouble("daily_goal_gross"), 0.001)
        assertEquals(160.0, jsonGoal.getDouble("daily_goal_net"), 0.001)
        assertEquals(120.0, jsonGoal.getDouble("daily_goal_km"), 0.001)
        assertEquals(18, jsonGoal.getInt("daily_goal_deliveries"))
        assertEquals(1500.0, jsonGoal.getDouble("weekly_goal_gross"), 0.001)
        assertEquals(950.0, jsonGoal.getDouble("weekly_goal_net"), 0.001)
        assertEquals(700.0, jsonGoal.getDouble("weekly_goal_km"), 0.001)
        assertEquals(100, jsonGoal.getInt("weekly_goal_deliveries"))
        assertEquals(6000.0, jsonGoal.getDouble("monthly_goal_gross"), 0.001)
        assertEquals(4000.0, jsonGoal.getDouble("monthly_goal_net"), 0.001)
        assertEquals(3000.0, jsonGoal.getDouble("monthly_goal_km"), 0.001)
        assertEquals(450, jsonGoal.getInt("monthly_goal_deliveries"))
    }

    // =========================================================================
    // TESTE 2: PULL COM MOCKWEBSERVER - REMOTO MAIS RECENTE (LWW)
    // =========================================================================
    @Test
    fun testPullUserGoals_remoteNewerLWW_replacesLocalAndUpdatesRoom() = runTest(testDispatcher) {
        val userId = "user-qa-pull-newer-456"
        dataOwnerStorage.setDataOwnerId(userId)

        // 1. Meta local defasada (updatedAt = 1000L)
        val initialLocal = UserGoals(
            id = 1,
            dailyGoalGross = 200.0,
            dailyGoalNet = 130.0,
            dailyGoalKm = 100.0,
            dailyGoalDeliveries = 15,
            weeklyGoalGross = 1200.0,
            weeklyGoalNet = 800.0,
            weeklyGoalKm = 600.0,
            weeklyGoalDeliveries = 90,
            monthlyGoalGross = 5000.0,
            monthlyGoalNet = 3200.0,
            monthlyGoalKm = 2500.0,
            monthlyGoalDeliveries = 400,
            remoteId = "goal-uuid-old",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L
        )
        userGoalsDao.insertOrUpdateUserGoals(initialLocal)

        // 2. MockWebServer responde HTTP 200 com array contendo registro de meta com updated_at mais recente (5000L)
        val remoteIsoDate = SupabaseMappers.toIsoString(5000L)
        val responseJson = """
            [
              {
                "id": "goal-uuid-remote-newer",
                "user_id": "$userId",
                "daily_goal_gross": 380.0,
                "daily_goal_net": 240.0,
                "daily_goal_km": 140.0,
                "daily_goal_deliveries": 22,
                "weekly_goal_gross": 2200.0,
                "weekly_goal_net": 1400.0,
                "weekly_goal_km": 800.0,
                "weekly_goal_deliveries": 120,
                "monthly_goal_gross": 9000.0,
                "monthly_goal_net": 5800.0,
                "monthly_goal_km": 3200.0,
                "monthly_goal_deliveries": 490,
                "created_at": "$remoteIsoDate",
                "updated_at": "$remoteIsoDate",
                "deleted_at": null
              }
            ]
        """.trimIndent()

        setMockServerGoalsDispatcher(responseJson)

        // 3. Executa PULL
        syncManager.pullRemoteChanges(userId)

        // 4. Reconciliação LWW sobrescreve a meta local e valida novos valores no Room
        val updatedLocal = userGoalsDao.getUserGoals()
        assertNotNull("Meta deve existir no Room", updatedLocal)
        assertEquals(1, updatedLocal?.id)
        assertEquals("goal-uuid-remote-newer", updatedLocal?.remoteId)
        assertEquals(380.0, updatedLocal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(240.0, updatedLocal?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(140.0, updatedLocal?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(22, updatedLocal?.dailyGoalDeliveries)
        assertEquals(2200.0, updatedLocal?.weeklyGoalGross ?: 0.0, 0.001)
        assertEquals(1400.0, updatedLocal?.weeklyGoalNet ?: 0.0, 0.001)
        assertEquals(9000.0, updatedLocal?.monthlyGoalGross ?: 0.0, 0.001)
        assertEquals(5800.0, updatedLocal?.monthlyGoalNet ?: 0.0, 0.001)
        assertEquals(SyncStatus.SYNCED, updatedLocal?.syncStatus)
        assertEquals(5000L, updatedLocal?.updatedAt)

        // Validação da requisição capturada pelo MockWebServer
        val recordedRequest = findRecordedRequest("/rest/v1/user_goals")
        assertNotNull("Requisição HTTP deve ter sido capturada", recordedRequest)
        assertEquals("GET", recordedRequest!!.method)
        assertTrue(recordedRequest.path?.startsWith("/rest/v1/user_goals") == true)
    }

    // =========================================================================
    // TESTE 3: PULL COM MOCKWEBSERVER - LOCAL MAIS RECENTE OFFLINE (LWW)
    // =========================================================================
    @Test
    fun testPullUserGoals_localNewerOfflineLWW_preservesLocalAndPendingStatus() = runTest(testDispatcher) {
        val userId = "user-qa-pull-older-789"
        dataOwnerStorage.setDataOwnerId(userId)

        // 1. Dado local possui alteração offline mais recente (updatedAt = 9000L, PENDING_UPDATE)
        val offlineLocal = UserGoals(
            id = 1,
            dailyGoalGross = 420.0,
            dailyGoalNet = 280.0,
            dailyGoalKm = 160.0,
            dailyGoalDeliveries = 24,
            weeklyGoalGross = 2500.0,
            weeklyGoalNet = 1600.0,
            weeklyGoalKm = 900.0,
            weeklyGoalDeliveries = 140,
            monthlyGoalGross = 10000.0,
            monthlyGoalNet = 6500.0,
            monthlyGoalKm = 3600.0,
            monthlyGoalDeliveries = 520,
            remoteId = "goal-uuid-offline",
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 9000L
        )
        userGoalsDao.insertOrUpdateUserGoals(offlineLocal)

        // 2. MockWebServer responde HTTP 200 com dado remoto defasado (updated_at = 3000L)
        val remoteOutdatedIso = SupabaseMappers.toIsoString(3000L)
        val responseJson = """
            [
              {
                "id": "goal-uuid-remote-outdated",
                "user_id": "$userId",
                "daily_goal_gross": 200.0,
                "daily_goal_net": 130.0,
                "daily_goal_km": 100.0,
                "daily_goal_deliveries": 15,
                "weekly_goal_gross": 1200.0,
                "weekly_goal_net": 800.0,
                "weekly_goal_km": 600.0,
                "weekly_goal_deliveries": 90,
                "monthly_goal_gross": 5000.0,
                "monthly_goal_net": 3200.0,
                "monthly_goal_km": 2500.0,
                "monthly_goal_deliveries": 400,
                "created_at": "$remoteOutdatedIso",
                "updated_at": "$remoteOutdatedIso",
                "deleted_at": null
              }
            ]
        """.trimIndent()

        setMockServerGoalsDispatcher(responseJson)

        // 3. Executa PULL
        syncManager.pullRemoteChanges(userId)

        // 4. A reconciliação preserva o dado local intacto e mantém o status pendente para envio posterior
        val currentLocal = userGoalsDao.getUserGoals()
        assertNotNull("Meta local deve existir", currentLocal)
        assertEquals(420.0, currentLocal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(280.0, currentLocal?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(160.0, currentLocal?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(24, currentLocal?.dailyGoalDeliveries)
        assertEquals(SyncStatus.PENDING_UPDATE, currentLocal?.syncStatus)
        assertEquals("goal-uuid-offline", currentLocal?.remoteId)
        assertEquals(9000L, currentLocal?.updatedAt)

        val recordedRequest = findRecordedRequest("/rest/v1/user_goals")
        assertNotNull(recordedRequest)
        assertEquals("GET", recordedRequest!!.method)
    }

    // =========================================================================
    // TESTE 4: PULL COM MOCKWEBSERVER - REGISTRO INEXISTENTE NA NUVEM
    // =========================================================================
    @Test
    fun testPullUserGoals_emptyRemoteArray_preservesLocalGoalsIntact() = runTest(testDispatcher) {
        val userId = "user-qa-empty-remote"
        dataOwnerStorage.setDataOwnerId(userId)

        // 1. Metas locais existentes
        val localGoal = UserGoals(
            id = 1,
            dailyGoalGross = 275.0,
            dailyGoalNet = 175.0,
            dailyGoalKm = 115.0,
            dailyGoalDeliveries = 17,
            weeklyGoalGross = 1450.0,
            weeklyGoalNet = 920.0,
            weeklyGoalKm = 680.0,
            weeklyGoalDeliveries = 98,
            monthlyGoalGross = 5800.0,
            monthlyGoalNet = 3700.0,
            monthlyGoalKm = 2800.0,
            monthlyGoalDeliveries = 430,
            remoteId = "goal-uuid-preserved",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 4000L
        )
        userGoalsDao.insertOrUpdateUserGoals(localGoal)

        // 2. MockWebServer responde HTTP 200 com array vazio []
        setMockServerGoalsDispatcher("[]")

        // 3. Executa PULL
        syncManager.pullRemoteChanges(userId)

        // 4. Metas locais existentes são preservadas intactas
        val currentLocal = userGoalsDao.getUserGoals()
        assertNotNull("Meta local deve continuar existindo", currentLocal)
        assertEquals("goal-uuid-preserved", currentLocal?.remoteId)
        assertEquals(275.0, currentLocal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(175.0, currentLocal?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(115.0, currentLocal?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(17, currentLocal?.dailyGoalDeliveries)
        assertEquals(SyncStatus.SYNCED, currentLocal?.syncStatus)
        assertEquals(4000L, currentLocal?.updatedAt)

        val recordedRequest = findRecordedRequest("/rest/v1/user_goals")
        assertNotNull(recordedRequest)
        assertEquals("GET", recordedRequest!!.method)
    }

    // =========================================================================
    // TESTE 5: ISOLAMENTO NA TROCA DE CONTA COM CONFIRMDISCARDDATA
    // =========================================================================
    @Test
    fun testAccountSwitch_confirmDiscardData_clearsUserAGoals_preservesTheme_andPullsUserBGoals() = runTest(testDispatcher) {
        val client = SupabaseClient.resetForTesting(app)
        client.testRestApi = restApi

        val userIdA = "user-A-uuid"
        val userIdB = "user-B-uuid"

        // 1. Simular Usuário A autenticado com metas customizadas A (faturamento diário R$ 350,00)
        client.setAuthSession(AuthSession("token_A", "refresh_A", userIdA, "userA@test.com"))
        dataOwnerStorage.setDataOwnerId(userIdA)

        val userAGoals = UserGoals(
            id = 1,
            dailyGoalGross = 350.0,
            dailyGoalNet = 230.0,
            dailyGoalKm = 140.0,
            dailyGoalDeliveries = 20,
            weeklyGoalGross = 2100.0,
            weeklyGoalNet = 1380.0,
            weeklyGoalKm = 840.0,
            weeklyGoalDeliveries = 120,
            monthlyGoalGross = 8750.0,
            monthlyGoalNet = 5750.0,
            monthlyGoalKm = 3500.0,
            monthlyGoalDeliveries = 500,
            remoteId = "goal-uuid-A",
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L
        )
        userGoalsDao.insertOrUpdateUserGoals(userAGoals)

        // Tema customizado claro (is_dark_theme = false) em giro_custo_preferences
        val appPrefs = app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        appPrefs.edit().putBoolean("is_dark_theme", false).commit()

        val syncPrefs = app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.edit().putLong("last_sync_timestamp", 12345L).commit()

        // Confirma metas do Usuário A presentes no Room
        val loadedGoalsA = userGoalsDao.getUserGoals()
        assertNotNull(loadedGoalsA)
        assertEquals(350.0, loadedGoalsA?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals("goal-uuid-A", loadedGoalsA?.remoteId)

        // 2. Simular login do Usuário B no SupabaseClient
        client.setAuthSession(AuthSession("token_B", "refresh_B", userIdB, "userB@test.com"))

        // MockWebServer configurado via Dispatcher:
        // Retorna metas do Usuário B (faturamento diário R$ 500,00) para GET /rest/v1/user_goals
        // e array vazio [] para outras tabelas durante a sincronização
        val userBIsoDate = SupabaseMappers.toIsoString(2000L)
        val userBGoalsJson = """
            [
              {
                "id": "goal-uuid-B",
                "user_id": "$userIdB",
                "daily_goal_gross": 500.0,
                "daily_goal_net": 320.0,
                "daily_goal_km": 180.0,
                "daily_goal_deliveries": 28,
                "weekly_goal_gross": 3000.0,
                "weekly_goal_net": 1920.0,
                "weekly_goal_km": 1080.0,
                "weekly_goal_deliveries": 168,
                "monthly_goal_gross": 12500.0,
                "monthly_goal_net": 8000.0,
                "monthly_goal_km": 4500.0,
                "monthly_goal_deliveries": 700,
                "created_at": "$userBIsoDate",
                "updated_at": "$userBIsoDate",
                "deleted_at": null
              }
            ]
        """.trimIndent()

        setMockServerGoalsDispatcher(userBGoalsJson)

        val viewModel = GiroCustoViewModel(app, dataOwnerStorage, repository)
        advanceUntilIdle()

        // 3. Simular troca de conta com descarte de dados (confirmDiscardData)
        viewModel.openDiscardConfirmation()
        var onCompleteCalled = false
        val latch = java.util.concurrent.CountDownLatch(1)
        viewModel.confirmDiscardData {
            onCompleteCalled = true
            latch.countDown()
        }
        val completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        advanceUntilIdle()

        assertTrue("onComplete deve ter sido acionado", completed && onCompleteCalled)

        // 4. Validações estritas de Isolamento e Integridade:
        // a) dataOwnerStorage agora aponta para o Usuário B
        assertEquals(userIdB, dataOwnerStorage.getDataOwnerId())

        // b) O Room agora possui exclusivamente as metas da Conta B (R$ 500,00)
        val loadedGoalsB = userGoalsDao.getUserGoals()
        assertNotNull("Room deve conter as metas da Conta B", loadedGoalsB)
        assertEquals(1, loadedGoalsB?.id)
        assertEquals("goal-uuid-B", loadedGoalsB?.remoteId)
        assertEquals(500.0, loadedGoalsB?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(320.0, loadedGoalsB?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(180.0, loadedGoalsB?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(28, loadedGoalsB?.dailyGoalDeliveries)
        assertEquals(3000.0, loadedGoalsB?.weeklyGoalGross ?: 0.0, 0.001)
        assertEquals(SyncStatus.SYNCED, loadedGoalsB?.syncStatus)

        // c) Confirma que NENHUM resquício da Conta A (R$ 350,00) restou
        assertNotEquals(350.0, loadedGoalsB?.dailyGoalGross ?: 0.0, 0.001)
        assertNotEquals("goal-uuid-A", loadedGoalsB?.remoteId)

        // d) Confirma que a preferência de tema permaneceu intacta
        assertFalse("isDarkTheme do ViewModel deve continuar false", viewModel.isDarkTheme.value)
        assertFalse("giro_custo_preferences deve continuar com is_dark_theme = false", appPrefs.getBoolean("is_dark_theme", true))

        // e) last_sync_timestamp da conta anterior (12345L) foi redefinido e atualizado para a Conta B
        assertNotEquals(12345L, syncPrefs.getLong("last_sync_timestamp", 0L))
        assertTrue("last_sync_timestamp deve refletir o sync da Conta B", syncPrefs.getLong("last_sync_timestamp", 0L) > 12345L)
    }
}
