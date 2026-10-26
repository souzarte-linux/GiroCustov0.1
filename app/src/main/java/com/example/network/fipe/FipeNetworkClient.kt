package com.example.network.fipe

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP Retrofit/OkHttp para consumo da API da Tabela FIPE.
 * Suporta injeção de [api] via construtor para desacoplamento e isolamento de dependências.
 */
open class FipeNetworkClient(
    customApi: FipeApi? = null
) {
    val api: FipeApi = customApi ?: createRealApi()

    companion object {
        private const val BASE_URL = "https://parallelum.com.br/fipe/api/v1/"

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
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
        }

        private val moshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        private fun createRealApi(): FipeApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(FipeApi::class.java)
        }

        private val defaultInstance: FipeNetworkClient by lazy {
            FipeNetworkClient(createRealApi())
        }

        @Volatile
        private var instance: FipeNetworkClient? = null

        val api: FipeApi
            get() = (instance ?: defaultInstance).api

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setInstance(client: FipeNetworkClient?) {
            synchronized(this) {
                instance = client
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setApi(api: FipeApi?) {
            synchronized(this) {
                instance = api?.let { FipeNetworkClient(it) }
            }
        }
    }
}
