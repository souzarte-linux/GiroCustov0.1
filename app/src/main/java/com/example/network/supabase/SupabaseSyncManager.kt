package com.example.network.supabase

import android.util.Log
import androidx.room.withTransaction
import com.example.data.*
import com.example.util.CurrencyUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.util.UUID

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: Throwable) : SyncResult()
}

class SupabaseSyncManager(
    private val db: GiroCustoDatabase,
    private val restApi: SupabaseRestApi,
    private val dataOwnerStorage: DataOwnerStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val client: SupabaseClient? = null
) {
    constructor(db: GiroCustoDatabase, client: SupabaseClient, dataOwnerStorage: DataOwnerStorage, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : this(db, client.restApi, dataOwnerStorage, ioDispatcher, client)
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()
    private val userProfileDao = db.userProfileDao()
    private val platformDao = db.platformDao()
    private val fuelRefillDao = db.fuelRefillDao()
    private val maintenanceRecordDao = db.maintenanceRecordDao()
    private val userGoalsDao = db.userGoalsDao()
    private val syncOutboxDao = db.syncOutboxDao()
    private val goalsPatchAdapter = Moshi.Builder().build().adapter(UserGoalsSyncPatchDto::class.java)

    internal suspend fun hasAnyLocalData(): Boolean {
        return vehicleDao.getAllVehicles().isNotEmpty() ||
                vehicleDao.getPendingVehicles().isNotEmpty() ||
                partDao.getAllParts().isNotEmpty() ||
                recordDao.getAllRecords().isNotEmpty() ||
                platformDao.getAllPlatforms().isNotEmpty() ||
                userProfileDao.getUserProfile() != null ||
                userGoalsDao.getUserGoals() != null
    }

    suspend fun sync(userId: String): SyncResult = withContext(ioDispatcher) {
        try {
            Log.d("SupabaseSyncManager", "Iniciando sincronização completa para userId: $userId")

            // 1. PUSH: Envia dados locais pendentes para o Supabase com suporte a renovação de sessão sob 401
            executeWithRetryOn401 { pushLocalChanges(userId) }

            // 2. PULL: Busca dados remotos do Supabase e aplica localmente (LWW)
            executeWithRetryOn401 { pullRemoteChanges(userId) }

            if (dataOwnerStorage.getDataOwnerId() == null) {
                dataOwnerStorage.setDataOwnerId(userId)
            }

            Log.d("SupabaseSyncManager", "Sincronização concluída com sucesso!")
            SyncResult.Success("Sincronização concluída com sucesso")
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Erro durante a sincronização: ${e.message}", e)
            val mappedError = if (isHttp401(e)) {
                SessionExpiredException("Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando.", e)
            } else {
                e
            }
            SyncResult.Error(mappedError)
        }
    }

    private suspend fun <T> executeWithRetryOn401(block: suspend () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            val syncClient = client ?: SupabaseClient.getInstance()
            if (isHttp401(e)) {
                Log.w("SupabaseSyncManager", "Recebido HTTP 401 durante sincronização. Tentando renovar sessão...")
                val refreshResult = syncClient.refreshCurrentSession()
                if (refreshResult.isSuccess) {
                    Log.d("SupabaseSyncManager", "Sessão renovada com sucesso após HTTP 401! Retentando operação...")
                    return block()
                } else {
                    Log.e("SupabaseSyncManager", "Falha ao renovar sessão após HTTP 401: ${refreshResult.exceptionOrNull()?.message}")
                    throw SessionExpiredException("Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando.", e)
                }
            }
            throw e
        }
    }

    private fun isHttp401(throwable: Throwable): Boolean {
        if (throwable is HttpException && throwable.code() == 401) return true
        val msg = throwable.message?.lowercase() ?: ""
        return "401" in msg || "jwt expired" in msg || "unauthorized" in msg
    }

    // =========================================================================
    // PUSH (Local -> Nuvem)
    // =========================================================================
    internal suspend fun pushLocalChanges(userId: String) {
        val ownerId = dataOwnerStorage.getDataOwnerId()
        if (ownerId != null && ownerId != userId) {
            throw DataOwnerMismatchException(
                ownerId = ownerId,
                currentUserId = userId,
                operation = "PUSH"
            )
        }
        if (ownerId == null && hasAnyLocalData()) {
            throw DataOwnerMismatchException(
                ownerId = null,
                currentUserId = userId,
                operation = "PUSH"
            )
        }
        // --- 1. VEÍCULOS ---
        val pendingVehicles = vehicleDao.getPendingVehicles()
        if (pendingVehicles.isNotEmpty()) {
            val dtos = pendingVehicles.map { v ->
                val remId = if (v.remoteId.isNotBlank()) v.remoteId else UUID.randomUUID().toString()
                v.copy(remoteId = remId).toDto(userId)
            }
            restApi.upsertVehicles(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingVehicles.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.model == dto.model) }
                    if (local != null) {
                        vehicleDao.updateVehicle(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // Recupera o veículo ativo remoto para servir de FK nas tabelas filhas
        val activeVehicle = vehicleDao.getActiveVehicle()
        val defaultRemoteVehicleId = activeVehicle?.remoteId?.takeIf { it.isNotBlank() }

        // --- 2. PEÇAS DE DESGASTE ---
        val pendingParts = partDao.getPendingParts()
        if (pendingParts.isNotEmpty()) {
            val allVehicles = vehicleDao.getAllVehicles()
            val dtos = pendingParts.map { part ->
                val remId = if (part.remoteId.isNotBlank()) part.remoteId else UUID.randomUUID().toString()
                val partRemoteVehicleId = allVehicles.firstOrNull { it.id == part.vehicleId }?.remoteId?.takeIf { it.isNotBlank() } ?: defaultRemoteVehicleId
                part.copy(remoteId = remId).toDto(userId, partRemoteVehicleId)
            }
            restApi.upsertVehicleParts(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingParts.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.name == dto.name) }
                    if (local != null) {
                        partDao.updatePart(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // --- 3. DIÁRIAS (DAILY RECORDS) ---
        val pendingRecords = recordDao.getPendingRecords()
        if (pendingRecords.isNotEmpty()) {
            val dtos = pendingRecords.map { r ->
                val remId = if (r.remoteId.isNotBlank()) r.remoteId else UUID.randomUUID().toString()
                r.copy(remoteId = remId).toDto(userId)
            }
            restApi.upsertDailyRecords(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingRecords.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.platform == dto.platform) }
                    if (local != null) {
                        recordDao.updateRecord(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // --- 4. PERFIL DO USUÁRIO ---
        val pendingProfile = userProfileDao.getPendingUserProfile()
        if (pendingProfile != null) {
            val remId = if (pendingProfile.remoteId.isNotBlank()) pendingProfile.remoteId else UUID.randomUUID().toString()
            val dto = pendingProfile.copy(remoteId = remId).toDto(userId)
            restApi.upsertUserProfiles(listOf(dto))
            userProfileDao.insertOrUpdateUserProfile(pendingProfile.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
        }

        // --- 5. PLATAFORMAS ---
        val pendingPlatforms = platformDao.getPendingPlatforms()
        if (pendingPlatforms.isNotEmpty()) {
            val dtos = pendingPlatforms.map { p ->
                val remId = if (p.remoteId.isNotBlank()) p.remoteId else UUID.randomUUID().toString()
                p.copy(remoteId = remId).toDto(userId)
            }
            restApi.upsertPlatforms(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingPlatforms.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.name == dto.name) }
                    if (local != null) {
                        platformDao.updatePlatform(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // --- 6. ABASTECIMENTOS (FUEL REFILLS) ---
        val pendingRefills = fuelRefillDao.getPendingRefills()
        if (pendingRefills.isNotEmpty()) {
            val dtos = pendingRefills.map { ref ->
                val remId = if (ref.remoteId.isNotBlank()) ref.remoteId else UUID.randomUUID().toString()
                val vehicle = vehicleDao.getVehicleById(ref.vehicleId)
                val remVehicleId = vehicle?.remoteId?.takeIf { it.isNotBlank() } ?: defaultRemoteVehicleId
                ref.copy(remoteId = remId).toDto(userId, remVehicleId)
            }
            restApi.upsertFuelRefills(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingRefills.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.totalPaid == CurrencyUtils.toCents(dto.totalPaid)) }
                    if (local != null) {
                        fuelRefillDao.updateRefill(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // --- 7. MANUTENÇÕES ---
        val pendingMaintenance = maintenanceRecordDao.getPendingRecords()
        if (pendingMaintenance.isNotEmpty()) {
            val dtos = pendingMaintenance.map { m ->
                val remId = if (m.remoteId.isNotBlank()) m.remoteId else UUID.randomUUID().toString()
                val vehicle = vehicleDao.getVehicleById(m.vehicleId)
                val remVehicleId = vehicle?.remoteId?.takeIf { it.isNotBlank() } ?: defaultRemoteVehicleId
                m.copy(remoteId = remId).toDto(userId, remVehicleId)
            }
            restApi.upsertMaintenanceRecords(dtos)
            db.withTransaction {
                for (dto in dtos) {
                    val local = pendingMaintenance.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.description == dto.description) }
                    if (local != null) {
                        maintenanceRecordDao.updateRecord(local.copy(remoteId = dto.id, syncStatus = SyncStatus.SYNCED))
                    }
                }
            }
        }

        // --- 8. METAS DO USUÁRIO ---
        dispatchGoalsOutbox(userId)
    }

    /**
     * Goal changes are sent through the RPC exclusively.  Unlike the former upsert,
     * this preserves the same operation id across process death and retries.
     */
    private suspend fun dispatchGoalsOutbox(userId: String) {
        val pendingGoals = userGoalsDao.getPendingUserGoals()
        if (pendingGoals != null && syncOutboxDao.getDispatchableForUser(userId).isEmpty()) {
            val patch = pendingGoals.toFullGoalsPatch()
            db.withTransaction {
                syncOutboxDao.insert(
                    SyncOutboxOperation(
                        operationId = patch.operationId,
                        userId = userId,
                        operationType = "SYNC_USER_GOALS_PATCH",
                        serializedPayload = goalsPatchAdapter.toJson(patch),
                        baseRevision = patch.baseRevision
                    )
                )
            }
        }
        for (operation in syncOutboxDao.getDispatchableForUser(userId)) {
            if (operation.attemptCount >= MAX_GOALS_SYNC_ATTEMPTS) {
                syncOutboxDao.setState(operation.operationId, OutboxState.DEAD_LETTER, "Limite de tentativas atingido")
                continue
            }
            val patch = goalsPatchAdapter.fromJson(operation.serializedPayload)
            if (patch == null || patch.operationId != operation.operationId) {
                syncOutboxDao.setState(operation.operationId, OutboxState.DEAD_LETTER, "Payload local inválido")
                continue
            }
            syncOutboxDao.markInFlight(operation.operationId)
            try {
                val response = restApi.syncUserGoals(SyncUserGoalsRpcRequest(patch))
                when (response.status) {
                    "SUCCESS", "NO_OP" -> db.withTransaction {
                        response.data?.let { userGoalsDao.insertOrUpdateUserGoals(it.toEntity(existingId = 1)) }
                        syncOutboxDao.setState(operation.operationId, OutboxState.SUCCESS)
                    }
                    "CONFLICT" -> syncOutboxDao.setState(
                        operation.operationId,
                        OutboxState.CONFLICT,
                        response.conflictingFields?.joinToString(",") ?: response.message
                    )
                    else -> syncOutboxDao.setState(operation.operationId, OutboxState.DEAD_LETTER, response.message ?: "Resposta RPC inválida")
                }
            } catch (error: Exception) {
                syncOutboxDao.setState(operation.operationId, OutboxState.FAILED_RETRYABLE, error.message)
                throw error
            }
        }
        syncOutboxDao.deleteSuccessfulBefore(System.currentTimeMillis() - OUTBOX_SUCCESS_RETENTION_MS)
    }

    private fun UserGoals.toFullGoalsPatch(): UserGoalsSyncPatchDto = UserGoalsSyncPatchDto(
        operationId = UUID.randomUUID().toString(),
        baseRevision = revision,
        userModifiedAt = SupabaseMappers.toIsoString(userModifiedAt.takeIf { it > 0 } ?: System.currentTimeMillis()),
        fields = linkedMapOf(
            "daily_goal_gross" to CurrencyUtils.centsToDouble(dailyGoalGross), "daily_goal_net" to CurrencyUtils.centsToDouble(dailyGoalNet),
            "daily_goal_km" to dailyGoalKm, "daily_goal_deliveries" to dailyGoalDeliveries,
            "weekly_goal_gross" to CurrencyUtils.centsToDouble(weeklyGoalGross), "weekly_goal_net" to CurrencyUtils.centsToDouble(weeklyGoalNet),
            "weekly_goal_km" to weeklyGoalKm, "weekly_goal_deliveries" to weeklyGoalDeliveries,
            "monthly_goal_gross" to CurrencyUtils.centsToDouble(monthlyGoalGross), "monthly_goal_net" to CurrencyUtils.centsToDouble(monthlyGoalNet),
            "monthly_goal_km" to monthlyGoalKm, "monthly_goal_deliveries" to monthlyGoalDeliveries,
            "annual_goal_gross" to CurrencyUtils.centsToDouble(annualGoalGross), "annual_goal_net" to CurrencyUtils.centsToDouble(annualGoalNet),
            "annual_goal_km" to annualGoalKm, "annual_goal_deliveries" to annualGoalDeliveries,
            "work_days_per_week" to workDaysPerWeek, "work_days_per_month" to workDaysPerMonth,
            "work_months_per_year" to workMonthsPerYear, "work_days_per_month_is_custom" to workDaysPerMonthIsCustom
        ),
        provenance = mapOf("annual_gross_origin" to annualGrossOrigin, "annual_net_origin" to annualNetOrigin, "annual_km_origin" to annualKmOrigin, "annual_deliveries_origin" to annualDeliveriesOrigin),
        explicitUserOverride = mapOf(
            "annual_gross" to (annualGrossOrigin == GoalOrigin.USER_DEFINED),
            "annual_net" to (annualNetOrigin == GoalOrigin.USER_DEFINED),
            "annual_km" to (annualKmOrigin == GoalOrigin.USER_DEFINED),
            "annual_deliveries" to (annualDeliveriesOrigin == GoalOrigin.USER_DEFINED)
        )
    )

    private companion object {
        const val MAX_GOALS_SYNC_ATTEMPTS = 10
        const val OUTBOX_SUCCESS_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }

    // =========================================================================
    // PULL (Nuvem -> Local) com Resolução Last-Write-Wins (LWW)
    // =========================================================================
    internal suspend fun pullRemoteChanges(userId: String) {
        val ownerId = dataOwnerStorage.getDataOwnerId()
        if (ownerId != null && ownerId != userId) {
            throw DataOwnerMismatchException(
                ownerId = ownerId,
                currentUserId = userId,
                operation = "PULL"
            )
        }
        if (ownerId == null && hasAnyLocalData()) {
            throw DataOwnerMismatchException(
                ownerId = null,
                currentUserId = userId,
                operation = "PULL"
            )
        }
        // --- 1. VEÍCULOS ---
        val remoteVehicles = restApi.getVehicles()
        db.withTransaction {
            for (dto in remoteVehicles) {
                var existing = vehicleDao.getVehicleByRemoteId(dto.id)
                if (existing == null) {
                    // Tenta reconciliar com o primeiro veículo local caso seu remoteId esteja vazio
                    val activeLocal = vehicleDao.getActiveVehicle()
                    if (activeLocal != null && activeLocal.remoteId.isBlank()) {
                        existing = activeLocal
                    }
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        vehicleDao.insertVehicle(dto.toEntity(existingId = 0))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        vehicleDao.updateVehicle(dto.toEntity(existingId = existing.id).copy(isDeleted = isDel))
                    }
                }
            }
        }

        val activeVehicle = vehicleDao.getActiveVehicle()
        val defaultLocalVehicleId = activeVehicle?.id ?: 1L

        // --- 2. PEÇAS DE DESGASTE ---
        val remoteParts = restApi.getVehicleParts()
        db.withTransaction {
            for (dto in remoteParts) {
                var existing = partDao.getPartByRemoteId(dto.id)
                if (existing == null) {
                    // Reconcilia com peça local de mesmo nome se remoteId vazio
                    val localMatch = partDao.getAllParts().firstOrNull { it.remoteId.isBlank() && it.name.equals(dto.name, ignoreCase = true) }
                    existing = localMatch
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        partDao.insertPart(dto.toEntity(existingId = 0, vehicleId = defaultLocalVehicleId))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        val vId = if (existing.vehicleId != 0L) existing.vehicleId else defaultLocalVehicleId
                        partDao.updatePart(dto.toEntity(existingId = existing.id, vehicleId = vId).copy(isDeleted = isDel))
                    }
                }
            }
        }

        // --- 3. DIÁRIAS (DAILY RECORDS) ---
        val remoteRecords = restApi.getDailyRecords()
        db.withTransaction {
            for (dto in remoteRecords) {
                var existing = recordDao.getRecordByRemoteId(dto.id)
                if (existing == null) {
                    // Reconcilia por dateString e platform se remoteId vazio
                    existing = recordDao.getAllRecords().firstOrNull {
                        it.remoteId.isBlank() && it.dateString == dto.dateString && it.platform == dto.platform
                    }
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        recordDao.insertRecord(dto.toEntity(existingId = 0))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        recordDao.updateRecord(dto.toEntity(existingId = existing.id).copy(isDeleted = isDel))
                    }
                }
            }
        }

        // --- 4. PERFIL DO USUÁRIO ---
        val remoteProfiles = restApi.getUserProfiles()
        val remoteProfile = remoteProfiles.firstOrNull()
        if (remoteProfile != null) {
            val existing = userProfileDao.getUserProfile()
            val remoteUpdated = SupabaseMappers.parseIsoTimestamp(remoteProfile.updatedAt)
            if (existing == null || existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt) {
                val isDel = remoteProfile.deletedAt != null
                userProfileDao.insertOrUpdateUserProfile(remoteProfile.toEntity(existingId = 1).copy(isDeleted = isDel))
            }
        }

        // --- 5. PLATAFORMAS ---
        val remotePlatforms = restApi.getPlatforms()
        db.withTransaction {
            for (dto in remotePlatforms) {
                var existing = platformDao.getPlatformByRemoteId(dto.id)
                if (existing == null) {
                    existing = platformDao.getAllPlatforms().firstOrNull {
                        it.remoteId.isBlank() && it.name.equals(dto.name, ignoreCase = true)
                    }
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        platformDao.insertOrUpdatePlatform(dto.toEntity(existingId = 0))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        platformDao.updatePlatform(dto.toEntity(existingId = existing.id).copy(isDeleted = isDel))
                    }
                }
            }
        }

        // --- 6. ABASTECIMENTOS ---
        val remoteRefills = restApi.getFuelRefills()
        db.withTransaction {
            for (dto in remoteRefills) {
                val localVehicle = dto.vehicleId?.let { vehicleDao.getVehicleByRemoteId(it) }
                val targetVehicleId = localVehicle?.id ?: defaultLocalVehicleId

                var existing = fuelRefillDao.getRefillByRemoteId(dto.id)
                if (existing == null) {
                    existing = fuelRefillDao.getAllRefillsOrderedByOdometer(targetVehicleId).firstOrNull {
                        it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.totalPaid == CurrencyUtils.toCents(dto.totalPaid)
                    }
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        fuelRefillDao.insertRefill(dto.toEntity(existingId = 0, localVehicleId = targetVehicleId))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        fuelRefillDao.updateRefill(dto.toEntity(existingId = existing.id, localVehicleId = targetVehicleId).copy(isDeleted = isDel))
                    }
                }
            }
        }

        // --- 7. MANUTENÇÕES ---
        val remoteMaintenance = restApi.getMaintenanceRecords()
        db.withTransaction {
            for (dto in remoteMaintenance) {
                val localVehicle = dto.vehicleId?.let { vehicleDao.getVehicleByRemoteId(it) }
                val targetVehicleId = localVehicle?.id ?: defaultLocalVehicleId

                var existing = maintenanceRecordDao.getRecordByRemoteId(dto.id)
                if (existing == null) {
                    existing = maintenanceRecordDao.getPendingRecords().firstOrNull {
                        it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.description == dto.description
                    }
                }

                val remoteUpdated = SupabaseMappers.parseIsoTimestamp(dto.updatedAt)
                if (existing == null) {
                    if (dto.deletedAt == null) {
                        maintenanceRecordDao.insertRecord(dto.toEntity(existingId = 0, localVehicleId = targetVehicleId))
                    }
                } else {
                    val shouldUpdate = existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt
                    if (shouldUpdate) {
                        val isDel = dto.deletedAt != null
                        maintenanceRecordDao.updateRecord(dto.toEntity(existingId = existing.id, localVehicleId = targetVehicleId).copy(isDeleted = isDel))
                    }
                }
            }
        }

        // --- 8. METAS DO USUÁRIO ---
        val remoteGoalsList = restApi.getUserGoals()
        val remoteGoals = remoteGoalsList.firstOrNull()
        if (remoteGoals != null) {
            val existing = userGoalsDao.getUserGoals()
            val remoteUpdated = SupabaseMappers.parseIsoTimestamp(remoteGoals.updatedAt)
            if (existing == null || existing.syncStatus == SyncStatus.SYNCED || remoteUpdated >= existing.updatedAt) {
                val isDel = remoteGoals.deletedAt != null
                userGoalsDao.insertOrUpdateUserGoals(remoteGoals.toEntity(existingId = 1).copy(isDeleted = isDel))
            }
        }

        if (dataOwnerStorage.getDataOwnerId() == null) {
            dataOwnerStorage.setDataOwnerId(userId)
        }
    }
}
