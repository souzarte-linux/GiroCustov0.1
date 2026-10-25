package com.example.network.supabase

import android.util.Log
import androidx.room.withTransaction
import com.example.data.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val error: Throwable) : SyncResult()
}

class SupabaseSyncManager(
    private val db: GiroCustoDatabase,
    private val restApi: SupabaseRestApi,
    private val dataOwnerStorage: DataOwnerStorage,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    constructor(db: GiroCustoDatabase, client: SupabaseClient, dataOwnerStorage: DataOwnerStorage, ioDispatcher: CoroutineDispatcher = Dispatchers.IO) : this(db, client.restApi, dataOwnerStorage, ioDispatcher)
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()
    private val userProfileDao = db.userProfileDao()
    private val platformDao = db.platformDao()
    private val fuelRefillDao = db.fuelRefillDao()
    private val maintenanceRecordDao = db.maintenanceRecordDao()

    internal suspend fun hasAnyLocalData(): Boolean {
        return vehicleDao.getAllVehicles().isNotEmpty() ||
                vehicleDao.getPendingVehicles().isNotEmpty() ||
                partDao.getAllParts().isNotEmpty() ||
                recordDao.getAllRecords().isNotEmpty() ||
                platformDao.getAllPlatforms().isNotEmpty() ||
                userProfileDao.getUserProfile() != null
    }

    suspend fun sync(userId: String): SyncResult = withContext(ioDispatcher) {
        try {
            Log.d("SupabaseSyncManager", "Iniciando sincronização completa para userId: $userId")

            // 1. PUSH: Envia dados locais pendentes para o Supabase
            pushLocalChanges(userId)

            // 2. PULL: Busca dados remotos do Supabase e aplica localmente (LWW)
            pullRemoteChanges(userId)

            if (dataOwnerStorage.getDataOwnerId() == null) {
                dataOwnerStorage.setDataOwnerId(userId)
            }

            Log.d("SupabaseSyncManager", "Sincronização concluída com sucesso!")
            SyncResult.Success("Sincronização concluída com sucesso")
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Erro durante a sincronização: ${e.message}", e)
            SyncResult.Error(e)
        }
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
                    val local = pendingRefills.firstOrNull { it.remoteId == dto.id || (it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.totalPaid == dto.totalPaid) }
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
                        it.remoteId.isBlank() && it.dateTimestamp == dto.dateTimestamp && it.totalPaid == dto.totalPaid
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

        if (dataOwnerStorage.getDataOwnerId() == null) {
            dataOwnerStorage.setDataOwnerId(userId)
        }
    }
}
