package com.example.network.viacep

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ViaCepResponse(
    @Json(name = "cep") val cep: String? = null,
    @Json(name = "logradouro") val logradouro: String? = null,
    @Json(name = "complemento") val complemento: String? = null,
    @Json(name = "bairro") val bairro: String? = null,
    @Json(name = "localidade") val localidade: String? = null,
    @Json(name = "uf") val uf: String? = null,
    @Json(name = "ibge") val ibge: String? = null,
    @Json(name = "ddd") val ddd: String? = null,
    @Json(name = "erro") val erroRaw: Any? = null
) {
    /** Verifica com segurança se a API retornou indicação de erro booleana ou em String */
    val isErro: Boolean
        get() = erroRaw == true || erroRaw?.toString()?.equals("true", ignoreCase = true) == true
}
