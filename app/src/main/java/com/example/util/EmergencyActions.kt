package com.example.util

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.model.LocationPoint

object EmergencyActions {

    fun dialEmergency(context: Context, number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareEmergencySos(
        context: Context,
        incidentId: String,
        userName: String,
        userPhone: String,
        location: LocationPoint,
        emergencyType: String
    ) {
        val message = """
            🚨 SAFEGUARD EMERGENCY SOS ALERT!
            Ref: $incidentId
            Name: $userName ($userPhone)
            Type: $emergencyType
            Landmark: ${location.landmark}, ${location.district}, ${location.state}
            📍 Live Google Maps Location:
            ${location.googleMapsUrl}
            
            ⚠️ Please alert local responders or dial 112 immediately!
            — Sent via SAFEGUARD Tamil Nadu Safety Network
        """.trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            putExtra(Intent.EXTRA_SUBJECT, "EMERGENCY ALERT: $userName needs immediate assistance!")
        }
        val chooser = Intent.createChooser(sendIntent, "Broadcast Emergency SOS to Contacts / WhatsApp")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)
    }

    fun openInGoogleMaps(context: Context, location: LocationPoint) {
        try {
            val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${Uri.encode(location.landmark)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(location.googleMapsUrl))
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
        }
    }

    fun openRouteInGoogleMaps(context: Context, origin: LocationPoint, destination: LocationPoint) {
        try {
            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&travelmode=driving")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}")
            val browserIntent = Intent(Intent.ACTION_VIEW, fallbackUri)
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
        }
    }

    fun getGoogleMapsRouteUrl(origin: LocationPoint, destination: LocationPoint): String {
        return "https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&travelmode=driving"
    }

    fun triggerHapticPulse(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
            }
        } catch (_: Exception) {}
    }

    private var isTorchOn = false

    fun toggleFlashlightStrobe(context: Context, enable: Boolean): Boolean {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            return true
        } catch (e: Exception) {
            return false
        }
    }
}
