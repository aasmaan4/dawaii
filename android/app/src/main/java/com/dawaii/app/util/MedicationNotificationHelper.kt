package com.dawaii.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dawaii.app.MainActivity
import com.dawaii.app.R
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.DailyMedicationLog
import com.dawaii.app.data.local.entity.DoseLog
import com.dawaii.app.data.local.entity.DoseStatus
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.receiver.MedicationActionReceiver
import com.dawaii.app.receiver.MedicationAlarmScheduler
import com.dawaii.app.ui.overlay.LockScreenOverlayActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

object MedicationNotificationHelper {
    private const val TAG = "MedNotifHelper"
    const val CHANNEL_ID = "dawaii_lockscreen_medication"
    private const val CHANNEL_NAME = "Medication Lock Screen Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent lock screen medication alerts. Remains pinned until marked taken."
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showMedicationAlarmNotification(
        context: Context,
        medicine: Medicine,
        scheduledTime: Long
    ) {
        createNotificationChannel(context)

        val notificationId = medicine.id.toInt()
        val prefs = AppPreferences.getInstance(context)
        if (!prefs.isNotificationsEnabled) {
            android.util.Log.d("MedNotificationHelper", "Medication notifications are disabled in user settings. Skipping.")
            return
        }

        // 1. Full-Screen Intent to wake the screen and display LockScreenOverlayActivity when sleeping/locked
        val fullScreenIntent = Intent(context, LockScreenOverlayActivity::class.java).apply {
            putExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, medicine.id)
            putExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, scheduledTime)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 100000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Regular Content Intent to open app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 200000,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Notification Action: Mark Taken
        val markTakenIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_MARK_TAKEN
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicine.id)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 300000,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Notification Action: Snooze 10m
        val snoozeIntent = Intent(context, MedicationActionReceiver::class.java).apply {
            action = MedicationActionReceiver.ACTION_SNOOZE
            putExtra(MedicationActionReceiver.EXTRA_MEDICATION_ID, medicine.id)
            putExtra(MedicationActionReceiver.EXTRA_SCHEDULED_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 400000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ampm = if (medicine.scheduledHour >= 12) "PM" else "AM"
        val h12 = if (medicine.scheduledHour % 12 == 0) 12 else medicine.scheduledHour % 12
        val timeFormatted = String.format("%02d:%02d %s", h12, medicine.scheduledMinute, ampm)
        val promptText = prefs.overlayText.ifBlank { "Time for your scheduled dose" }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("💊 ${medicine.name} • ${medicine.dosage}")
            .setContentText("$promptText (Scheduled: $timeFormatted)")
            .setSubText("DAWAII Medication Due")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible on lock screen
            .setOngoing(true) // Pinned until explicitly marked taken
            .setAutoCancel(false) // Does not dismiss automatically
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_launcher, "Mark Taken ✓", markTakenPendingIntent)
            .addAction(R.drawable.ic_launcher, "Snooze 10m ⏰", snoozePendingIntent)

        // Attach full-screen intent ONLY if overlay is enabled in settings
        if (prefs.isOverlayEnabled) {
            builder.setFullScreenIntent(fullScreenPendingIntent, true) // Wakes screen & shows lock screen overlay
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(soundUri)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Posted ongoing lockscreen notification for ${medicine.name} (id=$notificationId)")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException posting notification: ${e.message}")
        }
    }

    fun cancelNotification(context: Context, medicationId: Long) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(medicationId.toInt())
            Log.d(TAG, "Cancelled lockscreen notification for med id $medicationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling notification for med id $medicationId", e)
        }
    }

    fun markDoseTaken(context: Context, medicationId: Long, scheduledTime: Long, triggerType: String = "NOTIFICATION") {
        val database = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val today = LocalDate.now()

            // 1. Update daily medication log
            val existingDaily = database.dailyMedicationLogDao().getLogForMedicine(today, medicationId)
            database.dailyMedicationLogDao().insertOrUpdateLog(
                DailyMedicationLog(
                    id = existingDaily?.id ?: 0L,
                    date = today,
                    medicineId = medicationId,
                    isTaken = true
                )
            )

            // 2. Update dose logs
            database.doseLogDao().markActiveDoseTaken(medicationId, now)
            database.doseLogDao().insertLog(
                DoseLog(
                    medicationId = medicationId,
                    scheduledTimeMillis = scheduledTime,
                    actualTriggerTimeMillis = now,
                    actionTimeMillis = now,
                    status = DoseStatus.TAKEN,
                    triggerType = triggerType
                )
            )

            // 3. Remove persistent notification from lock screen
            cancelNotification(context, medicationId)

            // 4. Schedule next day's alarm for this medication
            val medicine = database.medicineDao().getMedicineById(medicationId)
            if (medicine != null && medicine.isActive) {
                MedicationAlarmScheduler.scheduleMedicineAlarm(context, medicine)
            }
        }
    }
}
