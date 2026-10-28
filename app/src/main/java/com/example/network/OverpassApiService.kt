package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface OverpassApiService {
    @FormUrlEncoded
    @POST
    suspend fun getNearbyStations(
        @Url url: String,
        @Field("data") data: String
    ): OverpassResponse
}

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", "GiroCustoApp/1.0 (https://github.com/girocusto; contato@girocusto.app)")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }
    .build()

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .baseUrl("https://overpass.openstreetmap.fr/") // Endpoint de referência padrão
    .client(okHttpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

val overpassApiService: OverpassApiService = retrofit.create(OverpassApiService::class.java)
