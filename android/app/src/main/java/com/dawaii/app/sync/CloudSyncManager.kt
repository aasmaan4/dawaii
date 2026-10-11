package com.dawaii.app.sync

import android.content.Context
import android.util.Log
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Appointment
import com.dawaii.app.data.local.entity.Doctor
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.local.entity.MedicineType
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.preferences.AppTheme
import com.dawaii.app.data.preferences.GestureAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val CLOUD_SYNC_URL = "https://dawaii-app.vercel.app/api/sync"
    private const val CLOUD_SYNC_STATIC_URL = "https://dawaii-app.vercel.app/sync.json"
    private const val LOCAL_SYNC_URL = "http://10.0.2.2:3000/api/sync"

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val doctorsCount: Int = 0,
        val medicinesCount: Int = 0,
        val appointmentsCount: Int = 0
    )

    /**
     * Pulls the latest cloud configuration and database records without needing
     * to download a new APK or reinstall the application.
     */
    suspend fun syncFromCloud(context: Context): SyncResult = withContext(Dispatchers.IO) {
        val urlsToTry = listOf(CLOUD_SYNC_URL, CLOUD_SYNC_STATIC_URL, LOCAL_SYNC_URL)
        var lastException: Exception? = null

        for (endpoint in urlsToTry) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseText = reader.use { it.readText() }
                    conn.disconnect()

                    val json = JSONObject(responseText)
                    return@withContext applyCloudPayload(context, json)
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Sync failed on $endpoint: ${e.message}")
                lastException = e
            }
        }

        SyncResult(
            success = false,
            message = "Could not connect to cloud sync server: ${lastException?.localizedMessage ?: "Unknown error"}"
        )
    }

    private suspend fun applyCloudPayload(context: Context, json: JSONObject): SyncResult {
        val prefs = AppPreferences.getInstance(context)
        val db = AppDatabase.getInstance(context)

        // 1. Sync App Preferences & Lock-Screen Overlay Controls
        if (json.has("overlayText")) {
            val overlay = json.optString("overlayText", "DAWA!")
            if (overlay.isNotBlank()) prefs.setOverlayTextPreference(overlay)
        }

        if (json.has("caregiverPhone")) {
            val phone = json.optString("caregiverPhone", "9692298485")
            if (phone.isNotBlank()) prefs.setCaregiverPhonePreference(phone)
        }

        if (json.has("caregiverEnabled")) {
            prefs.setCaregiverEnabledPreference(json.optBoolean("caregiverEnabled", true))
        }

        if (json.has("doubleTapAction")) {
            val action = GestureAction.fromString(json.optString("doubleTapAction", "Unlock Phone"))
            prefs.doubleTapAction = action
        }

        if (json.has("tripleTapAction")) {
            val action = GestureAction.fromString(json.optString("tripleTapAction", "Mark Taken"))
            prefs.tripleTapAction = action
        }

        if (json.has("brightenOnScroll")) {
            prefs.setBrightenOnScrollPreference(json.optBoolean("brightenOnScroll", true))
        }

        if (json.has("theme")) {
            val themeStr = json.optString("theme", "SAKURA").uppercase()
            val theme = if (themeStr == "NORMAL") AppTheme.NORMAL else AppTheme.SAKURA
            prefs.setThemePreference(theme)
        }

        var docCount = 0
        var medCount = 0
        var apptCount = 0

        // 2. Sync Doctors Table: Protected — only insert if table is empty or user explicitly requested sync
        val existingDoctors = db.doctorDao().getAllDoctors().first()
        val isExplicitUserSync = json.optBoolean("isUserSync", false)
        val doctorIdMapping = mutableMapOf<Long, Long>()

        if (json.has("doctors") && (isExplicitUserSync || existingDoctors.isEmpty())) {
            val doctorsArray = json.getJSONArray("doctors")
            for (i in 0 until doctorsArray.length()) {
                val dObj = doctorsArray.getJSONObject(i)
                val originalId = dObj.optLong("id", (i + 1).toLong())
                val name = dObj.optString("name", "Dr. Rajesh Sharma")
                val contact = dObj.optString("contact", "+91 98450 12345")
                val specialty = dObj.optString("specialty", "General Physician")

                val existingDoc = existingDoctors.find { it.id == originalId }
                val assignedId = if (existingDoc != null) {
                    if (isExplicitUserSync) {
                        db.doctorDao().updateDoctor(existingDoc.copy(name = name, contactNumber = contact, specialty = specialty))
                    }
                    originalId
                } else {
                    db.doctorDao().insertDoctor(Doctor(id = originalId, name = name, contactNumber = contact, specialty = specialty))
                }
                doctorIdMapping[originalId] = assignedId
                docCount++
            }
        }

        // 3. Sync Medicines Table (Never re-import medicines deleted by user, protect local user medicines)
        val existingLocalMeds = db.medicineDao().getAllActiveMedicines().first()
        val shouldSyncMeds = isExplicitUserSync || (!prefs.isInitialSeedDone && existingLocalMeds.isEmpty())

        if (json.has("medicines") && shouldSyncMeds) {
            val medsArray = json.getJSONArray("medicines")
            for (i in 0 until medsArray.length()) {
                val mObj = medsArray.getJSONObject(i)
                val originalMedId = mObj.optLong("id", (i + 1).toLong())
                val originalDocId = mObj.optLong("doctorId", 1L)
                val resolvedDocId = doctorIdMapping[originalDocId] ?: existingDoctors.firstOrNull()?.id ?: 1L

                val name = mObj.optString("name", "Blood Pressure (Amlodipine)")
                // Check if user has explicitly deleted this medicine
                if (prefs.isMedicineDeleted(originalMedId, name)) {
                    continue
                }
                val dosage = mObj.optString("dosage", "5mg")
                val typeStr = mObj.optString("type", "Pill").uppercase()
                val type = try {
                    MedicineType.valueOf(typeStr)
                } catch (_: Exception) {
                    if (typeStr.contains("CAPSULE")) MedicineType.CAPSULE
                    else if (typeStr.contains("CREAM") || typeStr.contains("BALM") || typeStr.contains("OINT")) MedicineType.CREAM
                    else if (typeStr.contains("SYRUP") || typeStr.contains("LIQUID")) MedicineType.SYRUP
                    else if (typeStr.contains("SOLUT") || typeStr.contains("DROP")) MedicineType.SOLUTION
                    else MedicineType.PILL
                }
                val hour = mObj.optInt("hour", 8)
                val minute = mObj.optInt("minute", 0)
                val pillsLeft = if (mObj.has("pillsLeft") && !mObj.isNull("pillsLeft")) mObj.getInt("pillsLeft") else null
                val daysToComplete = mObj.optInt("daysLeft", 30)
                val photoUri = if (mObj.has("photoUri") && !mObj.isNull("photoUri")) mObj.getString("photoUri") else null

                val ampm = if (hour >= 12) "PM" else "AM"
                val h12 = if (hour % 12 == 0) 12 else hour % 12
                val freq = "Daily at $h12:${String.format("%02d", minute)} $ampm"

                val med = Medicine(
                    id = originalMedId,
                    doctorId = resolvedDocId,
                    name = name,
                    dosage = dosage,
                    frequency = freq,
                    type = type,
                    pillsLeft = pillsLeft,
                    daysToComplete = daysToComplete,
                    photoUri = photoUri,
                    scheduledHour = hour,
                    scheduledMinute = minute,
                    isActive = true
                )
                db.medicineDao().insertMedicine(med)
                medCount++
            }
        }

        // 4. Sync Appointments Table: Protected — only insert if table is empty or user explicitly requested sync
        val existingAppts = db.appointmentDao().getAllAppointmentsWithDoctor().first()
        if (json.has("appointments") && (isExplicitUserSync || existingAppts.isEmpty())) {
            val apptsArray = json.getJSONArray("appointments")
            for (i in 0 until apptsArray.length()) {
                val aObj = apptsArray.getJSONObject(i)
                val originalDocId = aObj.optLong("doctorId", 1L)
                val resolvedDocId = doctorIdMapping[originalDocId] ?: existingDoctors.firstOrNull()?.id ?: 1L
                val date = aObj.optString("date", "Thursday, Oct 8")
                val time = aObj.optString("time", "3:30 PM")
                val location = aObj.optString("location", "Apollo Clinic, Indiranagar, Bengaluru")
                val notes = aObj.optString("notes", "")
                val isAttended = aObj.optBoolean("isAttended", false)

                val appt = Appointment(
                    id = aObj.optLong("id", (i + 1).toLong()),
                    doctorId = resolvedDocId,
                    date = date,
                    time = time,
                    location = location,
                    notes = notes,
                    isAttended = isAttended
                )
                db.appointmentDao().insertAppointments(listOf(appt))
                apptCount++
            }
        }

        return SyncResult(
            success = true,
            message = "Synchronized: $medCount medicines, $docCount doctors, $apptCount appointments, and overlay controls updated.",
            doctorsCount = docCount,
            medicinesCount = medCount,
            appointmentsCount = apptCount
        )
    }
}
