package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Geocoder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.LocationPoint
import com.example.util.LocationHelper
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Real Android Foreground Service for uninterrupted background location tracking.
 * Prevents OS throttling during active SOS alerts, Buddy System tracking, and live route sharing.
 */
class SafeguardLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "safeguard_guardian_location_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_TRACKING = "com.example.safeguard.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.safeguard.ACTION_STOP_TRACKING"
        const val ACTION_EMERGENCY_BOOST = "com.example.safeguard.ACTION_EMERGENCY_BOOST"

        const val EXTRA_TRACKING_MODE = "EXTRA_TRACKING_MODE"
        const val MODE_GUARDIAN = "MODE_GUARDIAN"
        const val MODE_SOS = "MODE_SOS"
        const val MODE_BUDDY = "MODE_BUDDY"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _serviceLocation = MutableStateFlow<LocationPoint?>(null)
        val serviceLocation: StateFlow<LocationPoint?> = _serviceLocation.asStateFlow()

        private val _activeMode = MutableStateFlow(MODE_GUARDIAN)
        val activeMode: StateFlow<String> = _activeMode.asStateFlow()

        fun start(context: Context, mode: String = MODE_GUARDIAN) {
            val intent = Intent(context, SafeguardLocationService::class.java).apply {
                action = ACTION_START_TRACKING
                putExtra(EXTRA_TRACKING_MODE, mode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SafeguardLocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                CoroutineScope(Dispatchers.IO).launch {
                    val point = resolveLocationPoint(loc.latitude, loc.longitude, loc.accuracy)
                    _serviceLocation.value = point
                    updateNotification(point)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_TRACKING

        when (action) {
            ACTION_START_TRACKING, ACTION_EMERGENCY_BOOST -> {
                val mode = intent?.getStringExtra(EXTRA_TRACKING_MODE) ?: MODE_GUARDIAN
                _activeMode.value = mode
                startForegroundTracking(mode)
            }
            ACTION_STOP_TRACKING -> {
                stopForegroundTracking()
            }
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking(mode: String) {
        val notification = buildNotification(
            title = if (mode == MODE_SOS) "🚨 EMERGENCY SOS ACTIVE" else "🛡️ SAFEGUARD Guardian Active",
            content = "Live GPS coordinates broadcasted to Buddy Network & Tamil Nadu Safety Grid."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        _isServiceRunning.value = true

        val interval = if (mode == MODE_SOS) 2000L else 4000L
        val minInterval = if (mode == MODE_SOS) 1000L else 2000L

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(minInterval)
            .setMinUpdateDistanceMeters(2f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopForegroundTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        _isServiceRunning.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SAFEGUARD Guardian Live Location",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous background location tracking for Emergency SOS and Buddy Safety Network"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SafeguardLocationService::class.java).apply {
            action = ACTION_STOP_TRACKING
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.safeguard_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Tracking", pendingStop)
            .build()
    }

    private fun updateNotification(location: LocationPoint) {
        val mode = _activeMode.value
        val title = if (mode == MODE_SOS) "🚨 SOS ACTIVE • BROADCASTING GPS" else "🛡️ SAFEGUARD • Real-time Protection"
        val content = "📍 ${location.landmark} (Lat: ${"%.4f".format(location.latitude)}, Lng: ${"%.4f".format(location.longitude)})"

        notificationManager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun resolveLocationPoint(lat: Double, lng: Double, accuracy: Float): LocationPoint {
        var landmark = "GPS Live (±${accuracy.toInt()}m) • Lat: ${"%.4f".format(lat)}, Lng: ${"%.4f".format(lng)}"
        var district = "Thanjavur"
        var state = "Tamil Nadu"

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    override fun onDestroy() {
        stopForegroundTracking()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
