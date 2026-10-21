package com.example.ui.navigation

sealed interface RootNavState {
    /** Carregamento inicial do token em disco e leitura inicial do Room */
    data object Initializing : RootNavState

    /** Usuário não logado: apresenta tela de boas-vindas / login / cadastro */
    data object Unauthenticated : RootNavState

    /** Usuário autenticado, mas com perfil incompleto (primeiro acesso) */
    data object OnboardingPending : RootNavState

    /** Usuário autenticado e cadastro completo: acesso total ao app */
    data object AuthenticatedReady : RootNavState
}
