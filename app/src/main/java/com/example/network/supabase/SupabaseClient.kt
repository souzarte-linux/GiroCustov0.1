package com.example.network.supabase

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

class SupabaseClient private constructor(context: Context? = null) {

    private var prefs: SharedPreferences? = context?.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<AuthSession?>(null)
    val sessionState: StateFlow<AuthSession?> = _sessionState.asStateFlow()

    @Volatile
    private var activeToken: String? = null

    init {
        restoreSavedSession()
    }

    fun hasSavedSession(): Boolean {
        val token = prefs?.getString(KEY_ACCESS_TOKEN, null)
        val userId = prefs?.getString(KEY_USER_ID, null)
        return !token.isNullOrBlank() && !userId.isNullOrBlank()
    }

    fun getSavedSession(): AuthSession? {
        val token = prefs?.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val userId = prefs?.getString(KEY_USER_ID, null) ?: return null
        val refresh = prefs?.getString(KEY_REFRESH_TOKEN, null) ?: ""
        val email = prefs?.getString(KEY_EMAIL, null)
        return AuthSession(
            accessToken = token,
            refreshToken = refresh,
            userId = userId,
            userEmail = email
        )
    }

    fun getSavedUserEmail(): String? {
        return prefs?.getString(KEY_EMAIL, null) ?: _sessionState.value?.userEmail
    }

    fun restoreSavedSession(): Boolean {
        val session = getSavedSession() ?: return false
        setAuthSession(session)
        return true
    }

    fun clearSavedSession() {
        setAuthSession(null)
    }

    fun setAuthSession(session: AuthSession?) {
        activeToken = session?.accessToken
        _sessionState.value = session
        prefs?.edit()?.apply {
            if (session != null) {
                putString(KEY_ACCESS_TOKEN, session.accessToken)
                putString(KEY_REFRESH_TOKEN, session.refreshToken)
                putString(KEY_USER_ID, session.userId)
                putString(KEY_EMAIL, session.userEmail)
            } else {
                remove(KEY_ACCESS_TOKEN)
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_USER_ID)
                remove(KEY_EMAIL)
            }
            commit()
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
                val errorMsg = response.errorBody()?.string() ?: "Erro ao registrar usuário (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                val errorMsg = response.errorBody()?.string() ?: "Credenciais inválidas (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
                val errorMsg = response.errorBody()?.string() ?: "Erro na autenticação com Google (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshCurrentSession(): Result<AuthSession> {
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
                val errorMsg = response.errorBody()?.string() ?: "Erro ao renovar token (${response.code()})"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
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

    companion object {
        private const val KEY_ACCESS_TOKEN = "sp_access_token"
        private const val KEY_REFRESH_TOKEN = "sp_refresh_token"
        private const val KEY_USER_ID = "sp_user_id"
        private const val KEY_EMAIL = "sp_user_email"

        @Volatile
        private var INSTANCE: SupabaseClient? = null

        fun getInstance(context: Context? = null): SupabaseClient {
            val existing = INSTANCE
            if (existing != null) {
                if (existing.prefs == null && context != null) {
                    existing.prefs = context.applicationContext.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE)
                    existing.restoreSavedSession()
                }
                return existing
            }
            return synchronized(this) {
                INSTANCE ?: SupabaseClient(context?.applicationContext).also { INSTANCE = it }
            }
        }

        fun resetForTesting(context: Context? = null): SupabaseClient {
            return synchronized(this) {
                SupabaseClient(context?.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
