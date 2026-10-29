package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.network.supabase.*
import com.example.ui.GiroCustoViewModel
import com.example.ui.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseSessionRefreshAndSyncTest {

    private lateinit var app: Application
    private lateinit var context: Context
    private lateinit var db: GiroCustoDatabase
    private lateinit var fakeStorage: FakeDataOwnerStorage
    private lateinit var fakeSessionStorage: InMemorySessionStorage
    private lateinit var client: SupabaseClient
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        context = app
        db = Room.inMemoryDatabaseBuilder(app, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeStorage = FakeDataOwnerStorage(initialOwnerId = "test-user-id")
        fakeSessionStorage = InMemorySessionStorage()
        client = SupabaseClient(context, fakeSessionStorage)
        SupabaseClient.setInstance(client)
    }

    @After
    fun tearDown() {
        SupabaseClient.setInstance(null)
        db.close()
        Dispatchers.resetMain()
    }

    private fun createJwtWithExp(expTimestampSeconds: Long): String {
        val header = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" // {"alg":"HS256","typ":"JWT"}
        val payloadJson = """{"sub":"test-user-id","exp":$expTimestampSeconds}"""
        val payloadEncoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray(Charsets.UTF_8))
        return "$header.$payloadEncoded.fakesignature"
    }

    private fun createStubRestApi(
        onGetVehicles: suspend () -> List<VehicleDto> = { emptyList() }
    ): SupabaseRestApi {
        return object : SupabaseRestApi {
            override suspend fun getVehicles(select: String, updatedAtFilter: String?): List<VehicleDto> = onGetVehicles()
            override suspend fun upsertVehicles(vehicles: List<VehicleDto>): List<VehicleDto> = vehicles
            override suspend fun deleteVehicle(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getVehicleParts(select: String, updatedAtFilter: String?): List<VehiclePartDto> = emptyList()
            override suspend fun upsertVehicleParts(parts: List<VehiclePartDto>): List<VehiclePartDto> = parts
            override suspend fun deleteVehiclePart(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getDailyRecords(select: String, updatedAtFilter: String?): List<DailyRecordDto> = emptyList()
            override suspend fun upsertDailyRecords(records: List<DailyRecordDto>): List<DailyRecordDto> = records
            override suspend fun deleteDailyRecord(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getUserProfiles(select: String, updatedAtFilter: String?): List<UserProfileDto> = emptyList()
            override suspend fun upsertUserProfiles(profiles: List<UserProfileDto>): List<UserProfileDto> = profiles
            override suspend fun getPlatforms(select: String, updatedAtFilter: String?): List<PlatformDto> = emptyList()
            override suspend fun upsertPlatforms(platforms: List<PlatformDto>): List<PlatformDto> = platforms
            override suspend fun deletePlatform(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getFuelRefills(select: String, updatedAtFilter: String?): List<FuelRefillDto> = emptyList()
            override suspend fun upsertFuelRefills(refills: List<FuelRefillDto>): List<FuelRefillDto> = refills
            override suspend fun deleteFuelRefill(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getMaintenanceRecords(select: String, updatedAtFilter: String?): List<MaintenanceRecordDto> = emptyList()
            override suspend fun upsertMaintenanceRecords(records: List<MaintenanceRecordDto>): List<MaintenanceRecordDto> = records
            override suspend fun deleteMaintenanceRecord(idFilter: String): Response<Unit> = Response.success(Unit)
            override suspend fun getUserGoals(select: String, updatedAtFilter: String?): List<UserGoalsDto> = emptyList()
            override suspend fun upsertUserGoals(goals: List<UserGoalsDto>): List<UserGoalsDto> = goals
            override suspend fun deleteUserAccount(): Response<Unit> = Response.success(Unit)
        }
    }

    @Test
    fun testIsTokenExpired_detectsExpiredAndActiveTokensAccurately() {
        val pastExp = (System.currentTimeMillis() / 1000L) - 3600L
        val futureExp = (System.currentTimeMillis() / 1000L) + 3600L

        val expiredJwt = createJwtWithExp(pastExp)
        val validJwt = createJwtWithExp(futureExp)

        assertTrue("Token com exp no passado deve ser detectado como expirado", client.isTokenExpired(expiredJwt))
        assertFalse("Token com exp no futuro deve ser considerado válido", client.isTokenExpired(validJwt))
        assertTrue("Token nulo ou vazio deve ser considerado expirado", client.isTokenExpired(null))
        assertTrue("Token em branco deve ser considerado expirado", client.isTokenExpired(""))
        assertFalse("String comum (não JWT) usada em testes de unidade não deve ser marcada como expirada por padrão", client.isTokenExpired("simple-fake-token"))
    }

    @Test
    fun testEnsureFreshSession_whenTokenExpired_refreshesSessionProactively() = runTest(testDispatcher) {
        val expiredJwt = createJwtWithExp((System.currentTimeMillis() / 1000L) - 600L)
        val session = AuthSession(
            accessToken = expiredJwt,
            refreshToken = "valid-refresh-token",
            userId = "test-user-id",
            userEmail = "user@test.com"
        )
        client.setAuthSession(session)

        var refreshCalled = false
        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithPassword(grantType: String, request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithIdToken(grantType: String, request: IdTokenRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun logout(): Response<Unit> = Response.success(Unit)
            override suspend fun getUser(): Response<SupabaseUser> = Response.success(SupabaseUser(id = "test-user-id"))
            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> {
                refreshCalled = true
                val freshJwt = createJwtWithExp((System.currentTimeMillis() / 1000L) + 3600L)
                return Response.success(AuthResponse(accessToken = freshJwt, refreshToken = "new-refresh-token"))
            }
        }

        val result = client.ensureFreshSession()
        assertTrue("ensureFreshSession deve ter sucesso", result.isSuccess)
        assertTrue("refreshToken deve ter sido invocado para renovar token expirado", refreshCalled)
        assertFalse("Novo token não deve estar expirado", client.isTokenExpired(client.currentAccessToken))
    }

    @Test
    fun testSync_whenHttp401Occurs_retriesWithRefreshedSessionAndSucceeds() = runTest(testDispatcher) {
        client.setAuthSession(AuthSession(
            accessToken = "expired-token",
            refreshToken = "valid-refresh-token",
            userId = "test-user-id",
            userEmail = "user@test.com"
        ))

        var refreshCallCount = 0
        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithPassword(grantType: String, request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithIdToken(grantType: String, request: IdTokenRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun logout(): Response<Unit> = Response.success(Unit)
            override suspend fun getUser(): Response<SupabaseUser> = Response.success(SupabaseUser(id = "test-user-id"))
            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> {
                refreshCallCount++
                return Response.success(AuthResponse(accessToken = "new-fresh-token", refreshToken = "new-refresh-token"))
            }
        }

        var attempts = 0
        val mockRestApi = createStubRestApi {
            attempts++
            if (attempts == 1) {
                val errorResponse = Response.error<List<VehicleDto>>(
                    401,
                    """{"message":"JWT expired"}""".toResponseBody("application/json".toMediaType())
                )
                throw HttpException(errorResponse)
            }
            emptyList()
        }

        val syncManager = SupabaseSyncManager(db, mockRestApi, fakeStorage, testDispatcher, client)
        val result = syncManager.sync("test-user-id")

        assertTrue("Sincronização deve se recuperar automaticamente do HTTP 401 e ter sucesso", result is SyncResult.Success)
        assertEquals("Deve ter executado o refresh de sessão exatamente uma vez", 1, refreshCallCount)
        assertEquals("Deve ter tentado a chamada à API 2 vezes (1 falha 401 + 1 retry com sucesso)", 2, attempts)
    }

    @Test
    fun testSync_whenHttp401OccursAndRefreshFails_returnsSessionExpiredExceptionWithFriendlyMessage() = runTest(testDispatcher) {
        client.setAuthSession(AuthSession(
            accessToken = "expired-token",
            refreshToken = "invalid-refresh-token",
            userId = "test-user-id",
            userEmail = "user@test.com"
        ))

        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithPassword(grantType: String, request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithIdToken(grantType: String, request: IdTokenRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun logout(): Response<Unit> = Response.success(Unit)
            override suspend fun getUser(): Response<SupabaseUser> = Response.success(SupabaseUser(id = "test-user-id"))
            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> {
                return Response.error(400, """{"error":"invalid_grant","error_description":"Invalid Refresh Token"}""".toResponseBody("application/json".toMediaType()))
            }
        }

        val mockRestApi = createStubRestApi {
            val errorResponse = Response.error<List<VehicleDto>>(
                401,
                """{"message":"JWT expired"}""".toResponseBody("application/json".toMediaType())
            )
            throw HttpException(errorResponse)
        }

        val syncManager = SupabaseSyncManager(db, mockRestApi, fakeStorage, testDispatcher, client)
        val result = syncManager.sync("test-user-id")

        assertTrue("Resultado deve ser Erro", result is SyncResult.Error)
        val error = (result as SyncResult.Error).error
        assertTrue(
            "Erro deve ser SessionExpiredException com mensagem explicativa em português e não HTTP 401 bruto. Obtido: ${error.message}",
            error is SessionExpiredException || error.message?.contains("sessão expirou", ignoreCase = true) == true
        )
    }

    @Test
    fun testViewModel_whenSyncFailsWithSessionExpired_setsFriendlyErrorMessage() = runTest(testDispatcher) {
        val expiredJwt = createJwtWithExp((System.currentTimeMillis() / 1000L) - 500L)
        client.setAuthSession(AuthSession(
            accessToken = expiredJwt,
            refreshToken = "invalid-refresh-token",
            userId = "test-user-id",
            userEmail = "user@test.com"
        ))

        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithPassword(grantType: String, request: AuthRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun signInWithIdToken(grantType: String, request: IdTokenRequest): Response<AuthResponse> = Response.success(AuthResponse())
            override suspend fun logout(): Response<Unit> = Response.success(Unit)
            override suspend fun getUser(): Response<SupabaseUser> = Response.success(SupabaseUser(id = "test-user-id"))
            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> {
                return Response.error(400, """{"error":"invalid_grant"}""".toResponseBody("application/json".toMediaType()))
            }
        }

        val repository = GiroCustoRepository(db, fakeStorage)
        val viewModel = GiroCustoViewModel(app, fakeStorage, repository)

        var callbackSuccess: Boolean? = null
        var callbackError: String? = null
        viewModel.triggerSync { success, errorMsg ->
            callbackSuccess = success
            callbackError = errorMsg
        }

        assertEquals(SyncState.ERROR, viewModel.syncState.value)
        assertEquals("Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando.", viewModel.syncErrorMessage.value)
        assertEquals(false, callbackSuccess)
        assertEquals("Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando.", callbackError)
    }
}
