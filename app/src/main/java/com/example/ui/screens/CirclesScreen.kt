package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SafetyMessage
import com.example.ui.SafeguardUiState
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import com.example.util.EmergencyActions
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CirclesScreen(
    viewModel: SafeguardViewModel,
    uiState: SafeguardUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val circleMessages by viewModel.circleMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var activeCircleId by remember { mutableStateOf("circle-family") }
    var messageInput by remember { mutableStateOf("") }

    val circles = listOf(
        Pair("circle-family", "Family Circle (Parents & Guardians)"),
        Pair("circle-transit", "Kumbakonam School Transit Watch"),
        Pair("circle-neighborhood", "Town Hall Neighborhood Patrol")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateLight)
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = SafeguardNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TRUSTED SAFETY CIRCLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyTeal,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Encrypted Community & Transit Check-ins",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Circle Tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(circles) { circle ->
                        val isSelected = activeCircleId == circle.first
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeCircleId = circle.first },
                            color = if (isSelected) SafetyTeal else SafeguardNavyCard
                        ) {
                            Text(
                                text = circle.second,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) SafeguardNavyDark else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick 1-Tap Safety Status Check-In Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Send message to circle...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_circle_message")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            viewModel.sendCircleMessage(
                                circleId = activeCircleId,
                                text = messageInput.trim(),
                                isQuickCheckIn = false
                            )
                            messageInput = ""
                        }
                    },
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
