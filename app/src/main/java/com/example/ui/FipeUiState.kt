package com.example.ui

/**
 * Estados da interface para o fluxo de consulta à Tabela FIPE.
 */
sealed interface FipeUiState {
    data object Idle : FipeUiState
    data object Loading : FipeUiState
    data object Success : FipeUiState
    data class Error(val message: String) : FipeUiState
}
