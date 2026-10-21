package com.example.network.viacep

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ViaCepNetworkClient {
    private const val BASE_URL = "https://viacep.com.br/ws/"

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
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

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val realApi: ViaCepApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ViaCepApi::class.java)

    @Volatile
    var testApi: ViaCepApi? = null

    val api: ViaCepApi
        get() = testApi ?: realApi
}
