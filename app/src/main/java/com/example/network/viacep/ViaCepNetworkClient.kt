package com.example.network.viacep

import androidx.annotation.VisibleForTesting
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP Retrofit/OkHttp para consumo da API do ViaCEP.
 * Suporta injeção de [api] via construtor para desacoplamento e isolamento de dependências.
 */
open class ViaCepNetworkClient(
    customApi: ViaCepApi? = null
) {
    val api: ViaCepApi = customApi ?: createRealApi()

    companion object {
        private const val BASE_URL = "https://viacep.com.br/ws/"

        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
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

        private fun createRealApi(): ViaCepApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ViaCepApi::class.java)
        }

        private val defaultInstance: ViaCepNetworkClient by lazy {
            ViaCepNetworkClient(createRealApi())
        }

        @Volatile
        private var instance: ViaCepNetworkClient? = null

        val api: ViaCepApi
            get() = (instance ?: defaultInstance).api

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setInstance(client: ViaCepNetworkClient?) {
            synchronized(this) {
                instance = client
            }
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        fun setApi(api: ViaCepApi?) {
            synchronized(this) {
                instance = api?.let { ViaCepNetworkClient(it) }
            }
        }
    }
}
