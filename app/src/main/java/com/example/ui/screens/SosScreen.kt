package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationPoint
import com.example.model.SosIncident
import com.example.model.SosStatus
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SosScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeSos by viewModel.activeSos.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedEmergencyType by remember { mutableStateOf("Physical Danger / Imminent Attack") }
    var showLocationSelector by remember { mutableStateOf(false) }

    // Google Play Services Location Permission Request Launcher
    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(fineGranted || coarseGranted)
    }

    // Pulsing animation for the SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Location & Quick Siren/Flash Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showLocationSelector = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SafetyTealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS Location",
                            tint = SafetyTealDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CURRENT LOCATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafetyTealDark,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change", tint = SlateTextMuted, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = uiState.currentLocation.landmark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SlateTextDark,
                            maxLines = 1
                        )
                    }
                }

                // Quick Strobe & Siren toggles
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleStrobeFlashlight() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (uiState.isStrobeActive) WarningAmberLight else SlateBorder.copy(alpha = 0.4f),
                            contentColor = if (uiState.isStrobeActive) WarningAmber else SlateTextDark
                        ),
                        modifier = Modifier.size(40.dp).testTag("button_strobe_flashlight")
                    ) {
                        Icon(Icons.Default.FlashlightOn, contentDescription = "Flashlight Strobe")
                    }

                    FilledTonalIconButton(
                        onClick = { viewModel.toggleSirenSound() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (uiState.isSirenPlaying) EmergencyRedLight else SlateBorder.copy(alpha = 0.4f),
                            contentColor = if (uiState.isSirenPlaying) EmergencyRed else SlateTextDark
                        ),
                        modifier = Modifier.size(40.dp).testTag("button_siren_alarm")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Siren Alert")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Google Play Services Runtime Permission & Tracking Card
        if (!uiState.hasLocationPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarningAmberLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationSearching,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Explicit GPS Permission Required",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SafeguardNavy
                            )
                            Text(
                                text = "Activate Google Play Services to track real-time coordinates during emergencies.",
                                fontSize = 11.sp,
                                color = SlateTextDark,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("button_grant_gps_permission")
                    ) {
                        Text("Grant GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // Live Play Services Tracking Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isRealtimeTrackingActive) SafeGreen else SlateBorder)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (uiState.isRealtimeTrackingActive) "GOOGLE PLAY SERVICES: REAL-TIME TRACKING ACTIVE" else "GPS STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isRealtimeTrackingActive) SafeGreenDark else SlateTextMuted
                            )
                            Text(
                                text = "Lat: ${"%.4f".format(uiState.currentLocation.latitude)}, Lng: ${"%.4f".format(uiState.currentLocation.longitude)} • ${uiState.currentLocation.district}",
                                fontSize = 11.sp,
                                color = SlateTextDark
                            )
                        }
                    }

                    TextButton(
                        onClick = { viewModel.toggleRealtimeTracking() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.isRealtimeTrackingActive) "Pause GPS" else "Resume GPS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyTealDark
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // If an SOS is currently active, display the Real-Time Truth Tracker!
        if (activeSos != null) {
            ActiveSosTrackerCard(
                incident = activeSos!!,
                onBroadcast = {
                    EmergencyActions.shareEmergencySos(
                        context = context,
                        incidentId = activeSos!!.id,
                        userName = currentUser.name,
                        userPhone = currentUser.phone,
                        location = activeSos!!.location,
                        emergencyType = activeSos!!.emergencyType
                    )
                },
                onOpenMap = {
                    EmergencyActions.openInGoogleMaps(context, activeSos!!.location)
                },
                onDismiss = {
                    viewModel.clearActiveSos()
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Emergency Type Selector
        Text(
            text = "SELECT EMERGENCY TYPE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EmergencyTypeChip(
                label = "Physical Danger",
                icon = Icons.Default.Warning,
                isSelected = selectedEmergencyType.startsWith("Physical"),
                onClick = { selectedEmergencyType = "Physical Danger / Imminent Attack" },
                modifier = Modifier.weight(1f)
            )
            EmergencyTypeChip(
                label = "Harassment",
                icon = Icons.Default.Security,
                isSelected = selectedEmergencyType.startsWith("Harassment"),
                onClick = { selectedEmergencyType = "Harassment / Stalking" },
                modifier = Modifier.weight(1f)
            )
            EmergencyTypeChip(
                label = "Medical 108",
                icon = Icons.Default.MedicalServices,
                isSelected = selectedEmergencyType.startsWith("Medical"),
                onClick = { selectedEmergencyType = "Medical Emergency" },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large SOS Trigger Button
        if (uiState.isSosCountdownActive) {
            // Countdown active view with 3s cancel window
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyRed),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EMERGENCY ALERT TRIGGERING IN",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.sosCountdownSeconds}",
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Notifying Tamil Nadu Police, Responders & Emergency Contacts with live coordinates...",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.cancelSosCountdown() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.weight(1f).testTag("button_cancel_sos")
                        ) {
                            Text("Cancel Alert", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.triggerInstantSos(selectedEmergencyType) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EmergencyRed),
                            modifier = Modifier.weight(1f).testTag("button_instant_send_sos")
                        ) {
                            Text("Send Now!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Main Big Pulsing SOS Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Outer Pulse Ring
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(EmergencyRed.copy(alpha = 0.15f))
                )
                // Middle Ring
                Box(
                    modifier = Modifier
                        .size(195.dp)
                        .clip(CircleShape)
                        .background(EmergencyRed.copy(alpha = 0.35f))
                )
                // Core Button
                Button(
                    onClick = { viewModel.startSosCountdown(selectedEmergencyType) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmergencyRed
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp),
                    modifier = Modifier
                        .size(165.dp)
                        .testTag("button_primary_sos")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Alert Icon",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "SOS",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ONE TAP ALERT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Call India 112 & Helplines (Crucial for emergencies!)
        Text(
            text = "OFFICIAL DIRECT EMERGENCY DIALERS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Primary 112 Banner
        Button(
            onClick = { EmergencyActions.dialEmergency(context, "112") },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("button_dial_112")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CALL 112 (INDIA EMERGENCY HELPLINE)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary Helplines Row (1091 Women, 1098 Childline, 108 Ambulance)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HelplineButton(
                title = "Women 1091",
                number = "1091",
                color = SafetyTealDark,
                onClick = { EmergencyActions.dialEmergency(context, "1091") },
                modifier = Modifier.weight(1f)
            )
            HelplineButton(
                title = "Childline 1098",
                number = "1098",
                color = PrimaryBlue,
                onClick = { EmergencyActions.dialEmergency(context, "1098") },
                modifier = Modifier.weight(1f)
            )
            HelplineButton(
                title = "Ambulance 108",
                number = "108",
                color = EmergencyRed,
                onClick = { EmergencyActions.dialEmergency(context, "108") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // WhatsApp / SMS Emergency Broadcast Quick Action
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SafeGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = SafeGreenDark)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WhatsApp & SMS Emergency Broadcast",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SafeguardNavy
                        )
                        Text(
                            text = "Instantly sends your live GPS coordinates & message to contacts",
                            fontSize = 11.sp,
                            color = SlateTextMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        EmergencyActions.shareEmergencySos(
                            context = context,
                            incidentId = activeSos?.id ?: "SG-TN-ALERT",
                            userName = currentUser.name,
                            userPhone = currentUser.phone,
                            location = uiState.currentLocation,
                            emergencyType = selectedEmergencyType
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                    modifier = Modifier.fillMaxWidth().testTag("button_broadcast_whatsapp")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Broadcast Location to WhatsApp / Contacts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Location selector modal for picking authentic Tamil Nadu locations
    if (showLocationSelector) {
        AlertDialog(
            onDismissRequest = { showLocationSelector = false },
            title = { Text("Select Location / Search", fontWeight = FontWeight.Bold, color = SafeguardNavy) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Choose your location or use device GPS. Kumbakonam is the launch community.",
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.refreshLocation()
                            showLocationSelector = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detect Current Device GPS")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Preset Tamil Nadu Landmarks:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LocationHelper.PRESET_LOCATIONS.take(6).forEach { loc ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.selectPresetLocation(loc)
                                        showLocationSelector = false
                                    },
                                color = if (uiState.currentLocation.landmark == loc.landmark) SafetyTealLight else SlateBorder.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = SafetyTealDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(loc.landmark, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SlateTextDark)
                                        Text("${loc.district}, ${loc.state}", fontSize = 10.sp, color = SlateTextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationSelector = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmergencyTypeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) EmergencyRed else SlateBorder,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmergencyRedLight else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) EmergencyRed else SlateTextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) EmergencyRedDark else SlateTextDark,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HelplineButton(
    title: String,
    number: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Tap to call", fontSize = 9.sp, color = SlateTextMuted)
        }
    }
}

@Composable
private fun ActiveSosTrackerCard(
    incident: SosIncident,
    onBroadcast: () -> Unit,
    onOpenMap: () -> Unit,
    onDismiss: () -> Unit
) {
    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(incident.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(2.dp, EmergencyRed),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACTIVE SOS INCIDENT",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = EmergencyRed,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = incident.id,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SafeguardNavy
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details
            Text(
                text = "${incident.emergencyType} • $formattedTime",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateTextDark
            )
            Text(
                text = "📍 ${incident.location.landmark}, ${incident.location.district}",
                fontSize = 12.sp,
                color = SlateTextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = SlateBorder)
            Spacer(modifier = Modifier.height(12.dp))

            // Truthful Progression Vertical Stepper
            Text(
                text = "TRUTHFUL INCIDENT STATUS PROGRESSION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextMuted,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            TruthfulStatusStep(
                title = "1. Created",
                subtitle = "Logged on device at $formattedTime",
                isCompleted = true,
                isActive = incident.status == SosStatus.CREATED
            )
            TruthfulStatusStep(
                title = "2. Saved Locally",
                subtitle = "Persisted in secure encrypted storage",
                isCompleted = incident.status.ordinal >= SosStatus.SAVED_ON_DEVICE.ordinal,
                isActive = incident.status == SosStatus.SAVED_ON_DEVICE
            )
            TruthfulStatusStep(
                title = "3. Sent to Server",
                subtitle = "Transmitted with GPS payload to SAFEGUARD cloud",
                isCompleted = incident.status.ordinal >= SosStatus.SENT_TO_SERVER.ordinal,
                isActive = incident.status == SosStatus.SENT_TO_SERVER
            )
            TruthfulStatusStep(
                title = "4. Notification Queued",
                subtitle = "Dispatched to regional emergency queue & SMS gateway",
                isCompleted = incident.status.ordinal >= SosStatus.QUEUED.ordinal,
                isActive = incident.status == SosStatus.QUEUED
            )
            TruthfulStatusStep(
                title = "5. Acknowledged by Responder",
                subtitle = if (incident.acknowledgedBy != null)
                    "${incident.acknowledgedBy}\nNote: ${incident.responderNotes ?: "Dispatched"}"
                else "Awaiting official response unit assignment...",
                isCompleted = incident.status.ordinal >= SosStatus.ACKNOWLEDGED.ordinal,
                isActive = incident.status == SosStatus.ACKNOWLEDGED,
                isFinal = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenMap,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Google Maps", fontSize = 11.sp)
                }
                Button(
                    onClick = onBroadcast,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Broadcast", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Dismiss / Close Alert View", color = SlateTextMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun TruthfulStatusStep(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean,
    isFinal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> EmergencyRed
                            isCompleted -> SafeGreenDark
                            else -> SlateBorder
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            if (!isFinal) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(20.dp)
                        .background(if (isCompleted) SafeGreenDark else SlateBorder)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Medium,
                color = if (isActive) EmergencyRedDark else if (isCompleted) SlateTextDark else SlateTextMuted
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = SlateTextMuted,
                lineHeight = 12.sp
            )
        }
    }
}
