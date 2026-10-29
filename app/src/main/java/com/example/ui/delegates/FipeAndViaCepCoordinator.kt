package com.example.ui.delegates

import com.example.network.fipe.FipeDetailDto
import com.example.network.fipe.FipeItemDto
import com.example.network.fipe.FipeNetworkClient
import com.example.ui.FipeUiState
import com.example.network.fipe.FipeVehicleType
import com.example.network.viacep.ViaCepNetworkClient
import com.example.ui.ViaCepUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Coordenador responsável pelo gerenciamento de estados assíncronos e chamadas de rede
 * para a Tabela FIPE (Parallelum API) e consulta de endereços via CEP (ViaCEP).
 */
class FipeAndViaCepCoordinator(
    private val scope: CoroutineScope
) {
    private val _viaCepState = MutableStateFlow<ViaCepUiState>(ViaCepUiState.Idle)
    val viaCepState: StateFlow<ViaCepUiState> = _viaCepState.asStateFlow()

    private val _fipeUiState = MutableStateFlow<FipeUiState>(FipeUiState.Idle)
    val fipeUiState: StateFlow<FipeUiState> = _fipeUiState.asStateFlow()

    private val _fipeMarcas = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeMarcas: StateFlow<List<FipeItemDto>> = _fipeMarcas.asStateFlow()

    private val _fipeModelos = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeModelos: StateFlow<List<FipeItemDto>> = _fipeModelos.asStateFlow()

    private val _fipeAnos = MutableStateFlow<List<FipeItemDto>>(emptyList())
    val fipeAnos: StateFlow<List<FipeItemDto>> = _fipeAnos.asStateFlow()

    private val _fipeDetail = MutableStateFlow<FipeDetailDto?>(null)
    val fipeDetail: StateFlow<FipeDetailDto?> = _fipeDetail.asStateFlow()

    fun searchCep(rawCep: String) {
        val cleanCep = rawCep.replace("\\D".toRegex(), "")
        if (cleanCep.length != 8) {
            _viaCepState.value = ViaCepUiState.Error("O CEP deve conter exatamente 8 dígitos numéricos.")
            return
        }

        scope.launch {
            _viaCepState.value = ViaCepUiState.Loading
            try {
                val response = ViaCepNetworkClient.api.getAddressByCep(cleanCep)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.isErro) {
                        _viaCepState.value = ViaCepUiState.Error("CEP não encontrado na base dos Correios.")
                    } else {
                        _viaCepState.value = ViaCepUiState.Success(body)
                    }
                } else {
                    _viaCepState.value = ViaCepUiState.Error("Serviço de CEP temporariamente indisponível (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _viaCepState.value = ViaCepUiState.Error("Tempo limite excedido ao consultar o CEP.")
            } catch (e: java.io.IOException) {
                _viaCepState.value = ViaCepUiState.Error("Sem conexão de internet para buscar o CEP.")
            } catch (e: Exception) {
                _viaCepState.value = ViaCepUiState.Error("Erro inesperado: ${e.localizedMessage ?: "Falha na consulta"}")
            }
        }
    }

    fun clearViaCepState() {
        _viaCepState.value = ViaCepUiState.Idle
    }

    fun loadFipeMarcas(vehicleType: String) {
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        scope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeMarcas.value = emptyList()
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getMarcas(fipeType)
                if (response.isSuccessful && response.body() != null) {
                    _fipeMarcas.value = response.body()!!
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar as marcas da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar a Tabela FIPE. Verifique sua conexão.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar a Tabela FIPE.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar a Tabela FIPE: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeModelos(vehicleType: String, brandCode: String) {
        if (brandCode.isBlank()) {
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        scope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeModelos.value = emptyList()
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getModelos(fipeType, brandCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeModelos.value = response.body()!!.modelos
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar os modelos da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar os modelos. Tente novamente.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar os modelos.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar modelos: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeAnos(vehicleType: String, brandCode: String, modelCode: String) {
        if (brandCode.isBlank() || modelCode.isBlank()) {
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        scope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeAnos.value = emptyList()
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getAnos(fipeType, brandCode, modelCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeAnos.value = response.body()!!
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar os anos do veículo (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar anos do veículo.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar anos do veículo.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar anos: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun loadFipeDetail(vehicleType: String, brandCode: String, modelCode: String, yearCode: String) {
        if (brandCode.isBlank() || modelCode.isBlank() || yearCode.isBlank()) {
            _fipeDetail.value = null
            return
        }
        val fipeType = FipeVehicleType.fromString(vehicleType).apiPath
        scope.launch {
            _fipeUiState.value = FipeUiState.Loading
            _fipeDetail.value = null
            try {
                val response = FipeNetworkClient.api.getDetail(fipeType, brandCode, modelCode, yearCode)
                if (response.isSuccessful && response.body() != null) {
                    _fipeDetail.value = response.body()
                    _fipeUiState.value = FipeUiState.Success
                } else {
                    _fipeUiState.value = FipeUiState.Error("Não foi possível carregar a avaliação da Tabela FIPE (${response.code()}).")
                }
            } catch (e: java.net.SocketTimeoutException) {
                _fipeUiState.value = FipeUiState.Error("Tempo limite excedido ao consultar detalhes da Tabela FIPE.")
            } catch (e: java.io.IOException) {
                _fipeUiState.value = FipeUiState.Error("Sem conexão com a internet para consultar detalhes da Tabela FIPE.")
            } catch (e: Exception) {
                _fipeUiState.value = FipeUiState.Error("Erro inesperado ao consultar detalhes: ${e.localizedMessage ?: "Falha na comunicação."}")
            }
        }
    }

    fun clearFipe() {
        _fipeUiState.value = FipeUiState.Idle
        _fipeMarcas.value = emptyList()
        _fipeModelos.value = emptyList()
        _fipeAnos.value = emptyList()
        _fipeDetail.value = null
    }
}
