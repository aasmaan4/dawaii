package com.dawaii.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.ui.cabinet.MasterCabinetScreen
import com.dawaii.app.ui.customize.CustomizeScreen
import com.dawaii.app.ui.dashboard.DashboardScreen
import com.dawaii.app.ui.doctors.DoctorsAndVisitsScreen
import com.dawaii.app.ui.howitworks.HowItWorksScreen
import com.dawaii.app.ui.medicines.DoctorMedicinesTabScreen
import com.dawaii.app.ui.recommendations.RecommendationsScreen
import com.dawaii.app.ui.scanner.SmartAiScannerScreen
import com.dawaii.app.ui.overlay.SetupOverlayScreen
import com.dawaii.app.ui.theme.*
import kotlinx.coroutines.launch

enum class NavDestination(
    val title: String,
    val icon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Outlined.Home),
    DOCTORS_AND_VISITS("Doctors & Visits", Icons.Outlined.MedicalServices),
    MASTER_CABINET("Medicine Cabinet", Icons.Outlined.Medication),
    RECOMMENDATIONS("When to Take", Icons.Outlined.Lightbulb),
    SETUP_OVERLAY("Setup Overlay", Icons.Outlined.Layers),
    SCAN_AI("Scan Medicine", Icons.Outlined.DocumentScanner),
    CUSTOMIZE("Customize", Icons.Outlined.Tune),
    HOW_IT_WORKS("How it Works", Icons.Outlined.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationScaffold(
    onLogout: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(NavDestination.DASHBOARD) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { com.dawaii.app.data.local.AppDatabase.getInstance(context) }
    val doctors by database.doctorDao().getAllDoctors().collectAsState(initial = emptyList())
    val medicinesWithDoctor by database.medicineDao().getAllMedicinesWithDoctor().collectAsState(initial = emptyList())
    val medicines = medicinesWithDoctor.map { it.medicine }

    // Active Doctor selected for DoctorMedicinesTabScreen
    var selectedDoctorForTabs by remember { mutableStateOf<Doctor?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val (currentVersionName, currentVersionCode) = remember { com.dawaii.app.util.AppUpdateManager.getCurrentVersion(context) }
    var appUpdatedCelebrationVersion by remember { mutableStateOf<String?>(null) }

    // Update check states
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var updateAvailableDialogInfo by remember { mutableStateOf<com.dawaii.app.util.AppUpdateManager.UpdateInfo?>(null) }
    var isLatestVersionDialogShown by remember { mutableStateOf(false) }

    // On app startup, fetch cloud feature configurations safely and detect if the app was freshly updated
    LaunchedEffect(Unit) {
        com.dawaii.app.sync.CloudSyncManager.syncFromCloud(context)
        val prefs = com.dawaii.app.data.preferences.AppPreferences.getInstance(context)
        val lastCode = prefs.lastSeenVersionCode
        if (lastCode > 0 && currentVersionCode > lastCode) {
            // App was freshly updated! Show confirmation dialog with version
            appUpdatedCelebrationVersion = "v$currentVersionName (Build $currentVersionCode)"
        }
        prefs.lastSeenVersionCode = currentVersionCode
        prefs.lastSeenVersionName = currentVersionName
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkCharcoalBg,
                drawerContentColor = SoftWhite,
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .background(DarkCharcoalBg)
            ) {
                Spacer(modifier = Modifier.height(28.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "DAWAII!!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = BodoniPosterFamily),
                        color = BlushPinkPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Daily Medicine Companion",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = BodoniPosterFamily),
                        color = MutedGreyPink
                    )
                }

                HorizontalDivider(
                    color = DarkBorder,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                NavDestination.values().forEach { destination ->
                    val isSelected = currentScreen == destination && selectedDoctorForTabs == null

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title,
                                tint = if (isSelected) BlushPinkPrimary else MutedGreyPink
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = if (isSelected) {
                                    MaterialTheme.typography.titleMedium.copy(fontFamily = BodoniPosterFamily)
                                } else {
                                    MaterialTheme.typography.bodyLarge.copy(fontFamily = BodoniPosterFamily)
                                },
                                color = if (isSelected) BlushPinkPrimary else SoftWhite
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            selectedDoctorForTabs = null
                            currentScreen = destination
                            coroutineScope.launch { drawerState.close() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BlushPinkContainer,
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(48.dp)
                    )
                }

                // In-App Update Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isCheckingUpdate || isDownloadingUpdate) return@OutlinedButton
                            isCheckingUpdate = true
                            updateStatusMessage = "Checking server for updates..."
                            coroutineScope.launch {
                                val info = com.dawaii.app.util.AppUpdateManager.checkUpdate(context)
                                isCheckingUpdate = false
                                if (info.hasUpdate) {
                                    updateAvailableDialogInfo = info
                                    updateStatusMessage = "New update available: v${info.serverVersionName} (Build ${info.serverVersionCode})"
                                } else {
                                    isLatestVersionDialogShown = true
                                    updateStatusMessage = "You're on the latest version: v${info.currentVersionName} (Build ${info.currentVersionCode})"
                                    android.widget.Toast.makeText(
                                        context,
                                        "Dawaii is on the latest version (v${info.currentVersionName})",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlushPinkPrimary.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlushPinkPrimary),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isCheckingUpdate -> Icons.Outlined.Sync
                                isDownloadingUpdate -> Icons.Outlined.CloudDownload
                                else -> Icons.Outlined.SystemUpdate
                            },
                            contentDescription = "Update",
                            tint = BlushPinkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isCheckingUpdate -> "Checking Updates..."
                                isDownloadingUpdate -> "Downloading Update..."
                                else -> "Check for Updates"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlushPinkPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current: v$currentVersionName (Build $currentVersionCode)",
                        fontSize = 10.sp,
                        color = MutedGreyPink,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    if (updateStatusMessage != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = updateStatusMessage ?: "",
                            fontSize = 10.sp,
                            color = if (updateStatusMessage?.contains("latest", ignoreCase = true) == true) OnTrackGreen else BlushPinkPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = DarkBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column {
                        Text("DAWAII", fontSize = 10.sp, color = MutedGreyPink, letterSpacing = 1.sp)
                        Text(
                            text = "v2.4.7",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlushPinkPrimary
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = DarkCharcoalBg,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (selectedDoctorForTabs == null) {
                    TopAppBar(
                        title = {
                            Text(
                                text = currentScreen.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = BlushPinkPrimary
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = BlushPinkPrimary
                                )
                            }
                        },
                        actions = {
                            if (currentScreen == NavDestination.DASHBOARD) {
                                IconButton(onClick = { currentScreen = NavDestination.SCAN_AI }) {
                                    Icon(Icons.Outlined.DocumentScanner, contentDescription = "Scan", tint = BlushPinkPrimary)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkCharcoalBg,
                            navigationIconContentColor = BlushPinkPrimary
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (selectedDoctorForTabs == null) innerPadding else PaddingValues(0.dp))
            ) {
                // Animated Screen Transitions
                AnimatedContent(
                    targetState = Pair(currentScreen, selectedDoctorForTabs),
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screenTransition"
                ) { (screen, doctor) ->
                    if (doctor != null) {
                        DoctorMedicinesTabScreen(
                            doctor = doctor,
                            medicines = medicines,
                            onBackClick = { selectedDoctorForTabs = null },
                            onSaveMedicine = { updated ->
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    database.medicineDao().insertMedicine(updated)
                                }
                            },
                            onDeleteMedicine = { deleted ->
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    database.medicineDao().deleteMedicine(deleted)
                                }
                            }
                        )
                    } else {
                        when (screen) {
                            NavDestination.DASHBOARD -> DashboardScreen()
                            NavDestination.DOCTORS_AND_VISITS -> DoctorsAndVisitsScreen(
                                onOpenDoctorTabs = { doc -> selectedDoctorForTabs = doc }
                            )
                            NavDestination.MASTER_CABINET -> MasterCabinetScreen(
                                medicines = medicines,
                                doctors = doctors,
                                onSaveMedicine = { updated ->
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        database.medicineDao().insertMedicine(updated)
                                        com.dawaii.app.receiver.MedicationAlarmScheduler.scheduleMedicineAlarm(context, updated)
                                    }
                                },
                                onDeleteMedicine = { deleted ->
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        database.medicineDao().deleteMedicine(deleted)
                                        com.dawaii.app.receiver.MedicationAlarmScheduler.cancelMedicineAlarm(context, deleted.id)
                                    }
                                }
                            )
                            NavDestination.RECOMMENDATIONS -> RecommendationsScreen()
                            NavDestination.SETUP_OVERLAY -> SetupOverlayScreen()
                            NavDestination.SCAN_AI -> SmartAiScannerScreen(
                                doctors = doctors,
                                onSaveMedicine = { newMed ->
                                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        database.medicineDao().insertMedicine(newMed)
                                        com.dawaii.app.receiver.MedicationAlarmScheduler.scheduleMedicineAlarm(context, newMed)
                                    }
                                    currentScreen = NavDestination.MASTER_CABINET
                                },
                                onCloseScanner = { currentScreen = NavDestination.DASHBOARD }
                            )
                            NavDestination.CUSTOMIZE -> CustomizeScreen()
                            NavDestination.HOW_IT_WORKS -> HowItWorksScreen()
                        }
                    }
                }
            }
        }
    }

    // Dialog 1: App Successfully Updated (Shown automatically on startup after an update)
    if (appUpdatedCelebrationVersion != null) {
        AlertDialog(
            onDismissRequest = { appUpdatedCelebrationVersion = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = OnTrackGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "App Updated!",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Dawaii has been successfully updated to $appUpdatedCelebrationVersion.",
                        color = SoftWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "All new features and improvements are active. Your medicine schedules, doctor appointments, and caregiver alert settings are completely safe and preserved.",
                        color = MutedGreyPink,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { appUpdatedCelebrationVersion = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BlushPinkPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it", color = DarkCharcoalBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog 2: Already on Latest Version Confirmation
    if (isLatestVersionDialogShown) {
        AlertDialog(
            onDismissRequest = { isLatestVersionDialogShown = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = OnTrackGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Latest Version",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "You are on the latest version of Dawaii!",
                        color = SoftWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Installed Version: v$currentVersionName (Build $currentVersionCode)\n\nNo newer update is required. You have the latest features and medicine intelligence.",
                        color = MutedGreyPink,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { isLatestVersionDialogShown = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BlushPinkPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("OK", color = DarkCharcoalBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Dialog 3: New Update Available Confirmation & Download
    updateAvailableDialogInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateAvailableDialogInfo = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    tint = BlushPinkPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Update Available",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "A new version of Dawaii is available!",
                        color = SoftWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Installed: v${info.currentVersionName} (Build ${info.currentVersionCode})\n• New: v${info.serverVersionName} (Build ${info.serverVersionCode})\n\nRelease notes:\n${info.releaseNotes}",
                        color = MutedGreyPink,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { updateAvailableDialogInfo = null }) {
                    Text("Later", color = MutedGreyPink)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetApkUrl = info.apkUrl
                        val targetVerName = info.serverVersionName
                        updateAvailableDialogInfo = null
                        isDownloadingUpdate = true
                        updateStatusMessage = "Downloading v$targetVerName..."
                        coroutineScope.launch {
                            com.dawaii.app.util.AppUpdateManager.downloadAndInstallApk(
                                context = context,
                                apkUrl = targetApkUrl,
                                onStatus = { status ->
                                    updateStatusMessage = status
                                }
                            )
                            isDownloadingUpdate = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BlushPinkPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Update to v${info.serverVersionName}", color = DarkCharcoalBg, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceVariant,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
