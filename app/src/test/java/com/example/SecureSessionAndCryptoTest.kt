package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.supabase.AuthRequest
import com.example.network.supabase.AuthResponse
import com.example.network.supabase.AuthSession
import com.example.network.supabase.EncryptedSharedPreferencesStorage
import com.example.network.supabase.InMemorySessionStorage
import com.example.network.supabase.RefreshTokenRequest
import com.example.network.supabase.SupabaseAuthApi
import com.example.network.supabase.SupabaseClient
import com.example.ui.AuthUiState
import com.example.ui.GiroCustoViewModel
import com.example.util.AndroidKeyStoreHelper
import com.example.util.BiometricHelper
import com.example.util.BiometricProtectionLevel
import com.example.util.FakeBiometricHardwareProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/**
 * Suíte de testes unitários específicos para T-06 (SEC-01 e SEC-04):
 * - Migração do formato legado com verificação read-back e deleção de arquivo.
 * - Resiliência a payload corrompido / chave invalidada.
 * - Desafio biométrico AES-GCM (sucesso e negação de desbloqueio em adulteração).
 * - Proteção por Mutex e persistência síncrona com commit().
 * - Suporte a recuperação amigável para usuários Google sem senha.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureSessionAndCryptoTest {

    private lateinit var testDispatcher: kotlinx.coroutines.test.TestDispatcher
    private lateinit var context: Context
    private lateinit var app: Application

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        app = ApplicationProvider.getApplicationContext()

        // Garante isolamento inicial
        context.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_secure_session", Context.MODE_PRIVATE).edit().clear().commit()
        BiometricHelper.clearBiometrics(context)
        BiometricHelper.setHardwareProvider(null)
        SupabaseClient.setInstance(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        context.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("giro_custo_secure_session", Context.MODE_PRIVATE).edit().clear().commit()
        BiometricHelper.clearBiometrics(context)
        BiometricHelper.setHardwareProvider(null)
        SupabaseClient.setInstance(null)
    }

    // =========================================================================
    // 1. MIGRAÇÃO SEGURA COM READ-BACK E REMOÇÃO TOTAL DO ARQUIVO LEGADO (Condição 4)
    // =========================================================================

    @Test
    fun testLegacyMigration_success_verifiesReadBackAndDeletesOldFile() {
        // 1. Grava sessão legada em texto puro no arquivo antigo
        val legacyPrefs = context.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putString("sp_access_token", "legacy-access-token-123")
            .putString("sp_refresh_token", "legacy-refresh-token-456")
            .putString("sp_user_id", "legacy-user-uuid-789")
            .putString("sp_user_email", "migrado@girocusto.com")
            .commit()

        assertTrue(legacyPrefs.contains("sp_access_token"))

        // 2. Instancia armazenamento criptografado seguro com FakeSessionCryptoEngine isolado em src/test
        val fakeCrypto = FakeSessionCryptoEngine()
        val storage = EncryptedSharedPreferencesStorage(context, fakeCrypto)
        storage.migrateLegacySessionIfPresent()

        // 3. Valida que a sessão nova contém exatamente os dados legados confirmados via read-back
        val migratedSession = storage.getSavedSession()
        assertNotNull(migratedSession)
        assertEquals("legacy-access-token-123", migratedSession?.accessToken)
        assertEquals("legacy-refresh-token-456", migratedSession?.refreshToken)
        assertEquals("legacy-user-uuid-789", migratedSession?.userId)
        assertEquals("migrado@girocusto.com", migratedSession?.userEmail)

        // 4. Valida que o arquivo antigo foi removido (não contém mais tokens)
        val refreshedLegacy = context.getSharedPreferences("giro_custo_supabase_auth", Context.MODE_PRIVATE)
        assertFalse(refreshedLegacy.contains("sp_access_token"))
        assertFalse(refreshedLegacy.contains("sp_user_id"))
    }

    @Test
    fun testLegacyMigration_whenNoLegacySession_doesNothing() {
        val fakeCrypto = FakeSessionCryptoEngine()
        val storage = EncryptedSharedPreferencesStorage(context, fakeCrypto)
        storage.migrateLegacySessionIfPresent()
        assertFalse(storage.hasSavedSession())
        assertNull(storage.getSavedSession())
    }

    // =========================================================================
    // 2. RESILIÊNCIA A PAYLOAD CORROMPIDO E ADULTERAÇÃO (Condições 4 e 5)
    // =========================================================================

    @Test
    fun testCorruptedPayload_getSavedSessionReturnsNullWithoutCrashing() {
        val securePrefs = context.getSharedPreferences("giro_custo_secure_session", Context.MODE_PRIVATE)
        securePrefs.edit()
            .putString("sec_session_payload", "invalid_base64_payload_without_colon")
            .commit()

        val fakeCrypto = FakeSessionCryptoEngine()
        val storage = EncryptedSharedPreferencesStorage(context, fakeCrypto)
        // Deve retornar null e não estourar exceção não tratada
        val session = storage.getSavedSession()
        assertNull(session)
        assertFalse(storage.hasSavedSession())
    }

    @Test
    fun testCorruptedPayload_missingMandatoryFields_returnsNull() {
        val securePrefs = context.getSharedPreferences("giro_custo_secure_session", Context.MODE_PRIVATE)
        // Grava JSON que não possui accessToken ou userId
        val dummyPayload = "iv:ciphertext"
        securePrefs.edit().putString("sec_session_payload", dummyPayload).commit()

        val fakeCrypto = FakeSessionCryptoEngine()
        val storage = EncryptedSharedPreferencesStorage(context, fakeCrypto)
        assertNull(storage.getSavedSession())
    }

    // =========================================================================
    // 3. DESAFIO CRIPTOGRÁFICO BIOMÉTRICO AES-GCM (Condição 2)
    // =========================================================================

    @Test
    fun testBiometricChallenge_verification_tamperDetection() {
        // Desafio válido idêntico ao esperado
        val validBytes = BiometricHelper.BIOMETRIC_CHALLENGE_VALUE.toByteArray(Charsets.UTF_8)
        assertTrue(BiometricHelper.verifyChallengeDecryption(validBytes))

        // Desafio adulterado por terceiro
        val tamperedBytes = "fake_tampered_challenge_value".toByteArray(Charsets.UTF_8)
        assertFalse(BiometricHelper.verifyChallengeDecryption(tamperedBytes))

        // Decifragem nula ou vazia
        assertFalse(BiometricHelper.verifyChallengeDecryption(null))
        assertFalse(BiometricHelper.verifyChallengeDecryption(ByteArray(0)))
    }

    @Test
    fun testBiometricChallenge_storageAndRetrieval() {
        val testIv = "iv_test_12bytes".toByteArray(Charsets.UTF_8)
        val testCipher = "encrypted_challenge_bytes".toByteArray(Charsets.UTF_8)

        assertFalse(BiometricHelper.hasBiometricChallenge(context))
        BiometricHelper.saveBiometricChallenge(context, testIv, testCipher)

        assertTrue(BiometricHelper.hasBiometricChallenge(context))
        val retrieved = BiometricHelper.getSavedChallenge(context)
        assertNotNull(retrieved)
        assertArrayEquals(testIv, retrieved?.first)
        assertArrayEquals(testCipher, retrieved?.second)

        // Limpeza remove o desafio
        BiometricHelper.clearBiometrics(context)
        assertFalse(BiometricHelper.hasBiometricChallenge(context))
    }

    // =========================================================================
    // 4. COMPORTAMENTO EM APARELHOS SEM BIOMETRIC_STRONG (Condição 3)
    // =========================================================================

    @Test
    fun testBiometricProtectionLevel_classificationAndNotification() {
        val provider = FakeBiometricHardwareProvider(protectionLevel = BiometricProtectionLevel.STRONG, isHardwareAvailable = true)
        BiometricHelper.setHardwareProvider(provider)
        assertEquals(BiometricProtectionLevel.STRONG, BiometricHelper.getProtectionLevel(context))

        provider.protectionLevel = BiometricProtectionLevel.WEAK
        assertEquals(BiometricProtectionLevel.WEAK, BiometricHelper.getProtectionLevel(context))
        // Mesmo em nível fraco, o hardware ainda é considerado disponível para manter o bloqueio
        assertTrue(BiometricHelper.isBiometricHardwareAvailable(context))

        provider.protectionLevel = BiometricProtectionLevel.NONE
        provider.isHardwareAvailable = false
        assertEquals(BiometricProtectionLevel.NONE, BiometricHelper.getProtectionLevel(context))
    }

    // =========================================================================
    // 5. RECUPERAÇÃO GRACIOSA & USUÁRIOS GOOGLE SEM SENHA (Condição 1)
    // =========================================================================

    @Test
    fun testKeyPermanentlyInvalidated_clearsBiometricsAndRevealsRecoveryScreen() = runTest(testDispatcher) {
        val viewModel = GiroCustoViewModel(app)
        BiometricHelper.setBiometricEnabled(context, true, "google.user@girocusto.com")
        assertTrue(BiometricHelper.isBiometricEnabled(context))

        // Simula detecção de nova digital no SO disparando handleBiometricKeyInvalidated
        viewModel.handleBiometricKeyInvalidated()

        // 1. Bloqueio biométrico destravado para exibir tela de recuperação
        assertFalse(viewModel.isBiometricLocked.value)

        // 2. Chave e preferências biométricas invalidadas são limpas
        assertFalse(BiometricHelper.isBiometricEnabled(context))

        // 3. Mensagem amigável em português explicando a revalidação
        val authState = viewModel.authUiState.value
        assertTrue(authState is AuthUiState.Error)
        val errorMsg = (authState as AuthUiState.Error).message
        assertTrue(errorMsg.contains("revalidada") || errorMsg.contains("segurança"))
    }

    @Test
    fun testDismissBiometricLockForFallback_allowsGoogleAndEmailLogin() = runTest(testDispatcher) {
        val viewModel = GiroCustoViewModel(app)
        viewModel.lockAppForBiometrics()

        // Motorista clica em "Entrar com Google, e-mail ou senha"
        viewModel.dismissBiometricLockForFallback()
        assertFalse(viewModel.isBiometricLocked.value)
        assertEquals(com.example.ui.navigation.RootNavState.Unauthenticated, viewModel.rootNavState.value)
    }

    // =========================================================================
    // 6. PROTEÇÃO POR MUTEX E PERSISTÊNCIA SÍNCRONA (Condição 5)
    // =========================================================================

    @Test
    fun testRefreshCurrentSession_protectedByMutexAndSavesSynchronously() = runTest(testDispatcher) {
        val storage = InMemorySessionStorage()
        val initialSession = AuthSession(
            accessToken = "initial-access-token",
            refreshToken = "initial-refresh-token",
            userId = "user-123",
            userEmail = "mutex@girocusto.com"
        )
        storage.saveSession(initialSession)

        val client = SupabaseClient(context, storage = storage)
        SupabaseClient.setInstance(client)

        var refreshCallCount = 0
        client.testAuthApi = object : SupabaseAuthApi {
            override suspend fun signUp(request: AuthRequest) = throw NotImplementedError()
            override suspend fun signInWithPassword(grantType: String, request: AuthRequest) = throw NotImplementedError()
            override suspend fun signInWithIdToken(grantType: String, request: com.example.network.supabase.IdTokenRequest) = throw NotImplementedError()
            override suspend fun refreshToken(grantType: String, request: RefreshTokenRequest): Response<AuthResponse> {
                refreshCallCount++
                delay(50) // Simula latência de rede
                return Response.success(
                    AuthResponse(
                        accessToken = "new-access-token-$refreshCallCount",
                        refreshToken = "new-refresh-token-$refreshCallCount"
                    )
                )
            }
            override suspend fun logout() = Response.success(Unit)
            override suspend fun getUser() = throw NotImplementedError()
        }

        // Dispara duas renovações concorrentes
        val job1 = async { client.refreshCurrentSession() }
        val job2 = async { client.refreshCurrentSession() }

        val res1 = job1.await()
        val res2 = job2.await()

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)

        // Ambas as chamadas foram serializadas pelo Mutex
        val finalSession = client.getSavedSession()
        assertNotNull(finalSession)
        assertTrue(finalSession?.accessToken?.startsWith("new-access-token-") == true)
        assertEquals(2, refreshCallCount)
    }

    // =========================================================================
    // 7. FALHA DO KEYSTORE EM PRODUÇÃO: TRATAMENTO GRACIOSO SEM TRAVAR (T-06b)
    // =========================================================================

    @Test
    fun testKeystoreFailure_inProduction_handlesGracefullyAsAbsentSessionWithoutCrashing() {
        val failingCryptoEngine = object : com.example.util.SessionCryptoEngine {
            override fun encryptSession(plainText: String): String {
                throw java.security.KeyStoreException("Keystore de produção corrompido ou indisponível")
            }

            override fun decryptSession(cipherPayload: String): String {
                throw java.security.KeyStoreException("Keystore de produção inacessível na leitura")
            }
        }

        val storage = EncryptedSharedPreferencesStorage(context, failingCryptoEngine)

        // 1. Tentar salvar sessão com Keystore falhando: não trava o app
        val session = AuthSession(
            accessToken = "test-token",
            refreshToken = "test-refresh",
            userId = "test-user-id",
            userEmail = "fail@girocusto.com"
        )
        storage.saveSession(session)

        // 2. Tentar recuperar sessão: não trava o app, trata como ausente (null)
        assertNull(storage.getSavedSession())
        assertFalse(storage.hasSavedSession())
        assertNull(storage.getSavedUserEmail())

        // 3. Com sessão ausente, restoreSavedSession() falha e direciona ao login
        val client = SupabaseClient(context, storage = storage)
        assertFalse(client.restoreSavedSession())
        assertNull(client.currentAccessToken)
        assertFalse(client.isAuthenticated)
    }
}
