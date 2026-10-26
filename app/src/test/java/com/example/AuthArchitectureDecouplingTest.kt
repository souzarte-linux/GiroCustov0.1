package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.supabase.AuthSession
import com.example.network.supabase.InMemorySessionStorage
import com.example.network.supabase.SupabaseClient
import com.example.util.BiometricAvailability
import com.example.util.BiometricHelper
import com.example.util.BiometricProtectionLevel
import com.example.util.FakeBiometricHardwareProvider
import com.example.util.GoogleAuthHandler
import com.example.util.GoogleAuthHelper
import com.example.util.GoogleAuthResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários para comprovar o desacoplamento arquitetural e injeção limpa de dependências (T-14a):
 * - Remoção de ganchos públicos 'test...', resetForTesting e reflexão de classes de teste.
 * - Injeção limpa via construtores e interfaces em GoogleAuthHelper, BiometricHelper e SupabaseClient.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthArchitectureDecouplingTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        GoogleAuthHelper.setAuthHandler(null)
        BiometricHelper.setHardwareProvider(null)
        BiometricHelper.clearBiometrics(context)
        SupabaseClient.setInstance(null)
    }

    @After
    fun tearDown() {
        GoogleAuthHelper.setAuthHandler(null)
        BiometricHelper.setHardwareProvider(null)
        BiometricHelper.clearBiometrics(context)
        SupabaseClient.setInstance(null)
    }

    // =========================================================================
    // 1. GOOGLE AUTH HELPER: INJEÇÃO VIA GOOGLEAUTHHANDLER
    // =========================================================================

    @Test
    fun testGoogleAuthHelper_delegatesToInjectedHandler() = runTest {
        var handlerCalled = false
        val expectedResult = GoogleAuthResult(idToken = "token-xyz-123", rawNonce = "nonce-abc-456")

        GoogleAuthHelper.setAuthHandler { _, serverClientId ->
            handlerCalled = true
            assertEquals("test-client-id", serverClientId)
            Result.success(expectedResult)
        }

        val result = GoogleAuthHelper.getGoogleIdToken(context, serverClientId = "test-client-id")
        assertTrue(result.isSuccess)
        assertTrue(handlerCalled)
        assertEquals("token-xyz-123", result.getOrNull()?.idToken)
        assertEquals("nonce-abc-456", result.getOrNull()?.rawNonce)
    }

    @Test
    fun testGoogleAuthHelper_resetHandlerRestoresDefaultBehavior() = runTest {
        GoogleAuthHelper.setAuthHandler { _, _ ->
            Result.success(GoogleAuthResult(idToken = "temp", rawNonce = "temp"))
        }
        GoogleAuthHelper.setAuthHandler(null)

        // Com handler nulo e client id em branco, deve acionar a validação padrão sem handler
        val result = GoogleAuthHelper.getGoogleIdToken(context, serverClientId = "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("GOOGLE_WEB_CLIENT_ID não configurado") == true)
    }

    // =========================================================================
    // 2. BIOMETRIC HELPER: INJEÇÃO VIA BIOMETRICHARDWAREPROVIDER
    // =========================================================================

    @Test
    fun testBiometricHelper_delegatesToInjectedHardwareProvider() {
        val fakeProvider = FakeBiometricHardwareProvider(
            protectionLevel = BiometricProtectionLevel.STRONG,
            isHardwareAvailable = true,
            availability = BiometricAvailability.AVAILABLE
        )

        BiometricHelper.setHardwareProvider(fakeProvider)

        assertEquals(BiometricProtectionLevel.STRONG, BiometricHelper.getProtectionLevel(context))
        assertTrue(BiometricHelper.isBiometricHardwareAvailable(context))
        assertEquals(BiometricAvailability.AVAILABLE, BiometricHelper.checkAvailability(context))

        // Altera propriedades dinâmicas do provider sem expor variáveis mutáveis no BiometricHelper
        fakeProvider.protectionLevel = BiometricProtectionLevel.WEAK
        fakeProvider.isHardwareAvailable = true
        fakeProvider.availability = BiometricAvailability.NONE_ENROLLED

        assertEquals(BiometricProtectionLevel.WEAK, BiometricHelper.getProtectionLevel(context))
        assertTrue(BiometricHelper.isBiometricHardwareAvailable(context))
        assertEquals(BiometricAvailability.NONE_ENROLLED, BiometricHelper.checkAvailability(context))

        fakeProvider.protectionLevel = BiometricProtectionLevel.NONE
        fakeProvider.isHardwareAvailable = false
        fakeProvider.availability = BiometricAvailability.NO_HARDWARE

        assertEquals(BiometricProtectionLevel.NONE, BiometricHelper.getProtectionLevel(context))
        assertFalse(BiometricHelper.isBiometricHardwareAvailable(context))
        assertEquals(BiometricAvailability.NO_HARDWARE, BiometricHelper.checkAvailability(context))
    }

    @Test
    fun testBiometricHelper_canUnlockWithBiometrics_combinesHardwareAndPreference() {
        val fakeProvider = FakeBiometricHardwareProvider(isHardwareAvailable = true)
        BiometricHelper.setHardwareProvider(fakeProvider)

        // Hardware disponível, mas biometria desligada nas preferências
        BiometricHelper.setBiometricEnabled(context, false)
        assertFalse(BiometricHelper.canUnlockWithBiometrics(context))

        // Hardware disponível e biometria ligada
        BiometricHelper.setBiometricEnabled(context, true, "motorista@girocusto.com")
        assertTrue(BiometricHelper.canUnlockWithBiometrics(context))

        // Biometria ligada nas preferências, mas hardware indisponível
        fakeProvider.isHardwareAvailable = false
        assertFalse(BiometricHelper.canUnlockWithBiometrics(context))
    }

    // =========================================================================
    // 3. SUPABASE CLIENT: INJEÇÃO DE STORAGE VIA CONSTRUTOR E SETINSTANCE
    // =========================================================================

    @Test
    fun testSupabaseClient_constructorInjectionOfStorage() {
        val inMemoryStorage = InMemorySessionStorage()
        val customClient = SupabaseClient(context, storage = inMemoryStorage)

        assertFalse(customClient.hasSavedSession())
        assertNull(customClient.getSavedSession())

        val session = AuthSession(
            accessToken = "token-construtor-123",
            refreshToken = "refresh-construtor-456",
            userId = "user-construtor-789",
            userEmail = "construtor@girocusto.com"
        )
        customClient.setAuthSession(session)

        assertTrue(customClient.hasSavedSession())
        assertEquals("token-construtor-123", customClient.currentAccessToken)
        assertEquals("user-construtor-789", customClient.currentUserId)
        assertEquals("token-construtor-123", inMemoryStorage.getSavedSession()?.accessToken)
    }

    @Test
    fun testSupabaseClient_setInstance_managesSingletonCleanly() {
        val storage = InMemorySessionStorage()
        val customClient = SupabaseClient(context, storage = storage)

        SupabaseClient.setInstance(customClient)
        assertSame(customClient, SupabaseClient.getInstance(context))

        SupabaseClient.setInstance(null)
        val defaultClient = SupabaseClient.getInstance(context)
        assertNotSame(customClient, defaultClient)
    }
}
