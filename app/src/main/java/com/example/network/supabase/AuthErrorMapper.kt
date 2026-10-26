package com.example.network.supabase

import androidx.annotation.StringRes
import com.example.R
import org.json.JSONObject
import java.io.IOException

/**
 * Contexto do fluxo de autenticação em execução.
 * Permite aplicar regras de segurança específicas de cada contexto (ex.: no LOGIN, evitar enumeração de contas;
 * no CADASTRO, informar que o e-mail já existe).
 */
enum class AuthFlow {
    SIGN_IN,
    SIGN_UP,
    GOOGLE_SIGN_IN,
    REFRESH_TOKEN,
    GENERIC
}

/**
 * Identificador tipado e selado para erros de autenticação da camada lógica/rede.
 * Cada subclasse encapsula a referência ao identificador do recurso em strings.xml (@StringRes Int).
 *
 * É PROIBIDO chamar `stringResource` nesta classe ou camada; a resolução do texto cabe
 * exclusivamente aos componentes @Composable da UI.
 */
sealed interface AuthError {
    @get:StringRes
    val stringResId: Int

    data object InvalidCredentials : AuthError {
        override val stringResId = R.string.auth_error_invalid_credentials
    }

    data object UserAlreadyExists : AuthError {
        override val stringResId = R.string.auth_error_user_already_exists
    }

    data object EmailNotConfirmed : AuthError {
        override val stringResId = R.string.auth_error_email_not_confirmed
    }

    data object RateLimit : AuthError {
        override val stringResId = R.string.auth_error_rate_limit
    }

    data object WeakPassword : AuthError {
        override val stringResId = R.string.auth_error_weak_password
    }

    data object Network : AuthError {
        override val stringResId = R.string.auth_error_network
    }

    data object Generic : AuthError {
        override val stringResId = R.string.auth_error_generic
    }
}

/**
 * Exceção tipada emitida por SupabaseClient ao falhar operações de autenticação.
 * Preserva o rawError e o httpCode para rastreabilidade e compatibilidade de testes existentes,
 * ao mesmo tempo que carrega o identificador tipado do recurso de mensagem (messageResId).
 */
open class SupabaseAuthException(
    @StringRes val messageResId: Int,
    val rawError: String? = null,
    val httpCode: Int = 0,
    cause: Throwable? = null
) : Exception(rawError ?: if (httpCode > 0) "HTTP $httpCode" else "Erro de autenticação", cause)

/**
 * Utilitário desacoplado para mapeamento de erros da API Supabase Auth (GoTrue).
 *
 * Analisa o código de status HTTP, o corpo JSON e/ou exceções de rede e retorna
 * um identificador tipado (AuthError ou @StringRes Int).
 */
object AuthErrorMapper {

    /**
     * Verifica se a resposta de cadastro do GoTrue corresponde a um usuário que já existia previamente.
     * Quando "Confirm email" está ativo no Supabase, a API retorna HTTP 200 com array identities vazio (`[]`).
     */
    fun isExistingUserSignUp(user: SupabaseUser?): Boolean {
        return user?.identities != null && user.identities.isEmpty()
    }

