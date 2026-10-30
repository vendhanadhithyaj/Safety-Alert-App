package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserRole
import com.example.ui.AppPortal
import com.example.ui.theme.*

@Composable
fun PortalSwitchDialog(
    currentPortal: AppPortal,
    onSelectPortal: (AppPortal, UserRole, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPortal by remember { mutableStateOf(currentPortal) }
    var responderRole by remember { mutableStateOf(UserRole.RESPONDER) }
    var authCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.safeguard_official_logo),
                        contentDescription = "SAFEGUARD Emblem",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "SAFEGUARD Portals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = SafeguardNavy
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select your authenticated portal. Officials and responders require authorized credentials.",
                    fontSize = 13.sp,
                    color = SlateTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // People Portal Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selectedPortal == AppPortal.PEOPLE) 2.dp else 1.dp,
                            color = if (selectedPortal == AppPortal.PEOPLE) SafetyTealDark else SlateBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedPortal = AppPortal.PEOPLE
                            errorMessage = null
                        }
                        .testTag("portal_option_people"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPortal == AppPortal.PEOPLE) SafetyTealLight.copy(alpha = 0.4f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "People",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PEOPLE PORTAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SafeguardNavy
                            )
                            Text(
                                text = "Students, Women, Parents & Citizens. Trigger SOS, view verified danger zones & report hazards.",
                                fontSize = 12.sp,
                                color = SlateTextMuted
                            )
                        }
                        if (selectedPortal == AppPortal.PEOPLE) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = SafetyTealDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Safety Network Portal Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (selectedPortal == AppPortal.SAFETY_NETWORK) 2.dp else 1.dp,
                            color = if (selectedPortal == AppPortal.SAFETY_NETWORK) EmergencyRed else SlateBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedPortal = AppPortal.SAFETY_NETWORK
                            errorMessage = null
                        }
                        .testTag("portal_option_network"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedPortal == AppPortal.SAFETY_NETWORK) EmergencyRedLight.copy(alpha = 0.4f) else Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmergencyRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Network",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SAFETY NETWORK PORTAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmergencyRedDark
                            )
                            Text(
                                text = "Tamil Nadu Police, Fire/Rescue & Verified Admins. Dispatch queue, review reports & activate danger zones.",
                                fontSize = 12.sp,
                                color = SlateTextMuted
                            )
                        }
                        if (selectedPortal == AppPortal.SAFETY_NETWORK) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = EmergencyRed
                            )
                        }
                    }
                }

                // If Safety Network is selected, require authorization credential
                if (selectedPortal == AppPortal.SAFETY_NETWORK) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Official Credential Verification",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = SafeguardNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = authCode,
                        onValueChange = { authCode = it; errorMessage = null },
                        label = { Text("Security Passcode / Badge Token") },
                        placeholder = { Text("e.g. TN-POLICE-112") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SlateTextMuted)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("responder_auth_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Quick Demo Credentials (Tap to fill):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateTextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                authCode = "TN-POLICE-112"
                                responderRole = UserRole.RESPONDER
                            },
                            label = { Text("TN Police", fontSize = 11.sp) }
                        )
                        AssistChip(
                            onClick = {
                                authCode = "ADMIN-SAFEGUARD"
                                responderRole = UserRole.ADMIN
                            },
                            label = { Text("District Admin", fontSize = 11.sp) }
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = EmergencyRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPortal == AppPortal.PEOPLE) {
                        onSelectPortal(AppPortal.PEOPLE, UserRole.PUBLIC, null)
                        onDismiss()
                    } else {
                        // Validate credential
                        val trimmed = authCode.trim().uppercase()
                        if (trimmed == "TN-POLICE-112" || trimmed == "POLICE" || trimmed == "112") {
                            onSelectPortal(AppPortal.SAFETY_NETWORK, UserRole.RESPONDER, "TN-POL-KUM-112")
                            onDismiss()
                        } else if (trimmed == "ADMIN-SAFEGUARD" || trimmed == "ADMIN") {
                            onSelectPortal(AppPortal.SAFETY_NETWORK, UserRole.ADMIN, "TN-GOV-ADMIN-01")
                            onDismiss()
                        } else {
                            errorMessage = "Invalid credentials. Use 'TN-POLICE-112' or 'ADMIN-SAFEGUARD' for authorized access."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedPortal == AppPortal.PEOPLE) PrimaryBlue else EmergencyRed
                ),
                modifier = Modifier.testTag("portal_confirm_button")
            ) {
                Text("Enter Portal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateTextMuted)
            }
        }
    )
}
