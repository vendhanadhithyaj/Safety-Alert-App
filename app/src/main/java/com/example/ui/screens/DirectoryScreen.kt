package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.example.model.EmergencyContact
import com.example.ui.SafeguardViewModel
import com.example.ui.theme.*
import com.example.util.EmergencyActions

@Composable
fun DirectoryScreen(
    viewModel: SafeguardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background == DarkBackground
    val contacts by viewModel.emergencyContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "National", "Tamil Nadu Official", "Kumbakonam Local")

    val filteredContacts = contacts.filter {
        val matchesCategory = selectedCategory == "All" || it.category == selectedCategory
        val matchesQuery = searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.number.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = SafeguardNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 3.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal.copy(alpha = 0.5f)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.safeguard_official_logo),
                            contentDescription = "SAFEGUARD Emblem",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AUTHORITATIVE DIRECTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafetyTeal,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Tamil Nadu Emergency & Police Contacts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                Text(
                    text = "Verified 24/7 helplines with direct 1-tap dialer connection.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // High-Contrast Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search police, ambulance, women helpline...",
                            fontSize = 12.sp,
                            color = if (isDark) DarkTextSecondary else SlateTextMuted
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SafetyTeal) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) DarkSurface else Color.White,
                        unfocusedContainerColor = if (isDark) DarkSurface else Color.White,
                        focusedTextColor = if (isDark) DarkTextPrimary else SlateTextDark,
                        unfocusedTextColor = if (isDark) DarkTextPrimary else SlateTextDark,
                        focusedBorderColor = SafetyTeal,
                        unfocusedBorderColor = if (isDark) DarkBorder else SlateBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = {
                        Text(
                            cat,
                            fontSize = 11.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Contact Cards List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredContacts) { contact ->
                EmergencyContactCard(
                    contact = contact,
                    isDark = isDark,
                    onCall = { EmergencyActions.dialEmergency(context, contact.number) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContact,
    isDark: Boolean,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isDark) DarkBorder else SlateBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (contact.number) {
                            "112" -> if (isDark) Color(0xFF5C101B) else EmergencyRedLight
                            "1091", "181" -> if (isDark) Color(0xFF1E3A5F) else SafetyTealLight
                            "108" -> if (isDark) Color(0xFF5C101B) else EmergencyRedLight
                            else -> if (isDark) DarkSurfaceVariant else Color(0xFFE2E8F0)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (contact.number) {
                        "112" -> Icons.Default.Campaign
                        "1091", "181" -> Icons.Default.Security
                        "1098" -> Icons.Default.ChildCare
                        "108" -> Icons.Default.LocalHospital
                        else -> Icons.Default.LocalPolice
                    },
                    contentDescription = null,
                    tint = when (contact.number) {
                        "112" -> EmergencyRedBright
                        "1091", "181" -> SafetyTeal
                        "108" -> EmergencyRedBright
                        else -> if (isDark) SafetyTeal else SafeguardNavy
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else SafeguardNavy
                )
                Text(
                    text = contact.description,
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else SlateTextSecondary,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Dial: ${contact.number}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (contact.number == "112") EmergencyRedBright else SafetyTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDark) Color(0xFF064E3B) else SafeGreenLight
                    ) {
                        Text(
                            text = contact.verifiedDate,
                            fontSize = 10.sp,
                            color = if (isDark) SafeGreenLight else SafeGreenDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onCall,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (contact.number == "112") EmergencyRed else SafetyTealDark,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("button_call_${contact.number}")
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
