package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            .background(SlateLight)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = SafeguardNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                Text(
                    text = "All helplines verified with official government sources. One-tap direct call.",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search police, ambulance, women helpline...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SlateTextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category Filter Tabs
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
                    label = { Text(cat, fontSize = 11.sp) }
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
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (contact.number) {
                            "112" -> EmergencyRedLight
                            "1091", "181" -> SafetyTealLight
                            "1098" -> PrimaryBlue.copy(alpha = 0.15f)
                            "108" -> EmergencyRedLight
                            else -> SlateLight
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
                        "112" -> EmergencyRed
                        "1091", "181" -> SafetyTealDark
                        "1098" -> PrimaryBlue
                        "108" -> EmergencyRed
                        else -> SafeguardNavy
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafeguardNavy
                    )
                }
                Text(
                    text = contact.description,
                    fontSize = 11.sp,
                    color = SlateTextMuted,
                    lineHeight = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Number: ${contact.number}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(4.dp), color = SafeGreenLight) {
                        Text(
                            text = contact.verifiedDate,
                            fontSize = 9.sp,
                            color = SafeGreenDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Button(
                onClick = onCall,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (contact.number == "112") EmergencyRed else SafetyTealDark
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp).testTag("button_call_${contact.number}")
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
