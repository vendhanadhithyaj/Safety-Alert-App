package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.SosIncident
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper

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
    selectedPoint: LocationPoint? = null,
    isSelectionMode: Boolean = false,
    onLocationPicked: ((Double, Double) -> Unit)? = null,
    onZoneSelected: ((DangerZone) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentTileType by remember { mutableStateOf(MapTileType.STREET) }

    // Generate JSON for markers and danger zones to inject into JS
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

    val htmlContent = remember(userLocation, zonesJson, incidentsJson, isSelectionMode, currentTileType) {
        buildMapHtml(
            userLat = userLocation.latitude,
            userLng = userLocation.longitude,
            zonesJson = zonesJson,
            incidentsJson = incidentsJson,
            isSelectionMode = isSelectionMode,
            tileType = currentTileType
        )
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
        fun onRouteRequested(lat: Double, lng: Double, landmark: String) {
            val destination = LocationPoint(lat, lng, landmark)
            EmergencyActions.openRouteInGoogleMaps(context, userLocation, destination)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2E8F0))
            .testTag("google_maps_platform_container")
    ) {
        // Embedded Interactive Map View
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(true)
                    settings.builtInZoomControls = false

                    addJavascriptInterface(WebAppInterface(), "AndroidBridge")

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            hasError = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                isLoading = false
                            }
                        }
                    }

                    loadDataWithBaseURL("https://maps.google.com/", htmlContent, "text/html", "UTF-8", null)
                    webViewRef = this
                }
            },
            update = { webView ->
                webViewRef = webView
                webView.loadDataWithBaseURL("https://maps.google.com/", htmlContent, "text/html", "UTF-8", null)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading State Overlay
        if (isLoading) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SafetyTealDark, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Loading Google Maps Platform...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeguardNavy
                    )
                    Text(
                        text = "Fetching live satellite and hazard layers",
                        fontSize = 10.sp,
                        color = SlateTextMuted
                    )
                }
            }
        }

        // Failure State Overlay
        if (hasError) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
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
                        tint = EmergencyRed,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to Load Google Maps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SafeguardNavy
                    )
                    Text(
                        text = "Check your device internet connectivity or GPS signal.",
                        fontSize = 11.sp,
                        color = SlateTextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasError = false
                            isLoading = true
                            webViewRef?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Connection", fontSize = 11.sp)
                    }
                }
            }
        }

        // Top Map Controls (Layer Switcher & Selection Indicator)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SafeguardNavy.copy(alpha = 0.9f),
                shadowElevation = 4.dp
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
                        text = if (isSelectionMode) "TAP TO PIN HAZARD" else "LIVE GPS ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Layer toggle (Street / Satellite)
            SmallFloatingActionButton(
                onClick = {
                    currentTileType = if (currentTileType == MapTileType.STREET) MapTileType.SATELLITE else MapTileType.STREET
                },
                containerColor = Color.White,
                contentColor = SafeguardNavy,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (currentTileType == MapTileType.STREET) Icons.Default.Satellite else Icons.Default.Map,
                    contentDescription = "Toggle Layer",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Recenter on GPS location button
        SmallFloatingActionButton(
            onClick = {
                webViewRef?.loadUrl("javascript:recenter(${userLocation.latitude}, ${userLocation.longitude});")
            },
            containerColor = Color.White,
            contentColor = SafetyTealDark,
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
    userLat: Double,
    userLng: Double,
    zonesJson: String,
    incidentsJson: String,
    isSelectionMode: Boolean,
    tileType: MapTileType
): String {
    val tileUrl = when (tileType) {
        MapTileType.SATELLITE -> "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        MapTileType.DARK -> "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        MapTileType.STREET -> "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    }

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #0A192F; font-family: -apple-system, Roboto, sans-serif; }
            .leaflet-control-attribution { display: none; }
            .user-pulse {
                width: 18px; height: 18px; background: #00B4D8; border-radius: 50%;
                border: 2px solid #FFFFFF; box-shadow: 0 0 10px #00B4D8; animation: pulse 1.8s infinite;
            }
            @keyframes pulse {
                0% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 180, 216, 0.7); }
                70% { transform: scale(1.1); box-shadow: 0 0 0 10px rgba(0, 180, 216, 0); }
                100% { transform: scale(0.9); box-shadow: 0 0 0 0 rgba(0, 180, 216, 0); }
            }
            .sos-beacon {
                width: 22px; height: 22px; background: #D32F2F; border-radius: 50%;
                border: 2px solid #FFFFFF; animation: sosPulse 1s infinite;
                display: flex; align-items: center; justify-content: center; color: white; font-size: 10px; font-weight: bold;
            }
            @keyframes sosPulse {
                0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.8); }
                70% { transform: scale(1.15); box-shadow: 0 0 0 12px rgba(211, 47, 47, 0); }
                100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(211, 47, 47, 0); }
            }
            .pin-selection {
                width: 28px; height: 28px; background: #D32F2F; border-radius: 50% 50% 50% 0;
                transform: rotate(-45deg); border: 2px solid white; box-shadow: 0 3px 6px rgba(0,0,0,0.4);
            }
            .info-popup {
                font-size: 12px; line-height: 1.4;
            }
            .route-btn {
                display: inline-block; background: #0F2C59; color: white !important; padding: 4px 10px;
                border-radius: 6px; text-decoration: none; font-weight: bold; margin-top: 6px; cursor: pointer;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map', { zoomControl: false }).setView([$userLat, $userLng], 14);
            L.tileLayer('$tileUrl', { maxZoom: 19 }).addTo(map);

            // User Geolocation Pulse Marker
            var userIcon = L.divIcon({ className: 'user-pulse', iconSize: [18, 18] });
            var userMarker = L.marker([$userLat, $userLng], { icon: userIcon }).addTo(map);
            userMarker.bindPopup("<div class='info-popup'><b>📍 Your Device Location</b><br>Kumbakonam / GPS Coordinates<br>Lat: $userLat, Lng: $userLng</div>");

            // Accuracy Circle (approx 50m)
            L.circle([$userLat, $userLng], {
                radius: 60,
                color: '#00B4D8',
                fillColor: '#00B4D8',
                fillOpacity: 0.15,
                weight: 1
            }).addTo(map);

            // Verified Danger Zones
            var zones = $zonesJson;
            zones.forEach(function(z) {
                var color = z.severity === 'HIGH' ? '#D32F2F' : '#F59E0B';
                var circle = L.circle([z.lat, z.lng], {
                    radius: z.radius,
                    color: color,
                    fillColor: color,
                    fillOpacity: 0.22,
                    weight: 2
                }).addTo(map);

                var popupContent = "<div class='info-popup'>" +
                    "<b style='color:" + color + "'>⚠️ DANGER ZONE: " + z.title + "</b><br>" +
                    z.desc + "<br>" +
                    "<b>Radius:</b> " + z.radius + "m • <b>Authority:</b> " + z.authority + "<br>" +
                    "<a class='route-btn' onclick='routeTo(" + z.lat + "," + z.lng + ", \"" + z.title + "\")'>Get Directions (Google Maps)</a>" +
                    "</div>";

                circle.bindPopup(popupContent);
                circle.on('click', function() {
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onZoneClicked(z.id);
                    }
                });
            });

            // Active SOS Incidents
            var incidents = $incidentsJson;
            incidents.forEach(function(inc) {
                var sosIcon = L.divIcon({ className: 'sos-beacon', html: '!', iconSize: [22, 22] });
                var m = L.marker([inc.lat, inc.lng], { icon: sosIcon }).addTo(map);
                var popupContent = "<div class='info-popup'>" +
                    "<b style='color:#D32F2F'>🚨 ACTIVE SOS: " + inc.id + "</b><br>" +
                    "<b>Type:</b> " + inc.type + "<br>" +
                    "<b>User:</b> " + inc.user + "<br>" +
                    "<b>Status:</b> " + inc.status + "<br>" +
                    "<a class='route-btn' onclick='routeTo(" + inc.lat + "," + inc.lng + ", \"" + inc.id + "\")'>Dispatch Route (Google Maps)</a>" +
                    "</div>";
                m.bindPopup(popupContent);
            });

            // Selection Mode Pin
            var selectionMarker = null;
            var isSelecting = $isSelectionMode;

            map.on('click', function(e) {
                if (isSelecting) {
                    if (selectionMarker) {
                        selectionMarker.setLatLng(e.latlng);
                    } else {
                        var selIcon = L.divIcon({ className: 'pin-selection', iconSize: [28, 28] });
                        selectionMarker = L.marker(e.latlng, { icon: selIcon }).addTo(map);
                    }
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onMapClicked(e.latlng.lat, e.latlng.lng);
                    }
                }
            });

            function recenter(lat, lng) {
                map.flyTo([lat, lng], 15);
            }

            function routeTo(lat, lng, landmark) {
                if (window.AndroidBridge) {
                    window.AndroidBridge.onRouteRequested(lat, lng, landmark);
                }
            }
        </script>
    </body>
    </html>
    """.trimIndent()
}
