package com.dawaii.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.service.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object MedicationAlarmScheduler {
    private const val TAG = "MedAlarmScheduler"

    fun scheduleAllAlarms(context: Context) {
        val database = AppDatabase.getInstance(context)
        val prefs = AppPreferences.getInstance(context)
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicines = database.medicineDao().getAllActiveMedicines().first()
                for (medicine in medicines) {
                    if (prefs.isMedicineReminderEnabled(medicine.id)) {
                        scheduleMedicineAlarm(appContext, medicine)
                    } else {
                        cancelMedicineAlarm(appContext, medicine.id)
                    }
                }
                Log.d(TAG, "Successfully armed alarms for ${medicines.size} medications")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling all medication alarms", e)
            }
        }
    }

    fun scheduleMedicineAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = AppPreferences.getInstance(context)

        if (!prefs.isMedicineReminderEnabled(medicine.id)) {
            cancelMedicineAlarm(context, medicine.id)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.scheduledHour)
            set(Calendar.MINUTE, medicine.scheduledMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(OverlayService.EXTRA_MEDICATION_ID, medicine.id)
            putExtra(OverlayService.EXTRA_SCHEDULED_TIME, cal.timeInMillis)
        }

        val requestCode = medicine.id.toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
            Log.d(TAG, "Scheduled alarm for ${medicine.name} at ${cal.time}")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm: ${e.message}. Falling back.")
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    fun cancelMedicineAlarm(context: Context, medicineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val requestCode = medicineId.toInt()
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm for medicine id $medicineId")
        }
    }
}
