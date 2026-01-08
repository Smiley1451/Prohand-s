package com.anand.prohands.ui.components

import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

@Composable
fun MapPickerComponent(initialLocation: Location?, onLocationSelected: (LatLng) -> Unit) {
    val cameraPositionState = rememberCameraPositionState()
    val markerState = rememberMarkerState()
    val coroutineScope = rememberCoroutineScope()

    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(initialLocation) {
        if (initialLocation != null && !hasInitialized) {
            val initialLatLng = LatLng(initialLocation.latitude, initialLocation.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
            markerState.position = initialLatLng
            onLocationSelected(initialLatLng)
            hasInitialized = true
        }
    }

    // Observe marker position changes (including after drag) and report them
    LaunchedEffect(markerState.position) {
        onLocationSelected(markerState.position)
    }

    if (!hasInitialized && initialLocation == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Waiting for location...")
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    markerState.position = latLng
                    // The LaunchedEffect above will handle the callback
                }
            ) {
                Marker(
                    state = markerState,
                    draggable = true
                )
            }
            Button(
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                onClick = {
                    if (initialLocation != null) {
                        val currentLatLng = LatLng(initialLocation.latitude, initialLocation.longitude)
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        }
                        markerState.position = currentLatLng
                        // The LaunchedEffect above will handle the callback
                    }
                }
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
            }
        }
    }
}
