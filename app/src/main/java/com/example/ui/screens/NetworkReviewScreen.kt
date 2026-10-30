package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.NetworkTab
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NetworkReviewScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val incidents by viewModel.allIncidents.collectAsState()
    val reports by viewModel.communityReports.collectAsState()
    val dangerZones by viewModel.dangerZones.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var reviewTargetReport by remember { mutableStateOf<CommunityReport?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }
    var showRejectionDialog by remember { mutableStateOf(false) }

    var showDangerZoneDialog by remember { mutableStateOf(false) }
    var dangerZoneRadius by remember { mutableStateOf(300) }
    var zoneSeverity by remember { mutableStateOf(ZoneSeverity.HIGH) }

    var dispatchTargetIncident by remember { mutableStateOf<SosIncident?>(null) }
    var patrolNoteInput by remember { mutableStateOf("") }
    var showDispatchDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight)
    ) {
        // Official Safety Network Top Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = EmergencyRedDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.safeguard_official_logo),
                                contentDescription = "SAFEGUARD Official Logo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SAFETY NETWORK OPERATIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${currentUser.name} (${currentUser.responderBadgeId ?: "Badge #112"})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = currentUser.district,
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Network Sub-tabs
        TabRow(
            selectedTabIndex = uiState.selectedNetworkTab.ordinal,
            containerColor = Color.White,
            contentColor = EmergencyRed
        ) {
            Tab(
                selected = uiState.selectedNetworkTab == NetworkTab.DISPATCH,
                onClick = { viewModel.setNetworkTab(NetworkTab.DISPATCH) },
                text = { Text("SOS Dispatch (${incidents.count { it.status != SosStatus.RESOLVED }})", fontSize = 11.sp) }
            )
            Tab(
                selected = uiState.selectedNetworkTab == NetworkTab.VERIFICATION,
                onClick = { viewModel.setNetworkTab(NetworkTab.VERIFICATION) },
                text = { Text("Report Review (${reports.count { it.status == ReviewStatus.PENDING_REVIEW }})", fontSize = 11.sp) }
            )
            Tab(
                selected = uiState.selectedNetworkTab == NetworkTab.DANGER_ZONES,
                onClick = { viewModel.setNetworkTab(NetworkTab.DANGER_ZONES) },
                text = { Text("Danger Zones (${dangerZones.count { it.isActive }})", fontSize = 11.sp) }
            )
            Tab(
                selected = uiState.selectedNetworkTab == NetworkTab.AUDIT_LOGS,
                onClick = { viewModel.setNetworkTab(NetworkTab.AUDIT_LOGS) },
                text = { Text("Audit Trail", fontSize = 11.sp) }
            )
        }

        // Sub-tab content
        when (uiState.selectedNetworkTab) {
            NetworkTab.DISPATCH -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (incidents.isEmpty()) {
                        item {
                            EmptyState(message = "No active SOS alerts in regional queue. System normal.")
                        }
                    } else {
                        items(incidents) { inc ->
                            IncidentDispatchCard(
                                incident = inc,
                                onAcknowledge = {
                                    dispatchTargetIncident = inc
                                    patrolNoteInput = "Patrol Unit TN-49-G-112 dispatched to ${inc.location.landmark}."
                                    showDispatchDialog = true
                                },
                                onResolve = {
                                    viewModel.resolveIncident(inc.id, "Verified on site by patrol team. Citizen safe.")
                                },
                                onCall = {
                                    EmergencyActions.dialEmergency(context, inc.userPhone)
                                },
                                onMap = {
                                    EmergencyActions.openInGoogleMaps(context, inc.location)
                                }
                            )
                        }
                    }
                }
            }

            NetworkTab.VERIFICATION -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (reports.isEmpty()) {
                        item {
                            EmptyState(message = "No submitted community reports pending review.")
                        }
                    } else {
                        items(reports) { rep ->
                            ReviewQueueCard(
                                report = rep,
                                onMarkUnderReview = {
                                    viewModel.reviewReport(rep.id, ReviewStatus.UNDER_REVIEW)
                                },
                                onVerifyAndActivateZone = {
                                    reviewTargetReport = rep
                                    showDangerZoneDialog = true
                                },
                                onReject = {
                                    reviewTargetReport = rep
                                    rejectionReasonInput = ""
                                    showRejectionDialog = true
                                },
                                onRequestInfo = {
                                    viewModel.reviewReport(
                                        reportId = rep.id,
                                        newStatus = ReviewStatus.INFO_REQUESTED,
                                        rejectionReason = "Please provide clearer landmark details or specific road junction name."
                                    )
                                }
                            )
                        }
                    }
                }
            }

            NetworkTab.DANGER_ZONES -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dangerZones) { zone ->
                        DangerZoneAdminCard(
                            zone = zone,
                            onDeactivate = { viewModel.deactivateZone(zone.id) },
                            onOpenMap = { EmergencyActions.openInGoogleMaps(context, zone.location) }
                        )
                    }
                }
            }

            NetworkTab.AUDIT_LOGS -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(auditLogs) { log ->
                        AuditLogRow(log)
                    }
                }
            }
        }
    }

    // Acknowledge & Dispatch Dialog
    if (showDispatchDialog && dispatchTargetIncident != null) {
        val inc = dispatchTargetIncident!!
        AlertDialog(
            onDismissRequest = { showDispatchDialog = false },
            title = { Text("Acknowledge & Dispatch Patrol", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Assigning official unit to alert ${inc.id} (${inc.userName})", fontSize = 12.sp, color = SlateTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = patrolNoteInput,
                        onValueChange = { patrolNoteInput = it },
                        label = { Text("Responder Note & Patrol Vehicle") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.acknowledgeIncidentAsResponder(inc.id, patrolNoteInput)
                        showDispatchDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Confirm Dispatch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDispatchDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Reject Report Dialog (Mandatory reason required)
    if (showRejectionDialog && reviewTargetReport != null) {
        AlertDialog(
            onDismissRequest = { showRejectionDialog = false },
            title = { Text("Reject Report", fontWeight = FontWeight.Bold, color = EmergencyRed) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("A mandatory reason is required by security policy to reject community reports.", fontSize = 12.sp, color = SlateTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Reason for Rejection *") },
                        placeholder = { Text("e.g. Duplicate report / Inaccurate coordinates") },
                        modifier = Modifier.fillMaxWidth().testTag("input_rejection_reason")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonInput.isNotBlank()) {
                            viewModel.reviewReport(
                                reportId = reviewTargetReport!!.id,
                                newStatus = ReviewStatus.REJECTED,
                                rejectionReason = rejectionReasonInput.trim()
                            )
                            showRejectionDialog = false
                        }
                    },
                    enabled = rejectionReasonInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Danger Zone Activation Dialog
    if (showDangerZoneDialog && reviewTargetReport != null) {
        AlertDialog(
            onDismissRequest = { showDangerZoneDialog = false },
            title = { Text("Verify & Activate Public Danger Zone", fontWeight = FontWeight.Bold, color = SafeguardNavy) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This creates an official public danger zone on the SAFEGUARD radar.", fontSize = 12.sp, color = SlateTextMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Hazard: ${reviewTargetReport!!.title}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Location: ${reviewTargetReport!!.location.landmark}", fontSize = 11.sp, color = SlateTextMuted)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Radius (Meters): $dangerZoneRadius m", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = dangerZoneRadius.toFloat(),
                        onValueChange = { dangerZoneRadius = it.toInt() },
                        valueRange = 100f..1000f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reviewReport(
                            reportId = reviewTargetReport!!.id,
                            newStatus = ReviewStatus.VERIFIED,
                            createDangerZone = true,
                            radiusMeters = dangerZoneRadius,
                            severity = zoneSeverity
                        )
                        showDangerZoneDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark)
                ) {
                    Text("Verify & Activate Danger Zone")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDangerZoneDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SafeGreenDark, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 12.sp, color = SlateTextMuted)
        }
    }
}

@Composable
private fun IncidentDispatchCard(
    incident: SosIncident,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit,
    onCall: () -> Unit,
    onMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (incident.status == SosStatus.RESOLVED) SafeGreen else EmergencyRed, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (incident.status == SosStatus.RESOLVED) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (incident.status == SosStatus.RESOLVED) SafeGreenDark else EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = incident.id,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SafeguardNavy
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (incident.status) {
                        SosStatus.RESOLVED -> SafeGreenLight
                        SosStatus.ACKNOWLEDGED -> SafetyTealLight
                        else -> EmergencyRedLight
                    }
                ) {
                    Text(
                        text = incident.status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (incident.status) {
                            SosStatus.RESOLVED -> SafeGreenDark
                            SosStatus.ACKNOWLEDGED -> SafetyTealDark
                            else -> EmergencyRed
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text("Citizen: ${incident.userName} (${incident.userPhone})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Emergency Type: ${incident.emergencyType}", fontSize = 12.sp, color = EmergencyRedDark)
            Text("📍 Location: ${incident.location.landmark}", fontSize = 11.sp, color = SlateTextMuted)

            if (incident.acknowledgedBy != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Assigned: ${incident.acknowledgedBy}", fontSize = 10.sp, color = SafetyTealDark, fontWeight = FontWeight.Medium)
                if (incident.responderNotes != null) {
                    Text("Note: ${incident.responderNotes}", fontSize = 10.sp, color = SlateTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(onClick = onCall, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onMap, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Map", fontSize = 11.sp)
                }
                if (incident.status != SosStatus.RESOLVED && incident.status != SosStatus.ACKNOWLEDGED) {
                    Button(
                        onClick = onAcknowledge,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Acknowledge", fontSize = 11.sp)
                    }
                } else if (incident.status != SosStatus.RESOLVED) {
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Mark Resolved", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueueCard(
    report: CommunityReport,
    onMarkUnderReview: () -> Unit,
    onVerifyAndActivateZone: () -> Unit,
    onReject: () -> Unit,
    onRequestInfo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(report.id, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SafeguardNavy)
                Surface(
                    shape = RoundedCornerShape(4.dp),
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(report.description, fontSize = 11.sp, color = SlateTextMuted)
            Text("📍 ${report.location.landmark}", fontSize = 11.sp, color = SafeguardNavy)

            if (report.rejectionReason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reason / Feedback: ${report.rejectionReason}", fontSize = 10.sp, color = EmergencyRedDark)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (report.status == ReviewStatus.PENDING_REVIEW) {
                    OutlinedButton(onClick = onMarkUnderReview, modifier = Modifier.weight(1f)) {
                        Text("Under Review", fontSize = 10.sp)
                    }
                }
                Button(
                    onClick = onVerifyAndActivateZone,
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreenDark),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Verify & Zone", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    modifier = Modifier.weight(0.8f)
                ) {
                    Text("Reject", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun DangerZoneAdminCard(
    zone: DangerZone,
    onDeactivate: () -> Unit,
    onOpenMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (zone.isActive) EmergencyRed else SlateBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(zone.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SafeguardNavy)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (zone.isActive) EmergencyRedLight else SlateBorder
                ) {
                    Text(
                        text = if (zone.isActive) "ACTIVE" else "DEACTIVATED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (zone.isActive) EmergencyRed else SlateTextMuted,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Text("📍 ${zone.location.landmark} (${zone.radiusMeters}m radius)", fontSize = 11.sp, color = SlateTextMuted)
            Text("Approved by: ${zone.approvedByAuthority}", fontSize = 10.sp, color = SafetyTealDark)

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                    Text("Google Maps", fontSize = 11.sp)
                }
                if (zone.isActive) {
                    Button(
                        onClick = onDeactivate,
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deactivate Zone", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogRow(log: AuditLogEntry) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formatted = timeFormat.format(Date(log.timestamp))

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatted, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SafetyTealDark)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("${log.action} • ${log.actor}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SafeguardNavy)
                Text(log.details, fontSize = 10.sp, color = SlateTextMuted)
            }
        }
    }
}
