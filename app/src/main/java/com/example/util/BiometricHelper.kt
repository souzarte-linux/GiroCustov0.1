package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import javax.crypto.Cipher

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

/**
 * Nível de proteção de hardware para biometria (Condição 3).
 */
enum class BiometricProtectionLevel {
    STRONG, // BIOMETRIC_STRONG suportado com CryptoObject AES-GCM
    WEAK,   // Apenas BIOMETRIC_WEAK (bloqueio mantido sem CryptoObject, avisando usuário)
    NONE    // Nenhum hardware biométrico
}

/**
 * Provedor de status do hardware biométrico do dispositivo.
 * Permite desacoplamento e injeção de dependência limpa para testes.
 */
interface BiometricHardwareProvider {
    fun getProtectionLevel(context: Context): BiometricProtectionLevel
    fun isBiometricHardwareAvailable(context: Context): Boolean
    fun checkAvailability(context: Context): BiometricAvailability
}

/**
 * Implementação padrão de produção utilizando a API BiometricManager do Android.
 */
class DefaultBiometricHardwareProvider : BiometricHardwareProvider {
    override fun getProtectionLevel(context: Context): BiometricProtectionLevel {
        val manager = BiometricManager.from(context)
        return when {
            manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricProtectionLevel.STRONG
            manager.canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS -> BiometricProtectionLevel.WEAK
            else -> BiometricProtectionLevel.NONE
        }
    }

    override fun isBiometricHardwareAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val canAuth = manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun checkAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SECURITY_UPDATE_REQUIRED
            else -> BiometricAvailability.UNSUPPORTED
        }
    }
}

object BiometricHelper {
    private const val PREFS_NAME = "giro_custo_biometrics"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_EMAIL = "biometric_user_email"
    private const val KEY_CHALLENGE_IV = "biometric_challenge_iv"
    private const val KEY_CHALLENGE_CIPHERTEXT = "biometric_challenge_ciphertext"

    const val BIOMETRIC_CHALLENGE_VALUE = "giro_custo_biometric_challenge_v1"
    const val ERROR_CRYPTO_FAILED = 992
    const val ERROR_KEY_INVALIDATED = 991

    private var hardwareProvider: BiometricHardwareProvider = DefaultBiometricHardwareProvider()

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setHardwareProvider(provider: BiometricHardwareProvider?) {
        hardwareProvider = provider ?: DefaultBiometricHardwareProvider()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getProtectionLevel(
        context: Context,
        provider: BiometricHardwareProvider = hardwareProvider
    ): BiometricProtectionLevel {
        return provider.getProtectionLevel(context)
    }

    fun isBiometricHardwareAvailable(
        context: Context,
        provider: BiometricHardwareProvider = hardwareProvider
    ): Boolean {
        return provider.isBiometricHardwareAvailable(context)
    }

    fun checkAvailability(
        context: Context,
        provider: BiometricHardwareProvider = hardwareProvider
    ): BiometricAvailability {
        return provider.checkAvailability(context)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun canUnlockWithBiometrics(
        context: Context,
        provider: BiometricHardwareProvider = hardwareProvider
    ): Boolean =
        isBiometricHardwareAvailable(context, provider) && isBiometricEnabled(context)

    fun setBiometricEnabled(context: Context, enabled: Boolean, email: String? = null) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            if (email != null) {
                putString(KEY_BIOMETRIC_EMAIL, email)
            } else if (!enabled) {
                remove(KEY_BIOMETRIC_EMAIL)
                remove(KEY_CHALLENGE_IV)
                remove(KEY_CHALLENGE_CIPHERTEXT)
            }
            commit()
        }
    }

