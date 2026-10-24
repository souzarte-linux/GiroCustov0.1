package com.example.util

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utilitário isolado e enxuto para comunicação com o Android Keystore (T-06 [SEC-01 e SEC-04]).
 *
 * Implementa arquitetura de duas chaves simétricas AES-256-GCM:
 * 1. giro_custo_session_key: Criptografia de tokens em repouso sem exigir biometria (permite sync em background).
 * 2. giro_custo_biometric_auth_key: Desbloqueio biométrico com CryptoObject e detecção de novas digitais.
 */
object AndroidKeyStoreHelper : SessionCryptoEngine {

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val SESSION_KEY_ALIAS = "giro_custo_session_key"
    private const val BIOMETRIC_KEY_ALIAS = "giro_custo_biometric_auth_key"

    fun isKeyStoreAvailable(): Boolean {
        return try {
            if (Security.getProvider(ANDROID_KEY_STORE) == null) return false
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore.load(null)
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun getKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        return keyStore
    }

    @Synchronized
    fun getOrCreateSessionKey(): SecretKey {
        if (!isKeyStoreAvailable()) {
            throw java.security.KeyStoreException("AndroidKeyStore provider não está disponível no dispositivo.")
        }
        val keyStore = getKeyStore()
        if (keyStore.containsAlias(SESSION_KEY_ALIAS)) {
            val entry = keyStore.getEntry(SESSION_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val spec = KeyGenParameterSpec.Builder(
            SESSION_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    @Synchronized
    fun getOrCreateBiometricKey(): SecretKey {
        val keyStore = getKeyStore()
        if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
            val entry = keyStore.getEntry(BIOMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val builder = KeyGenParameterSpec.Builder(
            BIOMETRIC_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }

        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }

    fun removeBiometricKey() {
        try {
            if (isKeyStoreAvailable()) {
                val keyStore = getKeyStore()
                if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                    keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
                }
            }
        } catch (_: Throwable) {}
    }

    /**
     * Cifra uma string em texto puro usando a chave de sessão do Keystore.
     * Retorna "IV_BASE64:CIPHERTEXT_BASE64".
     */
    override fun encryptSession(plainText: String): String {
        val key = getOrCreateSessionKey()
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivBase64:$cipherTextBase64"
    }

    /**
     * Decifra o payload gerado por encryptSession.
     */
    override fun decryptSession(cipherPayload: String): String {
        val parts = cipherPayload.split(":")
        require(parts.size == 2) { "Payload criptográfico inválido" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

        val key = getOrCreateSessionKey()
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Cria Cipher em modo ENCRYPT_MODE para chave biométrica.
     */
    fun createBiometricEncryptCipher(): Cipher {
        val key = getOrCreateBiometricKey()
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    /**
     * Cria Cipher em modo DECRYPT_MODE para chave biométrica com o IV fornecido.
     * Pode lançar KeyPermanentlyInvalidatedException caso uma nova digital tenha sido cadastrada.
     */
    @Throws(KeyPermanentlyInvalidatedException::class)
    fun createBiometricDecryptCipher(iv: ByteArray): Cipher {
        val key = getOrCreateBiometricKey()
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        return cipher
    }
}
