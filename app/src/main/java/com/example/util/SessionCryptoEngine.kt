package com.example.util

/**
 * Contrato de cifragem e decifragem para persistência de sessão segura (T-06b).
 * Em produção, implementado com hardware seguro por [AndroidKeyStoreHelper].
 * Em testes unitários, implementado com mecanismo isolado em src/test.
 */
interface SessionCryptoEngine {
    fun encryptSession(plainText: String): String
    fun decryptSession(cipherPayload: String): String
}
