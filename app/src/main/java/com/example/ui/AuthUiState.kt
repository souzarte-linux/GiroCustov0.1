package com.example.ui

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val message: String? = null) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
