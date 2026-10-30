package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LocationLimitationsCard(
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SafetyTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Browser & OS Location Limitations",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeguardNavy
                        )
                        Text(
                            text = "Background tracking rules & GPS accuracy disclosure",
                            fontSize = 10.sp,
                            color = SlateTextMuted
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = SlateTextMuted
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = SlateBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    LimitationItem(
                        title = "1. Android OS Background Location Throttling",
                        desc = "From Android 8.0 (Oreo) and Android 10+, Google limits background location polling to a few times per hour to preserve battery. For real-time updates, keep SAFEGUARD active or foreground-notified."
                    )
                    LimitationItem(
                        title = "2. Device Doze Mode & Deep Sleep",
                        desc = "When the screen is off and stationary, Android enters Doze Mode, suspending network and sensor operations until an explicit maintenance window or high-priority emergency alert arrives."
                    )
                    LimitationItem(
                        title = "3. Mobile Browser Geolocation Restrictions",
                        desc = "Web and browser versions restrict geolocation to active tabs and strictly forbid background tracking once minimized. Geolocation requires explicit per-session permission and secure HTTPS."
                    )
                    LimitationItem(
                        title = "4. GPS Satellite vs Indoors Accuracy",
                        desc = "Hardware GPS satellites provide ±3m to ±10m precision outdoors. Indoors, mobile devices fall back to Wi-Fi SSID and cellular triangulation with a wider ±50m to ±200m margin."
                    )
                    LimitationItem(
                        title = "5. Permission Transparency",
                        desc = "SAFEGUARD requests fine and coarse location solely for active emergency response, danger zone alerts, and consensual temporary sharing sessions."
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitationItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SafeguardNavy
        )
        Text(
            text = desc,
            fontSize = 10.sp,
            color = SlateTextDark,
            lineHeight = 13.sp
        )
    }
}
