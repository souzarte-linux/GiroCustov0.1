package com.example.ui.delegates

import android.content.Context
import com.example.network.GasStationRepository
import com.example.network.GasStationResult
import com.example.network.GasStationSearchResult
import com.example.network.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GasStationSearchCoordinator(
    private val scope: CoroutineScope
) {
    private val _nearbyGasStations = MutableStateFlow<List<GasStationResult>>(emptyList())
    val nearbyGasStations: StateFlow<List<GasStationResult>> = _nearbyGasStations.asStateFlow()

    private val _isSearchingStations = MutableStateFlow(false)
    val isSearchingStations: StateFlow<Boolean> = _isSearchingStations.asStateFlow()

    private val _stationSearchError = MutableStateFlow<String?>(null)
    val stationSearchError: StateFlow<String?> = _stationSearchError.asStateFlow()

    private val _lastSearchLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val lastSearchLocation: StateFlow<Pair<Double, Double>?> = _lastSearchLocation.asStateFlow()

    fun clearStationSearch() {
        _nearbyGasStations.value = emptyList()
        _stationSearchError.value = null
        _lastSearchLocation.value = null
    }

    fun searchNearbyGasStations(context: Context) {
        scope.launch {
            _isSearchingStations.value = true
            _stationSearchError.value = null
            _nearbyGasStations.value = emptyList()
            _lastSearchLocation.value = null

            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
                _lastSearchLocation.value = Pair(location.latitude, location.longitude)
                when (val result = GasStationRepository.findNearbyGasStations(location.latitude, location.longitude)) {
                    is GasStationSearchResult.Success -> {
                        _nearbyGasStations.value = result.stations
                        if (result.stations.isEmpty()) {
                            _stationSearchError.value = "Nenhum posto de combustível encontrado nas proximidades."
                        }
                    }
                    is GasStationSearchResult.Error -> {
                        _stationSearchError.value = result.message
                    }
                }
            } else {
                _stationSearchError.value = "Não foi possível obter a localização. Verifique se o GPS está ativo e a permissão concedida."
            }
            _isSearchingStations.value = false
        }
    }
}
