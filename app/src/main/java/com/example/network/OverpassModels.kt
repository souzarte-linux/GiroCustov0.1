package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OverpassResponse(
    val elements: List<OverpassElement>
)

@JsonClass(generateAdapter = true)
data class OverpassElement(
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val tags: Map<String, String>?
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
