package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.model.LocationPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class PlayServicesLocationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow(LocationHelper.DEFAULT_KUMBAKONAM)
    val currentLocation: StateFlow<LocationPoint> = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(hasPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val _lastUpdateTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastUpdateTimestamp: StateFlow<Long> = _lastUpdateTimestamp.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation ?: return
            _lastUpdateTimestamp.value = System.currentTimeMillis()

            CoroutineScope(Dispatchers.IO).launch {
                val point = resolveLocationPoint(
                    lat = lastLocation.latitude,
                    lng = lastLocation.longitude,
                    accuracy = lastLocation.accuracy
                )
                _currentLocation.value = point
            }
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            _isTracking.value = availability.isLocationAvailable
        }
    }

    fun hasPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun updatePermissionState() {
        _hasLocationPermission.value = hasPermission()
    }

    @SuppressLint("MissingPermission")
    fun startRealtimeTracking(intervalMillis: Long = 5000L) {
        updatePermissionState()
        if (!hasPermission()) {
            _isTracking.value = false
            return
        }

        try {
            // First fetch the last known location for instant response
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        _currentLocation.value = resolveLocationPoint(loc.latitude, loc.longitude, loc.accuracy)
                    }
                }
            }

            // Create high-accuracy real-time location request via Play Services
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
                .setMinUpdateIntervalMillis(2000L)
                .setMaxUpdateDelayMillis(10000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            _isTracking.value = true
        } catch (_: SecurityException) {
            _isTracking.value = false
        }
    }

    fun stopRealtimeTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            _isTracking.value = false
        } catch (_: Exception) {}
    }

    private fun resolveLocationPoint(lat: Double, lng: Double, accuracy: Float): LocationPoint {
        var landmark = "GPS Live (±${accuracy.toInt()}m) • Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}"
        var district = "Thanjavur"
        var state = "Tamil Nadu"

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Async geocoding or fallback
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    landmark = addr.getAddressLine(0) ?: addr.locality ?: landmark
                    district = addr.subAdminArea ?: addr.locality ?: district
                    state = addr.adminArea ?: state
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    landmark = addr.getAddressLine(0) ?: addr.locality ?: landmark
                    district = addr.subAdminArea ?: addr.locality ?: district
                    state = addr.adminArea ?: state
                }
            }
        } catch (_: Exception) {
            // Check if coordinates match near Kumbakonam presets
            val preset = LocationHelper.PRESET_LOCATIONS.minByOrNull {
                LocationHelper.distanceInMeters(lat, lng, it.latitude, it.longitude)
            }
            if (preset != null && LocationHelper.distanceInMeters(lat, lng, preset.latitude, preset.longitude) < 2000) {
                landmark = "${preset.landmark} (±${accuracy.toInt()}m)"
                district = preset.district
                state = preset.state
            }
        }

        return LocationPoint(
            latitude = lat,
            longitude = lng,
            landmark = landmark,
            district = district,
            state = state
        )
    }
}
