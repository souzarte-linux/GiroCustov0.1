package com.example.ui.screens.cloud

enum class AuthTab {
    LOGIN,
    REGISTER
}

fun translateAuthError(error: String): String {
    val lower = error.lowercase()
    return when {
        "401" in lower || "jwt expired" in lower || "unauthorized" in lower || "sessão expirou" in lower ->
            "Sua sessão expirou. Entre novamente com sua conta para continuar sincronizando."
        "invalid login credentials" in lower || "invalid_grant" in lower ->
            "E-mail ou senha incorretos. Verifique suas credenciais."
        "user already registered" in lower ->
            "Este e-mail já está cadastrado. Tente entrar."
        "password should be at least" in lower ->
            "A senha deve conter no mínimo 6 caracteres."
        "unknownhostexception" in lower || "connectexception" in lower || "failed to connect" in lower ->
            "Sem conexão com a internet. Verifique seu sinal ou Wi-Fi."
        else -> error
    }
}
