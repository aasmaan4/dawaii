package com.dawaii.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dawaii.app.data.preferences.AppPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CaregiverNotificationHelper {

    private const val TAG = "CaregiverSMS"

    fun sendMissedDoseAlert(
        context: Context,
        medicationName: String = "Medication",
        missedHour: Int? = null,
        missedMinute: Int? = null
    ) {
        val prefs = AppPreferences.getInstance(context)
        if (!prefs.caregiverEnabled) return

        val phoneNumber = prefs.caregiverPhone.trim().ifBlank { "9692298485" }
        if (phoneNumber.isBlank()) return

        val cal = Calendar.getInstance()
        val hour = missedHour ?: cal.get(Calendar.HOUR_OF_DAY)
        val minute = missedMinute ?: cal.get(Calendar.MINUTE)

        val timeOfDay = when (hour) {
            in 4..11 -> "Morning"
            in 12..15 -> "Noon"
            in 16..20 -> "Evening"
            else -> "Night"
        }

        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val formattedTime = timeFormat.format(cal.time)

        // Exact requested format: "(time of the day. eg. noon, morning) (medication name) missed at (time)"
        val message = "$timeOfDay $medicationName missed at $formattedTime"

        var sentDirectly = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.d(TAG, "Sent SMS directly via SmsManager to $phoneNumber: $message")
                sentDirectly = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending SMS via SmsManager, will try Intent fallback", e)
            }
        }

        if (!sentDirectly) {
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$phoneNumber")
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
                Log.d(TAG, "Triggered SMS Intent fallback for $phoneNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch SMS intent", e)
            }
        }
    }
}
