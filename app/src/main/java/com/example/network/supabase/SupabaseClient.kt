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

    private val prefs: SharedPreferences? = context?.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow<AuthSession?>(null)
    val sessionState: StateFlow<AuthSession?> = _sessionState.asStateFlow()

    @Volatile
    private var activeToken: String? = null

    init {
        // Carrega sessão salva anteriormente caso exista
        prefs?.let {
            val token = it.getString(KEY_ACCESS_TOKEN, null)
            val refresh = it.getString(KEY_REFRESH_TOKEN, null) ?: ""
            val userId = it.getString(KEY_USER_ID, null)
            val email = it.getString(KEY_EMAIL, null)
            if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
                activeToken = token
                _sessionState.value = AuthSession(
                    accessToken = token,
                    refreshToken = refresh,
                    userId = userId,
                    userEmail = email
                )
            }
        }
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
            apply()
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
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
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

    val authApi: SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    val restApi: SupabaseRestApi = retrofit.create(SupabaseRestApi::class.java)

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

    suspend fun signOut(): Result<Unit> {
        return try {
            authApi.logout()
            setAuthSession(null)
            Result.success(Unit)
        } catch (e: Exception) {
            setAuthSession(null)
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
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SupabaseClient(context?.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
