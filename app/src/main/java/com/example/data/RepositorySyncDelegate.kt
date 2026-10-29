package com.example.data

import com.example.network.supabase.AuthSession
import com.example.network.supabase.SessionExpiredException
import com.example.network.supabase.SupabaseClient
import com.example.network.supabase.SupabaseSyncManager
import com.example.network.supabase.SyncResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RepositorySyncDelegate(
    private val db: GiroCustoDatabase,
    private val dataOwnerStorage: DataOwnerStorage?,
    private val ioDispatcher: CoroutineDispatcher
) {
    private val vehicleDao = db.vehicleDao()
    private val partDao = db.vehiclePartDao()
    private val recordDao = db.dailyRecordDao()
    private val userProfileDao = db.userProfileDao()
    private val platformDao = db.platformDao()
    private val fuelRefillDao = db.fuelRefillDao()
    private val maintenanceRecordDao = db.maintenanceRecordDao()
    private val userGoalsDao = db.userGoalsDao()

    suspend fun hasPendingSyncChanges(): Boolean = withContext(ioDispatcher) {
        vehicleDao.getPendingVehicles().isNotEmpty() ||
        partDao.getPendingParts().isNotEmpty() ||
        recordDao.getPendingRecords().isNotEmpty() ||
        fuelRefillDao.getPendingRefills().isNotEmpty() ||
        maintenanceRecordDao.getPendingRecords().isNotEmpty() ||
        platformDao.getPendingPlatforms().isNotEmpty() ||
        userProfileDao.getPendingUserProfile() != null ||
        userGoalsDao.getPendingUserGoals() != null
    }

    suspend fun hasAnyLocalData(): Boolean = withContext(ioDispatcher) {
        vehicleDao.getAllVehicles().isNotEmpty() ||
        vehicleDao.getPendingVehicles().isNotEmpty() ||
        partDao.getAllParts().isNotEmpty() ||
        recordDao.getAllRecords().isNotEmpty() ||
        platformDao.getAllPlatforms().isNotEmpty() ||
        userProfileDao.getUserProfile() != null ||
        userGoalsDao.getUserGoals() != null
    }

    suspend fun countPendingChanges(): Int = withContext(ioDispatcher) {
        vehicleDao.getPendingVehicles().size +
        partDao.getPendingParts().size +
        recordDao.getPendingRecords().size +
        fuelRefillDao.getPendingRefills().size +
        maintenanceRecordDao.getPendingRecords().size +
        platformDao.getPendingPlatforms().size +
        (if (userProfileDao.getPendingUserProfile() != null) 1 else 0) +
        (if (userGoalsDao.getPendingUserGoals() != null) 1 else 0)
    }

    suspend fun syncWithSupabase(userId: String, authToken: String): SyncResult {
        val client = SupabaseClient.getInstance()
        if (authToken.isNotBlank()) {
            client.setAuthSession(
                AuthSession(
                    accessToken = authToken,
                    refreshToken = "",
                    userId = userId,
                    userEmail = null
                )
            )
        }
        val storage = dataOwnerStorage ?: return SyncResult.Error(IllegalStateException("DataOwnerStorage não configurado no repositório"))
        val syncManager = SupabaseSyncManager(db, client, storage, ioDispatcher)
        return syncManager.sync(userId)
    }

    suspend fun syncWithSupabase(userId: String): SyncResult {
        val client = SupabaseClient.getInstance()
        val storage = dataOwnerStorage ?: return SyncResult.Error(IllegalStateException("DataOwnerStorage não configurado no repositório"))
        val syncManager = SupabaseSyncManager(db, client, storage, ioDispatcher)
        return syncManager.sync(userId)
    }

    suspend fun syncCurrentSession(): SyncResult {
        val client = SupabaseClient.getInstance()
        val userId = client.currentUserId ?: return SyncResult.Error(IllegalStateException("Usuário não autenticado no Supabase"))

        // Garante que a sessão está fresca antes de iniciar a sincronização
        val sessionCheck = client.ensureFreshSession()
        if (sessionCheck.isFailure) {
            val failure = sessionCheck.exceptionOrNull()
            if (failure is SessionExpiredException) {
                return SyncResult.Error(failure)
            }
        }

        return syncWithSupabase(userId)
    }
}