    fun getBiometricEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_BIOMETRIC_EMAIL, null)
    }

    fun clearBiometrics(context: Context) {
        getPrefs(context).edit().clear().commit()
        AndroidKeyStoreHelper.removeBiometricKey()
    }

    fun hasBiometricChallenge(context: Context): Boolean {
        val prefs = getPrefs(context)
        return prefs.contains(KEY_CHALLENGE_IV) && prefs.contains(KEY_CHALLENGE_CIPHERTEXT)
    }

    fun saveBiometricChallenge(context: Context, iv: ByteArray, cipherText: ByteArray) {
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        getPrefs(context).edit().apply {
            putString(KEY_CHALLENGE_IV, ivBase64)
            putString(KEY_CHALLENGE_CIPHERTEXT, cipherBase64)
            commit()
        }
    }

    fun getSavedChallenge(context: Context): Pair<ByteArray, ByteArray>? {
        val prefs = getPrefs(context)
        val ivBase64 = prefs.getString(KEY_CHALLENGE_IV, null) ?: return null
        val cipherBase64 = prefs.getString(KEY_CHALLENGE_CIPHERTEXT, null) ?: return null
        return try {
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipher = Base64.decode(cipherBase64, Base64.NO_WRAP)
            Pair(iv, cipher)
        } catch (_: Throwable) {
            null
        }
    }

    fun verifyChallengeDecryption(decryptedBytes: ByteArray?): Boolean {
        if (decryptedBytes == null) return false
        val text = String(decryptedBytes, Charsets.UTF_8)
        return text == BIOMETRIC_CHALLENGE_VALUE
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Desbloqueio GiroCusto",
        subtitle: String = "Confirme sua digital ou reconhecimento facial",
        negativeButtonText: String = "Usar Senha",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val protectionLevel = getProtectionLevel(activity)

        // Se o aparelho não possui biometria de nenhum tipo
        if (protectionLevel == BiometricProtectionLevel.NONE) {
            onError(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE, "Biometria não disponível")
            return
        }

        // Cenário 1: BIOMETRIC_STRONG suportado com Android Keystore disponível (Condição 2)
        if (protectionLevel == BiometricProtectionLevel.STRONG && AndroidKeyStoreHelper.isKeyStoreAvailable()) {
            val hasChallenge = hasBiometricChallenge(activity)
            val cryptoCipher: Cipher?
            val isVerifyingChallenge: Boolean

            if (!hasChallenge) {
                // Primeira configuração: Cipher em ENCRYPT_MODE para gerar o desafio
                cryptoCipher = try {
                    AndroidKeyStoreHelper.createBiometricEncryptCipher()
                } catch (e: Exception) {
                    onError(ERROR_CRYPTO_FAILED, e.message ?: "Erro ao inicializar cifragem biométrica")
                    return
                }
                isVerifyingChallenge = false
            } else {
                // Desbloqueios subsequentes: Cipher em DECRYPT_MODE para conferir o desafio
                val challengePair = getSavedChallenge(activity)
                if (challengePair == null) {
                    onError(ERROR_CRYPTO_FAILED, "Desafio biométrico corrompido")
                    return
                }
                cryptoCipher = try {
                    AndroidKeyStoreHelper.createBiometricDecryptCipher(challengePair.first)
                } catch (_: KeyPermanentlyInvalidatedException) {
                    // Chave invalidada por nova digital cadastrada no SO
                    clearBiometrics(activity)
                    onError(ERROR_KEY_INVALIDATED, activity.getString(R.string.auth_biometric_key_invalidated))
                    return
                } catch (e: Exception) {
                    onError(ERROR_CRYPTO_FAILED, e.message ?: "Erro ao inicializar decifragem biométrica")
                    return
                }
                isVerifyingChallenge = true
            }

            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        val authenticatedCipher = result.cryptoObject?.cipher
                        if (authenticatedCipher == null) {
                            onFailed()
                            onError(ERROR_CRYPTO_FAILED, activity.getString(R.string.auth_biometric_decrypt_failed))
                            return
                        }

                        if (!isVerifyingChallenge) {
                            // Salva desafio recém-cifrado
                            try {
                                val encryptedBytes = authenticatedCipher.doFinal(BIOMETRIC_CHALLENGE_VALUE.toByteArray(Charsets.UTF_8))
                                val iv = authenticatedCipher.iv
                                saveBiometricChallenge(activity, iv, encryptedBytes)
                                onSuccess()
                            } catch (_: Exception) {
                                onFailed()
                                onError(ERROR_CRYPTO_FAILED, activity.getString(R.string.auth_biometric_decrypt_failed))
                            }
                        } else {
                            // Decifra desafio e valida resultado esperado
                            val challengePair = getSavedChallenge(activity)
                            if (challengePair == null) {
                                onFailed()
                                onError(ERROR_CRYPTO_FAILED, activity.getString(R.string.auth_biometric_decrypt_failed))
                                return
                            }
                            try {
                                val decryptedBytes = authenticatedCipher.doFinal(challengePair.second)
                                if (verifyChallengeDecryption(decryptedBytes)) {
                                    onSuccess()
                                } else {
                                    onFailed()
                                    onError(ERROR_CRYPTO_FAILED, activity.getString(R.string.auth_biometric_decrypt_failed))
                                }
                            } catch (_: Exception) {
                                onFailed()
                                onError(ERROR_CRYPTO_FAILED, activity.getString(R.string.auth_biometric_decrypt_failed))
                            }
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onError(errorCode, errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailed()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()

            prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cryptoCipher))
            return
        }

        // Cenário 2: Aparelho sem BIOMETRIC_STRONG (BIOMETRIC_WEAK) ou ambiente de teste sem Keystore (Condição 3)
        // Mantém o bloqueio ativo e solicita a autenticação suportada
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(
                if (protectionLevel == BiometricProtectionLevel.WEAK)
                    activity.getString(R.string.auth_biometric_weak_notice)
                else subtitle
            )
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}
