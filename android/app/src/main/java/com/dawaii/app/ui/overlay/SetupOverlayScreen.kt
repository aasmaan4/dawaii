package com.dawaii.app.ui.overlay

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.receiver.MedicationAlarmScheduler
import com.dawaii.app.service.OverlayService
import com.dawaii.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.dawaii.app.util.MedicationNotificationHelper
import java.util.Calendar

@Composable
fun SetupOverlayScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }
    val prefs = remember { AppPreferences.getInstance(context) }

    val medicines by database.medicineDao().getAllActiveMedicines().collectAsState(initial = emptyList())

    val alarmManager = remember { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else true
        )
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var isBatteryExempt by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }

    // Refresh permission checks
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasExactAlarmPermission = alarmManager.canScheduleExactAlarms()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        isBatteryExempt = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "SETUP OVERLAY",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = BirchTypography,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = BlushPinkPrimary
        )
        Text(
            text = "Always-on display overlay permissions & notification reminders",
            style = MaterialTheme.typography.bodySmall,
            color = MutedGreyPink
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            // Android OS Permissions Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = BlushPinkPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "ANDROID OS PERMISSIONS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BlushPinkPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. Lock Screen Alarm Display (Native Full Screen)
                        PermissionStatusRow(
                            title = "Lock Screen Alarm Display",
                            subtitle = "Displays overlay when alarms ring without restricted permissions",
                            isGranted = true,
                            onGrant = {
                                try {
                                    val previewIntent = Intent(context, LockScreenOverlayActivity::class.java).apply {
                                        putExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, 1L)
                                        putExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(previewIntent)
                                } catch (_: Exception) {}
                            }
                        )

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                        // 2. Notification Permission
                        PermissionStatusRow(
                            title = "Lock Screen Notifications",
                            subtitle = "Pinned reminder that stays until marked taken",
                            isGranted = hasNotificationPermission,
                            onGrant = {
                                try {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                        // 3. Exact Alarm Permission
                        PermissionStatusRow(
                            title = "Exact Alarm Timing",
                            subtitle = "Ensures timers ring on the exact minute",
                            isGranted = hasExactAlarmPermission,
                            onGrant = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    try {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            }
                        )

                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                        // 4. Battery Optimization Exemption
                        PermissionStatusRow(
                            title = "Battery Unrestricted",
                            subtitle = "Prevents OS from killing overlay when phone sleeps",
                            isGranted = isBatteryExempt,
                            onGrant = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }

            // Section Header: Medications and Timings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEDICATION OVERLAY TIMINGS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = BlushPinkPrimary
                    )
                    Text(
                        text = "${medicines.size} Prescriptions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGreyPink
                    )
                }
            }

            // Medication List with direct Time Picker
            items(medicines, key = { it.id }) { med ->
                val isReminder = prefs.isMedicineReminderEnabled(med.id)

                // Calculate countdown
                val now = Calendar.getInstance()
                val currentTotalMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val medTotalMin = med.scheduledHour * 60 + med.scheduledMinute
                var diff = medTotalMin - currentTotalMin
                if (diff <= 0) diff += 24 * 60
                val hrs = diff / 60
                val mins = diff % 60
                val countdown = "Alerts in ${hrs}h ${mins}m"

                val ampm = if (med.scheduledHour >= 12) "PM" else "AM"
                val h12 = if (med.scheduledHour % 12 == 0) 12 else med.scheduledHour % 12
                val timeFormatted = String.format("%02d:%02d %s", h12, med.scheduledMinute, ampm)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = med.name,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = SoftWhite
                            )
                            Text(
                                text = countdown,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isReminder) BlushPinkPrimary else MutedGreyPink
                            )
                        }

                        // Time Picker Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BlushPinkContainer)
                                .border(1.dp, BlushPinkPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable {
                                    val timePicker = TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val updated = med.copy(scheduledHour = hourOfDay, scheduledMinute = minute)
                                                database.medicineDao().updateMedicine(updated)
                                                MedicationAlarmScheduler.scheduleMedicineAlarm(context, updated)
                                            }
                                            Toast.makeText(context, "${med.name} scheduled for ${String.format("%02d:%02d", hourOfDay, minute)}", Toast.LENGTH_SHORT).show()
                                        },
                                        med.scheduledHour,
                                        med.scheduledMinute,
                                        false
                                    )
                                    timePicker.show()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "$timeFormatted ✎",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BlushPinkPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))



        OutlinedButton(
            onClick = {
                val med = medicines.firstOrNull() ?: Medicine(
                    id = 1L,
                    doctorId = 1L,
                    name = "Medication Demo",
                    dosage = "500mg",
                    frequency = "Daily",
                    scheduledHour = 20,
                    scheduledMinute = 0
                )
                // 1. Post persistent lock screen notification reminder
                MedicationNotificationHelper.showMedicationAlarmNotification(
                    context = context,
                    medicine = med,
                    scheduledTime = System.currentTimeMillis()
                )

                // 2. Launch always-on display / lock screen overlay
                val intent = Intent(context, LockScreenOverlayActivity::class.java).apply {
                    putExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, med.id)
                    putExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
                Toast.makeText(context, "Overlay & Lock Screen Notification Triggered!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(14.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BlushPinkPrimary)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = BlushPinkPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Overlay & Notification Reminder", color = BlushPinkPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SoftWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedGreyPink,
                fontSize = 11.sp
            )
        }

        if (isGranted) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF67D59A).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF67D59A), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Active ✓",
                    color = Color(0xFF67D59A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onGrant,
                colors = ButtonDefaults.buttonColors(containerColor = BlushPinkPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = "Grant",
                    color = DarkCharcoalBg,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
