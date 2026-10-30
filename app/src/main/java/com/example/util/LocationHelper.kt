package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.model.LocationPoint

object LocationHelper {

    // Default authentic landmark: Kumbakonam Central, Tamil Nadu
    val DEFAULT_KUMBAKONAM = LocationPoint(
        latitude = 10.9602,
        longitude = 79.3845,
        landmark = "Kumbakonam Town Hall Road / Bus Stand",
        district = "Thanjavur",
        state = "Tamil Nadu"
    )

    val PRESET_LOCATIONS = listOf(
        DEFAULT_KUMBAKONAM,
        LocationPoint(10.9587, 79.3789, "Mahamaham Tank West, Kumbakonam", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.9664, 79.3882, "Kumbakonam Railway Station Rd", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.9615, 79.3812, "Town High School Road, Kumbakonam", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.7870, 79.1378, "Old Bus Stand, Thanjavur", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.8050, 78.6856, "Chathiram Bus Stand, Tiruchirappalli (Trichy)", "Tiruchirappalli", "Tamil Nadu"),
        LocationPoint(13.0827, 80.2707, "Chennai Central / Poonamallee High Rd", "Chennai", "Tamil Nadu"),
        LocationPoint(11.0168, 76.9558, "Gandhipuram Bus Stand, Coimbatore", "Coimbatore", "Tamil Nadu"),
        LocationPoint(9.9252, 78.1198, "Periyar Bus Stand, Madurai", "Madurai", "Tamil Nadu")
    )

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): LocationPoint {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val gpsLoc: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val netLoc: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val passiveLoc: Location? = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                val best = gpsLoc ?: netLoc ?: passiveLoc
                if (best != null) {
                    return LocationPoint(
                        latitude = best.latitude,
                        longitude = best.longitude,
                        landmark = "GPS Current Coordinates (Accuracy ±${best.accuracy.toInt()}m)",
                        district = "Thanjavur",
                        state = "Tamil Nadu"
                    )
                }
            }
        } catch (_: Exception) {
            // Permission not yet granted or GPS inactive
        }
        return DEFAULT_KUMBAKONAM
    }

    fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0].toInt()
        } catch (_: Exception) {
            // Haversine fallback for unit tests and local JVM runs
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            (r * c).toInt()
        }
    }
}
