package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.network.GasStationResult
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

fun createMarkerIcon(context: Context, isSelected: Boolean): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) {
            android.graphics.Color.parseColor("#E53935") // Red
        } else {
            android.graphics.Color.parseColor("#00897B") // Teal
        }
    }
    
    // Outer circle
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    
    // Inner white circle
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, paint)
    
    // Center dot
    paint.color = if (isSelected) {
        android.graphics.Color.parseColor("#E53935")
    } else {
        android.graphics.Color.parseColor("#00897B")
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 6f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

fun createUserLocationIcon(context: Context): Drawable {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E88E5") // Blue
    }
    
    // Semi-transparent outer circle
    paint.alpha = 60
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Solid inner circle
    paint.alpha = 255
    canvas.drawCircle(size / 2f, size / 2f, size / 3.5f, paint)
    
    // White center dot
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 8f, paint)
    
    return BitmapDrawable(context.resources, bitmap)
}

@Composable
fun NearbyStationsMapView(
    userLat: Double,
    userLon: Double,
    stations: List<GasStationResult>,
    selectedStation: GasStationResult?,
    onStationSelected: (GasStationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(userLat, userLon))
        }
    }

    LaunchedEffect(selectedStation) {
        if (selectedStation != null) {
            mapView.controller.animateTo(GeoPoint(selectedStation.lat, selectedStation.lon))
        } else {
            mapView.controller.animateTo(GeoPoint(userLat, userLon))
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        update = { map ->
            map.overlays.clear()

            // 1. User Location Marker
            val userMarker = Marker(map).apply {
                position = GeoPoint(userLat, userLon)
                title = "Sua Localização"
                icon = createUserLocationIcon(map.context)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            map.overlays.add(userMarker)

            // 2. Gas Station Markers
            stations.forEach { station ->
                val isSelected = selectedStation != null && 
                        selectedStation.lat == station.lat && 
                        selectedStation.lon == station.lon
                
                val marker = Marker(map).apply {
                    position = GeoPoint(station.lat, station.lon)
                    title = station.name
                    snippet = station.brand ?: "Posto de Combustível"
                    icon = createMarkerIcon(map.context, isSelected)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    setOnMarkerClickListener { m, mv ->
                        onStationSelected(station)
                        m.showInfoWindow()
                        true
                    }
                }
                map.overlays.add(marker)
                
                if (isSelected) {
                    marker.showInfoWindow()
                }
            }

            map.invalidate()
        }
    )
}
