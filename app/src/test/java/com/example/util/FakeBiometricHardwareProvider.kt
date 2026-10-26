package com.example.util

import android.content.Context

/**
 * Provedor em memória de hardware biométrico para testes unitários.
 * Vive exclusivamente em src/test.
 */
class FakeBiometricHardwareProvider(
    var protectionLevel: BiometricProtectionLevel = BiometricProtectionLevel.STRONG,
    var isHardwareAvailable: Boolean = true,
    var availability: BiometricAvailability = BiometricAvailability.AVAILABLE
) : BiometricHardwareProvider {
    override fun getProtectionLevel(context: Context): BiometricProtectionLevel = protectionLevel
    override fun isBiometricHardwareAvailable(context: Context): Boolean = isHardwareAvailable
    override fun checkAvailability(context: Context): BiometricAvailability = availability
}
