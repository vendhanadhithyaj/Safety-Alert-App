package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.DangerZone
import com.example.model.LocationPoint
import com.example.model.SafeStation
import com.example.model.SafeStationType
import com.example.model.SosIncident
import com.example.ui.theme.*
import com.example.util.EmergencyActions

enum class MapTileType {
    STREET,
    SATELLITE,
    DARK
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleMapsPlatformView(
    userLocation: LocationPoint,
    dangerZones: List<DangerZone>,
    activeIncidents: List<SosIncident>,
    safeStations: List<SafeStation> = emptyList(),
    selectedPoint: LocationPoint? = null,
    searchedLocation: LocationPoint? = null,
    isSelectionMode: Boolean = false,
    isDarkTheme: Boolean = false,
    onLocationPicked: ((Double, Double) -> Unit)? = null,
    onZoneSelected: ((DangerZone) -> Unit)? = null,
    onStationSelected: ((SafeStation) -> Unit)? = null,
    onReportAtLocation: ((Double, Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isPageLoaded by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webViewKey by remember { mutableStateOf(0) } // For clean recreation on error/retry

    var currentTileType by remember(isDarkTheme) {
        mutableStateOf(if (isDarkTheme) MapTileType.DARK else MapTileType.STREET)
    }

    // Generate JSON for markers and danger zones
    val zonesJson = remember(dangerZones) {
        val items = dangerZones.filter { it.isActive }.map { z ->
            """{
                "id": "${z.id}",
                "title": "${z.title.replace("\"", "\\\"")}",
                "desc": "${z.description.replace("\"", "\\\"")}",
                "lat": ${z.location.latitude},
                "lng": ${z.location.longitude},
                "radius": ${z.radiusMeters},
                "severity": "${z.severity.name}",
                "authority": "${z.approvedByAuthority.replace("\"", "\\\"")}"
            }"""
        }
        "[${items.joinToString(",")}]"
    }

    val incidentsJson = remember(activeIncidents) {
        val items = activeIncidents.map { inc ->
            """{
                "id": "${inc.id}",
                "type": "${inc.emergencyType.replace("\"", "\\\"")}",
                "user": "${inc.userName.replace("\"", "\\\"")}",
                "lat": ${inc.location.latitude},
                "lng": ${inc.location.longitude},
                "landmark": "${inc.location.landmark.replace("\"", "\\\"")}",
                "status": "${inc.status.label}"
            }"""
        }
        "[${items.joinToString(",")}]"
    }

    val safeStationsJson = remember(safeStations) {
        val items = safeStations.map { stn ->
            """{
                "id": "${stn.id}",
                "name": "${stn.name.replace("\"", "\\\"")}",
                "type": "${stn.type.name}",
                "typeLabel": "${stn.type.label.replace("\"", "\\\"")}",
                "lat": ${stn.location.latitude},
                "lng": ${stn.location.longitude},
                "phone": "${stn.phone}",
                "landmark": "${stn.location.landmark.replace("\"", "\\\"")}"
            }"""
        }
        "[${items.joinToString(",")}]"
    }

    // Full HTML template generated only for page loads (tile switch / initial)
    val htmlContent = remember(currentTileType, isDarkTheme) {
        buildMapHtml(
            initialLat = userLocation.latitude,
            initialLng = userLocation.longitude,
            zonesJson = zonesJson,
            incidentsJson = incidentsJson,
            safeStationsJson = safeStationsJson,
            tileType = currentTileType,
            isDarkTheme = isDarkTheme
        )
    }

    // Update GPS position in running Leaflet without reloading page
    LaunchedEffect(userLocation.latitude, userLocation.longitude, isPageLoaded) {
        if (isPageLoaded && webViewRef != null) {
            webViewRef?.evaluateJavascript(
                "updateUserPosition(${userLocation.latitude}, ${userLocation.longitude});",
                null
            )
        }
    }

    // Update map layer markers dynamically without reloading page
    LaunchedEffect(zonesJson, incidentsJson, safeStationsJson, isPageLoaded) {
        if (isPageLoaded && webViewRef != null) {
            webViewRef?.evaluateJavascript(
                "updateLayers($zonesJson, $incidentsJson, $safeStationsJson);",
                null
            )
        }
    }

    // Trigger search animation whenever searchedLocation updates
    LaunchedEffect(searchedLocation, isPageLoaded) {
        if (isPageLoaded && searchedLocation != null) {
            val safeLabel = searchedLocation.landmark.replace("'", "\\'").replace("\"", "\\\"")
            webViewRef?.evaluateJavascript(
                "searchLocation(${searchedLocation.latitude}, ${searchedLocation.longitude}, '$safeLabel');",
                null
            )
        }
    }

    // Update selected point if provided
    LaunchedEffect(selectedPoint, isPageLoaded) {
        if (isPageLoaded && selectedPoint != null) {
            val safeLabel = selectedPoint.landmark.replace("'", "\\'").replace("\"", "\\\"")
            webViewRef?.evaluateJavascript(
                "dropSelectedPin(${selectedPoint.latitude}, ${selectedPoint.longitude}, '$safeLabel');",
                null
            )
        }
    }

    // Reload page if tile type changes
    LaunchedEffect(currentTileType) {
        if (webViewRef != null) {
            isLoading = true
            webViewRef?.loadDataWithBaseURL("https://maps.google.com/", htmlContent, "text/html", "UTF-8", null)
        }
    }

    // Clean up WebView when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.stopLoading()
                webViewRef?.removeJavascriptInterface("AndroidBridge")
                webViewRef?.destroy()
                webViewRef = null
            } catch (_: Exception) {}
        }
    }

