package com.example.network.supabase

import androidx.annotation.VisibleForTesting
import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SupabaseClient(
    context: Context? = null,
    internal var storage: SecureSessionStorage = createDefaultStorage(context)
) {

    private val refreshMutex = Mutex()

    private val _sessionState = MutableStateFlow<AuthSession?>(null)
    val sessionState: StateFlow<AuthSession?> = _sessionState.asStateFlow()

    @Volatile
    private var activeToken: String? = null

    init {
        restoreSavedSession()
    }

    fun hasSavedSession(): Boolean {
        return storage.hasSavedSession()
    }

    fun getSavedSession(): AuthSession? {
        return storage.getSavedSession()
    }

    fun getSavedUserEmail(): String? {
        return storage.getSavedUserEmail() ?: _sessionState.value?.userEmail
    }

    fun restoreSavedSession(): Boolean {
        val session = storage.getSavedSession() ?: return false
        setAuthSession(session)
        return true
    }

    fun clearSavedSession() {
        setAuthSession(null)
    }

    fun setAuthSession(session: AuthSession?) {
        activeToken = session?.accessToken
        _sessionState.value = session
        if (session != null) {
            storage.saveSession(session)
        } else {
            storage.clearSession()
        }
    }

    val currentUserId: String?
        get() = _sessionState.value?.userId

    val currentAccessToken: String?
        get() = activeToken

    val isAuthenticated: Boolean
        get() = !_sessionState.value?.accessToken.isNullOrBlank()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = activeToken ?: BuildConfig.SUPABASE_ANON_KEY
        val request = original.newBuilder()
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "application/json")
            .header("User-Agent", "GiroCusto-Android/1.0")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("apikey")
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val baseUrl: String = if (BuildConfig.SUPABASE_URL.endsWith("/")) {
        BuildConfig.SUPABASE_URL
    } else {
        "${BuildConfig.SUPABASE_URL}/"
    }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val realAuthApi: SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    private val realRestApi: SupabaseRestApi = retrofit.create(SupabaseRestApi::class.java)

    @Volatile
    var testAuthApi: SupabaseAuthApi? = null
    @Volatile
    var testRestApi: SupabaseRestApi? = null

    val authApi: SupabaseAuthApi
        get() = testAuthApi ?: realAuthApi

    val restApi: SupabaseRestApi
        get() = testRestApi ?: realRestApi

    // ==========================================
    // MÉTODOS DE AUTHENTICATION
    // ==========================================

    suspend fun signUp(email: String, pass: String): Result<AuthSession> {
        return try {
            val response = authApi.signUp(AuthRequest(email = email, password = pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Quando o Supabase possui a confirmação de e-mail ativada ("Confirm email"),
                // a tentativa de registrar um e-mail já existente retorna HTTP 200 com
                // o array identities vazio (`[]`). Trata explicitamente como erro de usuário existente.
                if (AuthErrorMapper.isExistingUserSignUp(body.user)) {
                    val resId = com.example.R.string.auth_error_user_already_exists
                    return Result.failure(
                        SupabaseAuthException(
                            messageResId = resId,
                            rawError = "User already registered",
                            httpCode = 400
                        )
                    )
                }

                val session = AuthSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user?.id ?: "",
                    userEmail = body.user?.email ?: email
                )
                if (session.accessToken.isNotBlank()) {
                    setAuthSession(session)
                }
                Result.success(session)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMsg = rawError ?: "Erro ao registrar usuário (${response.code()})"
                val resId = AuthErrorMapper.mapHttpError(response.code(), rawError, AuthFlow.SIGN_UP)
                Result.failure(SupabaseAuthException(messageResId = resId, rawError = errorMsg, httpCode = response.code()))
            }
        } catch (e: Exception) {
            val resId = AuthErrorMapper.mapThrowable(e, AuthFlow.SIGN_UP)
            Result.failure(if (e is SupabaseAuthException) e else SupabaseAuthException(messageResId = resId, rawError = e.message, cause = e))
        }
    }

    suspend fun signIn(email: String, pass: String): Result<AuthSession> {
        return try {
            val response = authApi.signInWithPassword(request = AuthRequest(email = email, password = pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val session = AuthSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user?.id ?: "",
                    userEmail = body.user?.email ?: email
                )
                setAuthSession(session)
                Result.success(session)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMsg = rawError ?: "Credenciais inválidas (${response.code()})"
                val resId = AuthErrorMapper.mapHttpError(response.code(), rawError, AuthFlow.SIGN_IN)
                Result.failure(SupabaseAuthException(messageResId = resId, rawError = errorMsg, httpCode = response.code()))
            }
        } catch (e: Exception) {
            val resId = AuthErrorMapper.mapThrowable(e, AuthFlow.SIGN_IN)
            Result.failure(if (e is SupabaseAuthException) e else SupabaseAuthException(messageResId = resId, rawError = e.message, cause = e))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String? = null): Result<AuthSession> {
        return try {
            val response = authApi.signInWithIdToken(
                grantType = "id_token",
                request = IdTokenRequest(
                    provider = "google",
                    idToken = idToken,
                    nonce = nonce
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val session = AuthSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user?.id ?: "",
                    userEmail = body.user?.email
                )
                setAuthSession(session)
                Result.success(session)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMsg = rawError ?: "Erro na autenticação com Google (${response.code()})"
                val resId = AuthErrorMapper.mapHttpError(response.code(), rawError, AuthFlow.GOOGLE_SIGN_IN)
                Result.failure(SupabaseAuthException(messageResId = resId, rawError = errorMsg, httpCode = response.code()))
            }
        } catch (e: Exception) {
            val resId = AuthErrorMapper.mapThrowable(e, AuthFlow.GOOGLE_SIGN_IN)
            Result.failure(if (e is SupabaseAuthException) e else SupabaseAuthException(messageResId = resId, rawError = e.message, cause = e))
        }
    }

    suspend fun refreshCurrentSession(): Result<AuthSession> = refreshMutex.withLock {
        val current = _sessionState.value ?: getSavedSession()
        if (current == null || current.refreshToken.isBlank()) {
            return Result.failure(IllegalStateException("Nenhum refresh token disponível"))
        }
        return try {
            val response = authApi.refreshToken(request = RefreshTokenRequest(refreshToken = current.refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newSession = AuthSession(
                    accessToken = body.accessToken,
                    refreshToken = if (body.refreshToken.isNotBlank()) body.refreshToken else current.refreshToken,
                    userId = body.user?.id ?: current.userId,
                    userEmail = body.user?.email ?: current.userEmail
                )
                setAuthSession(newSession)
                Result.success(newSession)
            } else {
                val rawError = response.errorBody()?.string()
                val errorMsg = rawError ?: "Erro ao renovar token (${response.code()})"
                val resId = AuthErrorMapper.mapHttpError(response.code(), rawError, AuthFlow.REFRESH_TOKEN)
                Result.failure(SupabaseAuthException(messageResId = resId, rawError = errorMsg, httpCode = response.code()))
            }
        } catch (e: Exception) {
            val resId = AuthErrorMapper.mapThrowable(e, AuthFlow.REFRESH_TOKEN)
            Result.failure(if (e is SupabaseAuthException) e else SupabaseAuthException(messageResId = resId, rawError = e.message, cause = e))
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            authApi.logout()
            clearSavedSession()
            Result.success(Unit)
        } catch (e: Exception) {
            clearSavedSession()
            Result.success(Unit)
        }
    }

    suspend fun deleteUserAccount(): Result<Unit> {
        return try {
            val response = restApi.deleteUserAccount()
            if (response.isSuccessful) {
                clearSavedSession()
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Falha ao excluir conta remota (HTTP ${response.code()}): $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SupabaseClient? = null

        fun createDefaultStorage(context: Context?): SecureSessionStorage {
            val appContext = context?.applicationContext ?: context
            return if (appContext != null) {
                val sec = EncryptedSharedPreferencesStorage(appContext)
                sec.migrateLegacySessionIfPresent()
                sec
            } else {
                AbsentSessionStorage
            }
        }

        fun getInstance(context: Context? = null): SupabaseClient {
            val existing = INSTANCE
            if (existing != null) {
                if (existing.storage is AbsentSessionStorage && context != null) {
                    val sec = EncryptedSharedPreferencesStorage(context.applicationContext)
                    sec.migrateLegacySessionIfPresent()
                    existing.storage = sec
                    existing.restoreSavedSession()
                }
                return existing
            }
            return synchronized(this) {
                INSTANCE ?: SupabaseClient(
                    context = context?.applicationContext,
                    storage = createDefaultStorage(context?.applicationContext)
                ).also { INSTANCE = it }
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setInstance(client: SupabaseClient?) {
            synchronized(this) {
                INSTANCE = client
            }
        }
    }
}
