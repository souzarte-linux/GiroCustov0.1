package com.example.network.supabase

/**
 * Implementação em memória de SecureSessionStorage para isolamento determinístico em testes unitários (T-06b).
 * Vive exclusivamente em src/test, proibido em código de produção.
 */
class InMemorySessionStorage : SecureSessionStorage {

    @Volatile
    private var inMemorySession: AuthSession? = null

    override fun getSavedSession(): AuthSession? = inMemorySession

    override fun saveSession(session: AuthSession) {
        inMemorySession = session
    }

    override fun clearSession() {
        inMemorySession = null
    }

    override fun hasSavedSession(): Boolean = inMemorySession != null

    override fun getSavedUserEmail(): String? = inMemorySession?.userEmail

    override fun migrateLegacySessionIfPresent() {
        // Nenhuma ação em memória
    }
}
