package com.example.ui.screens.fuel

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.network.GasStationResult
import com.example.ui.components.NearbyStationsMapView
import java.util.Locale

@Composable
fun FuelStationSearchResultsCard(
    nearbyStations: List<GasStationResult>,
    isSearchingStations: Boolean,
    stationSearchError: String?,
    lastSearchLocation: Pair<Double, Double>?,
    selectedStation: GasStationResult?,
    onStationSelected: (GasStationResult, String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var showMapView by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = nearbyStations.isNotEmpty() || stationSearchError != null || isSearchingStations,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.fuel_gps_nearby_stations_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp).testTag("close_nearby_stations_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close_station_search),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSearchingStations) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.fuel_gps_searching_stations), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (stationSearchError != null) {
                    Text(
                        text = stationSearchError,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (nearbyStations.isNotEmpty()) {
                    // Toggle Control for List vs Map
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = { showMapView = false },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (!showMapView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (!showMapView) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.fuel_gps_view_list), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        ElevatedButton(
                            onClick = { showMapView = true },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (showMapView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                contentColor = if (showMapView) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.fuel_gps_view_map), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showMapView && lastSearchLocation != null) {
                        NearbyStationsMapView(
                            userLat = lastSearchLocation.first,
                            userLon = lastSearchLocation.second,
                            stations = nearbyStations,
                            selectedStation = selectedStation,
                            onStationSelected = { station ->
                                val displayName = if (!station.brand.isNullOrBlank()) {
                                    context.getString(R.string.fuel_station_with_brand_format, station.name, station.brand)
                                } else {
                                    station.name
                                }
                                onStationSelected(station, displayName)
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            nearbyStations.take(5).forEach { station ->
                                val distanceText = if (station.distanceMeters >= 1000) {
                                    stringResource(R.string.fuel_format_distance_km, station.distanceMeters / 1000.0)
                                } else {
                                    stringResource(R.string.fuel_format_distance_meters, station.distanceMeters.toInt())
                                }
                                val displayName = if (!station.brand.isNullOrBlank()) {
                                    stringResource(R.string.fuel_station_with_brand_format, station.name, station.brand)
                                } else {
                                    station.name
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onStationSelected(station, displayName)
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalGasStation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = station.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!station.brand.isNullOrBlank()) {
                                                Text(
                                                    text = station.brand,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = station.address ?: stringResource(R.string.fuel_station_no_address),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = distanceText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
    }
}
