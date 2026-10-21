package com.example.network.fipe

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface Retrofit para a API da Tabela FIPE (Parallelum).
 */
interface FipeApi {
    @GET("{tipo}/marcas")
    suspend fun getMarcas(
        @Path("tipo") tipo: String
    ): Response<List<FipeItemDto>>

    @GET("{tipo}/marcas/{marcaId}/modelos")
    suspend fun getModelos(
        @Path("tipo") tipo: String,
        @Path("marcaId") marcaId: String
    ): Response<FipeModelsResponseDto>

    @GET("{tipo}/marcas/{marcaId}/modelos/{modeloId}/anos")
    suspend fun getAnos(
        @Path("tipo") tipo: String,
        @Path("marcaId") marcaId: String,
        @Path("modeloId") modeloId: String
    ): Response<List<FipeItemDto>>

    @GET("{tipo}/marcas/{marcaId}/modelos/{modeloId}/anos/{anoId}")
    suspend fun getDetail(
        @Path("tipo") tipo: String,
        @Path("marcaId") marcaId: String,
        @Path("modeloId") modeloId: String,
        @Path("anoId") anoId: String
    ): Response<FipeDetailDto>
}
