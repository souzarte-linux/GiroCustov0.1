package com.example

import android.util.Base64
import com.example.util.SessionCryptoEngine
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Implementação de SessionCryptoEngine para testes unitários em JVM / Robolectric (T-06b).
 * Vive exclusivamente em src/test.
 */
class FakeSessionCryptoEngine : SessionCryptoEngine {

    private val key: SecretKey = run {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256)
        kg.generateKey()
    }

    override fun encryptSession(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherTextBase64 = Base64.encodeToString(cipherText, Base64.NO_WRAP)
        return "$ivBase64:$cipherTextBase64"
    }

    override fun decryptSession(cipherPayload: String): String {
        val parts = cipherPayload.split(":")
        require(parts.size == 2) { "Payload criptográfico inválido" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }
}
