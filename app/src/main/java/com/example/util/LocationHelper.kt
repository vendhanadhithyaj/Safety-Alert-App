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
        LocationPoint(10.9520, 79.3560, "Swamimalai Temple Road, Kumbakonam", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.9610, 79.3990, "Cauvery North Bank Bridge, Kumbakonam", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.7870, 79.1378, "Old Bus Stand, Thanjavur", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.7828, 79.1318, "Brihadeeswarar Big Temple, Thanjavur", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.7580, 79.1050, "Thanjavur Govt Medical College Hospital", "Thanjavur", "Tamil Nadu"),
        LocationPoint(10.8050, 78.6856, "Chathiram Bus Stand, Tiruchirappalli (Trichy)", "Tiruchirappalli", "Tamil Nadu"),
        LocationPoint(10.7930, 78.6940, "Central Bus Stand & Junction, Trichy", "Tiruchirappalli", "Tamil Nadu"),
        LocationPoint(10.8280, 78.6960, "Rockfort Ucchi Pillayar Temple, Trichy", "Tiruchirappalli", "Tamil Nadu"),
        LocationPoint(13.0827, 80.2707, "Chennai Central / Poonamallee High Rd", "Chennai", "Tamil Nadu"),
        LocationPoint(13.0500, 80.2824, "Marina Beach Service Road, Chennai", "Chennai", "Tamil Nadu"),
        LocationPoint(13.0400, 80.2337, "T. Nagar Ranganathan Street, Chennai", "Chennai", "Tamil Nadu"),
        LocationPoint(13.0674, 80.1945, "Koyambedu CMBT Bus Terminus, Chennai", "Chennai", "Tamil Nadu"),
        LocationPoint(12.9815, 80.2180, "Guindy Industrial Estate & Metro, Chennai", "Chennai", "Tamil Nadu"),
        LocationPoint(12.9941, 80.1709, "Chennai International Airport (MAA)", "Chennai", "Tamil Nadu"),
        LocationPoint(11.0168, 76.9558, "Gandhipuram Central Bus Stand, Coimbatore", "Coimbatore", "Tamil Nadu"),
        LocationPoint(11.0080, 76.9400, "RS Puram DB Road, Coimbatore", "Coimbatore", "Tamil Nadu"),
        LocationPoint(10.9950, 76.9630, "Coimbatore Junction Railway Station", "Coimbatore", "Tamil Nadu"),
        LocationPoint(9.9252, 78.1198, "Periyar Bus Stand, Madurai", "Madurai", "Tamil Nadu"),
        LocationPoint(9.9195, 78.1193, "Meenakshi Amman Temple North Tower, Madurai", "Madurai", "Tamil Nadu"),
        LocationPoint(9.9420, 78.1530, "Mattuthavani Integrated Bus Terminal, Madurai", "Madurai", "Tamil Nadu"),
        LocationPoint(11.6643, 78.1460, "Salem New Bus Stand, Salem", "Salem", "Tamil Nadu"),
        LocationPoint(8.7274, 77.7275, "Tirunelveli Junction & Vannarpettai", "Tirunelveli", "Tamil Nadu"),
        LocationPoint(12.9165, 79.1325, "CMC Hospital & Golden Temple Road, Vellore", "Vellore", "Tamil Nadu"),
        LocationPoint(11.3410, 77.7172, "Erode Central Bus Terminal", "Erode", "Tamil Nadu"),
        LocationPoint(12.2253, 79.0747, "Annamalaiyar Temple Girivalam Path, Tiruvannamalai", "Tiruvannamalai", "Tamil Nadu"),
        LocationPoint(12.8342, 79.7036, "Ekambareswarar Sannathi St, Kanchipuram", "Kanchipuram", "Tamil Nadu"),
        LocationPoint(12.9716, 77.5946, "Majestic Bus Station & Metro, Bengaluru", "Bengaluru", "Karnataka"),
        LocationPoint(12.8452, 77.6602, "Electronic City Phase 1, Bengaluru", "Bengaluru", "Karnataka"),
        LocationPoint(17.3850, 78.4867, "Charminar & Old City Safety Post, Hyderabad", "Hyderabad", "Telangana"),
        LocationPoint(17.4474, 78.3762, "Hitech City Cyber Towers, Hyderabad", "Hyderabad", "Telangana"),
        LocationPoint(9.9816, 76.2999, "MG Road & Marine Drive, Kochi", "Ernakulam", "Kerala"),
        LocationPoint(8.5241, 76.9366, "Padmanabhaswamy Temple East Gate, Thiruvananthapuram", "Thiruvananthapuram", "Kerala"),
        LocationPoint(18.9400, 72.8354, "Chhatrapati Shivaji Maharaj Terminus (CSMT), Mumbai", "Mumbai", "Maharashtra"),
        LocationPoint(19.0600, 72.8656, "Bandra Kurla Complex (BKC), Mumbai", "Mumbai", "Maharashtra"),
        LocationPoint(28.6139, 77.2090, "Connaught Place & India Gate, New Delhi", "Central Delhi", "Delhi"),
        LocationPoint(28.5672, 77.2100, "AIIMS Trauma Center & Safdarjung, New Delhi", "South Delhi", "Delhi"),
        LocationPoint(22.5726, 88.3639, "Howrah Bridge & BBD Bagh, Kolkata", "Kolkata", "West Bengal")
    )

    fun searchLocations(query: String): List<LocationPoint> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return PRESET_LOCATIONS.take(6)

        // Coordinate parser if user inputs "10.96, 79.38"
        if (clean.contains(",")) {
            val parts = clean.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDoubleOrNull()
                val lng = parts[1].trim().toDoubleOrNull()
                if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                    return listOf(
                        LocationPoint(
                            latitude = lat,
                            longitude = lng,
                            landmark = "Pinned Coordinates (${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lng)})",
                            district = "Custom Pin",
                            state = "India"
                        )
                    )
                }
            }
        }

        return PRESET_LOCATIONS.filter {
            it.landmark.lowercase().contains(clean) ||
            it.district.lowercase().contains(clean) ||
            it.state.lowercase().contains(clean)
        }
    }

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
