package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

/**
 * Checks whether ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted.
 */
fun isLocationPermissionGranted(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

/**
 * Checks whether high-precision ACCESS_FINE_LOCATION specifically is granted.
 */
fun isFineLocationGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * State holder for runtime location permission handling in Jetpack Compose.
 */
@Composable
fun rememberLocationPermissionLauncher(
    onPermissionResult: (isGranted: Boolean, isFine: Boolean) -> Unit = { _, _ -> }
): () -> Unit {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val anyGranted = fineGranted || coarseGranted

        onPermissionResult(anyGranted, fineGranted)
    }

    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

/**
 * Contextual Material 3 Banner card alerting the user when GPS permissions are missing.
 * Provides 1-tap grant trigger and direct settings deep-link if denied.
 */
@Composable
fun LocationPermissionCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "High-Accuracy GPS Required",
    description: String = "Grant ACCESS_FINE_LOCATION for sub-10m responder dispatch, live buddy tracking, and verified danger zones in Tamil Nadu."
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == DarkBackground

    AnimatedVisibility(
        visible = !hasPermission,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("location_permission_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkSurfaceVariant else WarningAmberLight
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, WarningAmber)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) WarningAmber.copy(alpha = 0.25f) else WarningAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "Location Disabled",
                        tint = if (isDark) WarningAmber else Color(0xFFB45309),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextPrimary else SafeguardNavy
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) DarkTextSecondary else SlateTextDark,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) SafetyTeal else SafeguardNavy,
                                contentColor = if (isDark) SafeguardNavyDark else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("button_grant_location_permission")
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Allow GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) DarkBorder else SlateBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                "App Settings",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) SafetyTeal else SafeguardNavy
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Educational modal explaining the necessity of GPS precision before triggering SOS or Buddy tracking.
 */
@Composable
fun LocationPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == DarkBackground

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.GpsFixed,
                contentDescription = null,
                tint = SafetyTeal,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                "Enable Exact Geolocation",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = if (isDark) DarkTextPrimary else SafeguardNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "SAFEGUARD protects students, women, and citizens across Kumbakonam and Tamil Nadu.",
                    fontSize = 13.sp,
                    color = if (isDark) DarkTextPrimary else SlateTextDark
                )
                Text(
                    "• Emergency SOS: Sub-10m precision dispatched to Police 112 & Fire Responders.\n" +
                    "• Buddy System: Real-time breadcrumb sharing with trusted family & friends.\n" +
                    "• Danger Zones: Real-time proximity warnings when approaching unlit or flooded corridors.",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else SlateTextSecondary,
                    lineHeight = 16.sp
                )
                Text(
                    "Select 'While using the app' and 'Precise' on the following prompt.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyTeal
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                modifier = Modifier.testTag("dialog_confirm_location_permission")
            ) {
                Text("Continue to Permission", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = if (isDark) DarkTextSecondary else SlateTextMuted)
            }
        }
    )
}
