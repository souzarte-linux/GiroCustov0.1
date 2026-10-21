package com.example.network.fipe

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP Retrofit/OkHttp para consumo da API da Tabela FIPE.
 * Permite injeção de [testApi] para testes unitários isolados.
 */
object FipeNetworkClient {
    private const val BASE_URL = "https://parallelum.com.br/fipe/api/v1/"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "GiroCusto-Android/1.0")
                .header("Accept", "application/json")
                .build()
            chain.proceed(req)
        }
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val realApi: FipeApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(FipeApi::class.java)

    @Volatile
    var testApi: FipeApi? = null

    val api: FipeApi
        get() = testApi ?: realApi
}
