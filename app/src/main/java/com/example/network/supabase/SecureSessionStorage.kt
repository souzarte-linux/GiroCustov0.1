package com.example.network.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.util.AndroidKeyStoreHelper
import com.example.util.SessionCryptoEngine
import org.json.JSONObject

/**
 * Contrato de armazenamento seguro de sessão (T-06 [SEC-01 e SEC-04] / T-06b).
 */
interface SecureSessionStorage {
    fun getSavedSession(): AuthSession?
    fun saveSession(session: AuthSession)
    fun clearSession()
    fun hasSavedSession(): Boolean
    fun getSavedUserEmail(): String?
    fun migrateLegacySessionIfPresent()
}

/**
 * Implementação em produção de armazenamento seguro criptografado com Android Keystore.
 * Utiliza o arquivo isolado giro_custo_secure_session.xml (excluído de backups no AndroidManifest/rules).
 */
class EncryptedSharedPreferencesStorage(
    private val context: Context,
    private val cryptoEngine: SessionCryptoEngine = AndroidKeyStoreHelper
) : SecureSessionStorage {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasSavedSession(): Boolean {
        return getSavedSession() != null
    }

    override fun getSavedSession(): AuthSession? {
        val encryptedPayload = prefs.getString(KEY_ENCRYPTED_PAYLOAD, null) ?: return null
        return try {
            val decryptedJson = cryptoEngine.decryptSession(encryptedPayload)
            val json = JSONObject(decryptedJson)
            val accessToken = json.getString("accessToken")
            val refreshToken = json.optString("refreshToken", "")
            val userId = json.getString("userId")
            val userEmail = if (json.has("userEmail") && !json.isNull("userEmail")) {
                json.getString("userEmail")
            } else null

            if (accessToken.isBlank() || userId.isBlank()) {
                null
            } else {
                AuthSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = userId,
                    userEmail = userEmail
                )
            }
        } catch (_: Exception) {
            // Em caso de falha do Keystore, corrupção ou chave invalidada:
            // Não trava o app, registra aviso sem dados sensíveis e trata a sessão como ausente.
            Log.w("SecureSessionStorage", "Aviso de segurança: falha ao decifrar sessão no Keystore. Tratando sessão como ausente.")
            null
        }
    }

    override fun getSavedUserEmail(): String? {
        return getSavedSession()?.userEmail
    }

    override fun saveSession(session: AuthSession) {
        try {
            val json = JSONObject().apply {
                put("accessToken", session.accessToken)
                put("refreshToken", session.refreshToken)
                put("userId", session.userId)
                put("userEmail", session.userEmail ?: JSONObject.NULL)
            }
            val encryptedPayload = cryptoEngine.encryptSession(json.toString())
            prefs.edit().putString(KEY_ENCRYPTED_PAYLOAD, encryptedPayload).commit()
        } catch (_: Exception) {
            Log.w("SecureSessionStorage", "Aviso de segurança: falha ao cifrar sessão no Keystore.")
        }
    }

    override fun clearSession() {
        prefs.edit().clear().commit()
    }

    /**
     * Migração atômica e segura do formato legado em texto puro (giro_custo_supabase_auth.xml)
     * para o formato cifrado giro_custo_secure_session.xml (Condição 4).
     */
    override fun migrateLegacySessionIfPresent() {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val legacyToken = legacyPrefs.getString("sp_access_token", null)
        val legacyUserId = legacyPrefs.getString("sp_user_id", null)

        if (legacyToken.isNullOrBlank() || legacyUserId.isNullOrBlank()) {
            return
        }

        val legacyRefresh = legacyPrefs.getString("sp_refresh_token", null) ?: ""
        val legacyEmail = legacyPrefs.getString("sp_user_email", null)
        val legacySession = AuthSession(
            accessToken = legacyToken,
            refreshToken = legacyRefresh,
            userId = legacyUserId,
            userEmail = legacyEmail
        )

        try {
            // 1. Cifrar e gravar a nova sessão síncrono com commit()
            saveSession(legacySession)

            // 2. LER DE VOLTA e decifrar para confirmar que é 100% idêntica ANTES de apagar a sessão antiga
            val readBackSession = getSavedSession()
            if (readBackSession == legacySession) {
                // Confirmação com sucesso: apagar completamente o arquivo antigo
                context.deleteSharedPreferences(LEGACY_PREFS_NAME)
            } else {
                // Divergência de integridade: apagar sessão segura e arquivo antigo, redirecionar para login
                clearSession()
                context.deleteSharedPreferences(LEGACY_PREFS_NAME)
            }
        } catch (_: Exception) {
            // Se a cifragem falhar: não travar o app, apagar a sessão antiga e permitir login
            Log.w("SecureSessionStorage", "Aviso de segurança: falha na migração da sessão legada.")
            clearSession()
            try {
                context.deleteSharedPreferences(LEGACY_PREFS_NAME)
            } catch (_: Exception) {}
        }
    }

    companion object {
        const val PREFS_NAME = "giro_custo_secure_session"
        const val LEGACY_PREFS_NAME = "giro_custo_supabase_auth"
        private const val KEY_ENCRYPTED_PAYLOAD = "sec_session_payload"
    }
}

/**
 * Armazenamento de contingência para quando não há sessão ou contexto disponível (T-06b).
 * Sempre trata a sessão como ausente.
 */
object AbsentSessionStorage : SecureSessionStorage {
    override fun getSavedSession(): AuthSession? = null
    override fun saveSession(session: AuthSession) {}
    override fun clearSession() {}
    override fun hasSavedSession(): Boolean = false
    override fun getSavedUserEmail(): String? = null
    override fun migrateLegacySessionIfPresent() {}
}
