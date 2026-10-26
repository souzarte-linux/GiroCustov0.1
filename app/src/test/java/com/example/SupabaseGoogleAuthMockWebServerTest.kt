package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.supabase.AuthSession
import com.example.network.supabase.SupabaseAuthApi
import com.example.network.supabase.SupabaseClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Testes unitários com MockWebServer para validação da autenticação Google via SupabaseClient.signInWithGoogleIdToken.
 * Valida contratos HTTP, parâmetros do body/query, headers e desserialização de sessão e tratamento de erros.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SupabaseGoogleAuthMockWebServerTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var context: Context
    private lateinit var supabaseClient: SupabaseClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        supabaseClient = SupabaseClient.resetForTesting(context)
        supabaseClient.setAuthSession(null)

        val retrofit = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(supabaseClient.moshi))
            .build()

        supabaseClient.testAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    }

    @After
    fun tearDown() {
        supabaseClient.testAuthApi = null
        supabaseClient.setAuthSession(null)
        SupabaseClient.resetForTesting(context)
        mockWebServer.shutdown()
    }

    @Test
    fun testSignInWithGoogleIdToken_http200_success_parsesSessionAndVerifiesRequestBody() = runTest {
        val jsonResponseBody = """
            {
              "access_token": "mock-supabase-access-token-xyz",
              "token_type": "bearer",
              "expires_in": 3600,
              "refresh_token": "mock-supabase-refresh-token-xyz",
              "user": {
                "id": "mock-user-uuid-12345",
                "email": "motorista.giro@gmail.com",
                "phone": "+5511999998888",
                "created_at": "2026-10-22T12:00:00Z"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(jsonResponseBody)
        )

        val result = supabaseClient.signInWithGoogleIdToken(
            idToken = "mock-google-id-token-abc",
            nonce = "mock-raw-nonce-123"
        )

        // 1. Validação do resultado da sessão
        assertTrue("Resultado deve ser sucesso", result.isSuccess)
        val session = result.getOrNull()
        assertNotNull("Sessão não deve ser nula", session)
        assertEquals("mock-supabase-access-token-xyz", session?.accessToken)
        assertEquals("mock-supabase-refresh-token-xyz", session?.refreshToken)
        assertEquals("mock-user-uuid-12345", session?.userId)
        assertEquals("motorista.giro@gmail.com", session?.userEmail)

        // 2. Validação do estado interno do SupabaseClient
        assertTrue(supabaseClient.isAuthenticated)
        assertEquals("mock-supabase-access-token-xyz", supabaseClient.currentAccessToken)
        assertEquals("mock-user-uuid-12345", supabaseClient.currentUserId)
        val savedSession = supabaseClient.getSavedSession()
        assertEquals(session, savedSession)

        // 3. Validação do request HTTP recebido pelo MockWebServer
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/auth/v1/token?grant_type=id_token", recordedRequest.path)
        val requestBody = recordedRequest.body.readUtf8()
        val json = JSONObject(requestBody)
        assertEquals("google", json.getString("provider"))
        assertEquals("mock-google-id-token-abc", json.getString("id_token"))
        assertEquals("mock-raw-nonce-123", json.getString("nonce"))
    }

    @Test
    fun testSignInWithGoogleIdToken_http400_failure_returnsResultFailureWithErrorMessage() = runTest {
        val errorJson = """
            {
              "error": "invalid_request",
              "error_description": "Unacceptable audience or token signature in Google ID Token"
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(errorJson)
        )

        val result = supabaseClient.signInWithGoogleIdToken(
            idToken = "bad-token",
            nonce = "bad-nonce"
        )

        assertTrue("Resultado deve ser failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exceção não deve ser nula", exception)
        val errorMsg = exception?.message.orEmpty()
        assertTrue(
            "Mensagem deve conter o erro retornado pela API ou código HTTP",
            errorMsg.contains("Unacceptable audience") || errorMsg.contains("invalid_request") || errorMsg.contains("400")
        )
        assertFalse("SupabaseClient não deve estar autenticado", supabaseClient.isAuthenticated)
        assertNull("SupabaseClient não deve ter accessToken ativo", supabaseClient.currentAccessToken)

        // Valida que o request HTTP foi montado corretamente mesmo em caso de erro retornado pelo servidor
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/auth/v1/token?grant_type=id_token", recordedRequest.path)
        val json = JSONObject(recordedRequest.body.readUtf8())
        assertEquals("google", json.getString("provider"))
        assertEquals("bad-token", json.getString("id_token"))
        assertEquals("bad-nonce", json.getString("nonce"))
    }

    @Test
    fun testSignInWithGoogleIdToken_http401_failure_returnsResultFailureWithUnauthorizedMessage() = runTest {
        val errorJson = """
            {
              "message": "Token has expired or is invalid for this project",
              "status": 401
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(errorJson)
        )

        val result = supabaseClient.signInWithGoogleIdToken(
            idToken = "expired-token",
            nonce = null
        )

        assertTrue("Resultado deve ser failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exceção não deve ser nula", exception)
        val errorMsg = exception?.message.orEmpty()
        assertTrue(
            "Mensagem deve conter o erro da API ou 401",
            errorMsg.contains("Token has expired") || errorMsg.contains("401")
        )
        assertFalse(supabaseClient.isAuthenticated)

        // Valida o request HTTP
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("/auth/v1/token?grant_type=id_token", recordedRequest.path)
        val json = JSONObject(recordedRequest.body.readUtf8())
        assertEquals("google", json.getString("provider"))
        assertEquals("expired-token", json.getString("id_token"))
    }

    @Test
    fun testSignUp_http200_userAlreadyExistsWithEmptyIdentities_returnsFailureWithUserAlreadyExistsResId() = runTest {
        // GoTrue retorna HTTP 200 com array identities vazio quando a confirmação de e-mail está ativa
        // e o usuário já está cadastrado. O app deve detectar e falhar com R.string.auth_error_user_already_exists.
        val jsonResponseBody = """
            {
              "access_token": "",
              "token_type": "",
              "expires_in": 0,
              "refresh_token": "",
              "user": {
                "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
                "email": "existente@girocusto.com",
                "phone": "",
                "created_at": "2026-10-25T10:00:00Z",
                "identities": []
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(jsonResponseBody)
        )

        val result = supabaseClient.signUp(
            email = "existente@girocusto.com",
            pass = "senha123"
        )

        // Deve falhar com erro mapeado para auth_error_user_already_exists
        assertTrue("Resultado deve ser falha quando identities é vazio", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Exceção deve ser SupabaseAuthException", exception is com.example.network.supabase.SupabaseAuthException)
        val authException = exception as com.example.network.supabase.SupabaseAuthException
        assertEquals(
            "Deve mapear para auth_error_user_already_exists",
            com.example.R.string.auth_error_user_already_exists,
            authException.messageResId
        )
    }

    @Test
    fun testSignUp_http200_newUserWithNonEmptyIdentities_returnsSuccess() = runTest {
        val jsonResponseBody = """
            {
              "access_token": "",
              "token_type": "",
              "expires_in": 0,
              "refresh_token": "",
              "user": {
                "id": "mock-new-user-123",
                "email": "novousuario@girocusto.com",
                "phone": "",
                "created_at": "2026-10-25T11:00:00Z",
                "identities": [
                  {
                    "id": "mock-ident-1",
                    "user_id": "mock-new-user-123",
                    "provider": "email"
                  }
                ]
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(jsonResponseBody)
        )

        val result = supabaseClient.signUp(
            email = "novousuario@girocusto.com",
            pass = "senhaSegura123"
        )

        assertTrue("Resultado deve ser sucesso para novo cadastro", result.isSuccess)
        val session = result.getOrNull()
        assertNotNull("Sessão não deve ser nula", session)
        assertEquals("mock-new-user-123", session?.userId)
        assertEquals("novousuario@girocusto.com", session?.userEmail)
    }
}


