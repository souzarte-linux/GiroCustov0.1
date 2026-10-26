package com.example.network.supabase

import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Testes unitários para AuthErrorMapper (T-26 [UX-03]).
 * Valida o mapeamento de códigos HTTP, payloads JSON e exceções de rede para
 * identificadores tipados (@StringRes Int e AuthError sealed type).
 */
class AuthErrorMapperTest {

    @Test
    fun testMapHttpError_login_invalidCredentials_returnsInvalidCredentialsStringRes() {
        val payload = """{"code":400,"error_code":"invalid_credentials","msg":"Invalid login credentials"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_invalid_credentials, resId)
        assertEquals(AuthError.InvalidCredentials, authError)
    }

    @Test
    fun testMapHttpError_login_invalidGrant_returnsInvalidCredentialsStringRes() {
        val payload = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_invalid_credentials, resId)
    }

    @Test
    fun testMapHttpError_login_generic400WithoutBody_returnsInvalidCredentialsStringRes() {
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = "", flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_invalid_credentials, resId)
    }

    @Test
    fun testMapHttpError_login_userAlreadyExistsPayload_returnsInvalidCredentialsStringRes_preventingEnumeration() {
        // No fluxo de LOGIN, mesmo se o payload indicar user_already_exists, a mensagem deve ser
        // genérica anti-enumeração de usuários conforme a seção 1.4 de padroes-seguranca-ux.md
        val payload = """{"code":400,"error_code":"user_already_exists","msg":"User already registered"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_invalid_credentials, resId)
        assertEquals(AuthError.InvalidCredentials, authError)
    }

    @Test
    fun testMapHttpError_cadastro_userAlreadyExists_returnsUserAlreadyExistsStringRes() {
        val payload = """{"code":400,"error_code":"user_already_exists","msg":"User already registered"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_UP)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_UP)

        assertEquals(R.string.auth_error_user_already_exists, resId)
        assertEquals(AuthError.UserAlreadyExists, authError)
    }

    @Test
    fun testMapHttpError_cadastro_userAlreadyRegisteredMessage_returnsUserAlreadyExistsStringRes() {
        val payload = """{"message":"User already registered","status":400}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_UP)

