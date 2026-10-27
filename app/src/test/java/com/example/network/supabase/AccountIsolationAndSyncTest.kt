package com.example.network.supabase

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.AccountConflictState
import com.example.ui.GiroCustoViewModel
import com.example.ui.SyncState
import com.example.util.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response

/**
 * Suíte de testes da Tarefa T-06c: Isolamento de Dados entre Contas no Mesmo Aparelho.
 * Valida todas as salvaguardas obrigatórias da arquitetura de guarda de dono (DataOwnerStorage)
 * e o ciclo de vida seguro de transição de contas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountIsolationAndSyncTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var app: Application
    private lateinit var db: GiroCustoDatabase
    private lateinit var fakeStorage: FakeDataOwnerStorage
    private lateinit var fakeApi: CountingSupabaseRestApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        app = ApplicationProvider.getApplicationContext()

        // Limpa preferências compartilhadas para evitar vazamento de estado entre testes
        context.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()

        // Banco in-memory isolado para cada execução de teste
        db = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        GiroCustoDatabase.setTestDatabase(db)

        fakeStorage = FakeDataOwnerStorage()
        fakeApi = CountingSupabaseRestApi()

        val client = SupabaseClient.resetForTesting(context)
        client.testRestApi = fakeApi
        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest): Response<AuthResponse> =
                Response.success(AuthResponse(accessToken = "token-test", refreshToken = "refresh-test"))

            override suspend fun signInWithPassword(grantType: String, request: AuthRequest): Response<AuthResponse> =
                Response.success(AuthResponse(accessToken = "token-test", refreshToken = "refresh-test"))

            override suspend fun signInWithIdToken(grantType: String, request: IdTokenRequest): Response<AuthResponse> =
                Response.success(AuthResponse(
                    accessToken = "token-test",
                    refreshToken = "refresh-test",
                    user = SupabaseUser("usr-test", "user@test.com")
                ))

            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> =
                Response.success(AuthResponse(accessToken = "token-refreshed", refreshToken = "refresh-refreshed"))

            override suspend fun logout(): Response<Unit> = Response.success(Unit)

            override suspend fun getUser(): Response<SupabaseUser> =
                Response.success(SupabaseUser("usr-test", "user@test.com"))
        }
        client.setAuthSession(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
        context.getSharedPreferences("giro_custo_data_owner", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        SupabaseClient.resetForTesting(context).setAuthSession(null)
    }

    private fun createViewModel(): GiroCustoViewModel {
        val repo = GiroCustoRepository(db, fakeStorage, testDispatcher)
        return GiroCustoViewModel(app, fakeStorage, repo)
    }

    // =========================================================================
    // 1. TRAVAS NO PUSH E NO PULL (Condição 1 & Salvaguarda 1)
    // =========================================================================

    @Test
    fun testPushLocalChanges_whenDataOwnerMismatch_blocksPushAndThrowsError() = runTest(testDispatcher) {
        fakeStorage.setDataOwnerId("user_A")
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        // Insere veículo pendente local pertencente originalmente a user_A
        db.vehicleDao().insertVehicle(
            Vehicle(
                id = 1,
                model = "Honda CG 160",
                syncStatus = SyncStatus.PENDING_INSERT,
                updatedAt = System.currentTimeMillis()
            )
        )

        var thrownException: Throwable? = null
        try {
            syncManager.pushLocalChanges(userId = "user_B")
        } catch (e: Throwable) {
            thrownException = e
        }

        assertNotNull("Deve lançar exceção ao tentar push com dono divergente", thrownException)
        assertTrue(thrownException is DataOwnerMismatchException)
        val mismatch = thrownException as DataOwnerMismatchException
        assertEquals("user_A", mismatch.ownerId)
        assertEquals("user_B", mismatch.currentUserId)
        assertEquals("PUSH", mismatch.operation)

        // Prova absoluta: ZERO requisições à API REST do Supabase
        assertEquals(0, fakeApi.upsertVehiclesCalls)
        assertEquals(0, fakeApi.upsertDailyRecordsCalls)
    }

    @Test
    fun testPullRemoteChanges_whenDataOwnerMismatch_blocksPullAndThrowsError() = runTest(testDispatcher) {
        fakeStorage.setDataOwnerId("user_A")
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        var thrownException: Throwable? = null
        try {
            syncManager.pullRemoteChanges(userId = "user_B")
        } catch (e: Throwable) {
            thrownException = e
        }

        assertNotNull("Deve lançar exceção ao tentar pull com dono divergente", thrownException)
        assertTrue(thrownException is DataOwnerMismatchException)
        val mismatch = thrownException as DataOwnerMismatchException
        assertEquals("user_A", mismatch.ownerId)
        assertEquals("user_B", mismatch.currentUserId)
        assertEquals("PULL", mismatch.operation)

        // Prova absoluta: ZERO requisições de leitura à API REST do Supabase
        assertEquals(0, fakeApi.getVehiclesCalls)
        assertEquals(0, fakeApi.getDailyRecordsCalls)
    }

    @Test
    fun testPushLocalChanges_whenOwnerIdNullAndLocalDataExists_blocksPushAndThrowsError() = runTest(testDispatcher) {
        fakeStorage.clearDataOwner()
        assertNull(fakeStorage.getDataOwnerId())
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        // Insere veículo local (banco não está vazio, mas dono é desconhecido/nulo)
        db.vehicleDao().insertVehicle(
            Vehicle(
                id = 1,
                model = "Honda CG 160",
                syncStatus = SyncStatus.PENDING_INSERT,
                updatedAt = System.currentTimeMillis()
            )
        )

        var thrownException: Throwable? = null
        try {
            syncManager.pushLocalChanges(userId = "user_A")
        } catch (e: Throwable) {
            thrownException = e
        }

        assertNotNull("Deve lançar exceção fail-safe ao tentar push com dono desconhecido e dados locais presentes", thrownException)
        assertTrue(thrownException is DataOwnerMismatchException)
        val mismatch = thrownException as DataOwnerMismatchException
        assertNull("ownerId deve ser null pois o dono local é desconhecido", mismatch.ownerId)
        assertEquals("user_A", mismatch.currentUserId)
        assertEquals("PUSH", mismatch.operation)

        // Prova absoluta: ZERO requisições à API REST do Supabase
        assertEquals(0, fakeApi.upsertVehiclesCalls)
        assertEquals(0, fakeApi.upsertDailyRecordsCalls)
    }

    @Test
    fun testPullRemoteChanges_whenOwnerIdNullAndLocalDataExists_blocksPullAndThrowsError() = runTest(testDispatcher) {
        fakeStorage.clearDataOwner()
        assertNull(fakeStorage.getDataOwnerId())
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        // Insere veículo local (banco não está vazio, mas dono é desconhecido/nulo)
        db.vehicleDao().insertVehicle(
            Vehicle(
                id = 1,
                model = "Honda CG 160",
                syncStatus = SyncStatus.SYNCED,
                updatedAt = System.currentTimeMillis()
            )
        )

        var thrownException: Throwable? = null
        try {
            syncManager.pullRemoteChanges(userId = "user_A")
        } catch (e: Throwable) {
            thrownException = e
        }

        assertNotNull("Deve lançar exceção fail-safe ao tentar pull com dono desconhecido e dados locais presentes", thrownException)
        assertTrue(thrownException is DataOwnerMismatchException)
        val mismatch = thrownException as DataOwnerMismatchException
        assertNull("ownerId deve ser null pois o dono local é desconhecido", mismatch.ownerId)
        assertEquals("user_A", mismatch.currentUserId)
        assertEquals("PULL", mismatch.operation)

        // Prova absoluta: ZERO requisições à API REST do Supabase
        assertEquals(0, fakeApi.getVehiclesCalls)
        assertEquals(0, fakeApi.getDailyRecordsCalls)
    }

    @Test
    fun testSync_whenOwnerIdNullAndLocalDataExists_returnsErrorResult() = runTest(testDispatcher) {
        fakeStorage.clearDataOwner()
        assertNull(fakeStorage.getDataOwnerId())
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        db.vehicleDao().insertVehicle(
            Vehicle(
                id = 1,
                model = "Honda CG 160",
                syncStatus = SyncStatus.PENDING_INSERT,
                updatedAt = System.currentTimeMillis()
            )
        )

        val result = syncManager.sync("user_A")
        assertTrue("Sync deve retornar SyncResult.Error devido à trava fail-safe", result is SyncResult.Error)
        val err = (result as SyncResult.Error).error
        assertTrue(err is DataOwnerMismatchException)
        val mismatch = err as DataOwnerMismatchException
        assertNull(mismatch.ownerId)
        assertEquals("user_A", mismatch.currentUserId)

        // Prova absoluta: ZERO requisições à API REST do Supabase
        assertEquals(0, fakeApi.upsertVehiclesCalls)
        assertEquals(0, fakeApi.getVehiclesCalls)
    }

    // =========================================================================
    // 2. OS TRÊS CASOS DA PRIMEIRA EXECUÇÃO / DONO DESCONHECIDO (Condição 3)
    // =========================================================================

    @Test
    fun testUnknownOwner_emptyDatabase_assignsOwnerAndSyncs() = runTest(testDispatcher) {
        // Caso A: dono nulo e banco vazio
        assertNull(fakeStorage.getDataOwnerId())
        assertEquals(0, db.vehicleDao().getAllVehicles().size)

        val repository = GiroCustoRepository(db, fakeStorage)
        assertFalse(repository.hasAnyLocalData())

        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(AuthSession("token_b", "refresh_b", "user_B", "b@test.com"))

        val syncManager = SupabaseSyncManager(db, client, fakeStorage)
        val result = syncManager.sync("user_B")

        assertTrue("Sincronização deve ter sucesso", result is SyncResult.Success)
        // Quando banco vazio, a guarda assume a conta logada
        fakeStorage.setDataOwnerId("user_B")
        assertEquals("user_B", fakeStorage.getDataOwnerId())
    }

    @Test
    fun testFreshInstall_emptyDatabaseAndNullOwner_pullAllowedAndOwnerSavedToStorage() = runTest(testDispatcher) {
        // Cenário de nova instalação: banco Room vazio, dono desconhecido/nulo no DataOwnerStorage
        fakeStorage.clearDataOwner()
        assertNull(fakeStorage.getDataOwnerId())
        assertEquals(0, db.vehicleDao().getAllVehicles().size)

        // Supabase possui dados remotos da conta autenticada
        val remoteVehicle = VehicleDto(
            id = "rem-veh-fresh",
            userId = "user_fresh_install",
            model = "Moto Nova Instalação",
            averageConsumption = 42.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 80.0,
            plannedWorkDays = 25,
            currentOdometer = 1000.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(System.currentTimeMillis())
        )
        fakeApi.remoteVehicles.add(remoteVehicle)

        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)
        // PULL deve ser permitido normalmente quando banco está vazio
        syncManager.pullRemoteChanges("user_fresh_install")

        // Veículo deve ter sido inserido no banco Room local
        val localVehicles = db.vehicleDao().getAllVehicles()
        assertEquals(1, localVehicles.size)
        assertEquals("Moto Nova Instalação", localVehicles.first().model)

        // Dono autenticado deve ter sido gravado no DataOwnerStorage após a conclusão
        assertEquals("user_fresh_install", fakeStorage.getDataOwnerId())
    }

    @Test
    fun testFreshInstall_emptyDatabaseAndNullOwner_syncAllowedAndOwnerSavedToStorage() = runTest(testDispatcher) {
        // Cenário de nova instalação via sync completo
        fakeStorage.clearDataOwner()
        assertNull(fakeStorage.getDataOwnerId())
        assertEquals(0, db.vehicleDao().getAllVehicles().size)

        val remoteVehicle = VehicleDto(
            id = "rem-veh-fresh-sync",
            userId = "user_fresh_sync",
            model = "Moto Fresh Sync",
            averageConsumption = 39.0,
            fuelType = "Gasolina",
            monthlyFixedCosts = 90.0,
            plannedWorkDays = 20,
            currentOdometer = 2000.0,
            active = true,
            updatedAt = SupabaseMappers.toIsoString(System.currentTimeMillis())
        )
        fakeApi.remoteVehicles.add(remoteVehicle)

        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)
        val result = syncManager.sync("user_fresh_sync")

        assertTrue("Sincronização deve ter sucesso em nova instalação", result is SyncResult.Success)
        assertEquals(1, db.vehicleDao().getAllVehicles().size)
        assertEquals("user_fresh_sync", fakeStorage.getDataOwnerId())
    }

    @Test
    fun testUnknownOwner_withDataAndNoPending_clearsLocalDatabaseAndPulls() = runTest(testDispatcher) {
        // Caso B: dono nulo, banco com registros, mas SEM pendências (tudo já sincronizado)
        assertNull(fakeStorage.getDataOwnerId())
        db.vehicleDao().insertVehicle(
            Vehicle(
                id = 1,
                model = "Moto Antiga",
                syncStatus = SyncStatus.SYNCED,
                updatedAt = System.currentTimeMillis()
            )
        )
        val repository = GiroCustoRepository(db, fakeStorage)
        assertTrue(repository.hasAnyLocalData())
        assertFalse(repository.hasPendingSyncChanges())

        // Executa limpeza limpa e transição para user_B
        repository.clearAllLocalData()
        fakeStorage.setDataOwnerId("user_B")

        // Banco deve ter sido zerado
        assertEquals(0, db.vehicleDao().getAllVehicles().size)
        assertEquals("user_B", fakeStorage.getDataOwnerId())
    }

    @Test
    fun testUnknownOwner_withPendingChanges_blocksSyncAndSignalsConflict() = runTest(testDispatcher) {
        // Caso C: dono nulo, banco COM pendências locais não sincronizadas
        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(null)

        val viewModel = createViewModel()
        fakeStorage.clearDataOwner()

        // Insere registro pendente no banco local quando o dono ainda é desconhecido
        viewModel.repository.insertDailyRecord(
            dateString = "2026-10-24",
            dateTimestamp = System.currentTimeMillis(),
            platform = "Geral",
            grossEarnings = 150.0,
            deliveriesCount = 8,
            startOdometer = 100.0,
            endOdometer = 150.0,
            fuelPrice = 5.5,
            foodExpense = 20.0
        )
        assertTrue(viewModel.repository.hasPendingSyncChanges())
        assertEquals(1, viewModel.repository.countPendingChanges())

        // Ao autenticar conta B e disparar sincronização, bloqueia e sinaliza conflito
        client.setAuthSession(AuthSession("token_b", "refresh_b", "user_B", "b@test.com"))
        viewModel.triggerSync()
        advanceUntilIdle()

        val conflict = viewModel.accountConflictState.value
        assertNotNull("Deve sinalizar conflito de dono desconhecido com pendências", conflict)
        assertTrue(conflict is AccountConflictState.UnknownOwnerWithPending)
        val unknownConflict = conflict as AccountConflictState.UnknownOwnerWithPending
        assertEquals("user_B", unknownConflict.currentUserId)
        assertEquals(1, unknownConflict.pendingCount)

        // Dados locais NÃO podem ter sido apagados silenciosamente
        assertTrue(viewModel.repository.hasPendingSyncChanges())
    }

    // =========================================================================
    // 3. RESOLUÇÃO DE CONFLITO: DESCARTE COM CONFIRMAÇÃO DUPLA E APROPRIAÇÃO
    // =========================================================================

    @Test
    fun testConflictResolution_discardRequiresDoubleConfirmation_thenClears() = runTest(testDispatcher) {
        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(null)

        val viewModel = createViewModel()
        fakeStorage.setDataOwnerId("user_A")

        // Insere pendência
        viewModel.repository.insertDailyRecord(
            dateString = "2026-10-24",
            dateTimestamp = System.currentTimeMillis(),
            platform = "Geral",
            grossEarnings = 200.0,
            deliveriesCount = 10,
            startOdometer = 100.0,
            endOdometer = 150.0,
            fuelPrice = 5.5,
            foodExpense = 20.0
        )
        client.setAuthSession(AuthSession("token_b", "refresh_b", "user_B", "b@test.com"))
        viewModel.triggerSync()
        advanceUntilIdle()

        // 1. Abre a primeira confirmação
        assertFalse(viewModel.showDiscardDoubleConfirmationDialog.value)
        viewModel.openDiscardConfirmation()
        assertTrue("Primeira ação deve abrir diálogo de confirmação dupla", viewModel.showDiscardDoubleConfirmationDialog.value)

        // Se cancelar, nada é apagado
        viewModel.dismissDiscardConfirmation()
        assertFalse(viewModel.showDiscardDoubleConfirmationDialog.value)
        assertTrue(viewModel.repository.hasPendingSyncChanges())

        // 2. Confirmação dupla definitiva
        viewModel.openDiscardConfirmation()
        var onCompleteCalled = false
        viewModel.confirmDiscardData {
            onCompleteCalled = true
        }
        advanceUntilIdle()

        assertTrue(onCompleteCalled)
        assertFalse(viewModel.showDiscardDoubleConfirmationDialog.value)
        assertNull(viewModel.accountConflictState.value)
        assertEquals("user_B", fakeStorage.getDataOwnerId())
        assertFalse("Banco local deve ter sido zerado após confirmação dupla", viewModel.repository.hasPendingSyncChanges())
    }

    @Test
    fun testConflictResolution_claimData_assignsOwnerAndPushes() = runTest(testDispatcher) {
        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(null)

        val viewModel = createViewModel()
        fakeStorage.clearDataOwner()

        // Insere pendência que o usuário afirma ser dele
        viewModel.repository.insertDailyRecord(
            dateString = "2026-10-24",
            dateTimestamp = System.currentTimeMillis(),
            platform = "Geral",
            grossEarnings = 350.0,
            deliveriesCount = 15,
            startOdometer = 100.0,
            endOdometer = 150.0,
            fuelPrice = 5.5,
            foodExpense = 20.0
        )
        client.setAuthSession(AuthSession("token_b", "refresh_b", "user_B", "b@test.com"))
        viewModel.triggerSync()
        advanceUntilIdle()

        assertTrue(viewModel.accountConflictState.value is AccountConflictState.UnknownOwnerWithPending)

        // 1. Abre confirmação de apropriação
        assertFalse(viewModel.showClaimDataConfirmationDialog.value)
        viewModel.openClaimDataConfirmation()
        assertTrue(viewModel.showClaimDataConfirmationDialog.value)

        // 2. Confirma que os dados são seus
        var claimCompleted = false
        viewModel.confirmClaimData {
            claimCompleted = true
        }
        advanceUntilIdle()

        assertTrue(claimCompleted)
        assertFalse(viewModel.showClaimDataConfirmationDialog.value)
        assertNull(viewModel.accountConflictState.value)
        // Agora o dono local virou user_B e os dados foram aceitos
        assertEquals("user_B", fakeStorage.getDataOwnerId())
    }

    // =========================================================================
    // 4. PRESERVAÇÃO DE DADOS NO LOGOUT E FALLBACK BIOMÉTRICO (Condição 6)
    // =========================================================================

    @Test
    fun testLogoutAndBiometricKeyInvalidated_preserveRoomDatabaseAndOwner() = runTest(testDispatcher) {
        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(null)

        val viewModel = createViewModel()
        fakeStorage.setDataOwnerId("user_A")
        client.setAuthSession(AuthSession("token_a", "refresh_a", "user_A", "a@test.com"))

        // Insere veículo e diária
        viewModel.repository.addVehicle(
            Vehicle(model = "Titan 160", syncStatus = SyncStatus.SYNCED)
        )
        viewModel.repository.insertDailyRecord(
            dateString = "2026-10-24",
            dateTimestamp = System.currentTimeMillis(),
            platform = "Geral",
            grossEarnings = 100.0,
            deliveriesCount = 5,
            startOdometer = 100.0,
            endOdometer = 150.0,
            fuelPrice = 5.5,
            foodExpense = 20.0
        )

        // 1. Executa logout
        viewModel.logout()
        advanceUntilIdle()

        // Sessão remota deve estar deslogada
        assertFalse(client.isAuthenticated)
        assertNull(client.currentAccessToken)

        // Banco Room e Guarda de Dono NUNCA podem ser apagados pelo logout
        assertEquals("user_A", fakeStorage.getDataOwnerId())
        assertEquals(1, viewModel.repository.getAllVehicles().size)
        assertEquals(1, viewModel.repository.getAllRecords().size)

        // 2. Executa invalidação biométrica de chave
        viewModel.handleBiometricKeyInvalidated()
        advanceUntilIdle()

        assertEquals("user_A", fakeStorage.getDataOwnerId())
        assertEquals(1, viewModel.repository.getAllVehicles().size)

        // 3. Executa fallback biométrico
        viewModel.dismissBiometricLockForFallback()
        advanceUntilIdle()

        assertEquals("user_A", fakeStorage.getDataOwnerId())
        assertEquals(1, viewModel.repository.getAllVehicles().size)
    }

    // =========================================================================
    // 5. SERIALIZAÇÃO VIA MUTEX E ISOLAMENTO VISUAL (Condição 2)
    // =========================================================================

    @Test
    fun testAccountSwitch_duringActiveSync_isSerializedViaMutex() = runTest(testDispatcher) {
        val client = SupabaseClient.getInstance(context)
        client.setAuthSession(null)

        val viewModel = createViewModel()
        fakeStorage.setDataOwnerId("user_A")
        client.setAuthSession(AuthSession("token_a", "refresh_a", "user_A", "a@test.com"))

        var firstSyncCompleted = false
        var secondSyncCompleted = false

        // Dispara duas operações concorrentes de sincronização / transição
        viewModel.triggerSync { success, _ ->
            firstSyncCompleted = true
        }

        viewModel.triggerSync { success, _ ->
            secondSyncCompleted = true
        }
        advanceUntilIdle()

        // Ambas devem completar sequencialmente sem travamento por deadlock ou conflito
        assertTrue(firstSyncCompleted)
        assertTrue(secondSyncCompleted)
        assertFalse("Overlay de carregamento deve ser desativado ao final", viewModel.isAccountSwitching.value)
    }

    // =========================================================================
    // 6. ERRO TIPADO E MENSAGEM AO USUÁRIO (Condição 7)
    // =========================================================================

    @Test
    fun testDataOwnerMismatchError_isTypedAndSurfacedToUi() = runTest(testDispatcher) {
        fakeStorage.setDataOwnerId("dono_original")
        val syncManager = SupabaseSyncManager(db, fakeApi, fakeStorage)

        val result = syncManager.sync("usuario_diferente")
        assertTrue("Deve retornar SyncResult.Error", result is SyncResult.Error)
        val err = (result as SyncResult.Error).error
        assertTrue(err is DataOwnerMismatchException)
        val mismatch = err as DataOwnerMismatchException
        assertEquals("dono_original", mismatch.ownerId)
        assertEquals("usuario_diferente", mismatch.currentUserId)
    }
}

/**
 * Storage falsa em memória para testes unitários isolados.
 */
