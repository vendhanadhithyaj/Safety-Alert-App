package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UserRole
import com.example.ui.AppPortal
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EntranceScreen(
    viewModel: SafeguardViewModel,
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var autoEnterProgress by remember { mutableFloatStateOf(1f) }

    // Subtle breathing pulse animation for the beacon & logo
    val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Optional 4-second auto-enter countdown with skip affordance
    LaunchedEffect(Unit) {
        val totalSteps = 40
        for (i in 0..totalSteps) {
            delay(100)
            autoEnterProgress = 1f - (i.toFloat() / totalSteps)
        }
        onEnterApp()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SafeguardNavy,
                        SafeguardNavySurface,
                        Color(0xFF060D18)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Skip button & Competition Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
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
                            text = "TAMIL NADU SAFETY GRID",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                TextButton(
                    onClick = onEnterApp,
                    modifier = Modifier.testTag("button_skip_entrance")
                ) {
                    Text(
                        text = "Skip Intro",
                        color = SafetyTeal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Skip",
                        tint = SafetyTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero Section: Crisp SAFEGUARD Logo with Animated Pulse Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
                ) {
                    // Outer ambient glowing halo
                    Box(
                        modifier = Modifier
                            .size(230.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        SafetyTeal.copy(alpha = 0.25f),
                                        EmergencyRed.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Logo Card container with clean white background so the official logo colors pop vividly
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(170.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, SafetyTeal.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                        color = Color.White,
                        shadowElevation = 12.dp
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.safeguard_logo),
                            contentDescription = "Official SAFEGUARD Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("entrance_safeguard_logo")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mission & Slogan
                Text(
                    text = "SAFEGUARD",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "EMERGENCY & COMMUNITY SAFETY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SafetyTeal,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Official Slogan in prominent high-contrast container
                Surface(
                    color = SafeguardNavyCard,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = EmergencyRedBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "One tap to alert. A network to protect.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Kumbakonam & Tamil Nadu State Innovation Project",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Real-Time Grid Status Indicators
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SafeguardNavyCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E4B75))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SAFETY NETWORK STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SafetyTeal,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GPS Location Engine", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Text("ACTIVE", fontSize = 12.sp, color = SafeGreen, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = SafetyTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buddy SOS Dispatch", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Text("SYNCHRONIZED", fontSize = 12.sp, color = SafetyTeal, fontWeight = FontWeight.ExtraBold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = EmergencyRedBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency 112 Direct", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                        Text("STANDBY", fontSize = 12.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Enter App or Instant SOS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Enter App button
                Button(
                    onClick = onEnterApp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_enter_safeguard"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SafetyTealDark,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "ENTER SAFEGUARD",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Quick Official Portal Switch
                OutlinedButton(
                    onClick = {
                        viewModel.setPortal(AppPortal.SAFETY_NETWORK)
                        com.example.data.SafeguardRepository.switchRole(UserRole.POLICE, "TN-OFFICER-77")
                        onEnterApp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("button_entrance_responder_login"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmergencyRedBright, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Responder Login (Police / Fire / Admin)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Linear auto-enter progress
                LinearProgressIndicator(
                    progress = { autoEnterProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SafetyTeal,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}
