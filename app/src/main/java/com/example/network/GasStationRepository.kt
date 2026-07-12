package com.example.network

import kotlin.math.*

object GasStationRepository {
    val OVERPASS_ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass.openstreetmap.ru/api/interpreter"
    )

    fun buildOverpassQuery(lat: Double, lon: Double, radiusMeters: Int): String {
        return """[out:json][timeout:25];nwr["amenity"="fuel"](around:$radiusMeters,$lat,$lon);out center;"""
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun extractAddress(tags: Map<String, String>?): String? {
        if (tags == null) return null
        val street = tags["addr:street"]
        val number = tags["addr:housenumber"]
        val suburb = tags["addr:suburb"]
        val city = tags["addr:city"]

        return when {
            !street.isNullOrBlank() && !number.isNullOrBlank() -> "$street $number"
            !street.isNullOrBlank() -> street
            !suburb.isNullOrBlank() -> suburb
            !city.isNullOrBlank() -> city
            else -> null
        }
    }

    suspend fun findNearbyGasStations(
        lat: Double,
        lon: Double,
        radiusMeters: Int = 3000
    ): GasStationSearchResult {
        val query = buildOverpassQuery(lat, lon, radiusMeters)
        var lastException: Throwable? = null

        for (endpoint in OVERPASS_ENDPOINTS) {
            try {
                val response = overpassApiService.getNearbyStations(endpoint, query)
                val elements = response.elements
                val results = elements.mapNotNull { element ->
                    val name = element.tags?.get("name") ?: return@mapNotNull null
                    val brand = element.tags.get("brand") ?: element.tags.get("operator")
                    val elementLat = element.lat ?: element.center?.lat ?: return@mapNotNull null
                    val elementLon = element.lon ?: element.center?.lon ?: return@mapNotNull null
                    val distance = calculateDistance(lat, lon, elementLat, elementLon)
                    val address = extractAddress(element.tags)
                    GasStationResult(
                        name = name,
                        brand = brand,
                        lat = elementLat,
                        lon = elementLon,
                        distanceMeters = distance,
                        address = address
                    )
                }.sortedBy { it.distanceMeters }

                return GasStationSearchResult.Success(results)
            } catch (e: Exception) {
                lastException = e
                // Fall back to the next endpoint
            }
        }

        val errMsg = lastException?.localizedMessage ?: "Erro de rede"
        return GasStationSearchResult.Error("Não foi possível conectar: $errMsg")
    }
}
