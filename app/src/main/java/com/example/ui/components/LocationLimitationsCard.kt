package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == DarkBackground

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, if (isDark) DarkBorder else SlateBorder, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        tint = SafetyTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Browser & OS Location Disclosures",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else SafeguardNavy
                        )
                        Text(
                            text = "Android Doze limits & GPS accuracy requirements",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) DarkTextSecondary else SlateTextSecondary
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (isDark) SafetyTeal else SlateTextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = if (isDark) DarkBorder else SlateBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    LimitationItem(
                        title = "1. Android OS Background Location Throttling",
                        desc = "From Android 8.0 and Android 10+, background GPS polling is throttled to save battery. For uninterrupted turn-by-turn emergency tracking, keep SAFEGUARD open or Foreground Service enabled.",
                        isDark = isDark
                    )
                    LimitationItem(
                        title = "2. Device Doze Mode & Deep Sleep",
                        desc = "When the phone is stationary, Android suspends non-essential background tasks until user movement or high-priority 112 emergency alerts wake the system.",
                        isDark = isDark
                    )
                    LimitationItem(
                        title = "3. Web & Browser Geolocation Restrictions",
                        desc = "Browser webviews require user consent per session and require secure HTTPS. Background geolocation in mobile browsers is terminated once tabs are minimized.",
                        isDark = isDark
                    )
                    LimitationItem(
                        title = "4. GPS Satellite vs Indoor Accuracy",
                        desc = "Hardware GPS satellites provide ±3m to ±8m accuracy in open areas. Indoors or in basements, devices rely on cellular/Wi-Fi triangulation with a ±40m to ±150m margin.",
                        isDark = isDark
                    )
                    LimitationItem(
                        title = "5. Emergency Transparency Guarantee",
                        desc = "Location data is processed exclusively for emergency SOS alerts, danger zone proximity warnings, and consent-based temporary sharing.",
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitationItem(title: String, desc: String, isDark: Boolean) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) SafetyTeal else PrimaryBlue
        )
        Text(
            text = desc,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = if (isDark) DarkTextSecondary else SlateTextDark,
            lineHeight = 15.sp
        )
    }
}
