package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.supabase.*
import com.example.ui.GiroCustoViewModel
import com.example.util.CurrencyUtils
import com.example.util.UserDataExportHelper
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
import org.json.JSONObject
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
 * Suíte de testes automatizados para Portabilidade e Exclusão Segura de Conta (LGPD - T-10 [SEC-06]).
 * Cobre:
 * 1. Exportação completa de dados locais em JSON estruturado (Portabilidade Art. 18, V).
 * 2. Exclusão de conta com sucesso via RPC remota e expurgo atômico local (Art. 18, VI).
 * 3. Proteção Fail-Safe em erro HTTP remoto (preserva 100% dos dados locais).
 * 4. Proteção Fail-Safe em falha de conexão/offline (preserva 100% dos dados locais).
 * 5. Exclusão de dados para usuário operando exclusivamente de forma local.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserAccountDeletionTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var app: Application
    private lateinit var inMemoryDb: GiroCustoDatabase
    private lateinit var dataOwnerStorage: DataOwnerStorage
    private lateinit var repository: GiroCustoRepository
    private lateinit var restApi: SupabaseRestApi
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var viewModel: GiroCustoViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()

        // 1. Inicia MockWebServer
        mockWebServer = MockWebServer()
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return MockResponse().setResponseCode(200).setBody("{}")
            }
        }
        mockWebServer.start()

        // 2. Configura Retrofit e SupabaseRestApi conectada ao MockWebServer
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

        // 4. Configura DataOwnerStorage e Repositório
        dataOwnerStorage = SharedPreferencesDataOwnerStorage(app)
        dataOwnerStorage.clearDataOwner()
        repository = GiroCustoRepository(inMemoryDb, dataOwnerStorage, testDispatcher)

        // 5. Configura SupabaseClient
        val authApi = retrofit.create(SupabaseAuthApi::class.java)
        supabaseClient = SupabaseClient.resetForTesting(app)
        supabaseClient.testRestApi = restApi
        supabaseClient.testAuthApi = authApi
        supabaseClient.setAuthSession(null)

        // 6. Limpa arquivos de preferências
        app.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()

        // 7. Instancia ViewModel
        viewModel = GiroCustoViewModel(
            application = app,
            dataOwnerStorage = dataOwnerStorage,
            repository = repository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        inMemoryDb.close()
        GiroCustoDatabase.resetForTesting()
        dataOwnerStorage.clearDataOwner()
        mockWebServer.shutdown()
    }

    @Test
    fun testExportUserData_containsAllEntitiesAndValidJson() = runTest(testDispatcher) {
        // 1. Popula dados nas tabelas locais
        inMemoryDb.userProfileDao().insertOrUpdateUserProfile(
            UserProfile(
                id = 1,
                name = "Carlos Entregador",
                phone = "11987654321",
                city = "São Paulo",
                email = "carlos@exemplo.com",
                cep = "01001-000",
                street = "Praça da Sé",
                addressNumber = "100",
                neighborhood = "Sé",
                stateUf = "SP",
                onboardingCompleted = true
            )
        )

        inMemoryDb.userGoalsDao().insertOrUpdateUserGoals(
            UserGoals(
                id = 1,
                dailyGoalGross = 250.0,
                dailyGoalNet = 160.0,
                dailyGoalKm = 120.0,
                dailyGoalDeliveries = 20
            )
        )

        val vehicleId = inMemoryDb.vehicleDao().insertVehicle(
            Vehicle(
                model = "CG 160 Cargo",
                averageConsumption = 38.5,
                fuelType = "Gasolina",
                active = true
            )
        )

        inMemoryDb.vehiclePartDao().insertPart(
            VehiclePart(
                vehicleId = vehicleId,
                name = "Óleo do Motor",
                price = 45.0,
                lifespanKm = 1500.0,
                runKmSinceChange = 400.0
            )
        )

        inMemoryDb.dailyRecordDao().insertRecord(
            DailyRecord(
                dateString = "2026-10-27",
                dateTimestamp = 1793059200000L,
                platform = "iFood",
                grossEarnings = 220.0,
                deliveriesCount = 18,
                startOdometer = 10000.0,
                endOdometer = 10100.0,
                fuelPrice = 5.59,
                foodExpense = 25.0,
                fuelCost = 14.50,
                wearCost = 5.00,
                proportionalFixedCost = 6.00,
                netProfit = 169.50
            )
        )

        inMemoryDb.fuelRefillDao().insertRefill(
            FuelRefill(
                vehicleId = vehicleId,
                dateString = "2026-10-27",
                dateTimestamp = 1793059200000L,
                gasStation = "Posto Ipiranga",
                fuelType = "Gasolina Comum",
                pricePerLiter = 5.59,
                liters = 10.0,
                discount = 0.0,
                totalPaid = 55.90,
                odometer = 10000.0,
                isFullTank = true,
                paymentMethod = "PIX"
            )
        )

        inMemoryDb.maintenanceRecordDao().insertRecord(
            MaintenanceRecord(
                vehicleId = vehicleId,
                dateString = "2026-10-26",
                dateTimestamp = 1792972800000L,
                description = "Troca de óleo e filtro",
                location = "Oficina do Zé",
                value = 65.0,
                odometer = 9800.0
            )
        )

        inMemoryDb.platformDao().insertOrUpdatePlatform(
            Platform(
                name = "iFood Entrega",
                segment = "delivery",
                paymentModel = "producao",
                cycle = "semanal",
                paymentDay = "QUA",
                fixedPayDelay = 7,
                active = true
            )
        )

        // 2. Executa exportação via ViewModel
        var exportedJson: String? = null
        viewModel.exportUserData { json ->
            exportedJson = json
        }

        assertNotNull("JSON exportado não pode ser nulo", exportedJson)
        val jsonRoot = JSONObject(exportedJson!!)

        // 3. Valida campos do JSON consolidado
        assertEquals(1, jsonRoot.getInt("export_format_version"))
        assertEquals("GiroCusto", jsonRoot.getString("app_name"))
        assertTrue(jsonRoot.has("exported_at"))

        // Perfil
        val profileJson = jsonRoot.getJSONObject("user_profile")
        assertEquals("Carlos Entregador", profileJson.getString("name"))
        assertEquals("São Paulo", profileJson.getString("city"))

        // Metas
        val goalsJson = jsonRoot.getJSONObject("user_goals")
        assertEquals(250.0, goalsJson.getDouble("daily_goal_gross"), 0.001)
        assertEquals(160.0, goalsJson.getDouble("daily_goal_net"), 0.001)

        // Veículos
        val vehiclesArr = jsonRoot.getJSONArray("vehicles")
        assertEquals(1, vehiclesArr.length())
        assertEquals("CG 160 Cargo", vehiclesArr.getJSONObject(0).getString("model"))

        // Peças
        val partsArr = jsonRoot.getJSONArray("vehicle_parts")
        assertEquals(1, partsArr.length())
        assertEquals("Óleo do Motor", partsArr.getJSONObject(0).getString("name"))

        // Corridas
        val recordsArr = jsonRoot.getJSONArray("daily_records")
        assertEquals(1, recordsArr.length())
        assertEquals("iFood", recordsArr.getJSONObject(0).getString("platform"))

        // Abastecimentos
        val refillsArr = jsonRoot.getJSONArray("fuel_refills")
        assertEquals(1, refillsArr.length())
        assertEquals("Posto Ipiranga", refillsArr.getJSONObject(0).getString("gas_station"))

        // Manutenções
        val maintenanceArr = jsonRoot.getJSONArray("maintenance_records")
        assertEquals(1, maintenanceArr.length())
        assertEquals("Troca de óleo e filtro", maintenanceArr.getJSONObject(0).getString("description"))

        // Plataformas
        val platformsArr = jsonRoot.getJSONArray("platforms")
        assertEquals(1, platformsArr.length())
        assertEquals("iFood Entrega", platformsArr.getJSONObject(0).getString("name"))
    }

    @Test
    fun testDeleteAccount_remoteRpcSuccess_clearsAllLocalDataAndDataOwnerAndSession() = runTest(testDispatcher) {
        val testUserId = "user_uuid_12345"
        supabaseClient.setAuthSession(
            AuthSession(
                accessToken = "valid_test_token",
                refreshToken = "valid_refresh",
                userId = testUserId,
                userEmail = "carlos@exemplo.com"
            )
        )
        dataOwnerStorage.setDataOwnerId(testUserId)

        // Popula dados locais
        inMemoryDb.userGoalsDao().insertOrUpdateUserGoals(
            UserGoals(id = 1, dailyGoalGross = 300.0)
        )
        inMemoryDb.vehicleDao().insertVehicle(
            Vehicle(model = "Titan 160", active = true)
        )

        assertTrue(repository.hasAnyLocalData())
        assertEquals(testUserId, dataOwnerStorage.getDataOwnerId())
        assertTrue(supabaseClient.isAuthenticated)

        // Configura MockWebServer para responder com sucesso à RPC de exclusão e ao logout
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/rest/v1/rpc/delete_user_account") -> {
                        MockResponse().setResponseCode(204)
                    }
                    path.startsWith("/auth/v1/logout") -> {
                        MockResponse().setResponseCode(200).setBody("{}")
                    }
                    else -> MockResponse().setResponseCode(200).setBody("[]")
                }
            }
        }

        // Executa exclusão de conta
        val latch = java.util.concurrent.CountDownLatch(1)
        var callbackSuccess: Boolean? = null
        var callbackError: String? = null
        viewModel.deleteAccountAndData { ok, err ->
            callbackSuccess = ok
            callbackError = err
            latch.countDown()
        }
        val completed = latch.await(5, TimeUnit.SECONDS)
        advanceUntilIdle()

        assertTrue("Operação assíncrona deve ter finalizado", completed)
        assertTrue("A exclusão deve retornar sucesso", callbackSuccess == true)
        assertNull("Não deve haver mensagem de erro", callbackError)

        // Asserções de Expurgo Total Local:
        assertFalse("Todas as tabelas Room locais devem ter sido expurgadas", repository.hasAnyLocalData())
        assertNull("Metas devem ter sido apagadas", inMemoryDb.userGoalsDao().getUserGoals())
        assertTrue("Veículos devem ter sido apagados", inMemoryDb.vehicleDao().getAllVehicles().isEmpty())
        assertNull("DataOwnerStorage deve estar limpo", dataOwnerStorage.getDataOwnerId())
        assertFalse("DataOwnerStorage não pode ter dono", dataOwnerStorage.hasOwner())
        assertFalse("SupabaseClient não pode estar autenticado", supabaseClient.isAuthenticated)
        assertNull("ID do usuário da sessão deve ser nulo", supabaseClient.currentUserId)
    }

    @Test
    fun testDeleteAccount_remoteRpcHttpError_failSafePreservesAllLocalDataAndDataOwnerAndSession() = runTest(testDispatcher) {
        val testUserId = "user_uuid_fail_safe"
        supabaseClient.setAuthSession(
            AuthSession(
                accessToken = "valid_test_token",
                refreshToken = "valid_refresh",
                userId = testUserId,
                userEmail = "seguro@exemplo.com"
            )
        )
        dataOwnerStorage.setDataOwnerId(testUserId)

        // Popula dados locais
        inMemoryDb.userGoalsDao().insertOrUpdateUserGoals(
            UserGoals(id = 1, dailyGoalGross = 400.0)
        )
        val vehicleId = inMemoryDb.vehicleDao().insertVehicle(
            Vehicle(model = "Yamaha Fazer 250", active = true)
        )

        // Configura MockWebServer para simular ERRO 500 no servidor Supabase
        mockWebServer.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                return when {
                    path.startsWith("/rest/v1/rpc/delete_user_account") -> {
                        MockResponse()
                            .setResponseCode(500)
                            .setBody("{\"message\":\"Internal Server Database Error\"}")
                    }
                    else -> MockResponse().setResponseCode(200).setBody("[]")
                }
            }
        }

        val latch = java.util.concurrent.CountDownLatch(1)
        var callbackSuccess: Boolean? = null
        var callbackError: String? = null
        viewModel.deleteAccountAndData { ok, err ->
            callbackSuccess = ok
            callbackError = err
            latch.countDown()
        }
        val completed = latch.await(5, TimeUnit.SECONDS)
        advanceUntilIdle()

        assertTrue("Operação assíncrona deve ter finalizado", completed)
        // Asserções do Fail-Safe:
        assertFalse("Operação remota falhou, sucesso deve ser false", callbackSuccess == true)
        assertNotNull("Deve haver mensagem de erro informando a falha remota", callbackError)
        assertTrue("Erro deve mencionar falha HTTP 500", callbackError!!.contains("500"))

        // NENHUM DADO LOCAL PODE SER APAGADO:
        assertTrue("O banco Room local deve permanecer rigorosamente intacto", repository.hasAnyLocalData())
        assertNotNull("As metas locais devem permanecer intactas", inMemoryDb.userGoalsDao().getUserGoals())
        assertEquals(400.0, CurrencyUtils.centsToDouble(inMemoryDb.userGoalsDao().getUserGoals()!!.dailyGoalGross), 0.001)
        assertEquals(1, inMemoryDb.vehicleDao().getAllVehicles().size)
        assertEquals("Yamaha Fazer 250", inMemoryDb.vehicleDao().getAllVehicles()[0].model)

        // Dono e Sessão devem ser preservados intactos:
        assertEquals(testUserId, dataOwnerStorage.getDataOwnerId())
        assertTrue(dataOwnerStorage.hasOwner())
        assertTrue(supabaseClient.isAuthenticated)
        assertEquals(testUserId, supabaseClient.currentUserId)
    }

    @Test
    fun testDeleteAccount_unauthenticatedLocalOnly_clearsLocalDataAndDataOwner() = runTest(testDispatcher) {
        // Usuário puramente offline (nunca logou no Supabase)
        supabaseClient.setAuthSession(null)
        dataOwnerStorage.setDataOwnerId("offline_local_owner")

        inMemoryDb.userGoalsDao().insertOrUpdateUserGoals(
            UserGoals(id = 1, dailyGoalGross = 180.0)
        )
        inMemoryDb.vehicleDao().insertVehicle(
            Vehicle(model = "Pop 110i", active = true)
        )

        assertTrue(repository.hasAnyLocalData())

        val latch = java.util.concurrent.CountDownLatch(1)
        var callbackSuccess: Boolean? = null
        viewModel.deleteAccountAndData { ok, _ ->
            callbackSuccess = ok
            latch.countDown()
        }
        val completed = latch.await(5, TimeUnit.SECONDS)
        advanceUntilIdle()

        assertTrue("Operação assíncrona deve ter finalizado", completed)
        assertTrue("Exclusão puramente local deve retornar sucesso", callbackSuccess == true)
        assertFalse("Banco Room deve estar vazio", repository.hasAnyLocalData())
        assertNull("Metas devem ter sido apagadas", inMemoryDb.userGoalsDao().getUserGoals())
        assertNull("DataOwnerStorage deve estar limpo", dataOwnerStorage.getDataOwnerId())
    }
}