        assertEquals(R.string.auth_error_user_already_exists, resId)
    }

    @Test
    fun testMapHttpError_emailNotConfirmed_onLogin_returnsEmailNotConfirmedStringRes() {
        val payload = """{"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_email_not_confirmed, resId)
        assertEquals(AuthError.EmailNotConfirmed, authError)
    }

    @Test
    fun testMapHttpError_emailNotConfirmed_onCadastro_returnsEmailNotConfirmedStringRes() {
        val payload = """{"code":400,"error_code":"email_not_confirmed","msg":"Email not confirmed"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_UP)

        assertEquals(R.string.auth_error_email_not_confirmed, resId)
    }

    @Test
    fun testMapHttpError_rateLimit_http429_returnsRateLimitStringRes() {
        val resId = AuthErrorMapper.mapHttpError(httpCode = 429, errorBody = null, flow = AuthFlow.GENERIC)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 429, errorBody = null, flow = AuthFlow.GENERIC)

        assertEquals(R.string.auth_error_rate_limit, resId)
        assertEquals(AuthError.RateLimit, authError)
    }

    @Test
    fun testMapHttpError_rateLimit_overEmailSendRateLimit_returnsRateLimitStringRes() {
        val payload = """{"error":"over_email_send_rate_limit","error_description":"For security purposes, you can only request this once every 60 seconds"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 400, errorBody = payload, flow = AuthFlow.SIGN_IN)

        assertEquals(R.string.auth_error_rate_limit, resId)
    }

    @Test
    fun testMapHttpError_rateLimit_emailRateLimitExceededMessage_returnsRateLimitStringRes() {
        val payload = """{"code":429,"msg":"email rate limit exceeded"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 429, errorBody = payload, flow = AuthFlow.SIGN_UP)

        assertEquals(R.string.auth_error_rate_limit, resId)
    }

    @Test
    fun testMapHttpError_weakPassword_returnsWeakPasswordStringRes() {
        val payload = """{"code":422,"error_code":"weak_password","msg":"Password should be at least 6 characters"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 422, errorBody = payload, flow = AuthFlow.SIGN_UP)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 422, errorBody = payload, flow = AuthFlow.SIGN_UP)

        assertEquals(R.string.auth_error_weak_password, resId)
        assertEquals(AuthError.WeakPassword, authError)
    }

    @Test
    fun testMapThrowable_ioException_returnsNetworkStringRes() {
        val ex = IOException("Falha de conexão com o servidor")
        val resId = AuthErrorMapper.mapThrowable(ex)
        val authError = AuthErrorMapper.mapThrowableToAuthError(ex)

        assertEquals(R.string.auth_error_network, resId)
        assertEquals(AuthError.Network, authError)
    }

    @Test
    fun testMapThrowable_socketTimeoutException_returnsNetworkStringRes() {
        val ex = SocketTimeoutException("connect timed out")
        val resId = AuthErrorMapper.mapThrowable(ex)

        assertEquals(R.string.auth_error_network, resId)
    }

    @Test
    fun testMapThrowable_unknownHostException_returnsNetworkStringRes() {
        val ex = UnknownHostException("Unable to resolve host [OCULTO].supabase.co: No address associated with hostname")
        val resId = AuthErrorMapper.mapThrowable(ex)

        assertEquals(R.string.auth_error_network, resId)
    }

    @Test
    fun testMapHttpError_unknownServerError500_returnsGenericStringRes() {
        val payload = """{"code":500,"msg":"Internal Server Error"}"""
        val resId = AuthErrorMapper.mapHttpError(httpCode = 500, errorBody = payload, flow = AuthFlow.GENERIC)
        val authError = AuthErrorMapper.mapToAuthError(httpCode = 500, errorBody = payload, flow = AuthFlow.GENERIC)

        assertEquals(R.string.auth_error_generic, resId)
        assertEquals(AuthError.Generic, authError)
    }

    @Test
    fun testMapHttpError_malformedJson_returnsGenericStringRes() {
        val payload = "<html><body>502 Bad Gateway</body></html>"
        val resId = AuthErrorMapper.mapHttpError(httpCode = 502, errorBody = payload, flow = AuthFlow.GENERIC)

        assertEquals(R.string.auth_error_generic, resId)
    }

    @Test
    fun testMapThrowable_supabaseAuthException_preservesMappedResId() {
        val ex = SupabaseAuthException(
            messageResId = R.string.auth_error_weak_password,
            rawError = "weak",
            httpCode = 422
        )
        val resId = AuthErrorMapper.mapThrowable(ex)
        val authError = AuthErrorMapper.mapThrowableToAuthError(ex)

        assertEquals(R.string.auth_error_weak_password, resId)
        assertEquals(AuthError.WeakPassword, authError)
        assertEquals("weak", ex.message)
    }

    @Test
    fun testAuthError_sealedTypes_stringResIdsMatchExpected() {
        assertEquals(R.string.auth_error_invalid_credentials, AuthError.InvalidCredentials.stringResId)
        assertEquals(R.string.auth_error_user_already_exists, AuthError.UserAlreadyExists.stringResId)
        assertEquals(R.string.auth_error_email_not_confirmed, AuthError.EmailNotConfirmed.stringResId)
        assertEquals(R.string.auth_error_rate_limit, AuthError.RateLimit.stringResId)
        assertEquals(R.string.auth_error_weak_password, AuthError.WeakPassword.stringResId)
        assertEquals(R.string.auth_error_network, AuthError.Network.stringResId)
        assertEquals(R.string.auth_error_generic, AuthError.Generic.stringResId)
    }
}
