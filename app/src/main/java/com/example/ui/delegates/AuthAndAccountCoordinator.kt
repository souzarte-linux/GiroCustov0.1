package com.example.ui.delegates

import android.app.Application
import android.content.Context
import com.example.data.DataOwnerStorage
import com.example.data.GiroCustoRepository
import com.example.network.supabase.AuthErrorMapper
import com.example.network.supabase.AuthFlow
import com.example.ui.AuthUiState
import com.example.network.supabase.SupabaseAuthException
import com.example.network.supabase.SupabaseClient
import com.example.ui.SyncState
import com.example.util.BiometricHelper
import com.example.util.GoogleAuthHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthAndAccountCoordinator(
    private val scope: CoroutineScope,
    private val application: Application,
    private val repository: GiroCustoRepository,
    private val dataOwnerStorage: DataOwnerStorage,
    private val supabaseClient: SupabaseClient,
    private val syncCoordinator: SyncAndConflictCoordinator,
    private val onClearCaches: () -> Unit
) {
    private val _isBiometricLocked = MutableStateFlow(
        supabaseClient.hasSavedSession() && BiometricHelper.canUnlockWithBiometrics(application)
    )
    val isBiometricLocked: StateFlow<Boolean> = _isBiometricLocked.asStateFlow()

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _authErrorResId = MutableStateFlow<Int?>(null)
    val authErrorResId: StateFlow<Int?> = _authErrorResId.asStateFlow()

    private val _isBiometricAvailable = MutableStateFlow(BiometricHelper.isBiometricHardwareAvailable(application))
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    fun refreshBiometricAvailability() {
        _isBiometricAvailable.value = BiometricHelper.isBiometricHardwareAvailable(application)
    }

    fun setBiometricLockedDirect(locked: Boolean) {
        _isBiometricLocked.value = locked
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (email.isBlank() || pass.isBlank()) {
            onResult(false, "Informe e-mail e senha.")
            return
        }
        scope.launch {
            _authUiState.value = AuthUiState.Loading
            _authErrorResId.value = null
            syncCoordinator.setSyncStateDirect(SyncState.SYNCING)
            syncCoordinator.setSyncErrorMessageDirect(null)
            val result = supabaseClient.signIn(email.trim(), pass)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success()
                _authErrorResId.value = null
                syncCoordinator.setSyncStateDirect(SyncState.IDLE)
                BiometricHelper.setBiometricEnabled(application, true, email.trim())
                syncCoordinator.triggerSync()
                onResult(true, null)
            } else {
                syncCoordinator.setSyncStateDirect(SyncState.ERROR)
                val exception = result.exceptionOrNull()
                val resId = (exception as? SupabaseAuthException)?.messageResId
                    ?: AuthErrorMapper.mapThrowable(exception, AuthFlow.SIGN_IN)
                _authErrorResId.value = resId
                val err = exception?.message ?: "Falha ao autenticar"
                syncCoordinator.setSyncErrorMessageDirect(err)
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        if (email.isBlank() || pass.length < 6) {
            onResult(false, "A senha deve conter no mínimo 6 caracteres.")
            return
        }
        scope.launch {
            _authUiState.value = AuthUiState.Loading
            _authErrorResId.value = null
            syncCoordinator.setSyncStateDirect(SyncState.SYNCING)
            syncCoordinator.setSyncErrorMessageDirect(null)
            val result = supabaseClient.signUp(email.trim(), pass)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                val session = result.getOrNull()
                if (session != null && session.accessToken.isNotBlank()) {
                    _authUiState.value = AuthUiState.Success("Conta criada com sucesso!")
                    _authErrorResId.value = null
                    syncCoordinator.setSyncStateDirect(SyncState.IDLE)
                    BiometricHelper.setBiometricEnabled(application, true, email.trim())
                    syncCoordinator.triggerSync()
                    onResult(true, null)
                } else {
                    _authUiState.value = AuthUiState.Success("Conta criada com sucesso! Verifique seu e-mail para confirmar o cadastro.")
                    _authErrorResId.value = null
                    syncCoordinator.setSyncStateDirect(SyncState.IDLE)
                    onResult(true, null)
                }
            } else {
                syncCoordinator.setSyncStateDirect(SyncState.ERROR)
                val exception = result.exceptionOrNull()
                val resId = (exception as? SupabaseAuthException)?.messageResId
                    ?: AuthErrorMapper.mapThrowable(exception, AuthFlow.SIGN_UP)
                _authErrorResId.value = resId
                val err = exception?.message ?: "Falha ao registrar conta"
                syncCoordinator.setSyncErrorMessageDirect(err)
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        scope.launch {
            _authUiState.value = AuthUiState.Loading
            _authErrorResId.value = null
            syncCoordinator.setSyncStateDirect(SyncState.SYNCING)
            syncCoordinator.setSyncErrorMessageDirect(null)

            val authResult = GoogleAuthHelper.getGoogleIdToken(context)
            if (authResult.isFailure) {
                syncCoordinator.setSyncStateDirect(SyncState.ERROR)
                val err = authResult.exceptionOrNull()?.message ?: "Falha ao autenticar com o Google"
                syncCoordinator.setSyncErrorMessageDirect(err)
                _authErrorResId.value = null
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
                return@launch
            }

            val authData = authResult.getOrNull()!!
            val result = supabaseClient.signInWithGoogleIdToken(authData.idToken, authData.rawNonce)
            if (result.isSuccess) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success()
                _authErrorResId.value = null
                syncCoordinator.setSyncStateDirect(SyncState.IDLE)
                val session = result.getOrNull()
                val email = session?.userEmail?.takeIf { it.isNotBlank() } ?: "google_user"
                BiometricHelper.setBiometricEnabled(application, true, email)
                syncCoordinator.triggerSync()
                onResult(true, null)
            } else {
                syncCoordinator.setSyncStateDirect(SyncState.ERROR)
                val exception = result.exceptionOrNull()
                val resId = (exception as? SupabaseAuthException)?.messageResId
                    ?: AuthErrorMapper.mapThrowable(exception, AuthFlow.GOOGLE_SIGN_IN)
                _authErrorResId.value = resId
                val err = exception?.message ?: "Falha ao autenticar com o Google"
                syncCoordinator.setSyncErrorMessageDirect(err)
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun unlockWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        scope.launch {
            _authUiState.value = AuthUiState.Loading
            _authErrorResId.value = null
            val restored = supabaseClient.restoreSavedSession()
            if (restored) {
                _isBiometricLocked.value = false
                _authUiState.value = AuthUiState.Success("Autenticado via biometria com sucesso!")
                _authErrorResId.value = null
                onResult(true, null)
                launch {
                    try {
                        supabaseClient.refreshCurrentSession()
                        syncCoordinator.triggerSync()
                    } catch (e: CancellationException) {
                        // coroutine cancelled
                    } catch (e: Exception) {
                        // Resiliente a falhas de rede ou encerramento assíncrono
                    }
                }
            } else {
                _isBiometricLocked.value = false
                val err = application.getString(com.example.R.string.auth_session_revalidation_notice)
                _authErrorResId.value = com.example.R.string.auth_session_revalidation_notice
                _authUiState.value = AuthUiState.Error(err)
                onResult(false, err)
            }
        }
    }

    fun handleBiometricKeyInvalidated() {
        _isBiometricLocked.value = false
        supabaseClient.clearSavedSession()
        BiometricHelper.clearBiometrics(application)
        val msg = application.getString(com.example.R.string.auth_session_revalidation_notice)
        _authErrorResId.value = com.example.R.string.auth_session_revalidation_notice
        _authUiState.value = AuthUiState.Error(msg)
    }

    fun dismissBiometricLockForFallback() {
        _isBiometricLocked.value = false
        supabaseClient.clearSavedSession()
        _authErrorResId.value = null
        _authUiState.value = AuthUiState.Idle
    }

    fun resetAuthError() {
        _authErrorResId.value = null
        if (_authUiState.value is AuthUiState.Error || _authUiState.value is AuthUiState.Success) {
            _authUiState.value = AuthUiState.Idle
        }
    }

    fun signInWithBiometrics(onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        unlockWithBiometrics(onResult)
    }

    fun lockAppForBiometrics() {
        if (supabaseClient.hasSavedSession() && BiometricHelper.canUnlockWithBiometrics(application)) {
            _isBiometricLocked.value = true
        }
    }

    fun logout() {
        scope.launch {
            _isBiometricLocked.value = false
            supabaseClient.signOut()
            BiometricHelper.clearBiometrics(application)
            _authErrorResId.value = null
            _authUiState.value = AuthUiState.Idle
            syncCoordinator.setSyncStateDirect(SyncState.IDLE)
            syncCoordinator.setSyncErrorMessageDirect(null)
            onClearCaches()
        }
    }

    fun exportUserData(onResult: (String) -> Unit) {
        scope.launch {
            val json = repository.exportAllUserDataAsJson()
            onResult(json)
        }
    }

    fun deleteAccountAndData(onResult: (Boolean, String?) -> Unit) {
        scope.launch {
            try {
                val currentUserId = supabaseClient.currentUserId
                if (currentUserId != null) {
                    val rpcResult = supabaseClient.deleteUserAccount()
                    if (rpcResult.isFailure) {
                        val error = rpcResult.exceptionOrNull()?.message ?: "Falha ao excluir conta no servidor"
                        onResult(false, error)
                        return@launch
                    }
                }

                repository.clearAllLocalData()
                dataOwnerStorage.clearDataOwner()
                if (currentUserId != null) {
                    supabaseClient.signOut()
                } else {
                    supabaseClient.clearSavedSession()
                }
                BiometricHelper.clearBiometrics(application)
                syncCoordinator.resetAccountSpecificPreferences()

                _isBiometricLocked.value = false
                _authErrorResId.value = null
                _authUiState.value = AuthUiState.Idle
                syncCoordinator.setSyncStateDirect(SyncState.IDLE)
                syncCoordinator.setSyncErrorMessageDirect(null)
                syncCoordinator.clearAccountConflictState()

                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message ?: "Erro inesperado ao excluir conta")
            }
        }
    }
}
