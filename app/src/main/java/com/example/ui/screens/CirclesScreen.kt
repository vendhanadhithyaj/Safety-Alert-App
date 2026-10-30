package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.components.LocationPermissionCard
import com.example.ui.components.rememberLocationPermissionLauncher
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import com.example.util.LocationHelper
import java.text.SimpleDateFormat
import java.util.*

enum class CirclesTabMode {
    BUDDY_GROUP,
    COMMUNITY_CIRCLES
}

@Composable
fun CirclesScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val circleMessages by viewModel.circleMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val buddies by viewModel.buddies.collectAsState()
    val incomingAlert by viewModel.incomingBuddySosAlert.collectAsState()

    var selectedMode by remember { mutableStateOf(CirclesTabMode.BUDDY_GROUP) }
    var activeCircleId by remember { mutableStateOf("circle-family") }
    var messageInput by remember { mutableStateOf("") }

    var showInviteDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    val requestLocationPermission = rememberLocationPermissionLauncher { granted, _ ->
        viewModel.onLocationPermissionResult(granted)
    }

    val communityCircles = listOf(
        Pair("circle-family", "Family Circle (Parents & Guardians)"),
        Pair("circle-transit", "Kumbakonam School Transit Watch"),
        Pair("circle-neighborhood", "Town Hall Neighborhood Patrol")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header with Segmented Tab Switcher
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
                    Column {
                        Text(
                            text = "TRUSTED SAFETY NETWORK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyTeal,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (selectedMode == CirclesTabMode.BUDDY_GROUP) "Buddy Guardian Group" else "Community Circles",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Background Service Status Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                if (uiState.isForegroundServiceRunning) {
                                    viewModel.stopForegroundLocationService()
                                } else {
                                    viewModel.startForegroundLocationService()
                                }
                            },
                        color = if (uiState.isForegroundServiceRunning) SafeGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isForegroundServiceRunning) SafeGreen else Color.LightGray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isForegroundServiceRunning) "BG Tracking ON" else "BG Standby",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isForegroundServiceRunning) SafeGreenLight else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SafeguardNavyCard)
                        .padding(3.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedMode = CirclesTabMode.BUDDY_GROUP }
                            .testTag("tab_buddy_group"),
                        color = if (selectedMode == CirclesTabMode.BUDDY_GROUP) SafetyTeal else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (selectedMode == CirclesTabMode.BUDDY_GROUP) SafeguardNavyDark else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Buddy Group (${buddies.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == CirclesTabMode.BUDDY_GROUP) SafeguardNavyDark else Color.White
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedMode = CirclesTabMode.COMMUNITY_CIRCLES }
                            .testTag("tab_community_circles"),
                        color = if (selectedMode == CirclesTabMode.COMMUNITY_CIRCLES) SafetyTeal else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (selectedMode == CirclesTabMode.COMMUNITY_CIRCLES) SafeguardNavyDark else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Community Feed",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == CirclesTabMode.COMMUNITY_CIRCLES) SafeguardNavyDark else Color.White
                            )
                        }
                    }
                }
            }
        }

        // Location Permission Banner if not granted
        LocationPermissionCard(
            hasPermission = uiState.hasLocationPermission,
            onRequestPermission = requestLocationPermission
        )

        // Incoming Buddy SOS Emergency Banner (if active)
        if (incomingAlert != null) {
            IncomingBuddySosCard(
                alert = incomingAlert!!,
                currentLocation = uiState.currentLocation,
                onAcknowledge = { type ->
                    viewModel.acknowledgeBuddyAlert(incomingAlert!!.incidentId, type)
                },
                onNavigate = {
                    EmergencyActions.openRouteInGoogleMaps(context, uiState.currentLocation, incomingAlert!!.location)
                },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${incomingAlert!!.victimPhone}"))
                    context.startActivity(intent)
                },
                onDismiss = { viewModel.dismissBuddySosAlert() }
            )
        }

        // Mode Content
        when (selectedMode) {
            CirclesTabMode.BUDDY_GROUP -> {
                BuddyGroupContent(
                    buddies = buddies,
                    isBgRunning = uiState.isForegroundServiceRunning,
                    onOpenInvite = { showInviteDialog = true },
                    onOpenJoin = { showJoinDialog = true },
                    onSimulateSos = { viewModel.simulateIncomingBuddySos() },
                    onToggleBgService = {
                        if (uiState.isForegroundServiceRunning) {
                            viewModel.stopForegroundLocationService()
                        } else {
                            viewModel.startForegroundLocationService()
                        }
                    },
                    onToggleSos = { viewModel.toggleBuddySosPrivilege(it) },
                    onToggleLocation = { viewModel.toggleBuddyLiveLocation(it) },
                    onRemove = { viewModel.removeBuddy(it) },
                    modifier = Modifier.weight(1f)
                )
            }
            CirclesTabMode.COMMUNITY_CIRCLES -> {
                CommunityCirclesContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    communityCircles = communityCircles,
                    activeCircleId = activeCircleId,
                    onSelectCircle = { activeCircleId = it },
                    circleMessages = circleMessages,
                    currentUser = currentUser,
                    messageInput = messageInput,
                    onMessageChange = { messageInput = it },
                    onSendMessage = {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendCircleMessage(
                                circleId = activeCircleId,
                                text = messageInput.trim(),
                                isQuickCheckIn = false
                            )
                            messageInput = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Invite Buddy Dialog
    if (showInviteDialog) {
        InviteBuddyDialog(
            onDismiss = { showInviteDialog = false },
            onSendInvite = { name, phone, relation ->
                viewModel.inviteBuddy(name, phone, relation)
                showInviteDialog = false
            }
        )
    }

    // Join via Code Dialog
    if (showJoinDialog) {
        JoinBuddyDialog(
            onDismiss = { showJoinDialog = false },
            onAcceptCode = { code ->
                viewModel.acceptBuddyInvite(code)
                showJoinDialog = false
            }
        )
    }
}

// ---------------------------------------------------------
// BUDDY GROUP TAB CONTENT
// ---------------------------------------------------------
@Composable
private fun BuddyGroupContent(
    buddies: List<BuddyMember>,
    isBgRunning: Boolean,
    onOpenInvite: () -> Unit,
    onOpenJoin: () -> Unit,
    onSimulateSos: () -> Unit,
    onToggleBgService: () -> Unit,
    onToggleSos: (String) -> Unit,
    onToggleLocation: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Buddy System Explanation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SafetyTealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Diversity3, contentDescription = null, tint = SafetyTealDark, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SAFEGUARD BUDDY SYSTEM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafetyTealDark,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Your Personal First-Responder Circle",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeguardNavy
                            )
                        }
                    }

                    Text(
                        text = "When you trigger SOS, your Buddies receive an immediate loud alarm, live GPS coordinates, battery level, and 1-tap Google Maps directions to reach you in Kumbakonam.",
                        fontSize = 11.sp,
                        color = SlateTextDark,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenInvite,
                            colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("button_invite_buddy")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Invite Buddy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenJoin,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("button_join_with_code")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = SafeguardNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enter Code", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeguardNavy)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Innovation Competition Demo Simulation Button
                    OutlinedButton(
                        onClick = onSimulateSos,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("button_simulate_buddy_sos")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate Inbound Buddy SOS (Competition Demo)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                    }
                }
            }
        }

        // Real Location Service Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isBgRunning) SafeGreenLight else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isBgRunning) SafeGreen else SlateBorder)
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
                            imageVector = if (isBgRunning) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                            contentDescription = null,
                            tint = if (isBgRunning) SafeGreenDark else SlateTextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBgRunning) "Foreground GPS Service Running" else "Background Guardian Stopped",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SafeguardNavy
                            )
                            Text(
                                text = if (isBgRunning) "Live coordinates shared continuously even when phone is locked" else "Tap toggle to keep GPS alive in background",
                                fontSize = 10.sp,
                                color = SlateTextDark,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBgRunning,
                        onCheckedChange = { onToggleBgService() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SafeGreenDark
                        ),
                        modifier = Modifier.testTag("switch_background_location_service")
                    )
                }
            }
        }

        // Buddies List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ACTIVE GUARDIANS & BUDDIES (${buddies.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Auto-notified on SOS",
                    fontSize = 10.sp,
                    color = SafetyTealDark,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Buddy Cards
        items(buddies) { buddy ->
            BuddyCard(
                buddy = buddy,
                onToggleSos = { onToggleSos(buddy.id) },
                onToggleLocation = { onToggleLocation(buddy.id) },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${buddy.phone}"))
                    context.startActivity(intent)
                },
                onShareCode = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Join my SAFEGUARD emergency safety buddy group with code: ${buddy.inviteCode ?: "SG-TN-0000"}")
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Buddy Invite Code"))
                },
                onRemove = { onRemove(buddy.id) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ---------------------------------------------------------
// BUDDY ITEM CARD
// ---------------------------------------------------------
@Composable
private fun BuddyCard(
    buddy: BuddyMember,
    onToggleSos: () -> Unit,
    onToggleLocation: () -> Unit,
    onCall: () -> Unit,
    onShareCode: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when (buddy.relation) {
                                    BuddyRelation.PARENT -> PrimaryBlueLight
                                    BuddyRelation.CLASSMATE -> SafetyTealLight
                                    BuddyRelation.TEACHER -> WarningAmberLight
                                    else -> SlateBorder.copy(alpha = 0.3f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buddy.name.take(2).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = SafeguardNavy
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buddy.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SafeguardNavy
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (buddy.status) {
                                    BuddyStatus.ACTIVE -> SafeGreenLight
                                    BuddyStatus.PENDING_INVITE -> WarningAmberLight
                                    BuddyStatus.INVITATION_RECEIVED -> SafetyTealLight
                                }
                            ) {
                                Text(
                                    text = buddy.status.label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (buddy.status) {
                                        BuddyStatus.ACTIVE -> SafeGreenDark
                                        BuddyStatus.PENDING_INVITE -> WarningAmber
                                        BuddyStatus.INVITATION_RECEIVED -> SafetyTealDark
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${buddy.relation.label} • ${buddy.phone}",
                            fontSize = 11.sp,
                            color = SlateTextMuted
                        )
                        Text(
                            text = buddy.lastCheckIn,
                            fontSize = 10.sp,
                            color = SlateTextDark
                        )
                    }
                }

                // Call action
                IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = SafetyTealDark, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Privileges & Permissions Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = buddy.isTrustedForSos,
                        onClick = onToggleSos,
                        label = { Text("Loud SOS Siren", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                if (buddy.isTrustedForSos) Icons.Default.Campaign else Icons.Default.VolumeOff,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmergencyRedLight,
                            selectedLabelColor = EmergencyRedDark
                        )
                    )

                    FilterChip(
                        selected = buddy.canViewLiveLocation,
                        onClick = onToggleLocation,
                        label = { Text("GPS Access", fontSize = 10.sp) },
                        leadingIcon = {
                            Icon(
                                if (buddy.canViewLiveLocation) Icons.Default.LocationOn else Icons.Default.LocationOff,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafetyTealLight,
                            selectedLabelColor = SafetyTealDark
                        )
                    )
                }

                if (buddy.status == BuddyStatus.PENDING_INVITE) {
                    TextButton(onClick = onShareCode, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text("Share Code", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                    }
                } else {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = SlateTextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// INCOMING BUDDY SOS EMERGENCY BANNER
// ---------------------------------------------------------
@Composable
private fun IncomingBuddySosCard(
    alert: BuddySosAlert,
    currentLocation: LocationPoint,
    onAcknowledge: (String) -> Unit,
    onNavigate: () -> Unit,
    onCall: () -> Unit,
    onDismiss: () -> Unit
) {
    val distanceMeters = LocationHelper.distanceInMeters(
        currentLocation.latitude,
        currentLocation.longitude,
        alert.location.latitude,
        alert.location.longitude
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseSos")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sosBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .border(2.dp, EmergencyRed.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
            .testTag("card_incoming_buddy_sos"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmergencyRedLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmergencyRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "🚨 BUDDY EMERGENCY SOS DISPATCHED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmergencyRedDark,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${alert.victimName} needs immediate assistance!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeguardNavy
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = SlateTextDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "📍 Location: ${alert.location.landmark}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SafeguardNavy
                    )
                    Text(
                        text = "📏 Distance: ${"%.2f".format(distanceMeters / 1000.0)} km from you • Lat: ${"%.4f".format(alert.location.latitude)}, Lng: ${"%.4f".format(alert.location.longitude)}",
                        fontSize = 11.sp,
                        color = SlateTextDark
                    )
                    Text(
                        text = "⚠️ Alert: ${alert.emergencyType} • 🔋 Battery: ${alert.batteryPercent}%",
                        fontSize = 11.sp,
                        color = EmergencyRedDark,
                        fontWeight = FontWeight.Medium
                    )

                    if (alert.isAcknowledged) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✅ Your response: ${alert.responseType ?: "Acknowledged"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreenDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { onAcknowledge("I am on my way!") },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.2f).height(38.dp).testTag("button_buddy_on_my_way")
                ) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I'm On My Way", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigate,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.1f).height(38.dp).testTag("button_buddy_navigate_route")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Route Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCall,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(0.9f).height(38.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmergencyRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmergencyRed)
                }
            }
        }
    }
}

// ---------------------------------------------------------
// INVITE BUDDY DIALOG
// ---------------------------------------------------------
@Composable
private fun InviteBuddyDialog(
    onDismiss: () -> Unit,
    onSendInvite: (name: String, phone: String, relation: BuddyRelation) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf(BuddyRelation.PARENT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Invite Trusted Buddy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SafeguardNavy)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Buddies form your private safety group. When you trigger SOS, they receive audible alerts and live GPS tracking.",
                    fontSize = 11.sp,
                    color = SlateTextDark
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Buddy Name (e.g. Appa / Sister)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_buddy_name")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+91 ...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_buddy_phone")
                )

                Text("RELATIONSHIP:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateTextMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BuddyRelation.values()) { rel ->
                        FilterChip(
                            selected = relation == rel,
                            onClick = { relation = rel },
                            label = { Text(rel.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSendInvite(name.trim(), phone.trim(), relation)
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                modifier = Modifier.testTag("button_submit_buddy_invite")
            ) {
                Text("Send Invite", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextMuted)
            }
        }
    )
}

