package com.example

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.*
import com.example.network.supabase.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserGoalsDatabaseAndSyncTest {

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GiroCustoDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    private lateinit var context: Context
    private lateinit var inMemoryDb: GiroCustoDatabase
    private lateinit var userGoalsDao: UserGoalsDao
    private lateinit var dataOwnerStorage: DataOwnerStorage
    private lateinit var repository: GiroCustoRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, GiroCustoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userGoalsDao = inMemoryDb.userGoalsDao()
        dataOwnerStorage = SharedPreferencesDataOwnerStorage(context)
        dataOwnerStorage.clearDataOwner()
        repository = GiroCustoRepository(inMemoryDb, dataOwnerStorage)

        // Limpa arquivos de preferências que serão testados
        context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
        dataOwnerStorage.clearDataOwner()
        context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // =========================================================================
    // 1. TESTES DE MIGRAÇÃO ROOM 13 -> 14 (MIGRATION_13_14 E SCHEMA 14.JSON)
    // =========================================================================

    @Test
    fun testMigration13To14_createsUserGoalsTableAndIndexesWithMigrationTestHelper() = runTest {
        val dbName = "migration_13_14_test_${System.currentTimeMillis()}.db"

        // 1. Cria o banco de dados na versão 13 a partir do schema 13.json
        val dbV13 = migrationTestHelper.createDatabase(dbName, 13)

        // 2. Insere dados em veículo na versão 13 para verificar integridade pós-migração
        val cvVehicle = ContentValues().apply {
            put("id", 101L)
            put("model", "Honda CG 160 Titan v13")
            put("averageConsumption", 40.0)
            put("fuelType", "Gasolina")
            put("monthlyFixedCosts", 180.0)
            put("plannedWorkDays", 22)
            put("currentOdometer", 15350.0)
            put("active", 1)
            put("vehicleType", "MOTO")
            put("brand", "Honda")
            put("manufacturingYear", "2024")
            put("tankCapacity", 16.0)
            put("hasTopBox", 0)
            put("hasDeliveryBag", 0)
            put("carBodyType", "")
            put("remoteId", "veh-v13-uuid-101")
            put("syncStatus", "SYNCED")
            put("updatedAt", 1761000000000L)
            put("isDeleted", 0)
        }
        dbV13.insert("vehicle", 0, cvVehicle)
        dbV13.close()

        // 3. Executa a migração para a versão 14 e valida rigorosamente contra 14.json
        val dbV14 = migrationTestHelper.runMigrationsAndValidate(
            dbName,
            14,
            true,
            GiroCustoDatabase.MIGRATION_13_14
        )

        // 4. Confirma que os dados da versão 13 permaneceram intactos
        val cursorVehicle = dbV14.query("SELECT model, remoteId FROM vehicle WHERE id = 101")
        assertTrue("Vehicle deve existir após migração", cursorVehicle.moveToFirst())
        assertEquals("Honda CG 160 Titan v13", cursorVehicle.getString(0))
        assertEquals("veh-v13-uuid-101", cursorVehicle.getString(1))
        cursorVehicle.close()

        // 5. Confirma que a nova tabela user_goals foi criada e aceita inserção e busca
        val cvGoals = ContentValues().apply {
            put("id", 1)
            put("dailyGoalGross", 250.0)
            put("dailyGoalNet", 160.0)
            put("dailyGoalKm", 120.0)
            put("dailyGoalDeliveries", 18)
            put("weeklyGoalGross", 1500.0)
            put("weeklyGoalNet", 950.0)
            put("weeklyGoalKm", 700.0)
            put("weeklyGoalDeliveries", 100)
            put("monthlyGoalGross", 6000.0)
            put("monthlyGoalNet", 4000.0)
            put("monthlyGoalKm", 3000.0)
            put("monthlyGoalDeliveries", 450)
            put("remoteId", "goal-uuid-1")
            put("syncStatus", "PENDING_UPDATE")
            put("updatedAt", 1761050000000L)
            put("isDeleted", 0)
        }
        val insertedRow = dbV14.insert("user_goals", 0, cvGoals)
        assertTrue("Inserção em user_goals deve ter sucesso", insertedRow > 0)

        val cursorGoals = dbV14.query("SELECT dailyGoalGross, dailyGoalNet, remoteId, syncStatus FROM user_goals WHERE id = 1")
        assertTrue("user_goals deve existir", cursorGoals.moveToFirst())
        assertEquals(250.0, cursorGoals.getDouble(0), 0.001)
        assertEquals(160.0, cursorGoals.getDouble(1), 0.001)
        assertEquals("goal-uuid-1", cursorGoals.getString(2))
        assertEquals("PENDING_UPDATE", cursorGoals.getString(3))
        cursorGoals.close()

        // 6. Confirma que os índices de user_goals foram criados
        val cursorIndexes = dbV14.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'user_goals'")
        val indexNames = mutableListOf<String>()
        while (cursorIndexes.moveToNext()) {
            indexNames.add(cursorIndexes.getString(0))
        }
        cursorIndexes.close()
        assertTrue("Índice index_user_goals_remoteId deve existir", indexNames.contains("index_user_goals_remoteId"))
        assertTrue("Índice index_user_goals_syncStatus deve existir", indexNames.contains("index_user_goals_syncStatus"))

        dbV14.close()

        // 7. Confirmação de abertura do Room na versão 14 com DAOs
        val roomDb = Room.databaseBuilder(context, GiroCustoDatabase::class.java, dbName)
            .addMigrations(GiroCustoDatabase.MIGRATION_13_14)
            .allowMainThreadQueries()
            .build()

        val loadedGoals = roomDb.userGoalsDao().getUserGoals()
        assertNotNull(loadedGoals)
        assertEquals(250.0, loadedGoals?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(160.0, loadedGoals?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals("goal-uuid-1", loadedGoals?.remoteId)
        roomDb.close()
    }

    // =========================================================================
    // 2. TESTES DE DAO E ENTIDADE UserGoals
    // =========================================================================

    @Test
    fun testUserGoalsDao_singletonOperationsAndFlow() = runTest {
        assertNull(userGoalsDao.getUserGoals())
        assertNull(userGoalsDao.getUserGoalsFlow().first())

        val initialGoals = UserGoals(
            id = 1,
            dailyGoalGross = 220.0,
            dailyGoalNet = 140.0,
            dailyGoalKm = 110.0,
            dailyGoalDeliveries = 16,
            weeklyGoalGross = 1300.0,
            weeklyGoalNet = 850.0,
            weeklyGoalKm = 650.0,
            weeklyGoalDeliveries = 95,
            monthlyGoalGross = 5500.0,
            monthlyGoalNet = 3500.0,
            monthlyGoalKm = 2600.0,
            monthlyGoalDeliveries = 420,
            remoteId = "remote-1",
            syncStatus = SyncStatus.SYNCED
        )
        userGoalsDao.insertOrUpdateUserGoals(initialGoals)

        val retrieved = userGoalsDao.getUserGoals()
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.id)
        assertEquals(220.0, retrieved?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(140.0, retrieved?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals("remote-1", retrieved?.remoteId)
        assertEquals(SyncStatus.SYNCED, retrieved?.syncStatus)

        // Flow emite o valor gravado
        val flowValue = userGoalsDao.getUserGoalsFlow().first()
        assertNotNull(flowValue)
        assertEquals(220.0, flowValue?.dailyGoalGross ?: 0.0, 0.001)

        // Busca por remoteId
        val byRemote = userGoalsDao.getUserGoalsByRemoteId("remote-1")
        assertNotNull(byRemote)
        assertEquals(1, byRemote?.id)

        // Pendências (quando SYNCED, getPendingUserGoals é null)
        assertNull(userGoalsDao.getPendingUserGoals())

        // Atualização para PENDING_UPDATE
        userGoalsDao.insertOrUpdateUserGoals(initialGoals.copy(dailyGoalGross = 300.0, syncStatus = SyncStatus.PENDING_UPDATE))
        val pending = userGoalsDao.getPendingUserGoals()
        assertNotNull(pending)
        assertEquals(300.0, pending?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(SyncStatus.PENDING_UPDATE, pending?.syncStatus)

        // Exclusão total
        userGoalsDao.deleteAllGoals()
        assertNull(userGoalsDao.getUserGoals())
    }

    // =========================================================================
    // 3. TESTES DO REPOSITÓRIO (MÉTODOS DE GOALS E MIGRAÇÃO TRANSPARENTE)
    // =========================================================================

    @Test
    fun testRepository_goalsMethodsAndPendingChecks() = runTest {
        assertFalse(repository.hasAnyLocalData())
        assertFalse(repository.hasPendingSyncChanges())
        assertEquals(0, repository.countPendingChanges())

        // Inserção inicial
        val goals = UserGoals(id = 1, dailyGoalGross = 200.0, syncStatus = SyncStatus.SYNCED)
        repository.insertInitialUserGoals(goals)

        assertTrue(repository.hasAnyLocalData())
        assertFalse(repository.hasPendingSyncChanges())
        assertEquals(0, repository.countPendingChanges())

        // Atualização via repositório marca PENDING_UPDATE e gera remoteId se vazio
        repository.updateUserGoals(goals.copy(dailyGoalGross = 350.0, remoteId = ""))
        val updated = repository.getUserGoals()
        assertNotNull(updated)
        assertEquals(350.0, updated?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(SyncStatus.PENDING_UPDATE, updated?.syncStatus)
        assertTrue(updated?.remoteId?.isNotBlank() == true)

        assertTrue(repository.hasPendingSyncChanges())
        assertEquals(1, repository.countPendingChanges())
    }

    @Test
    fun testRepository_migrateLegacyGoalsIfNeeded_withLegacyCustomGoals() = runTest {
        val legacyPrefs = context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putFloat("daily_goal_gross", 280.0f)
            .putFloat("daily_goal_net", 180.0f)
            .putFloat("daily_goal_km", 130.0f)
            .putInt("daily_goal_deliveries", 20)
            .putFloat("weekly_goal_gross", 1600.0f)
            .putFloat("weekly_goal_net", 1100.0f)
            .putFloat("weekly_goal_km", 750.0f)
            .putInt("weekly_goal_deliveries", 110)
            .putFloat("monthly_goal_gross", 6500.0f)
            .putFloat("monthly_goal_net", 4200.0f)
            .putFloat("monthly_goal_km", 3100.0f)
            .putInt("monthly_goal_deliveries", 480)
            .putBoolean("is_dark_theme", false)
            .commit()

        repository.migrateLegacyGoalsIfNeeded(context)

        val migratedGoals = repository.getUserGoals()
        assertNotNull(migratedGoals)
        assertEquals(280.0, migratedGoals?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(180.0, migratedGoals?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(130.0, migratedGoals?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(20, migratedGoals?.dailyGoalDeliveries)
        assertEquals(1600.0, migratedGoals?.weeklyGoalGross ?: 0.0, 0.001)
        assertEquals(1100.0, migratedGoals?.weeklyGoalNet ?: 0.0, 0.001)
        assertEquals(750.0, migratedGoals?.weeklyGoalKm ?: 0.0, 0.001)
        assertEquals(110, migratedGoals?.weeklyGoalDeliveries)
        assertEquals(6500.0, migratedGoals?.monthlyGoalGross ?: 0.0, 0.001)
        assertEquals(4200.0, migratedGoals?.monthlyGoalNet ?: 0.0, 0.001)
        assertEquals(3100.0, migratedGoals?.monthlyGoalKm ?: 0.0, 0.001)
        assertEquals(480, migratedGoals?.monthlyGoalDeliveries)
        assertEquals(SyncStatus.PENDING_UPDATE, migratedGoals?.syncStatus)

        // is_dark_theme migrado para giro_custo_preferences e giro_custo_app_prefs
        val newPrefs = context.getSharedPreferences("giro_custo_preferences", Context.MODE_PRIVATE)
        assertEquals(false, newPrefs.getBoolean("is_dark_theme", true))

        val appPrefs = context.getSharedPreferences("giro_custo_app_prefs", Context.MODE_PRIVATE)
        assertEquals(false, appPrefs.getBoolean("is_dark_theme", true))

        // Arquivo legado foi limpo
        assertFalse(legacyPrefs.contains("daily_goal_gross"))
        assertFalse(legacyPrefs.contains("is_dark_theme"))
    }

    @Test
    fun testRepository_migrateLegacyGoalsIfNeeded_freshInstallDefaults() = runTest {
        val legacyPrefs = context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        legacyPrefs.edit().clear().commit()

        repository.migrateLegacyGoalsIfNeeded(context)

        val defaultGoals = repository.getUserGoals()
        assertNotNull(defaultGoals)
        assertEquals(200.0, defaultGoals?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(130.0, defaultGoals?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(100.0, defaultGoals?.dailyGoalKm ?: 0.0, 0.001)
        assertEquals(15, defaultGoals?.dailyGoalDeliveries)
        assertEquals(1200.0, defaultGoals?.weeklyGoalGross ?: 0.0, 0.001)
        assertEquals(800.0, defaultGoals?.weeklyGoalNet ?: 0.0, 0.001)
        assertEquals(600.0, defaultGoals?.weeklyGoalKm ?: 0.0, 0.001)
        assertEquals(90, defaultGoals?.weeklyGoalDeliveries)
        assertEquals(5000.0, defaultGoals?.monthlyGoalGross ?: 0.0, 0.001)
        assertEquals(3200.0, defaultGoals?.monthlyGoalNet ?: 0.0, 0.001)
        assertEquals(2500.0, defaultGoals?.monthlyGoalKm ?: 0.0, 0.001)
        assertEquals(400, defaultGoals?.monthlyGoalDeliveries)
        assertEquals(SyncStatus.SYNCED, defaultGoals?.syncStatus)
    }

    @Test
    fun testRepository_migrateLegacyGoalsIfNeeded_idempotentWhenAlreadyPresent() = runTest {
        // Já existe um registro no banco
        repository.insertInitialUserGoals(UserGoals(id = 1, dailyGoalGross = 999.0))

        val legacyPrefs = context.getSharedPreferences("giro_custo_goals", Context.MODE_PRIVATE)
        legacyPrefs.edit().putFloat("daily_goal_gross", 444.0f).commit()

        repository.migrateLegacyGoalsIfNeeded(context)

        // Não deve ser sobrescrito
        val currentGoals = repository.getUserGoals()
        assertEquals(999.0, currentGoals?.dailyGoalGross ?: 0.0, 0.001)
    }

    // =========================================================================
    // 4. TESTES DE SINCRONIZAÇÃO SUPABASE (PUSH, PULL E LWW)
    // =========================================================================

    @Test
    fun testSyncManager_pushLocalGoals_whenPendingUpdate() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val userId = "user-goals-123"
        dataOwnerStorage.setDataOwnerId(userId)

        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi, dataOwnerStorage)

        val localGoals = UserGoals(
            id = 1,
            dailyGoalGross = 275.0,
            dailyGoalNet = 175.0,
            remoteId = "rem-goal-1",
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 1000L
        )
        userGoalsDao.insertOrUpdateUserGoals(localGoals)

        syncManager.pushLocalChanges(userId)

        // Verifica envio para a API
        assertEquals(1, fakeApi.upsertedUserGoals.size)
        val sentDto = fakeApi.upsertedUserGoals.first()
        assertEquals("rem-goal-1", sentDto.id)
        assertEquals(userId, sentDto.userId)
        assertEquals(275.0, sentDto.dailyGoalGross, 0.001)
        assertEquals(175.0, sentDto.dailyGoalNet, 0.001)

        // Verifica transição local para SYNCED
        val updatedLocal = userGoalsDao.getUserGoals()
        assertEquals(SyncStatus.SYNCED, updatedLocal?.syncStatus)
        assertEquals("rem-goal-1", updatedLocal?.remoteId)
    }

    @Test
    fun testSyncManager_pullRemoteGoals_remoteNewerLWW() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val userId = "user-goals-456"
        dataOwnerStorage.setDataOwnerId(userId)

        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi, dataOwnerStorage)

        // Dado local antigo
        val localGoals = UserGoals(
            id = 1,
            dailyGoalGross = 200.0,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = 1000L
        )
        userGoalsDao.insertOrUpdateUserGoals(localGoals)

        // Dado remoto mais recente (timestamp 2000L)
        fakeApi.remoteUserGoals.add(
            UserGoalsDto(
                id = "rem-goal-newer",
                userId = userId,
                dailyGoalGross = 380.0,
                dailyGoalNet = 250.0,
                dailyGoalKm = 150.0,
                dailyGoalDeliveries = 25,
                weeklyGoalGross = 2000.0,
                weeklyGoalNet = 1300.0,
                weeklyGoalKm = 800.0,
                weeklyGoalDeliveries = 130,
                monthlyGoalGross = 8000.0,
                monthlyGoalNet = 5000.0,
                monthlyGoalKm = 3500.0,
                monthlyGoalDeliveries = 500,
                updatedAt = SupabaseMappers.toIsoString(2000L)
            )
        )

        syncManager.pullRemoteChanges(userId)

        val updatedLocal = userGoalsDao.getUserGoals()
        assertNotNull(updatedLocal)
        assertEquals("rem-goal-newer", updatedLocal?.remoteId)
        assertEquals(380.0, updatedLocal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(250.0, updatedLocal?.dailyGoalNet ?: 0.0, 0.001)
        assertEquals(SyncStatus.SYNCED, updatedLocal?.syncStatus)
    }

    @Test
    fun testSyncManager_pullRemoteGoals_localNewerOfflineLWW_preservesLocal() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        val userId = "user-goals-789"
        dataOwnerStorage.setDataOwnerId(userId)

        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi, dataOwnerStorage)

        // Local alterado offline mais recente (timestamp 3000L, PENDING_UPDATE)
        val localGoals = UserGoals(
            id = 1,
            dailyGoalGross = 450.0,
            syncStatus = SyncStatus.PENDING_UPDATE,
            updatedAt = 3000L
        )
        userGoalsDao.insertOrUpdateUserGoals(localGoals)

        // Remoto defasado (timestamp 1500L)
        fakeApi.remoteUserGoals.add(
            UserGoalsDto(
                id = "rem-defasado",
                userId = userId,
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
                updatedAt = SupabaseMappers.toIsoString(1500L)
            )
        )

        syncManager.pullRemoteChanges(userId)

        // O dado local offline NÃO deve ter sido sobrescrito pelo remoto defasado
        val currentLocal = userGoalsDao.getUserGoals()
        assertEquals(450.0, currentLocal?.dailyGoalGross ?: 0.0, 0.001)
        assertEquals(SyncStatus.PENDING_UPDATE, currentLocal?.syncStatus)
    }

    @Test
    fun testSyncManager_hasAnyLocalData_failsafeBlocksPushIfUnowned() = runTest {
        val fakeApi = FakeSupabaseRestApi()
        // Nenhuma propriedade definida no aparelho
        dataOwnerStorage.clearDataOwner()

        val syncManager = SupabaseSyncManager(inMemoryDb, fakeApi, dataOwnerStorage)

        // Apenas user_goals existe no banco local
        userGoalsDao.insertOrUpdateUserGoals(UserGoals(id = 1, dailyGoalGross = 200.0))

        assertTrue("hasAnyLocalData deve retornar true quando apenas metas existem", syncManager.hasAnyLocalData())

        try {
            syncManager.pushLocalChanges("intruder-user-id")
            fail("Deveria lançar DataOwnerMismatchException para evitar vazamento de metas locais para outra conta")
        } catch (e: DataOwnerMismatchException) {
            assertEquals("PUSH", e.operation)
            assertNull(e.ownerId)
            assertEquals("intruder-user-id", e.currentUserId)
        }
    }
}
