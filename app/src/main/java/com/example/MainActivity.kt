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
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(themeMode = uiState.themeMode) {
                if (uiState.isEntranceVisible) {
                    EntranceScreen(
                        viewModel = viewModel,
                        onEnterApp = { viewModel.dismissEntrance() }
                    )
                } else {
                    SafeguardApp(viewModel)
                }
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SafeguardTopAppBar(
                currentPortal = uiState.currentPortal,
                themeMode = uiState.themeMode,
                onToggleTheme = { viewModel.toggleThemeMode() },
                onShowEntrance = { viewModel.showEntrance() },
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
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onShowEntrance: () -> Unit,
    onOpenPortalSwitch: () -> Unit
) {
    Surface(
        color = SafeguardNavy,
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding & Slogan (Clickable to view Entrance/About screen)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onShowEntrance)
                    .padding(4.dp)
                    .testTag("app_bar_brand_header")
            ) {
                // Official Safeguard Logo in a clean badge
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SafetyTeal.copy(alpha = 0.6f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.safeguard_official_logo),
                        contentDescription = "SAFEGUARD Emblem",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SAFEGUARD",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = EmergencyRed,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "TN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "One tap to alert. A network to protect.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1
                    )
                }
            }

            // Right Actions: Theme Toggle + Portal Switcher
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme Mode Switcher
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.DARK -> Icons.Default.LightMode
                            ThemeMode.LIGHT -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                        },
                        contentDescription = "Toggle Theme (Current: $themeMode)",
                        tint = when (themeMode) {
                            ThemeMode.DARK -> Color(0xFFFBBF24)
                            ThemeMode.LIGHT -> Color(0xFF38BDF8)
                            ThemeMode.SYSTEM -> Color.White
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Portal Switcher Pill Badge
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onOpenPortalSwitch)
                        .testTag("portal_switcher_badge"),
                    color = if (currentPortal == AppPortal.PEOPLE) SafetyTealDark else EmergencyRed,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (currentPortal == AppPortal.PEOPLE) Icons.Default.People else Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentPortal == AppPortal.PEOPLE) "PEOPLE" else "NETWORK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Portal",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
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
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

        NavigationBarItem(
            selected = selectedTab == PeopleTab.SOS,
            onClick = { onSelectTab(PeopleTab.SOS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "SOS Alert",
                    tint = if (selectedTab == PeopleTab.SOS) EmergencyRed else unselectedColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "SOS",
                    fontWeight = if (selectedTab == PeopleTab.SOS) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == PeopleTab.SOS) EmergencyRed else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    tint = if (selectedTab == PeopleTab.MAP) SafetyTeal else unselectedColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Map",
                    fontWeight = if (selectedTab == PeopleTab.MAP) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == PeopleTab.MAP) SafetyTeal else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                    tint = if (selectedTab == PeopleTab.REPORT) SafetyTeal else unselectedColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Report",
                    fontWeight = if (selectedTab == PeopleTab.REPORT) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == PeopleTab.REPORT) SafetyTeal else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                    tint = if (selectedTab == PeopleTab.CIRCLES) SafetyTeal else unselectedColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Circles",
                    fontWeight = if (selectedTab == PeopleTab.CIRCLES) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == PeopleTab.CIRCLES) SafetyTeal else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
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
                    tint = if (selectedTab == PeopleTab.DIRECTORY) SafetyTeal else unselectedColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            label = {
                Text(
                    text = "Directory",
                    fontWeight = if (selectedTab == PeopleTab.DIRECTORY) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (selectedTab == PeopleTab.DIRECTORY) SafetyTeal else unselectedColor
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.testTag("nav_item_directory")
        )
    }
}
