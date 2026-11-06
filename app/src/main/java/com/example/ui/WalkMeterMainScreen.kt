package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.TrackingState
import com.example.ui.components.DigitalDistanceDisplay
import com.example.ui.components.GpsStatusBadge
import com.example.ui.components.LiveRouteCanvas
import com.example.ui.components.WalkControlsBar
import com.example.ui.components.WalkHistoryScreen
import com.example.ui.components.WalkMetricsGrid
import com.example.ui.components.WalkSettingsScreen
import com.example.ui.components.WalkSummaryModal
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.WalkMeterViewModel
import kotlinx.coroutines.launch

@Composable
fun WalkMeterMainScreen(
    viewModel: WalkMeterViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Screen Awake Handler
    DisposableEffect(uiState.trackingState, uiState.keepScreenOn) {
        val window = (context as? Activity)?.window
        val shouldKeepAwake = uiState.keepScreenOn &&
                (uiState.trackingState == TrackingState.TRACKING || uiState.trackingState == TrackingState.PAUSED)
        if (shouldKeepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Refresh GPS status periodically
    LaunchedEffect(Unit) {
        viewModel.checkGpsStatus()
    }

    // Permissions Launcher
    val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val fineLocationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.startTracking(isSimulation = false)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission is required for GPS tracking.")
            }
        }
    }

    fun onStartRequested() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.startTracking(isSimulation = false)
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextSecondary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Meter"
                        )
                    },
                    label = { Text("Meter") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = Color(0xFF003E1A)
                    ),
                    modifier = Modifier.testTag("nav_meter_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = Color(0xFF003E1A)
                    ),
                    modifier = Modifier.testTag("nav_history_tab")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = Color(0xFF003E1A)
                    ),
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Main Meter Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top App Brand Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsWalk,
                                            contentDescription = "Walk Meter Logo",
                                            tint = Color(0xFF003918),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Walk Meter",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Live GPS Distance Tracking",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            // GPS Status & Accuracy Banner
                            GpsStatusBadge(
                                accuracyLevel = uiState.gpsAccuracyLevel,
                                accuracyMeters = uiState.currentAccuracyMeters,
                                isGpsHardwareEnabled = uiState.isGpsHardwareEnabled,
                                isSimulating = uiState.isSimulating,
                                filteredPoints = uiState.totalPointsFilteredOut
                            )

                            // Authentic Circular Radial Speed & Distance Gauge Meter
                            com.example.ui.components.RadialDistanceMeter(
                                distanceMeters = uiState.distanceMeters,
                                currentSpeedKmh = uiState.currentSpeedKmh,
                                trackingState = uiState.trackingState,
                                displayMode = uiState.displayMode,
                                onToggleDisplayMode = { viewModel.toggleDisplayMode() }
                            )

                            // 4-Card Live Metrics Grid
                            WalkMetricsGrid(
                                elapsedSeconds = uiState.elapsedSeconds,
                                currentSpeedKmh = uiState.currentSpeedKmh,
                                avgSpeedKmh = uiState.avgSpeedKmh,
                                steps = uiState.steps,
                                isHardwareStepSensor = uiState.isHardwareStepSensor,
                                paceString = uiState.paceString,
                                calories = uiState.calories
                            )

                            // Live Route Breadcrumb Trail
                            LiveRouteCanvas(
                                routePoints = uiState.routePoints
                            )

                            // Control Buttons: Start / Pause / Stop / Reset
                            WalkControlsBar(
                                trackingState = uiState.trackingState,
                                onStartClick = { onStartRequested() },
                                onPauseClick = { viewModel.pauseTracking() },
                                onResumeClick = { viewModel.resumeTracking() },
                                onStopClick = { viewModel.stopTracking() },
                                onResetClick = { viewModel.resetMeter() },
                                onSaveClick = { viewModel.saveCurrentWalk() },
                                onSimulateClick = { viewModel.startTracking(isSimulation = true) },
                                isSimulating = uiState.isSimulating
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    1 -> {
                        // History Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Walk History",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            WalkHistoryScreen(
                                historyList = uiState.historyList,
                                onDeleteSession = { viewModel.deleteHistoryItem(it) },
                                onClearAll = { viewModel.clearHistory() }
                            )
                        }
                    }

                    2 -> {
                        // Settings & Info Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                text = "Settings & Help",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                            WalkSettingsScreen(
                                keepScreenOn = uiState.keepScreenOn,
                                onToggleKeepScreenOn = { viewModel.toggleKeepScreenOn(it) },
                                filterStrictness = uiState.filterStrictness,
                                onSelectFilterStrictness = { viewModel.setFilterStrictness(it) },
                                isSimulating = uiState.isSimulating,
                                onToggleSimulation = {
                                    if (uiState.isSimulating) {
                                        viewModel.resetMeter()
                                    } else {
                                        viewModel.startTracking(isSimulation = true)
                                        selectedTab = 0
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Walk Completion Summary Dialog
    if (uiState.showSummaryDialog) {
        WalkSummaryModal(
            session = uiState.lastFinishedSession,
            onSaveClick = { viewModel.saveCurrentWalk() },
            onDismissClick = { viewModel.dismissSummaryDialog() }
        )
    }
}
