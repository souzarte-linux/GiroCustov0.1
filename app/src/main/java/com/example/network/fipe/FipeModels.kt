package com.example.network.fipe

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Tipos de veículos suportados pela API da Tabela FIPE (Parallelum).
 */
enum class FipeVehicleType(val apiPath: String, val label: String) {
    CARS("carros", "Carros"),
    MOTORCYCLES("motos", "Motos"),
    TRUCKS("caminhoes", "Caminhões");

    companion object {
        fun fromString(value: String): FipeVehicleType {
            return when (value.trim().lowercase()) {
                "motos", "moto", "motocicleta", "motorcycle", "2_rodas" -> MOTORCYCLES
                "caminhoes", "caminhão", "caminhao", "truck", "trucks", "pesado" -> TRUCKS
                else -> CARS
            }
        }
    }
}

/**
 * DTO para itens de Marcas, Modelos e Anos retornados pela Tabela FIPE.
 * Suporta deserialização resiliente do campo 'codigo' tanto como String ("1", "1992-1")
 * quanto como valor numérico (1, 59).
 */
@JsonClass(generateAdapter = true)
data class FipeItemDto(
    @Json(name = "nome") val nome: String = "",
    @Json(name = "codigo") val codigoRaw: Any? = null
) {
    val codigo: String
        get() = when (codigoRaw) {
            null -> ""
            is Number -> {
                val num = (codigoRaw as Number).toDouble()
                if (num == num.toLong().toDouble()) {
                    num.toLong().toString()
                } else {
                    num.toString()
                }
            }
            else -> codigoRaw.toString().trim()
        }
}

/**
 * Resposta da busca de modelos de uma marca na Tabela FIPE.
 * Contém a lista de [modelos] e a lista de [anos].
 */
@JsonClass(generateAdapter = true)
data class FipeModelsResponseDto(
    @Json(name = "modelos") val modelos: List<FipeItemDto> = emptyList(),
    @Json(name = "anos") val anos: List<FipeItemDto> = emptyList()
)

/**
 * Detalhes da avaliação e especificações do veículo retornados pela Tabela FIPE.
 */
@JsonClass(generateAdapter = true)
data class FipeDetailDto(
    @Json(name = "TipoVeiculo") val tipoVeiculoRaw: Any? = null,
    @Json(name = "Valor") val valor: String = "",
    @Json(name = "Marca") val marca: String = "",
    @Json(name = "Modelo") val modelo: String = "",
    @Json(name = "AnoModelo") val anoModeloRaw: Any? = null,
    @Json(name = "Combustivel") val combustivel: String = "",
    @Json(name = "CodigoFipe") val codigoFipe: String = "",
    @Json(name = "MesReferencia") val mesReferencia: String = "",
    @Json(name = "SiglaCombustivel") val siglaCombustivel: String = ""
) {
    val anoModelo: Int
        get() = when (anoModeloRaw) {
            is Number -> (anoModeloRaw as Number).toInt()
            is String -> anoModeloRaw.toIntOrNull() ?: 0
            else -> 0
        }

    val tipoVeiculo: Int
        get() = when (tipoVeiculoRaw) {
            is Number -> (tipoVeiculoRaw as Number).toInt()
            is String -> tipoVeiculoRaw.toIntOrNull() ?: 0
            else -> 0
        }

    /**
     * Extrai o valor monetário numérico (ex: "R$ 45.230,00" -> 45230.0).
     */
    val valorNumerico: Double
        get() {
            return try {
                val clean = valor.replace("R$", "")
                    .replace(".", "")
                    .replace(",", ".")
                    .trim()
                clean.toDoubleOrNull() ?: 0.0
            } catch (_: Exception) {
                0.0
            }
        }
}
