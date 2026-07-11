package com.example.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface OverpassApiService {
    @GET
    suspend fun getNearbyStations(
        @Url url: String,
        @Query("data") data: String
    ): OverpassResponse
}

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", "GiroCusto/1.0")
            .build()
        chain.proceed(request)
    }
    .build()

private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

private val retrofit = Retrofit.Builder()
    .baseUrl("https://overpass-api.de/") // Placeholder baseUrl overridden by dynamic @Url
    .client(okHttpClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()

val overpassApiService: OverpassApiService = retrofit.create(OverpassApiService::class.java)
