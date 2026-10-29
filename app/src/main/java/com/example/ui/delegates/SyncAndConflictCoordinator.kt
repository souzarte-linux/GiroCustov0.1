package com.example.ui.delegates

import android.app.Application
import android.content.Context
import com.example.data.DataOwnerStorage
import com.example.data.GiroCustoRepository
import com.example.network.supabase.DataOwnerMismatchException
import com.example.network.supabase.SessionExpiredException
import com.example.network.supabase.SupabaseClient
import com.example.network.supabase.SyncResult
import com.example.ui.AccountConflictState
import com.example.ui.SyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SyncAndConflictCoordinator(
    private val scope: CoroutineScope,
    private val application: Application,
    private val repository: GiroCustoRepository,
    private val dataOwnerStorage: DataOwnerStorage,
    private val supabaseClient: SupabaseClient,
    private val onResetSpecificPreferences: () -> Unit
) {
    var onLogoutFallback: () -> Unit = {}
    private val syncMutex = Mutex()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _isAccountSwitching = MutableStateFlow(false)
    val isAccountSwitching: StateFlow<Boolean> = _isAccountSwitching.asStateFlow()

    private val _accountConflictState = MutableStateFlow<AccountConflictState?>(null)
    val accountConflictState: StateFlow<AccountConflictState?> = _accountConflictState.asStateFlow()

    private val _showDiscardDoubleConfirmationDialog = MutableStateFlow(false)
    val showDiscardDoubleConfirmationDialog: StateFlow<Boolean> = _showDiscardDoubleConfirmationDialog.asStateFlow()

    private val _showClaimDataConfirmationDialog = MutableStateFlow(false)
    val showClaimDataConfirmationDialog: StateFlow<Boolean> = _showClaimDataConfirmationDialog.asStateFlow()

    private val syncPrefs = application.getSharedPreferences("giro_custo_sync_prefs", Context.MODE_PRIVATE)

    private val _lastSyncTime = MutableStateFlow<Long?>(
        syncPrefs.getLong("last_sync_timestamp", 0L).takeIf { it > 0L }
    )
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    fun setSyncStateDirect(state: SyncState) {
        _syncState.value = state
    }

    fun setSyncErrorMessageDirect(msg: String?) {
        _syncErrorMessage.value = msg
    }

    fun clearAccountConflictState() {
        _accountConflictState.value = null
    }

    fun openDiscardConfirmation() {
        _showDiscardDoubleConfirmationDialog.value = true
    }

    fun dismissDiscardConfirmation() {
        _showDiscardDoubleConfirmationDialog.value = false
    }

    fun openClaimDataConfirmation() {
        _showClaimDataConfirmationDialog.value = true
    }

    fun dismissClaimDataConfirmation() {
        _showClaimDataConfirmationDialog.value = false
    }

    fun resolveConflictReturnToPreviousAccount() {
        _accountConflictState.value = null
        _showDiscardDoubleConfirmationDialog.value = false
        _showClaimDataConfirmationDialog.value = false
        onLogoutFallback()
    }

    fun confirmDiscardData(onComplete: () -> Unit = {}) {
        _showDiscardDoubleConfirmationDialog.value = false
        val currentUserId = supabaseClient.currentUserId ?: return
        scope.launch {
            syncMutex.withLock {
                _isAccountSwitching.value = true
                _syncState.value = SyncState.SYNCING
                try {
                    repository.clearAllLocalData()
                    resetAccountSpecificPreferences()
                    dataOwnerStorage.setDataOwnerId(currentUserId)
                    _accountConflictState.value = null
                    performSyncInternal(currentUserId, null)
                    onComplete()
                } finally {
                    _isAccountSwitching.value = false
                }
            }
        }
    }

    fun confirmClaimData(onComplete: () -> Unit = {}) {
        _showClaimDataConfirmationDialog.value = false
        val currentUserId = supabaseClient.currentUserId ?: return
        scope.launch {
            syncMutex.withLock {
                _isAccountSwitching.value = true
                _syncState.value = SyncState.SYNCING
                try {
                    dataOwnerStorage.setDataOwnerId(currentUserId)
                    _accountConflictState.value = null
                    performSyncInternal(currentUserId, null)
                    onComplete()
                } finally {
                    _isAccountSwitching.value = false
                }
            }
        }
    }

    fun resetAccountSpecificPreferences() {
        syncPrefs.edit().remove("last_sync_timestamp").apply()
        _lastSyncTime.value = null
        onResetSpecificPreferences()
    }

    fun triggerSync(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (!supabaseClient.isAuthenticated) {
            _syncState.value = SyncState.IDLE
            onComplete?.invoke(false, "Usuário não autenticado")
            return
        }

        _syncState.value = SyncState.SYNCING
        _syncErrorMessage.value = null

        scope.launch {
            try {
                executeAccountSyncOrTransition(onComplete)
            } catch (e: CancellationException) {
                // coroutine cancelled
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
                val err = e.localizedMessage ?: "Erro na sincronização"
                _syncErrorMessage.value = err
                onComplete?.invoke(false, err)
            }
        }
    }

    fun syncNow(onComplete: ((Boolean, String?) -> Unit)? = null) = triggerSync(onComplete)

    private suspend fun executeAccountSyncOrTransition(onComplete: ((Boolean, String?) -> Unit)? = null) {
        syncMutex.withLock {
            if (!supabaseClient.isAuthenticated) {
                _syncState.value = SyncState.IDLE
                onComplete?.invoke(false, "Usuário não autenticado")
                return
            }

            val currentUserId = supabaseClient.currentUserId
            if (currentUserId.isNullOrBlank()) {
                _syncState.value = SyncState.ERROR
                val err = "ID do usuário não encontrado na sessão ativa"
                _syncErrorMessage.value = err
                onComplete?.invoke(false, err)
                return
            }

            val ownerId = dataOwnerStorage.getDataOwnerId()

            // Caso 1: Dono coincide (fluxo normal do mesmo usuário)
            if (ownerId == currentUserId) {
                performSyncInternal(currentUserId, onComplete)
                return
            }

            // Caso 2: Dono desconhecido (ownerId == null)
            if (ownerId == null) {
                val hasData = repository.hasAnyLocalData()
                if (!hasData) {
                    dataOwnerStorage.setDataOwnerId(currentUserId)
                    performSyncInternal(currentUserId, onComplete)
                    return
                }

                val hasPending = repository.hasPendingSyncChanges()
                if (!hasPending) {
                    switchAccountCleanly(currentUserId, onComplete)
                    return
                } else {
                    val count = repository.countPendingChanges()
                    _accountConflictState.value = AccountConflictState.UnknownOwnerWithPending(
                        currentUserId = currentUserId,
                        pendingCount = count
                    )
                    _syncState.value = SyncState.IDLE
                    onComplete?.invoke(false, "Conflito de dados locais detectado")
                    return
                }
            }

            // Caso 3: Troca de conta entre donos diferentes (ownerId != currentUserId)
            val hasPending = repository.hasPendingSyncChanges()
            if (!hasPending) {
                switchAccountCleanly(currentUserId, onComplete)
                return
            } else {
                val count = repository.countPendingChanges()
                _accountConflictState.value = AccountConflictState.KnownOwnerMismatchWithPending(
                    previousOwnerId = ownerId,
                    currentUserId = currentUserId,
                    pendingCount = count
                )
                _syncState.value = SyncState.IDLE
                onComplete?.invoke(false, "Lançamentos pendentes pertencem a outra conta")
                return
            }
        }
    }

    private suspend fun switchAccountCleanly(newUserId: String, onComplete: ((Boolean, String?) -> Unit)?) {
        _isAccountSwitching.value = true
        _syncState.value = SyncState.SYNCING
        try {
            repository.clearAllLocalData()
            resetAccountSpecificPreferences()
            dataOwnerStorage.setDataOwnerId(newUserId)
            performSyncInternal(newUserId, onComplete)
        } finally {
            _isAccountSwitching.value = false
        }
    }

    private suspend fun performSyncInternal(userId: String, onComplete: ((Boolean, String?) -> Unit)?) {
        _syncState.value = SyncState.SYNCING
        _syncErrorMessage.value = null

        val result = repository.syncCurrentSession()
        when (result) {
            is SyncResult.Success -> {
                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                syncPrefs.edit().putLong("last_sync_timestamp", now).apply()
                _syncState.value = SyncState.SUCCESS
                onComplete?.invoke(true, null)
            }
            is SyncResult.Error -> {
                val err = result.error
                val isOffline = err is java.net.UnknownHostException ||
                        err is java.net.ConnectException ||
                        err is java.net.SocketTimeoutException
                _syncState.value = if (isOffline) SyncState.OFFLINE else SyncState.ERROR
                val isExpired = err is SessionExpiredException ||
                        (err is retrofit2.HttpException && err.code() == 401) ||
                        ("401" in (err.message ?: "")) ||
                        ("jwt expired" in (err.message ?: "").lowercase())
                val msg = when {
                    err is DataOwnerMismatchException -> {
                        "Sincronização pausada por segurança: os registros salvos neste aparelho pertencem a outra conta. Entre com a conta proprietária para sincronizá-los ou descarte os dados locais."
                    }
                    isExpired -> {
                        "Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando."
                    }
                    else -> {
                        err.localizedMessage ?: err.message ?: "Erro na sincronização"
                    }
                }
                _syncErrorMessage.value = msg
                onComplete?.invoke(false, msg)
            }
        }
    }

    fun clearSyncError() {
        _syncErrorMessage.value = null
        if (_syncState.value == SyncState.ERROR) {
            _syncState.value = SyncState.IDLE
        }
    }

    fun formatLastSyncTime(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return "Nunca sincronizado"
        val now = Calendar.getInstance()
        val syncCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val isToday = now.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == syncCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = now.get(Calendar.YEAR) == syncCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - syncCal.get(Calendar.DAY_OF_YEAR) == 1

        return when {
            isToday -> "Hoje às ${timeFormat.format(Date(timestamp))}"
            isYesterday -> "Ontem às ${timeFormat.format(Date(timestamp))}"
            else -> dateFormat.format(Date(timestamp))
        }
    }
}
