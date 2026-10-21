package com.example.network.viacep

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ViaCepApi {
    @GET("{cep}/json/")
    suspend fun getAddressByCep(@Path("cep") cep: String): Response<ViaCepResponse>

    @GET("{cep}/json/")
    suspend fun getAddress(@Path("cep") cep: String): Response<ViaCepResponse>
}
