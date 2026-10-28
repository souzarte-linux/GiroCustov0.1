package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OverpassCenter(
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val id: Long = 0L,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

data class GasStationResult(
    val name: String,
    val brand: String?,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double,
    val address: String? = null
)

sealed class GasStationSearchResult {
    data class Success(val stations: List<GasStationResult>) : GasStationSearchResult()
    data class Error(val message: String) : GasStationSearchResult()
}