    // JS interface for Android callbacks
    class WebAppInterface {
        @JavascriptInterface
        fun onMapClicked(lat: Double, lng: Double) {
            onLocationPicked?.invoke(lat, lng)
        }

        @JavascriptInterface
        fun onZoneClicked(zoneId: String) {
            val zone = dangerZones.firstOrNull { it.id == zoneId }
            if (zone != null) {
                onZoneSelected?.invoke(zone)
            }
        }

        @JavascriptInterface
        fun onStationClicked(stationId: String) {
            val station = safeStations.firstOrNull { it.id == stationId }
            if (station != null) {
                onStationSelected?.invoke(station)
            }
        }

        @JavascriptInterface
        fun onRouteRequested(lat: Double, lng: Double, landmark: String) {
            val destination = LocationPoint(lat, lng, landmark)
            EmergencyActions.openRouteInGoogleMaps(context, userLocation, destination)
        }

        @JavascriptInterface
        fun onReportRequested(lat: Double, lng: Double) {
            onReportAtLocation?.invoke(lat, lng)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDarkTheme) DarkSurface else Color(0xFFE2E8F0))
            .testTag("google_maps_platform_container")
    ) {
        key(webViewKey) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // CRITICAL: Software layer prevents Chromium GPU/renderer crash in virtualization/containers
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }

                        addJavascriptInterface(WebAppInterface(), "AndroidBridge")

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                hasError = false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                isPageLoaded = true
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isLoading = false
                                }
                            }

                            // CRITICAL FIX: Handle renderer process termination safely so Android does NOT terminate host app
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                hasError = true
                                isLoading = false
                                isPageLoaded = false
                                try {
                                    (view?.parent as? ViewGroup)?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {}
                                webViewRef = null
                                return true // Return true to indicate crash handled and app should NOT be killed
                            }
                        }

                        loadDataWithBaseURL("https://maps.google.com/", htmlContent, "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                // Do NOT reload webview inside update lambda; use LaunchedEffect and JS calls instead!
                update = { webView ->
                    webViewRef = webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading State Overlay
        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = (if (isDarkTheme) DarkBackground else Color.White).copy(alpha = 0.88f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SafetyTeal, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Loading Google Maps Platform...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy
                    )
                    Text(
                        text = "Fetching live satellite, hazard, and safe zone layers",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted
                    )
                }
            }
        }

        // Failure State Overlay with One-Tap Safe Retry
        if (hasError) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) DarkSurface else Color.White,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalWifiOff,
                        contentDescription = "Map Error",
                        tint = EmergencyRedBright,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Map Renderer Reload Required",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarkTheme) DarkTextPrimary else SafeguardNavy
                    )
                    Text(
                        text = "Tap below to restore the interactive map view.",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) DarkTextSecondary else SlateTextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasError = false
                            isLoading = true
                            isPageLoaded = false
                            webViewKey++ // Cleanly recreate WebView
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Top Map Controls (Layer Switcher & Status Badge)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SafeguardNavy.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = if (isSelectionMode) "TAP MAP TO PIN" else "LIVE GPS ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Layer toggle (Street / Satellite / Dark)
            SmallFloatingActionButton(
                onClick = {
                    currentTileType = when (currentTileType) {
                        MapTileType.STREET -> MapTileType.SATELLITE
                        MapTileType.SATELLITE -> if (isDarkTheme) MapTileType.DARK else MapTileType.STREET
                        MapTileType.DARK -> MapTileType.STREET
                    }
                },
                containerColor = if (isDarkTheme) DarkSurface else Color.White,
                contentColor = if (isDarkTheme) SafetyTeal else SafeguardNavy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = when (currentTileType) {
                        MapTileType.SATELLITE -> Icons.Default.Public
                        MapTileType.DARK -> Icons.Default.DarkMode
                        MapTileType.STREET -> Icons.Default.Map
                    },
                    contentDescription = "Toggle Layer",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Recenter on GPS location button
        SmallFloatingActionButton(
            onClick = {
                webViewRef?.evaluateJavascript(
                    "recenter(${userLocation.latitude}, ${userLocation.longitude});",
                    null
                )
            },
            containerColor = if (isDarkTheme) DarkSurface else Color.White,
            contentColor = SafetyTeal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(36.dp)
                .testTag("button_recenter_gps")
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Recenter GPS", modifier = Modifier.size(18.dp))
        }
    }
}

