package com.example.util

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

/**
 * Resultado da autenticação pelo Credential Manager com Google ID.
 */
data class GoogleAuthResult(
    val idToken: String,
    val rawNonce: String
)

/**
 * Manipulador de autenticação Google via Credential Manager.
 * Permite desacoplamento e injeção controlada de dependência.
 */
fun interface GoogleAuthHandler {
    suspend fun authenticate(context: Context, serverClientId: String): Result<GoogleAuthResult>
}

/**
 * Helper para fluxo nativo de autenticação com o Google usando Credential Manager e Google ID.
 * Implementa geração de Nonce com hash SHA-256 e tratamento de exceções em Português Brasileiro.
 */
object GoogleAuthHelper {

    private var authHandler: GoogleAuthHandler? = null

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setAuthHandler(handler: GoogleAuthHandler?) {
        this.authHandler = handler
    }

    /**
     * Gera um nonce aleatório seguro.
     */
    fun generateNonce(): String = UUID.randomUUID().toString()

    /**
     * Calcula o hash SHA-256 em hexadecimal em caixa baixa do nonce informado.
     */
    fun hashNonce(rawNonce: String): String {
        val bytes = rawNonce.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Inicia a autenticação com o Google via Credential Manager.
     */
    suspend fun getGoogleIdToken(
        context: Context,
        serverClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
    ): Result<GoogleAuthResult> {
        authHandler?.let { handler ->
            return handler.authenticate(context, serverClientId)
        }

        if (serverClientId.isBlank()) {
            return Result.failure(IllegalStateException("GOOGLE_WEB_CLIENT_ID não configurado."))
        }

        return try {
            val rawNonce = generateNonce()
            val hashedNonce = hashNonce(rawNonce)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(context)
            val response = credentialManager.getCredential(context = context, request = request)

            when (val credential = response.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        Result.success(GoogleAuthResult(idToken = idToken, rawNonce = rawNonce))
                    } else {
                        Result.failure(Exception("Tipo de credencial não suportado: ${credential.type}"))
                    }
                }
                else -> {
                    Result.failure(Exception("Credencial inesperada recebida do provedor Google."))
                }
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Autenticação cancelada pelo usuário.", e))
        } catch (e: NoCredentialException) {
            Result.failure(Exception("Nenhuma conta Google encontrada no dispositivo.", e))
        } catch (e: GetCredentialInterruptedException) {
            Result.failure(Exception("A autenticação foi interrompida. Tente novamente.", e))
        } catch (e: GetCredentialProviderConfigurationException) {
            Result.failure(Exception("Configuração do Google Play Services indisponível ou desatualizada.", e))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Falha ao processar as credenciais da conta Google.", e))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Sem conexão com a internet. Verifique sua rede e tente novamente.", e))
        } catch (e: GetCredentialException) {
            val msg = e.message.orEmpty()
            val errorMsg = if (msg.contains("network", ignoreCase = true) || e.type.contains("network", ignoreCase = true)) {
                "Sem conexão com a internet. Verifique sua rede e tente novamente."
            } else {
                "Falha ao autenticar com o Google: ${e.message ?: "erro desconhecido"}"
            }
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val errorMsg = if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                "Sem conexão com a internet. Verifique sua rede e tente novamente."
            } else {
                e.localizedMessage ?: "Não foi possível concluir a autenticação com o Google."
            }
            Result.failure(Exception(errorMsg, e))
        }
    }
}
