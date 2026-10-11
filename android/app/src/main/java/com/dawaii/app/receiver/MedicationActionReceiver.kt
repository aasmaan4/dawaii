package com.dawaii.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dawaii.app.ui.overlay.LockScreenOverlayActivity
import com.dawaii.app.util.MedicationNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MedActionReceiver"
        const val ACTION_MARK_TAKEN = "com.dawaii.app.ACTION_MARK_TAKEN"
        const val ACTION_SNOOZE = "com.dawaii.app.ACTION_SNOOZE"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val SNOOZE_MINUTES = 10
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        if (medicationId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_TAKEN -> {
                        Log.d(TAG, "Notification action MARK_TAKEN for medication $medicationId")
                        MedicationNotificationHelper.markDoseTaken(
                            context = context,
                            medicationId = medicationId,
                            scheduledTime = scheduledTime,
                            triggerType = "LOCKSCREEN_NOTIFICATION"
                        )
                    }
                    ACTION_SNOOZE -> {
                        Log.d(TAG, "Notification action SNOOZE for medication $medicationId (10 minutes)")
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                        val triggerAtMillis = System.currentTimeMillis() + (SNOOZE_MINUTES * 60 * 1000L)

                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
                        val newHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                        val newMinute = cal.get(java.util.Calendar.MINUTE)

                        val db = com.dawaii.app.data.local.AppDatabase.getInstance(context)
                        val med = db.medicineDao().getMedicineById(medicationId)
                        if (med != null) {
                            val updated = med.copy(scheduledHour = newHour, scheduledMinute = newMinute)
                            db.medicineDao().updateMedicine(updated)
                            MedicationAlarmScheduler.scheduleMedicineAlarm(context, updated)
                        }

                        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra(LockScreenOverlayActivity.EXTRA_MEDICATION_ID, medicationId)
                            putExtra(LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, triggerAtMillis)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            medicationId.toInt(),
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        alarmManager?.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing action ${intent.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