// ---------------------------------------------------------
// JOIN VIA CODE DIALOG
// ---------------------------------------------------------
@Composable
private fun JoinBuddyDialog(
    onDismiss: () -> Unit,
    onAcceptCode: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Join Buddy Safety Group", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SafeguardNavy)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter the 6-character Buddy Invitation Code shared by your family member or classmate (e.g., SG-TN-8890).",
                    fontSize = 12.sp,
                    color = SlateTextDark
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Invitation Code (e.g. SG-TN-8890)") },
                    singleLine = true,
                    placeholder = { Text("SG-TN-XXXX") },
                    modifier = Modifier.fillMaxWidth().testTag("input_buddy_code")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onAcceptCode(code.trim())
                    }
                },
                enabled = code.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SafetyTealDark),
                modifier = Modifier.testTag("button_confirm_join_code")
            ) {
                Text("Accept & Connect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextMuted)
            }
        }
    )
}

// ---------------------------------------------------------
// COMMUNITY FEED CONTENT
// ---------------------------------------------------------
@Composable
private fun CommunityCirclesContent(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    communityCircles: List<Pair<String, String>>,
    activeCircleId: String,
    onSelectCircle: (String) -> Unit,
    circleMessages: List<SafetyMessage>,
    currentUser: UserProfile,
    messageInput: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Circle Selector Pills
        LazyRow(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(communityCircles) { circle ->
                val isSelected = activeCircleId == circle.first
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectCircle(circle.first) },
                    color = if (isSelected) SafetyTeal else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) SafetyTealDark else SlateBorder)
                ) {
                    Text(
                        text = circle.second,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) SafeguardNavyDark else SlateTextDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Quick 1-Tap Safety Check-in Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "1-TAP QUICK SAFETY CHECK-INS:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateTextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        QuickCheckInChip(
                            text = "Reached Safely",
                            color = SafeGreenDark,
                            bgColor = SafeGreenLight,
                            icon = Icons.Default.CheckCircle,
                            onClick = {
                                viewModel.sendCircleMessage(
                                    circleId = activeCircleId,
                                    text = "I reached destination safely. Location verified.",
                                    isQuickCheckIn = true,
                                    locationTag = uiState.currentLocation.landmark
                                )
                            }
                        )
                    }
                    item {
                        QuickCheckInChip(
                            text = "Bus Delayed, Safe",
                            color = WarningAmber,
                            bgColor = WarningAmberLight,
                            icon = Icons.Default.DirectionsBus,
                            onClick = {
                                viewModel.sendCircleMessage(
                                    circleId = activeCircleId,
                                    text = "School bus delayed near bypass, but I am safe inside.",
                                    isQuickCheckIn = true,
                                    locationTag = uiState.currentLocation.landmark
                                )
                            }
                        )
                    }
                    item {
                        QuickCheckInChip(
                            text = "Walking Home",
                            color = SafetyTealDark,
                            bgColor = SafetyTealLight,
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                            onClick = {
                                viewModel.sendCircleMessage(
                                    circleId = activeCircleId,
                                    text = "Walking home now accompanied by classmates.",
                                    isQuickCheckIn = true,
                                    locationTag = uiState.currentLocation.landmark
                                )
                            }
                        )
                    }
                    item {
                        QuickCheckInChip(
                            text = "Please Call Me",
                            color = EmergencyRed,
                            bgColor = EmergencyRedLight,
                            icon = Icons.AutoMirrored.Filled.PhoneCallback,
                            onClick = {
                                viewModel.sendCircleMessage(
                                    circleId = activeCircleId,
                                    text = "Please call me on phone when you see this.",
                                    isQuickCheckIn = true,
                                    locationTag = uiState.currentLocation.landmark
                                )
                            }
                        )
                    }
                }
            }
        }

        // Message Feed
        val filteredMessages = circleMessages.filter { it.circleId == activeCircleId }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredMessages) { msg ->
                MessageBubble(msg = msg, isMine = msg.senderName.contains("You") || msg.senderName == currentUser.name)
            }
        }

        // Bottom Message Composer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = onMessageChange,
                    placeholder = { Text("Send message to circle...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_circle_message")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSendMessage,
                    modifier = Modifier
                        .size(44.dp)
                        .background(SafetyTealDark, CircleShape)
                        .testTag("button_send_circle_message")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickCheckInChip(
    text: String,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun MessageBubble(msg: SafetyMessage, isMine: Boolean) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(msg.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = msg.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMine) SafetyTealDark else SafeguardNavy
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formattedTime,
                fontSize = 9.sp,
                color = SlateTextMuted
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMine) 14.dp else 2.dp,
                bottomEnd = if (isMine) 2.dp else 14.dp
            ),
            color = if (msg.isQuickCheckIn) SafeGreenLight else if (isMine) SafetyTealLight else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (msg.isQuickCheckIn) SafeGreen else SlateBorder),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (msg.isQuickCheckIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = SafeGreenDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("QUICK SAFETY CHECK-IN", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = SafeGreenDark)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = msg.message,
                    fontSize = 12.sp,
                    color = SlateTextDark,
                    lineHeight = 15.sp
                )

                if (msg.locationTag != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 ${msg.locationTag}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = SafeguardNavy
                    )
                }
            }
        }
    }
}