    /**
     * Mapeia código HTTP e payload de erro para o tipo selado AuthError.
     */
    fun mapToAuthError(
        httpCode: Int,
        errorBody: String?,
        flow: AuthFlow = AuthFlow.GENERIC
    ): AuthError {
        // 1. Limite de requisições por HTTP status code
        if (httpCode == 429) {
            return AuthError.RateLimit
        }

        val rawText = errorBody?.trim().orEmpty()
        if (rawText.isBlank()) {
            return when {
                flow == AuthFlow.SIGN_IN && (httpCode == 400 || httpCode == 401) -> AuthError.InvalidCredentials
                else -> AuthError.Generic
            }
        }

        val (errorCode, message) = parseErrorBody(rawText)
        val combined = "$errorCode $message $rawText".lowercase()

        // 2. E-mail não confirmado (aplicável tanto a LOGIN quanto a CADASTRO)
        if (combined.contains("email_not_confirmed") ||
            combined.contains("email not confirmed") ||
            combined.contains("confirm your email")
        ) {
            return AuthError.EmailNotConfirmed
        }

        // 3. Taxa de envio excedida (rate limit)
        if (combined.contains("over_email_send_rate_limit") ||
            combined.contains("email rate limit exceeded") ||
            combined.contains("rate_limit") ||
            combined.contains("rate limit") ||
            combined.contains("too many requests") ||
            combined.contains("once every")
        ) {
            return AuthError.RateLimit
        }

        // 4. Usuário já existente
        if (combined.contains("user_already_exists") ||
            combined.contains("user already registered") ||
            combined.contains("user already exists") ||
            combined.contains("already registered")
        ) {
            // No fluxo de LOGIN, mantém mensagem genérica para evitar enumeração de contas
            return if (flow == AuthFlow.SIGN_IN) {
                AuthError.InvalidCredentials
            } else {
                AuthError.UserAlreadyExists
            }
        }

        // 5. Senha fraca / curta
        if (combined.contains("weak_password") ||
            combined.contains("password should be at least") ||
            combined.contains("password is too short") ||
            combined.contains("weak password")
        ) {
            return AuthError.WeakPassword
        }

        // 6. Credenciais inválidas (Login)
        if (combined.contains("invalid_credentials") ||
            combined.contains("invalid login credentials") ||
            combined.contains("invalid credentials") ||
            combined.contains("invalid_grant") ||
            (flow == AuthFlow.SIGN_IN && (httpCode == 400 || httpCode == 401))
        ) {
            return AuthError.InvalidCredentials
        }

        // 7. Erros desconhecidos: mensagem genérica segura sem expor JSON, códigos em inglês ou stack traces
        return AuthError.Generic
    }

    /**
     * Mapeia código HTTP e payload de erro diretamente para o identificador @StringRes Int.
     */
    @StringRes
    fun mapHttpError(
        httpCode: Int,
        errorBody: String?,
        flow: AuthFlow = AuthFlow.GENERIC
    ): Int {
        return mapToAuthError(httpCode, errorBody, flow).stringResId
    }

    /**
     * Mapeia Throwable (erros de rede, timeouts, exceções tipadas) para o tipo selado AuthError.
     */
    fun mapThrowableToAuthError(
        throwable: Throwable?,
        flow: AuthFlow = AuthFlow.GENERIC
    ): AuthError {
        if (throwable == null) return AuthError.Generic

        if (throwable is SupabaseAuthException) {
            return when (throwable.messageResId) {
                R.string.auth_error_invalid_credentials -> AuthError.InvalidCredentials
                R.string.auth_error_user_already_exists -> AuthError.UserAlreadyExists
                R.string.auth_error_email_not_confirmed -> AuthError.EmailNotConfirmed
                R.string.auth_error_rate_limit -> AuthError.RateLimit
                R.string.auth_error_weak_password -> AuthError.WeakPassword
                R.string.auth_error_network -> AuthError.Network
                else -> AuthError.Generic
            }
        }

        if (throwable is IOException) {
            return AuthError.Network
        }

        val message = throwable.message?.lowercase().orEmpty()
        if (message.contains("unable to resolve host") ||
            message.contains("failed to connect") ||
            message.contains("timeout") ||
            message.contains("network") ||
            message.contains("connection")
        ) {
            return AuthError.Network
        }

        return mapToAuthError(httpCode = 0, errorBody = throwable.message, flow = flow)
    }

    /**
     * Mapeia Throwable diretamente para o identificador @StringRes Int.
     */
    @StringRes
    fun mapThrowable(
        throwable: Throwable?,
        flow: AuthFlow = AuthFlow.GENERIC
    ): Int {
        return mapThrowableToAuthError(throwable, flow).stringResId
    }

    private fun parseErrorBody(raw: String): Pair<String, String> {
        return try {
            val json = JSONObject(raw)
            val errorCode = when {
                json.has("error_code") -> json.optString("error_code")
                json.has("error") -> json.optString("error")
                else -> ""
            }
            val message = when {
                json.has("msg") -> json.optString("msg")
                json.has("message") -> json.optString("message")
                json.has("error_description") -> json.optString("error_description")
                else -> ""
            }
            Pair(errorCode, message)
        } catch (_: Exception) {
            Pair("", "")
        }
    }
}
