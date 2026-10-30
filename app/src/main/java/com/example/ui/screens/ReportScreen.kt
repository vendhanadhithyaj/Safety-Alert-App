package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.model.CommunityReport
import com.example.model.ReportCategory
import com.example.model.ReviewStatus
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val reports by viewModel.communityReports.collectAsState()

    var showForm by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ReportCategory.UNLIT_ROAD) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var voiceNoteDuration by remember { mutableStateOf(0) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var submittedRefCode by remember { mutableStateOf<String?>(null) }
    var customLocation by remember { mutableStateOf<com.example.model.LocationPoint?>(null) }
    var showMapPickerModal by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.pendingReportLocation) {
        uiState.pendingReportLocation?.let { loc ->
            customLocation = loc
            showForm = true
            viewModel.clearPendingReportLocation()
        }
    }

    val effectiveLocation = customLocation ?: uiState.currentLocation

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedPhotoUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight)
    ) {
        // Top Header with "Report Hazard" Action Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = SafeguardNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal.copy(alpha = 0.5f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.safeguard_official_logo),
                                contentDescription = "SAFEGUARD Emblem",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "COMMUNITY SAFETY REPORTS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafetyTeal,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Report Street & Civic Hazards",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showForm = !showForm },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showForm) SlateBorder.copy(alpha = 0.3f) else EmergencyRed
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("button_toggle_report_form")
                    ) {
                        Icon(
                            imageVector = if (showForm) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showForm) "Close" else "File Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "Reports undergo authorized review before public danger-zone publication. Kumbakonam & Tamil Nadu.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Newly submitted confirmation notification
        if (submittedRefCode != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SafeGreenLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, SafeGreen)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Report Submitted Successfully!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafeGreenDark)
                            Text("Reference: $submittedRefCode • Status: PENDING REVIEW", fontSize = 11.sp, color = SlateTextDark)
                        }
                    }
                    IconButton(onClick = { submittedRefCode = null }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateTextMuted)
                    }
                }
            }
        }

        // Submission Form (when open)
        if (showForm) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NEW HAZARD SUBMISSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeguardNavy,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Selector
                    Text("Category:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SlateTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    var categoryExpanded by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                            .clickable { categoryExpanded = true },
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(selectedCategory.displayName, fontSize = 12.sp, color = SlateTextDark, fontWeight = FontWeight.Medium)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = SlateTextMuted)
                        }
                    }
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ReportCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName, fontSize = 12.sp) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Short Title / Hazard Description") },
                        placeholder = { Text("e.g. Broken streetlight on Cauvery North Road") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_report_title")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Explanation & Risk Context") },
                        placeholder = { Text("Describe visibility, timings, student risk...") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("input_report_description")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location Info & Google Maps Pinning
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (customLocation != null) SafetyTealLight else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (customLocation != null) SafetyTealDark else SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = if (customLocation != null) EmergencyRed else SafetyTealDark, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (customLocation != null) "Custom Pinned Location" else "Auto-detected GPS Location",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SafeguardNavy
                                    )
                                }

                                TextButton(
                                    onClick = { showMapPickerModal = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick on Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = effectiveLocation.landmark,
                                fontSize = 11.sp,
                                color = SlateTextDark,
                                maxLines = 2
                            )
                            Text(
                                text = "Coordinates: ${"%.4f".format(effectiveLocation.latitude)}, ${"%.4f".format(effectiveLocation.longitude)}",
                                fontSize = 10.sp,
                                color = SlateTextMuted
                            )

                            if (customLocation != null) {
                                TextButton(
                                    onClick = { customLocation = null },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset to Device GPS", fontSize = 10.sp, color = EmergencyRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Evidence Attachment (Camera / Gallery Picker)
                    Text("ATTACH EVIDENCE (PHOTO & AUDIO):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("button_attach_photo")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedPhotoUri != null) "Change Photo" else "Add Photo", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                isRecordingAudio = !isRecordingAudio
                                if (!isRecordingAudio) {
                                    voiceNoteDuration = 8 // simulated recorded seconds
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isRecordingAudio) EmergencyRed else SafeguardNavy
                            ),
                            modifier = Modifier.weight(1f).testTag("button_record_voice_note")
                        ) {
                            Icon(if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecordingAudio) "Stop (0:05)" else if (voiceNoteDuration > 0) "Voice (0:08)" else "Voice Memo", fontSize = 11.sp)
                        }
                    }

                    // Photo Preview (with mandatory policy compliance label!)
                    if (selectedPhotoUri != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedPhotoUri),
                                contentDescription = "Attached Evidence",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Mandatory disclaimer tag
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "SAMPLE — NOT REAL EVIDENCE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IconButton(
                                onClick = { selectedPhotoUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val rep = viewModel.submitReport(
                                    category = selectedCategory,
                                    title = title.trim(),
                                    description = description.trim(),
                                    location = effectiveLocation,
                                    photoUri = selectedPhotoUri?.toString(),
                                    audioDurationSec = voiceNoteDuration
                                )
                                submittedRefCode = "REP-TN-${(1000..9999).random()}"
                                title = ""
                                description = ""
                                selectedPhotoUri = null
                                voiceNoteDuration = 0
                                customLocation = null
                                showForm = false
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SafeguardNavy),
                        modifier = Modifier.fillMaxWidth().testTag("button_submit_report")
                    ) {
                        Text("Submit Report for Official Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Community Reports Feed
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "VERIFICATION QUEUE & COMMUNITY SUBMISSIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextMuted,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(reports) { rep ->
                ReportItemCard(report = rep, userLocation = uiState.currentLocation)
            }
        }
    }

    // Google Maps Location Picker Modal
    if (showMapPickerModal) {
        var tempLat by remember { mutableStateOf(effectiveLocation.latitude) }
        var tempLng by remember { mutableStateOf(effectiveLocation.longitude) }

        AlertDialog(
            onDismissRequest = { showMapPickerModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Hazard Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SafeguardNavy)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    Text(
                        text = "Tap on Google Maps to pin the incident. Coordinates: ${"%.4f".format(tempLat)}, ${"%.4f".format(tempLng)}",
                        fontSize = 11.sp,
                        color = SlateTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    com.example.ui.components.GoogleMapsPlatformView(
                        userLocation = uiState.currentLocation,
                        dangerZones = emptyList(),
                        activeIncidents = emptyList(),
                        isSelectionMode = true,
                        onLocationPicked = { lat, lng ->
                            tempLat = lat
                            tempLng = lng
                        },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customLocation = com.example.model.LocationPoint(
                            latitude = tempLat,
                            longitude = tempLng,
                            landmark = "Pinned on Google Maps (${"%.4f".format(tempLat)}, ${"%.4f".format(tempLng)})",
                            district = "Thanjavur",
                            state = "Tamil Nadu"
                        )
                        showMapPickerModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Confirm Pinned Location")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapPickerModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReportItemCard(
    report: CommunityReport,
    userLocation: com.example.model.LocationPoint
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(report.timestamp))

    val distanceMeters = com.example.util.LocationHelper.distanceInMeters(
        userLocation.latitude,
        userLocation.longitude,
        report.location.latitude,
        report.location.longitude
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Category & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (report.category) {
                                    ReportCategory.HARASSMENT -> EmergencyRedLight
                                    ReportCategory.UNLIT_ROAD -> WarningAmberLight
                                    ReportCategory.WATERLOGGING -> SafetyTealLight
                                    else -> SlateBorder.copy(alpha = 0.5f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (report.category) {
                                ReportCategory.HARASSMENT -> Icons.Default.Security
                                ReportCategory.UNLIT_ROAD -> Icons.Default.Lightbulb
                                ReportCategory.WATERLOGGING -> Icons.Default.Water
                                ReportCategory.TRAFFIC_HAZARD -> Icons.Default.Traffic
                                else -> Icons.Default.Report
                            },
                            contentDescription = null,
                            tint = when (report.category) {
                                ReportCategory.HARASSMENT -> EmergencyRed
                                ReportCategory.UNLIT_ROAD -> WarningAmber
                                ReportCategory.WATERLOGGING -> SafetyTealDark
                                else -> SafeguardNavy
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = report.category.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeguardNavy
                    )
                }

                // Truthful Review Status Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (report.status) {
                        ReviewStatus.PENDING_REVIEW -> WarningAmberLight
                        ReviewStatus.UNDER_REVIEW -> SafetyTealLight
                        ReviewStatus.VERIFIED -> SafeGreenLight
                        ReviewStatus.REJECTED -> EmergencyRedLight
                        ReviewStatus.INFO_REQUESTED -> SlateBorder
                    }
                ) {
                    Text(
                        text = report.status.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (report.status) {
                            ReviewStatus.PENDING_REVIEW -> WarningAmber
                            ReviewStatus.UNDER_REVIEW -> SafetyTealDark
                            ReviewStatus.VERIFIED -> SafeGreenDark
                            ReviewStatus.REJECTED -> EmergencyRed
                            ReviewStatus.INFO_REQUESTED -> SlateTextDark
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = report.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SlateTextDark
            )
            Text(
                text = report.description,
                fontSize = 12.sp,
                color = SlateTextMuted,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📍 ${report.location.landmark}",
                    fontSize = 11.sp,
                    color = SlateTextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = "${"%.1f".format(distanceMeters / 1000.0)} km away",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyTealDark
                )
            }

            // Evidence tags if attached
            if (report.audioDurationSec > 0 || report.photoUri != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (report.photoUri != null) {
                        Surface(shape = RoundedCornerShape(4.dp), color = SlateBorder.copy(alpha = 0.4f)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(12.dp), tint = SlateTextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Photo Attached", fontSize = 10.sp, color = SlateTextDark)
                            }
                        }
                    }
                    if (report.audioDurationSec > 0) {
                        Surface(shape = RoundedCornerShape(4.dp), color = SlateBorder.copy(alpha = 0.4f)) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(12.dp), tint = SlateTextMuted)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Voice Memo (0:${report.audioDurationSec})", fontSize = 10.sp, color = SlateTextDark)
                            }
                        }
                    }
                }
            }

            // Reviewer attribution if verified or under review
            if (report.verifiedBy != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SlateLight
                ) {
                    Text(
                        text = "Official Action: ${report.verifiedBy}",
                        fontSize = 10.sp,
                        color = SafeguardNavy,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ref: ${report.id} • By ${report.reporterName}",
                        fontSize = 10.sp,
                        color = SlateTextMuted
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 9.sp,
                        color = SlateTextMuted
                    )
                }

                // Route directions button to incident location
                OutlinedButton(
                    onClick = {
                        com.example.util.EmergencyActions.openRouteInGoogleMaps(context, userLocation, report.location)
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions", fontSize = 10.sp)
                }
            }
        }
    }
}
