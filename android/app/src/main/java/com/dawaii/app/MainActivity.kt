package com.dawaii.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.receiver.MedicationAlarmScheduler
import com.dawaii.app.ui.navigation.AppNavigationScaffold
import com.dawaii.app.ui.theme.DarkCharcoalBg
import com.dawaii.app.ui.theme.DawaiiTheme
import com.dawaii.app.util.MedicationNotificationHelper

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            MedicationNotificationHelper.createNotificationChannel(this)
            MedicationAlarmScheduler.scheduleAllAlarms(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences.getInstance(this)

        // Deal with Android OS: Request required permissions for notifications, exact alarms, overlay, & battery
        requestRequiredSystemPermissions()

        // Arm all exact medication overlay timers on app launch
        MedicationAlarmScheduler.scheduleAllAlarms(this)

        setContent {
            val currentTheme by preferences.themeFlow.collectAsState()

            DawaiiTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkCharcoalBg
                ) {
                    AppNavigationScaffold()
                }
            }
        }
    }

    private fun requestRequiredSystemPermissions() {
        // 1. Post Notifications (Android 13+ / API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Exact Alarms (Android 12+ / API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        // 3. Battery Optimization Exemption: Ensure OS does not freeze alarms during device sleep
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {}
        }

        // 5. Full-Screen Intent Permission (Android 14+ / API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }
}