private fun buildMapHtml(
    initialLat: Double,
    initialLng: Double,
    zonesJson: String,
    incidentsJson: String,
    safeStationsJson: String,
    tileType: MapTileType,
    isDarkTheme: Boolean
): String {
    val tileUrl = when (tileType) {
        MapTileType.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        MapTileType.DARK -> "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        MapTileType.STREET -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    val popupBg = if (isDarkTheme) "#0F1E33" else "#FFFFFF"
    val popupTextColor = if (isDarkTheme) "#F8FAFC" else "#0F172A"
    val popupSubTextColor = if (isDarkTheme) "#94A3B8" else "#475569"
    val popupBorder = if (isDarkTheme) "#2E4B75" else "#CBD5E1"

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #0A192F; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
            .leaflet-control-attribution { display: none; }
            
            .leaflet-popup-content-wrapper {
                background: $popupBg !important;
                color: $popupTextColor !important;
                border-radius: 12px !important;
                border: 1px solid $popupBorder !important;
                box-shadow: 0 4px 16px rgba(0,0,0,0.3) !important;
                padding: 4px;
            }
            .leaflet-popup-tip {
                background: $popupBg !important;
            }

            .user-pulse {
                width: 20px; height: 20px; background: #00B4D8; border-radius: 50%;
                border: 2.5px solid #FFFFFF; box-shadow: 0 0 12px #00B4D8; animation: pulse 1.8s infinite;
            }
            @keyframes pulse {
                0% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 180, 216, 0.7); }
                70% { transform: scale(1.15); box-shadow: 0 0 0 12px rgba(0, 180, 216, 0); }
                100% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 180, 216, 0); }
            }

            .sos-beacon {
                width: 24px; height: 24px; background: #D32F2F; border-radius: 50%;
                border: 2px solid #FFFFFF; animation: sosPulse 1s infinite;
                display: flex; align-items: center; justify-content: center; color: white; font-size: 12px; font-weight: bold;
            }
            @keyframes sosPulse {
                0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.85); }
                70% { transform: scale(1.2); box-shadow: 0 0 0 14px rgba(211, 47, 47, 0); }
                100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0); }
            }

            .station-badge {
                width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center;
                justify-content: center; font-size: 14px; border: 2px solid white; box-shadow: 0 3px 8px rgba(0,0,0,0.35);
            }
            .station-police { background: #0F2C59; }
            .station-hospital { background: #DC2626; }
            .station-women { background: #7C3AED; }

            .pin-selection {
                width: 28px; height: 28px; background: #E11D48; border-radius: 50% 50% 50% 0;
                transform: rotate(-45deg); border: 2.5px solid white; box-shadow: 0 4px 10px rgba(0,0,0,0.45);
            }

            .search-beacon {
                display: flex; align-items: center; justify-content: center;
                animation: searchBounce 0.8s ease-in-out infinite alternate;
            }
            @keyframes searchBounce {
                0% { transform: translateY(0); }
                100% { transform: translateY(-8px); }
            }

            .info-popup {
                font-size: 13px; line-height: 1.5; color: $popupTextColor;
            }
            .info-popup .sub {
                font-size: 11px; color: $popupSubTextColor; margin: 4px 0;
            }
            .route-btn {
                display: inline-block; background: #0077B6; color: white !important; padding: 6px 12px;
                border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 11px; margin-top: 6px; cursor: pointer;
            }
            .report-btn {
                display: inline-block; background: #D32F2F; color: white !important; padding: 6px 12px;
                border-radius: 6px; text-decoration: none; font-weight: bold; font-size: 11px; margin-top: 6px; margin-left: 6px; cursor: pointer;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map', { zoomControl: false, preferCanvas: true }).setView([$initialLat, $initialLng], 14);
            L.tileLayer('$tileUrl', { maxZoom: 19 }).addTo(map);

            // Layer Groups for dynamic, zero-reload updates
            var userGroup = L.layerGroup().addTo(map);
            var zonesLayer = L.layerGroup().addTo(map);
            var stationsLayer = L.layerGroup().addTo(map);
            var incidentsLayer = L.layerGroup().addTo(map);
            var selectionLayer = L.layerGroup().addTo(map);
            var searchLayer = L.layerGroup().addTo(map);

            // User Geolocation Pulse Marker
            var userIcon = L.divIcon({ className: 'user-pulse', iconSize: [20, 20] });
            var userMarker = L.marker([$initialLat, $initialLng], { icon: userIcon }).addTo(userGroup);
            userMarker.bindPopup("<div class='info-popup'><b>📍 Your Real-time Location</b><div class='sub'>Kumbakonam & Tamil Nadu GPS<br>Lat: $initialLat, Lng: $initialLng</div></div>");

            var accuracyCircle = L.circle([$initialLat, $initialLng], {
                radius: 60,
                color: '#00B4D8',
                fillColor: '#00B4D8',
                fillOpacity: 0.15,
                weight: 1
            }).addTo(userGroup);

            function updateUserPosition(lat, lng) {
                userMarker.setLatLng([lat, lng]);
                accuracyCircle.setLatLng([lat, lng]);
            }

            function updateLayers(zones, incidents, stations) {
                // Clear existing markers
                zonesLayer.clearLayers();
                stationsLayer.clearLayers();
                incidentsLayer.clearLayers();

                // Render Zones
                zones.forEach(function(z) {
                    var color = z.severity === 'HIGH' ? '#D32F2F' : '#F59E0B';
                    var circle = L.circle([z.lat, z.lng], {
                        radius: z.radius,
                        color: color,
                        fillColor: color,
                        fillOpacity: 0.24,
                        weight: 2
                    }).addTo(zonesLayer);

                    var popupContent = "<div class='info-popup'>" +
                        "<b style='color:" + color + "'>⚠️ DANGER ZONE: " + z.title + "</b>" +
                        "<div class='sub'>" + z.desc + "<br>" +
                        "<b>Radius:</b> " + z.radius + "m • <b>Authority:</b> " + z.authority + "</div>" +
                        "<a class='route-btn' onclick='routeTo(" + z.lat + "," + z.lng + ", \"" + z.title + "\")'>Directions (Google Maps)</a>" +
                        "</div>";

                    circle.bindPopup(popupContent);
                    circle.on('click', function() {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onZoneClicked(z.id);
                        }
                    });
                });

                // Render Safe Stations
                stations.forEach(function(stn) {
                    var iconClass = 'station-police';
                    var iconEmoji = '🛡️';
                    if (stn.type === 'HOSPITAL') {
                        iconClass = 'station-hospital';
                        iconEmoji = '🏥';
                    } else if (stn.type === 'WOMEN_HELP_DESK') {
                        iconClass = 'station-women';
                        iconEmoji = '🌸';
                    }

                    var stnIcon = L.divIcon({
                        className: 'station-badge ' + iconClass,
                        html: iconEmoji,
                        iconSize: [28, 28]
                    });

                    var sm = L.marker([stn.lat, stn.lng], { icon: stnIcon }).addTo(stationsLayer);
                    var popupContent = "<div class='info-popup'>" +
                        "<b>" + iconEmoji + " " + stn.name + "</b>" +
                        "<div class='sub'><b>" + stn.typeLabel + "</b><br>" + stn.landmark + "<br>📞 24/7 Helpline: <b>" + stn.phone + "</b></div>" +
                        "<a class='route-btn' onclick='routeTo(" + stn.lat + "," + stn.lng + ", \"" + stn.name + "\")'>Directions (Google Maps)</a>" +
                        "</div>";
                    sm.bindPopup(popupContent);
                    sm.on('click', function() {
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onStationClicked(stn.id);
                        }
                    });
                });

                // Render Incidents
                incidents.forEach(function(inc) {
                    var sosIcon = L.divIcon({ className: 'sos-beacon', html: '!', iconSize: [24, 24] });
                    var m = L.marker([inc.lat, inc.lng], { icon: sosIcon }).addTo(incidentsLayer);
                    var popupContent = "<div class='info-popup'>" +
                        "<b style='color:#D32F2F'>🚨 ACTIVE SOS: " + inc.id + "</b>" +
                        "<div class='sub'><b>Type:</b> " + inc.type + "<br>" +
                        "<b>User:</b> " + inc.user + "<br>" +
                        "<b>Landmark:</b> " + inc.landmark + "</div>" +
                        "<a class='route-btn' onclick='routeTo(" + inc.lat + "," + inc.lng + ", \"" + inc.id + "\")'>Dispatch Route (Google Maps)</a>" +
                        "</div>";
                    m.bindPopup(popupContent);
                });
            }

            // Populate initial layers
            updateLayers($zonesJson, $incidentsJson, $safeStationsJson);

            // Selection / Tap-to-Pin Marker
            var selectionMarker = null;

            map.on('click', function(e) {
                var lat = e.latlng.lat;
                var lng = e.latlng.lng;
                if (selectionMarker) {
                    selectionMarker.setLatLng(e.latlng);
                } else {
                    var selIcon = L.divIcon({ className: 'pin-selection', iconSize: [28, 28] });
                    selectionMarker = L.marker(e.latlng, { icon: selIcon }).addTo(selectionLayer);
                }
                var popupContent = "<div class='info-popup'><b>📌 PINNED HAZARD / DESTINATION</b>" +
                    "<div class='sub'>Lat: " + lat.toFixed(4) + ", Lng: " + lng.toFixed(4) + "</div>" +
                    "<a class='route-btn' onclick='routeTo(" + lat + "," + lng + ", \"Pinned Point\")'>Get Directions</a>" +
                    "<a class='report-btn' onclick='reportAt(" + lat + "," + lng + ")'>Report Hazard Here</a>" +
                    "</div>";
                selectionMarker.bindPopup(popupContent).openPopup();

                if (window.AndroidBridge) {
                    window.AndroidBridge.onMapClicked(lat, lng);
                }
            });

            var searchMarker = null;
            function searchLocation(lat, lng, label) {
                map.flyTo([lat, lng], 15, { duration: 1.0 });
                if (searchMarker) {
                    searchMarker.setLatLng([lat, lng]);
                } else {
                    var searchIcon = L.divIcon({
                        className: 'search-beacon',
                        html: '<div style=\"background:#00B4D8;border:2.5px solid white;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;color:white;font-size:16px;box-shadow:0 3px 10px rgba(0,0,0,0.45);\">🔍</div>',
                        iconSize: [32, 32]
                    });
                    searchMarker = L.marker([lat, lng], { icon: searchIcon }).addTo(searchLayer);
                }
                var popupHtml = "<div class='info-popup'><b style='color:#0077B6'>🔍 SEARCHED LOCATION</b>" +
                    "<div class='sub'><b>" + label + "</b><br>Lat: " + lat.toFixed(4) + ", Lng: " + lng.toFixed(4) + "</div>" +
                    "<a class='route-btn' onclick='routeTo(" + lat + "," + lng + ", \"" + label + "\")'>Directions (Google Maps)</a>" +
                    "<a class='report-btn' onclick='reportAt(" + lat + "," + lng + ")'>Report Hazard Here</a>" +
                    "</div>";
                searchMarker.bindPopup(popupHtml).openPopup();
            }

            function dropSelectedPin(lat, lng, label) {
                if (selectionMarker) {
                    selectionMarker.setLatLng([lat, lng]);
                } else {
                    var selIcon = L.divIcon({ className: 'pin-selection', iconSize: [28, 28] });
                    selectionMarker = L.marker([lat, lng], { icon: selIcon }).addTo(selectionLayer);
                }
                selectionMarker.bindPopup("<div class='info-popup'><b>📌 PINNED LOCATION</b><div class='sub'>" + label + "<br>Lat: " + lat.toFixed(4) + ", Lng: " + lng.toFixed(4) + "</div><a class='route-btn' onclick='routeTo(" + lat + "," + lng + ", \"" + label + "\")'>Directions (Google Maps)</a></div>").openPopup();
            }

            function recenter(lat, lng) {
                map.flyTo([lat, lng], 15, { duration: 0.8 });
            }

            function routeTo(lat, lng, landmark) {
                if (window.AndroidBridge) {
                    window.AndroidBridge.onRouteRequested(lat, lng, landmark);
                }
            }

            function reportAt(lat, lng) {
                if (window.AndroidBridge) {
                    window.AndroidBridge.onReportRequested(lat, lng);
                }
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