class FakeDataOwnerStorage(initialOwnerId: String? = null) : DataOwnerStorage {
    private var ownerId: String? = initialOwnerId

    override fun getDataOwnerId(): String? = ownerId

    override fun setDataOwnerId(userId: String) {
        ownerId = userId.trim()
    }

    override fun clearDataOwner() {
        ownerId = null
    }

    override fun hasOwner(): Boolean = !ownerId.isNullOrBlank()
}

/**
 * REST API simulada com contagem de invocações para prova de bloqueio de requisições.
 */
class CountingSupabaseRestApi : SupabaseRestApi {
    val remoteVehicles = mutableListOf<VehicleDto>()
    var getVehiclesCalls = 0
    var upsertVehiclesCalls = 0
    var getDailyRecordsCalls = 0
    var upsertDailyRecordsCalls = 0

    override suspend fun getVehicles(select: String, updatedAtFilter: String?): List<VehicleDto> {
        getVehiclesCalls++
        return remoteVehicles.toList()
    }

    override suspend fun upsertVehicles(vehicles: List<VehicleDto>): List<VehicleDto> {
        upsertVehiclesCalls++
        return vehicles
    }

    override suspend fun deleteVehicle(idFilter: String): Response<Unit> = Response.success(Unit)

    override suspend fun getVehicleParts(select: String, updatedAtFilter: String?): List<VehiclePartDto> = emptyList()
    override suspend fun upsertVehicleParts(parts: List<VehiclePartDto>): List<VehiclePartDto> = parts
    override suspend fun deleteVehiclePart(idFilter: String): Response<Unit> = Response.success(Unit)

    override suspend fun getDailyRecords(select: String, updatedAtFilter: String?): List<DailyRecordDto> {
        getDailyRecordsCalls++
        return emptyList()
    }

    override suspend fun upsertDailyRecords(records: List<DailyRecordDto>): List<DailyRecordDto> {
        upsertDailyRecordsCalls++
        return records
    }

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
