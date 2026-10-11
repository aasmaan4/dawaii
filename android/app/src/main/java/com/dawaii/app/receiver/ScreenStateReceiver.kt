package com.dawaii.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.dawaii.app.R
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.service.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ScreenStateReceiver : BroadcastReceiver() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var firstChimeRunnable: Runnable? = null
        private var secondChimeRunnable: Runnable? = null
        private var quietCancelRunnable: Runnable? = null
        private var isScreenOffInWindow = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AppPreferences(context)
        if (!prefs.isNightShiftEnabled) return

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                if (isCurrentTimeInNightShiftWindow(prefs)) {
                    isScreenOffInWindow = true
                    scheduleNightShiftChimes(context, prefs)
                }
            }
            Intent.ACTION_SCREEN_ON -> {
                if (isScreenOffInWindow) {
                    isScreenOffInWindow = false
                    cancelPendingNightShiftChimes()

                    // User woke up screen -> Display overlay for latest pending medication
                    triggerOverlayForPending(context)
                }
            }
        }
    }

    private fun isCurrentTimeInNightShiftWindow(prefs: AppPreferences): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        val startMinutes = prefs.nightShiftStartHour * 60 + prefs.nightShiftStartMin
        val endMinutes = prefs.nightShiftEndHour * 60 + prefs.nightShiftEndMin

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Window spans midnight, e.g. 23:00 to 02:00
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    private fun scheduleNightShiftChimes(context: Context, prefs: AppPreferences) {
        cancelPendingNightShiftChimes()

        // 1 minute after screen off -> play chime
        firstChimeRunnable = Runnable {
            playNightChime(context)
        }
        handler.postDelayed(firstChimeRunnable!!, 60 * 1000L)

        // 2 minutes after screen off -> play chime
        secondChimeRunnable = Runnable {
            playNightChime(context)
        }
        handler.postDelayed(secondChimeRunnable!!, 120 * 1000L)

        // 1 hour after window ends -> Quietly cancel alarms
        val quietCancelDelayMs = calculateQuietCancelDelayMs(prefs)
        quietCancelRunnable = Runnable {
            quietCancelAlarms(context)
        }
        handler.postDelayed(quietCancelRunnable!!, quietCancelDelayMs)
    }

    private fun cancelPendingNightShiftChimes() {
        firstChimeRunnable?.let { handler.removeCallbacks(it) }
        secondChimeRunnable?.let { handler.removeCallbacks(it) }
        quietCancelRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun playNightChime(context: Context) {
        try {
            val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val r = android.media.RingtoneManager.getRingtone(context, notification)
            r?.play()
        } catch (_: Exception) {}
    }

    private fun triggerOverlayForPending(context: Context) {
        val database = AppDatabase.getInstance(context)
        CoroutineScope(Dispatchers.IO).launch {
            val pendingDose = database.doseLogDao().getLatestPendingDose()
            val medicationId = pendingDose?.medicationId ?: 1L // fallback to primary

            try {
                val overlayIntent = Intent(context, com.dawaii.app.ui.overlay.LockScreenOverlayActivity::class.java).apply {
                    putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_MEDICATION_ID, medicationId)
                    putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(overlayIntent)
            } catch (_: Exception) {}
        }
    }

    private fun quietCancelAlarms(context: Context) {
        // Quietly cancels pending alarms so they don't wake user later
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val dummyIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            dummyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun calculateQuietCancelDelayMs(prefs: AppPreferences): Long {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val endMinutes = prefs.nightShiftEndHour * 60 + prefs.nightShiftEndMin
        val cancelMinutes = (endMinutes + (prefs.nightShiftQuietCancelOffsetHours * 60)) % (24 * 60)

        val diffMinutes = if (cancelMinutes >= currentMinutes) {
            cancelMinutes - currentMinutes
        } else {
            (24 * 60 - currentMinutes) + cancelMinutes
        }
        return diffMinutes * 60 * 1000L
    }
}
