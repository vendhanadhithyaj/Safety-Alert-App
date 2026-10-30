package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DangerZone
import com.example.model.LocationPoint
import com.example.model.ReportCategory
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.*

enum class MapFilter {
    ALL,
    DANGER_ZONES,
    POLICE_BOOTHS,
    SHELTERS_HOSPITALS,
    ACTIVE_INCIDENTS
}

enum class MapDisplayMode {
    GOOGLE_MAPS,
    RADAR_GRID
}

@Composable
fun MapScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dangerZones by viewModel.dangerZones.collectAsState()
    val activeIncidents by viewModel.allIncidents.collectAsState()
    val activeLiveShares by viewModel.activeLiveShares.collectAsState()

    var mapMode by remember { mutableStateOf(MapDisplayMode.GOOGLE_MAPS) }
    var selectedFilter by remember { mutableStateOf(MapFilter.ALL) }
    var selectedZone by remember { mutableStateOf<DangerZone?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }

    val requestLocationPermission = com.example.ui.components.rememberLocationPermissionLauncher { granted, _ ->
        viewModel.onLocationPermissionResult(granted)
    }

    val activeShare = activeLiveShares.firstOrNull { it.isActive && it.expiresAtEpoch > System.currentTimeMillis() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight)
    ) {
        // Location Permission Banner if not granted
        com.example.ui.components.LocationPermissionCard(
            hasPermission = uiState.hasLocationPermission,
            onRequestPermission = requestLocationPermission,
            title = "Enable GPS for Exact Map Navigation",
            description = "High-accuracy geolocation is needed to render your live positioning beacon and compute distances to danger zones in Kumbakonam."
        )

        // Active Live Location Sharing Alert Banner (if currently sharing)
        AnimatedVisibility(visible = activeShare != null) {
            if (activeShare != null) {
                val remainingMinutes = ((activeShare.expiresAtEpoch - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SafetyTealDark),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
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
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SHARING LIVE GPS: ${activeShare.contactName}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Active for next $remainingMinutes mins • Consent-based session",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.stopLiveShare(activeShare.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top Filter Chips & View Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == MapFilter.ALL,
                        onClick = { selectedFilter = MapFilter.ALL },
                        label = { Text("All (${dangerZones.filter { it.isActive }.size})", fontSize = 11.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == MapFilter.DANGER_ZONES,
                        onClick = { selectedFilter = MapFilter.DANGER_ZONES },
                        label = { Text("Danger Zones", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(14.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == MapFilter.POLICE_BOOTHS,
                        onClick = { selectedFilter = MapFilter.POLICE_BOOTHS },
                        label = { Text("Police", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.LocalPolice, contentDescription = null, tint = SafeguardNavy, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Switch between Google Maps and Radar Grid
            FilledTonalButton(
                onClick = {
                    mapMode = if (mapMode == MapDisplayMode.GOOGLE_MAPS) MapDisplayMode.RADAR_GRID else MapDisplayMode.GOOGLE_MAPS
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = if (mapMode == MapDisplayMode.GOOGLE_MAPS) Icons.Default.Radar else Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (mapMode == MapDisplayMode.GOOGLE_MAPS) "Radar" else "Google Maps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Map View Area: Either Google Maps Platform or Radar Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.05f)
                .padding(horizontal = 12.dp)
        ) {
            if (mapMode == MapDisplayMode.GOOGLE_MAPS) {
                com.example.ui.components.GoogleMapsPlatformView(
                    userLocation = uiState.currentLocation,
                    dangerZones = dangerZones,
                    activeIncidents = activeIncidents,
                    onZoneSelected = { selectedZone = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Interactive Radar / Safety Canvas Map View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SafeguardNavy)
                        .testTag("interactive_safety_map")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val maxRadius = (size.width.coerceAtMost(size.height) / 2) * 0.85f

                        drawCircle(color = SafetyTeal.copy(alpha = 0.15f), radius = maxRadius * 0.33f, center = center, style = Stroke(width = 1.dp.toPx()))
                        drawCircle(color = SafetyTeal.copy(alpha = 0.2f), radius = maxRadius * 0.66f, center = center, style = Stroke(width = 1.dp.toPx()))
                        drawCircle(color = SafetyTeal.copy(alpha = 0.25f), radius = maxRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))

                        drawLine(
                            color = SafetyTeal.copy(alpha = 0.2f),
                            start = Offset(center.x, 0f),
                            end = Offset(center.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = SafetyTeal.copy(alpha = 0.2f),
                            start = Offset(0f, center.y),
                            end = Offset(size.width, center.y),
                            strokeWidth = 1.dp.toPx()
                        )

                        drawCircle(color = EmergencyRed.copy(alpha = 0.25f), radius = 35.dp.toPx(), center = Offset(center.x + 80.dp.toPx(), center.y - 45.dp.toPx()))
                        drawCircle(color = WarningAmber.copy(alpha = 0.22f), radius = 28.dp.toPx(), center = Offset(center.x - 70.dp.toPx(), center.y + 60.dp.toPx()))

                        drawCircle(color = SafetyTeal, radius = 7.dp.toPx(), center = center)
                        drawCircle(color = SafetyTeal.copy(alpha = 0.4f), radius = 16.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SafeguardNavyCard.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SafeGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Offline Radar Grid • GPS Live",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Floating action buttons on top of map (Quick SOS, Share Location, Google Maps Intent)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.setPeopleTab(com.example.ui.PeopleTab.SOS)
                        viewModel.startSosCountdown("Radar Map Quick SOS")
                    },
                    containerColor = EmergencyRed,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("button_map_quick_sos")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = "Emergency SOS", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SOS ALERT", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallFloatingActionButton(
                        onClick = {
                            EmergencyActions.openInGoogleMaps(context, uiState.currentLocation)
                        },
                        containerColor = SafetyTealDark,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("button_open_google_maps")
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = "Route in Maps", modifier = Modifier.size(18.dp))
                    }

                    FloatingActionButton(
                        onClick = { showShareSheet = true },
                        containerColor = SafetyTeal,
                        contentColor = SafeguardNavyDark,
                        modifier = Modifier.testTag("button_open_share_sheet")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Verified Danger Zones List for Quick Navigation
        Text(
            text = "VERIFIED DANGER ZONES & SAFE CORRIDORS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val visibleZones = dangerZones.filter { it.isActive }
            items(visibleZones) { zone ->
                DangerZoneItemCard(
                    zone = zone,
                    currentLocation = uiState.currentLocation,
                    onClick = { selectedZone = zone },
                    onNavigate = {
                        EmergencyActions.openRouteInGoogleMaps(context, uiState.currentLocation, zone.location)
                    }
                )
            }

            // Browser and OS Limitations Documentation Card
            item {
                com.example.ui.components.LocationLimitationsCard()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Detail Dialog for Selected Danger Zone
    if (selectedZone != null) {
        val zone = selectedZone!!
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val expiryDate = dateFormat.format(Date(zone.expiresAt))

        AlertDialog(
            onDismissRequest = { selectedZone = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(zone.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = zone.description,
                        fontSize = 13.sp,
                        color = SlateTextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📍 Landmark: ${zone.location.landmark}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeguardNavy
                    )
                    Text(
                        text = "Radius: ${zone.radiusMeters} meters • Severity: ${zone.severity.name}",
                        fontSize = 11.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SafetyTealLight
                    ) {
                        Text(
                            text = "Verified By: ${zone.approvedByAuthority}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SafetyTealDark,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active Until: $expiryDate",
                        fontSize = 10.sp,
                        color = SlateTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        EmergencyActions.openInGoogleMaps(context, zone.location)
                        selectedZone = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions in Google Maps")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedZone = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Live Location Sharing Sheet Dialog
    if (showShareSheet) {
        var selectedDurationMinutes by remember { mutableStateOf(30) }
        var contactName by remember { mutableStateOf("Amma (Mother)") }
        var contactPhone by remember { mutableStateOf("+91 94431 88990") }

        AlertDialog(
            onDismissRequest = { showShareSheet = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShareLocation, contentDescription = null, tint = SafetyTealDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share My Live Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Share temporary, consent-based GPS tracking with your trusted contacts. You can revoke it anytime.",
                        fontSize = 12.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("SELECT DURATION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            FilterChip(
                                selected = selectedDurationMinutes == mins,
                                onClick = { selectedDurationMinutes = mins },
                                label = { Text("${if (mins >= 60) "${mins/60}h" else "${mins}m"}") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Trusted Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone / WhatsApp") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Limitation Note: OS power management restricts background GPS. Keep app active for uninterrupted updates.",
                        fontSize = 10.sp,
                        color = SlateTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startLiveLocationShare(
                            contactName = contactName,
                            contactPhone = contactPhone,
                            durationMinutes = selectedDurationMinutes
                        )
                        showShareSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark)
                ) {
                    Text("Start Live Sharing")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareSheet = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DangerZoneItemCard(
    zone: DangerZone,
    currentLocation: LocationPoint,
    onClick: () -> Unit,
    onNavigate: () -> Unit
) {
    val distance = LocationHelper.distanceInMeters(
        currentLocation.latitude,
        currentLocation.longitude,
        zone.location.latitude,
        zone.location.longitude
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (zone.severity == com.example.model.ZoneSeverity.HIGH) EmergencyRedLight else WarningAmberLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (zone.category) {
                        ReportCategory.UNLIT_ROAD -> Icons.Default.Lightbulb
                        ReportCategory.WATERLOGGING -> Icons.Default.Water
                        ReportCategory.TRAFFIC_HAZARD -> Icons.Default.Traffic
                        else -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = if (zone.severity == com.example.model.ZoneSeverity.HIGH) EmergencyRed else WarningAmber
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SafeguardNavy
                )
                Text(
                    text = zone.location.landmark,
                    fontSize = 11.sp,
                    color = SlateTextMuted,
                    maxLines = 1
                )
                Text(
                    text = "${distance / 1000.0} km away • Verified Danger Zone",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (zone.severity == com.example.model.ZoneSeverity.HIGH) EmergencyRedDark else SafetyTealDark
                )
            }

            IconButton(onClick = onNavigate) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Navigate",
                    tint = SafetyTealDark
                )
            }
        }
    }
}
