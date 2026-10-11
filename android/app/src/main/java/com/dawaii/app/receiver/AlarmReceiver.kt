package com.dawaii.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.dawaii.app.DawaiiApp
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.ui.overlay.LockScreenOverlayActivity
import com.dawaii.app.util.MedicationNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, -1L)
        val scheduledTime = intent.getLongExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        if (medicationId == -1L) return

        val database = AppDatabase.getInstance(context)
        val prefs = AppPreferences.getInstance(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medication = database.medicineDao().getMedicineById(medicationId)
                if (medication == null || !medication.isActive) {
                    Log.d(TAG, "Medication $medicationId is null or inactive. Skipping alarm.")
                    return@launch
                }

                // Check if already taken today
                val isTakenToday = try {
                    val today = LocalDate.now()
                    val todayLog = database.dailyMedicationLogDao().getLogForMedicine(today, medicationId)
                    todayLog != null && todayLog.isTaken
                } catch (e: Exception) {
                    false
                }

                if (isTakenToday) {
                    Log.d(TAG, "Medication ${medication.name} is already marked taken for today. Skipping alarm.")
                    return@launch
                }

                // 1. Post persistent lock screen notification reminder.
                // It stays pinned on lockscreen & notification bar until marked taken or snoozed!
                MedicationNotificationHelper.showMedicationAlarmNotification(context, medication, scheduledTime)

                // 2. Lock-screen Always-On Overlay:
                // Triggers when device is locked or screen is off/sleeping
                val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                val isLocked = keyguardManager?.isKeyguardLocked ?: true
                val isAppInForeground = DawaiiApp.isAppInForeground
                Log.d(TAG, "Alarm triggered: isAppInForeground=$isAppInForeground, isLocked=$isLocked, isOverlayEnabled=${prefs.isOverlayEnabled}")

                if ((isLocked || !isAppInForeground) && prefs.isOverlayEnabled) {
                    try {
                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                        @Suppress("DEPRECATION")
                        val wakeLock = powerManager.newWakeLock(
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                            "dawaii:alarm_lockscreen_wake"
                        )
                        wakeLock.acquire(10000L)
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not acquire wake lock in AlarmReceiver: ${e.message}")
                    }

                    try {
                        val overlayIntent = Intent(context, LockScreenOverlayActivity::class.java).apply {
                            putExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, scheduledTime)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(overlayIntent)
                        Log.d(TAG, "Launched LockScreenOverlayActivity over lockscreen")
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not start LockScreenOverlayActivity directly: ${e.message}. FullScreenIntent will wake display.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing alarm in AlarmReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
