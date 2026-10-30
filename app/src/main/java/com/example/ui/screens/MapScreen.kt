package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.model.*
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.ThemeMode
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.*

enum class MapFilter(val label: String) {
    ALL("All Layers"),
    DANGER_ZONES("Danger Zones"),
    POLICE_BOOTHS("Police & Desks"),
    SHELTERS_HOSPITALS("Hospitals & Care"),
    ACTIVE_INCIDENTS("Active SOS")
}

enum class MapDisplayMode {
    GOOGLE_MAPS,
    RADAR_GRID
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (uiState.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val dangerZones by viewModel.dangerZones.collectAsState()
    val activeIncidents by viewModel.allIncidents.collectAsState()
    val safeStations by viewModel.safeStations.collectAsState()
    val activeLiveShares by viewModel.activeLiveShares.collectAsState()

    var mapMode by remember { mutableStateOf(MapDisplayMode.GOOGLE_MAPS) }
    var selectedFilter by remember { mutableStateOf(MapFilter.ALL) }
    var selectedZone by remember { mutableStateOf<DangerZone?>(null) }
    var selectedStation by remember { mutableStateOf<SafeStation?>(null) }
    var showShareSheet by remember { mutableStateOf(false) }

    // Search and Selection States
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchedLocation by remember { mutableStateOf<LocationPoint?>(null) }
    var pinnedLocation by remember { mutableStateOf<LocationPoint?>(null) }

    val requestLocationPermission = com.example.ui.components.rememberLocationPermissionLauncher { granted, _ ->
        viewModel.onLocationPermissionResult(granted)
    }

    val activeShare = activeLiveShares.firstOrNull { it.isActive && it.expiresAtEpoch > System.currentTimeMillis() }

    // Filter datasets according to chosen chip
    val filteredZones = remember(dangerZones, selectedFilter) {
        when (selectedFilter) {
            MapFilter.ALL, MapFilter.DANGER_ZONES -> dangerZones.filter { it.isActive }
            else -> emptyList()
        }
    }

    val filteredIncidents = remember(activeIncidents, selectedFilter) {
        when (selectedFilter) {
            MapFilter.ALL, MapFilter.ACTIVE_INCIDENTS -> activeIncidents
            else -> emptyList()
        }
    }

    val filteredStations = remember(safeStations, selectedFilter) {
        when (selectedFilter) {
            MapFilter.ALL -> safeStations
            MapFilter.POLICE_BOOTHS -> safeStations.filter { it.type == SafeStationType.POLICE || it.type == SafeStationType.WOMEN_HELP_DESK }
            MapFilter.SHELTERS_HOSPITALS -> safeStations.filter { it.type == SafeStationType.HOSPITAL || it.type == SafeStationType.SHELTER }
            else -> emptyList()
        }
    }

    // Search results
    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else LocationHelper.searchLocations(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Location Permission Banner if not granted
        com.example.ui.components.LocationPermissionCard(
            hasPermission = uiState.hasLocationPermission,
            onRequestPermission = requestLocationPermission,
            title = "Enable GPS for Exact Map Navigation",
            description = "High-accuracy geolocation renders your live beacon, computes distances to verified safe zones, and speeds up emergency dispatch in Tamil Nadu."
        )

        // Active Live Location Sharing Alert Banner
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Active for next $remainingMinutes mins • Consent-based session",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.stopLiveShare(activeShare.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Top Search Bar for Locations across India & Tamil Nadu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    isSearchActive = it.isNotBlank()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_map_search"),
                placeholder = {
                    Text(
                        "Search location in India (e.g. Chennai, Kumbakonam)...",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SafetyTeal,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchActive = false
                            searchedLocation = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafetyTeal,
                    unfocusedBorderColor = if (isDarkTheme) DarkBorder else SlateBorder,
                    focusedContainerColor = if (isDarkTheme) DarkSurface else Color.White,
                    unfocusedContainerColor = if (isDarkTheme) DarkSurface else Color.White
                )
            )

            // Popular City Quick Suggestions
            if (searchQuery.isEmpty() && !isSearchActive) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val popularHubs = listOf(
                        LocationPoint(10.9602, 79.3845, "Kumbakonam Town Hall", "Thanjavur", "Tamil Nadu"),
                        LocationPoint(10.7870, 79.1378, "Thanjavur Bus Stand", "Thanjavur", "Tamil Nadu"),
                        LocationPoint(10.8050, 78.6856, "Trichy Chathiram", "Tiruchirappalli", "Tamil Nadu"),
                        LocationPoint(13.0827, 80.2707, "Chennai Central", "Chennai", "Tamil Nadu"),
                        LocationPoint(11.0168, 76.9558, "Coimbatore Gandhipuram", "Coimbatore", "Tamil Nadu"),
                        LocationPoint(9.9252, 78.1198, "Madurai Periyar", "Madurai", "Tamil Nadu"),
                        LocationPoint(12.9716, 77.5946, "Bengaluru Majestic", "Bengaluru", "Karnataka"),
                        LocationPoint(28.6139, 77.2090, "New Delhi India Gate", "Delhi", "Delhi")
                    )
                    items(popularHubs) { hub ->
                        SuggestionChip(
                            onClick = {
                                searchedLocation = hub
                                pinnedLocation = hub
                            },
                            label = { Text(hub.landmark.split(" ").take(2).joinToString(" "), fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isDarkTheme) DarkSurfaceVariant else Color(0xFFF1F5F9),
                                labelColor = if (isDarkTheme) DarkTextPrimary else SlateTextDark
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isDarkTheme) DarkBorder else SlateBorder
                            )
                        )
                    }
                }
            }

            // Search Results Dropdown List
            if (isSearchActive && searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) DarkSurface else Color.White,
                    shadowElevation = 6.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) DarkBorder else SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        searchResults.take(4).forEach { loc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        searchedLocation = loc
                                        pinnedLocation = loc
                                        searchQuery = loc.landmark
                                        isSearchActive = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = SafetyTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(loc.landmark, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) DarkTextPrimary else SlateTextDark)
                                    Text("${loc.district}, ${loc.state}", fontSize = 10.sp, color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted)
                                }
                                val dist = LocationHelper.distanceInMeters(uiState.currentLocation.latitude, uiState.currentLocation.longitude, loc.latitude, loc.longitude)
                                Text(
                                    text = if (dist > 1000) "${dist / 1000} km" else "$dist m",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SafetyTeal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Layer Filter Chips & View Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(MapFilter.values()) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            val count = when (filter) {
                                MapFilter.ALL -> dangerZones.count { it.isActive } + safeStations.size + activeIncidents.size
                                MapFilter.DANGER_ZONES -> dangerZones.count { it.isActive }
                                MapFilter.POLICE_BOOTHS -> safeStations.count { it.type == SafeStationType.POLICE || it.type == SafeStationType.WOMEN_HELP_DESK }
                                MapFilter.SHELTERS_HOSPITALS -> safeStations.count { it.type == SafeStationType.HOSPITAL }
                                MapFilter.ACTIVE_INCIDENTS -> activeIncidents.size
                            }
                            Text("${filter.label} ($count)", fontSize = 11.sp, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal)
                        },
                        leadingIcon = {
                            when (filter) {
                                MapFilter.DANGER_ZONES -> Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                                MapFilter.POLICE_BOOTHS -> Icon(Icons.Default.LocalPolice, contentDescription = null, tint = SafetyTeal, modifier = Modifier.size(14.dp))
                                MapFilter.SHELTERS_HOSPITALS -> Icon(Icons.Default.LocalHospital, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                                MapFilter.ACTIVE_INCIDENTS -> Icon(Icons.Default.Campaign, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(14.dp))
                                else -> null
                            }
                        }
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
                modifier = Modifier.height(34.dp)
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

        // Map View Area: Either Google Maps Platform or Tactical Radar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.1f)
                .padding(horizontal = 12.dp)
        ) {
            if (mapMode == MapDisplayMode.GOOGLE_MAPS) {
                com.example.ui.components.GoogleMapsPlatformView(
                    userLocation = uiState.currentLocation,
                    dangerZones = filteredZones,
                    activeIncidents = filteredIncidents,
                    safeStations = filteredStations,
                    searchedLocation = searchedLocation,
                    selectedPoint = pinnedLocation,
                    isDarkTheme = isDarkTheme,
                    onLocationPicked = { lat, lng ->
                        val dist = LocationHelper.distanceInMeters(uiState.currentLocation.latitude, uiState.currentLocation.longitude, lat, lng)
                        pinnedLocation = LocationPoint(
                            latitude = lat,
                            longitude = lng,
                            landmark = "Pinned Map Location (${"%.4f".format(lat)}, ${"%.4f".format(lng)})",
                            district = "Thanjavur",
                            state = "Tamil Nadu"
                        )
                    },
                    onZoneSelected = { selectedZone = it },
                    onStationSelected = { selectedStation = it },
                    onReportAtLocation = { lat, lng ->
                        val pt = LocationPoint(
                            latitude = lat,
                            longitude = lng,
                            landmark = "Map Pin (${"%.4f".format(lat)}, ${"%.4f".format(lng)})",
                            district = "Thanjavur",
                            state = "Tamil Nadu"
                        )
                        viewModel.initiateReportAtLocation(pt)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Interactive Tactical Radar / Safety Canvas Map View
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

                        // Render sample danger circles on radar
                        drawCircle(color = EmergencyRed.copy(alpha = 0.3f), radius = 35.dp.toPx(), center = Offset(center.x + 80.dp.toPx(), center.y - 45.dp.toPx()))
                        drawCircle(color = WarningAmber.copy(alpha = 0.25f), radius = 28.dp.toPx(), center = Offset(center.x - 70.dp.toPx(), center.y + 60.dp.toPx()))

                        // User GPS beacon
                        drawCircle(color = SafetyTeal, radius = 8.dp.toPx(), center = center)
                        drawCircle(color = SafetyTeal.copy(alpha = 0.45f), radius = 18.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SafeguardNavyCard.copy(alpha = 0.88f)
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
                                    text = "Tactical Radar Grid • GPS Live",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Floating action buttons on top of map
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

        // Pinned / Selected Location Action Banner (if user pinned or searched a place)
        AnimatedVisibility(visible = pinnedLocation != null) {
            pinnedLocation?.let { pin ->
                val distanceMeters = LocationHelper.distanceInMeters(
                    uiState.currentLocation.latitude,
                    uiState.currentLocation.longitude,
                    pin.latitude,
                    pin.longitude
                )
                val distStr = if (distanceMeters > 1000) "${"%.1f".format(distanceMeters / 1000.0)} km" else "$distanceMeters meters"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) DarkSurfaceVariant else Color(0xFFF1F5F9)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = EmergencyRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = pin.landmark,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) DarkTextPrimary else SlateTextDark,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Distance: $distStr away • Lat: ${"%.4f".format(pin.latitude)}, Lng: ${"%.4f".format(pin.longitude)}",
                                        fontSize = 10.sp,
                                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                                    )
                                }
                            }
                            IconButton(onClick = { pinnedLocation = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateTextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    EmergencyActions.openRouteInGoogleMaps(context, uiState.currentLocation, pin)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Directions (Maps)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.initiateReportAtLocation(pin)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report Hazard Here", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Verified Danger Zones & Safe Corridors List
        Text(
            text = "VERIFIED DANGER ZONES & SAFE CORRIDORS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val visibleZones = dangerZones.filter { it.isActive }
            items(visibleZones) { zone ->
                DangerZoneItemCard(
                    zone = zone,
                    currentLocation = uiState.currentLocation,
                    isDarkTheme = isDarkTheme,
                    onClick = {
                        selectedZone = zone
                        pinnedLocation = zone.location
                    },
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
                    Text(zone.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = zone.description,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) DarkTextPrimary else SlateTextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📍 Landmark: ${zone.location.landmark}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafetyTeal
                    )
                    Text(
                        text = "Radius: ${zone.radiusMeters} meters • Severity: ${zone.severity.name}",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) DarkSurfaceVariant else SafetyTealLight
                    ) {
                        Text(
                            text = "Verified By: ${zone.approvedByAuthority}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) SafetyTeal else SafetyTealDark,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Active Until: $expiryDate",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
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

    // Detail Dialog for Selected Safe Station / Police Station
    if (selectedStation != null) {
        val stn = selectedStation!!
        AlertDialog(
            onDismissRequest = { selectedStation = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (stn.type == SafeStationType.HOSPITAL) Icons.Default.LocalHospital else Icons.Default.LocalPolice,
                        contentDescription = null,
                        tint = SafetyTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stn.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Type: ${stn.type.label}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafetyTeal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 Location: ${stn.location.landmark}",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) DarkTextPrimary else SlateTextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "📞 24/7 Official Contact: ${stn.phone}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) SafeGreen else SafeGreenDark
                    )
                    Text(
                        text = "Jurisdiction: ${stn.jurisdiction}",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        EmergencyActions.dialEmergency(context, stn.phone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Station")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        EmergencyActions.openRouteInGoogleMaps(context, uiState.currentLocation, stn.location)
                        selectedStation = null
                    }
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions")
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
                    Text("Share My Live Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Share temporary, consent-based GPS tracking with your trusted contacts. You can revoke it anytime.",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("SELECT DURATION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) DarkTextPrimary else SlateTextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 120).forEach { mins ->
                            FilterChip(
                                selected = selectedDurationMinutes == mins,
                                onClick = { selectedDurationMinutes = mins },
                                label = { Text(if (mins >= 60) "${mins/60}h" else "${mins}m") }
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
                        text = "⚠️ Limitation Note: OS power management restricts background GPS. Keep app active or foreground service enabled for updates.",
                        fontSize = 10.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
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
    isDarkTheme: Boolean,
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
            .border(1.dp, if (isDarkTheme) DarkBorder else SlateBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) DarkSurface else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (zone.severity == ZoneSeverity.HIGH) {
                            if (isDarkTheme) Color(0xFF5C101B) else EmergencyRedLight
                        } else {
                            if (isDarkTheme) Color(0xFF452B05) else WarningAmberLight
                        }
                    ),
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
                    tint = if (zone.severity == ZoneSeverity.HIGH) EmergencyRedBright else WarningAmber
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy
                )
                Text(
                    text = zone.location.landmark,
                    fontSize = 11.sp,
                    color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted,
                    maxLines = 1
                )
                Text(
                    text = "${"%.1f".format(distance / 1000.0)} km away • Verified Danger Zone",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (zone.severity == ZoneSeverity.HIGH) EmergencyRedBright else SafetyTeal
                )
            }

            IconButton(onClick = onNavigate) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Navigate",
                    tint = SafetyTeal
                )
            }
        }
    }
}
