package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.data.SafeguardRepository
import com.example.ui.*
import com.example.ui.components.PortalSwitchDialog
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: SafeguardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SafeguardApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeguardApp(viewModel: SafeguardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display status messages via Snackbar
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Back handling: If on secondary tab, return to SOS
    BackHandler(enabled = uiState.selectedPeopleTab != PeopleTab.SOS && uiState.currentPortal == AppPortal.PEOPLE) {
        viewModel.setPeopleTab(PeopleTab.SOS)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SafeguardTopAppBar(
                currentPortal = uiState.currentPortal,
                onOpenPortalSwitch = { viewModel.showPortalDialog(true) }
            )
        },
        floatingActionButton = {
            // Omnipresent Emergency SOS button when navigating secondary tabs
            if (uiState.currentPortal == AppPortal.PEOPLE && uiState.selectedPeopleTab != PeopleTab.SOS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.setPeopleTab(PeopleTab.SOS)
                        viewModel.startSosCountdown("Quick Alert from ${uiState.selectedPeopleTab.name} Screen")
                    },
                    containerColor = EmergencyRed,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier.testTag("floating_quick_sos_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Emergency SOS",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "QUICK SOS",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        fontSize = 13.sp
                    )
                }
            }
        },
        bottomBar = {
            if (uiState.currentPortal == AppPortal.PEOPLE) {
                SafeguardBottomNavigation(
                    selectedTab = uiState.selectedPeopleTab,
                    onSelectTab = { viewModel.setPeopleTab(it) }
                )
            } else {
                // Safety Network Portal Bottom Bar
                Surface(
                    color = SafeguardNavyDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmergencyRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OFFICIAL SAFETY NETWORK MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.setPortal(AppPortal.PEOPLE)
                                SafeguardRepository.switchRole(com.example.model.UserRole.PUBLIC)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("button_exit_network_portal")
                        ) {
                            Text("Switch to People", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentPortal) {
                AppPortal.PEOPLE -> {
                    when (uiState.selectedPeopleTab) {
                        PeopleTab.SOS -> SosScreen(viewModel, uiState)
                        PeopleTab.MAP -> MapScreen(viewModel, uiState)
                        PeopleTab.REPORT -> ReportScreen(viewModel, uiState)
                        PeopleTab.CIRCLES -> CirclesScreen(viewModel, uiState)
                        PeopleTab.DIRECTORY -> DirectoryScreen(viewModel)
                    }
                }
                AppPortal.SAFETY_NETWORK -> {
                    NetworkReviewScreen(viewModel, uiState)
                }
            }
        }
    }

    // Portal Switch Dialog (Authentication Gate)
    if (uiState.showPortalSwitchDialog) {
        PortalSwitchDialog(
            currentPortal = uiState.currentPortal,
            onSelectPortal = { portal, role, badge ->
                viewModel.setPortal(portal)
                SafeguardRepository.switchRole(role, badge)
                viewModel.showPortalDialog(false)
            },
            onDismiss = { viewModel.showPortalDialog(false) }
        )
    }
}

@Composable
fun SafeguardTopAppBar(
    currentPortal: AppPortal,
    onOpenPortalSwitch: () -> Unit
) {
    Surface(
        color = SafeguardNavy,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Slogan
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Official Safeguard Logo Drawable
                Image(
                    painter = painterResource(id = R.drawable.safeguard_logo),
                    contentDescription = "SAFEGUARD Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }

            // Portal Switcher Pill Badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpenPortalSwitch)
                    .testTag("portal_switcher_badge"),
                color = if (currentPortal == AppPortal.PEOPLE) SafetyTealDark else EmergencyRed
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentPortal == AppPortal.PEOPLE) Icons.Default.People else Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentPortal == AppPortal.PEOPLE) "PEOPLE" else "NETWORK",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Switch Portal",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SafeguardBottomNavigation(
    selectedTab: PeopleTab,
    onSelectTab: (PeopleTab) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = SafeguardNavy,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = selectedTab == PeopleTab.SOS,
            onClick = { onSelectTab(PeopleTab.SOS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "SOS Alert",
                    tint = if (selectedTab == PeopleTab.SOS) EmergencyRed else SlateTextMuted
                )
            },
            label = {
                Text(
                    text = "SOS",
                    fontWeight = if (selectedTab == PeopleTab.SOS) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = EmergencyRedLight
            ),
            modifier = Modifier.testTag("nav_item_sos")
        )

        NavigationBarItem(
            selected = selectedTab == PeopleTab.MAP,
            onClick = { onSelectTab(PeopleTab.MAP) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Radar Map",
                    tint = if (selectedTab == PeopleTab.MAP) SafetyTealDark else SlateTextMuted
                )
            },
            label = {
                Text(
                    text = "Map",
                    fontWeight = if (selectedTab == PeopleTab.MAP) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = SafetyTealLight
            ),
            modifier = Modifier.testTag("nav_item_map")
        )

        NavigationBarItem(
            selected = selectedTab == PeopleTab.REPORT,
            onClick = { onSelectTab(PeopleTab.REPORT) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = "Report Hazard",
                    tint = if (selectedTab == PeopleTab.REPORT) PrimaryBlue else SlateTextMuted
                )
            },
            label = {
                Text(
                    text = "Report",
                    fontWeight = if (selectedTab == PeopleTab.REPORT) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = SafetyTealLight
            ),
            modifier = Modifier.testTag("nav_item_report")
        )

        NavigationBarItem(
            selected = selectedTab == PeopleTab.CIRCLES,
            onClick = { onSelectTab(PeopleTab.CIRCLES) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = "Safety Circles",
                    tint = if (selectedTab == PeopleTab.CIRCLES) PrimaryBlue else SlateTextMuted
                )
            },
            label = {
                Text(
                    text = "Circles",
                    fontWeight = if (selectedTab == PeopleTab.CIRCLES) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = SafetyTealLight
            ),
            modifier = Modifier.testTag("nav_item_circles")
        )

        NavigationBarItem(
            selected = selectedTab == PeopleTab.DIRECTORY,
            onClick = { onSelectTab(PeopleTab.DIRECTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.ContactPhone,
                    contentDescription = "Emergency Directory",
                    tint = if (selectedTab == PeopleTab.DIRECTORY) PrimaryBlue else SlateTextMuted
                )
            },
            label = {
                Text(
                    text = "Directory",
                    fontWeight = if (selectedTab == PeopleTab.DIRECTORY) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = SafetyTealLight
            ),
            modifier = Modifier.testTag("nav_item_directory")
        )
    }
}
