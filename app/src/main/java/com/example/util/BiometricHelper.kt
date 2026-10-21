package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NONE_ENROLLED,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
}

object BiometricHelper {
    private const val PREFS_NAME = "giro_custo_biometrics"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_BIOMETRIC_EMAIL = "biometric_user_email"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isBiometricHardwareAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val canAuth = manager.canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
        return canAuth == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun checkAvailability(context: Context): BiometricAvailability {
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

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean, email: String? = null) {
        getPrefs(context).edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            if (email != null) {
                putString(KEY_BIOMETRIC_EMAIL, email)
            } else if (!enabled) {
                remove(KEY_BIOMETRIC_EMAIL)
            }
            apply()
        }
    }

    fun getBiometricEmail(context: Context): String? {
        return getPrefs(context).getString(KEY_BIOMETRIC_EMAIL, null)
    }

    fun clearBiometrics(context: Context) {
        getPrefs(context).edit().clear().apply()
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
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}
