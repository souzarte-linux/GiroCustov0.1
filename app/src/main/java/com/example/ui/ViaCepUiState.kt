package com.example.ui

import com.example.network.viacep.ViaCepResponse

sealed interface ViaCepUiState {
    data object Idle : ViaCepUiState
    data object Loading : ViaCepUiState
    data class Success(val address: ViaCepResponse) : ViaCepUiState
    data class Error(val message: String) : ViaCepUiState
}
