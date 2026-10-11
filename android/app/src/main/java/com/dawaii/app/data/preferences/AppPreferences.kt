package com.dawaii.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SAKURA,
    NORMAL
}

enum class GestureAction(val label: String) {
    UNLOCK_PHONE("Unlock Phone"),
    MARK_TAKEN("Mark Taken"),
    NONE("None");

    companion object {
        fun fromString(value: String): GestureAction {
            return entries.find { 
                it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) 
            } ?: when (value.uppercase()) {
                "DISMISS" -> UNLOCK_PHONE
                "SNOOZE_DEFAULT" -> NONE
                else -> NONE
            }
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("dawaii_settings", Context.MODE_PRIVATE)

    companion object {
        const val DEVELOPER_GEMINI_KEY = ""
        private const val KEY_AUTH_MODE = "key_auth_mode"
        private const val KEY_THEME = "key_theme"
        private const val KEY_DIMNESS = "key_dimness"
        private const val KEY_CHIME_ENABLED = "key_chime_enabled"
        private const val KEY_CHIME_ON_OVERLAY = "key_chime_on_overlay"
        private const val KEY_CHIME_ON_NOTIFICATION = "key_chime_on_notification"
        private const val KEY_CHIME_DELAY_SECONDS = "key_chime_delay_sec"
        private const val KEY_DOUBLE_TAP_ACTION = "key_double_tap_action"
        private const val KEY_TRIPLE_TAP_ACTION = "key_triple_tap_action"
        private const val KEY_OVERLAY_TEXT = "key_overlay_text"
        private const val KEY_IS_24_HOUR = "key_is_24_hour"
        private const val KEY_WHEEL_MIN = "key_wheel_min"
        private const val KEY_WHEEL_MAX = "key_wheel_max"
        private const val KEY_WHEEL_STEP = "key_wheel_step"
        private const val KEY_NIGHT_SHIFT_ENABLED = "key_night_shift_enabled"
        private const val KEY_NIGHT_SHIFT_START_HOUR = "key_ns_start_hour"
        private const val KEY_NIGHT_SHIFT_START_MIN = "key_ns_start_min"
        private const val KEY_NIGHT_SHIFT_END_HOUR = "key_ns_end_hour"
        private const val KEY_NIGHT_SHIFT_END_MIN = "key_ns_end_min"
        private const val KEY_NIGHT_SHIFT_QUIET_CANCEL_HOURS = "key_ns_cancel_hours"
        private const val KEY_CAREGIVER_PHONE = "key_caregiver_phone"
        private const val KEY_CAREGIVER_ENABLED = "key_caregiver_enabled"
        private const val KEY_OVERLAY_ENABLED = "key_overlay_enabled"
        private const val KEY_BRIGHTEN_ON_SCROLL = "key_brighten_on_scroll"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_INITIAL_SEED_DONE = "key_initial_seed_done"
        private const val KEY_LAST_SEEN_VERSION_CODE = "key_last_seen_version_code"
        private const val KEY_LAST_SEEN_VERSION_NAME = "key_last_seen_version_name"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun readTheme(): AppTheme {
        return runCatching { AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.SAKURA.name)!!) }
            .getOrDefault(AppTheme.SAKURA)
    }

    private fun readChime(): Boolean {
        return prefs.getBoolean(KEY_CHIME_ENABLED, true)
    }

    private fun readDimness(): Float {
        return prefs.getFloat(KEY_DIMNESS, 40f)
    }

    private fun readOverlayText(): String {
        return prefs.getString(KEY_OVERLAY_TEXT, "DAWA!") ?: "DAWA!"
    }

    private fun readIs24Hour(): Boolean {
        return prefs.getBoolean(KEY_IS_24_HOUR, false)
    }

    private fun readCaregiverPhone(): String {
        return prefs.getString(KEY_CAREGIVER_PHONE, "9692298485") ?: "9692298485"
    }

    private fun readCaregiverEnabled(): Boolean {
        return prefs.getBoolean(KEY_CAREGIVER_ENABLED, true)
    }

    private fun readOverlayEnabled(): Boolean {
        return prefs.getBoolean(KEY_OVERLAY_ENABLED, true)
    }

    private fun readBrightenOnScroll(): Boolean {
        return prefs.getBoolean(KEY_BRIGHTEN_ON_SCROLL, true)
    }

    private val _themeFlow = MutableStateFlow(readTheme())
    val themeFlow: StateFlow<AppTheme> = _themeFlow.asStateFlow()

    private val _overlayEnabledFlow = MutableStateFlow(readOverlayEnabled())
    val overlayEnabledFlow: StateFlow<Boolean> = _overlayEnabledFlow.asStateFlow()

    private val _brightenOnScrollFlow = MutableStateFlow(readBrightenOnScroll())
    val brightenOnScrollFlow: StateFlow<Boolean> = _brightenOnScrollFlow.asStateFlow()

    private val _chimeFlow = MutableStateFlow(readChime())
    val chimeFlow: StateFlow<Boolean> = _chimeFlow.asStateFlow()

    private val _chimeOnOverlayFlow = MutableStateFlow(prefs.getBoolean(KEY_CHIME_ON_OVERLAY, true))
    val chimeOnOverlayFlow: StateFlow<Boolean> = _chimeOnOverlayFlow.asStateFlow()

    private val _chimeOnNotificationFlow = MutableStateFlow(prefs.getBoolean(KEY_CHIME_ON_NOTIFICATION, true))
    val chimeOnNotificationFlow: StateFlow<Boolean> = _chimeOnNotificationFlow.asStateFlow()

    private val _chimeDelayFlow = MutableStateFlow(prefs.getInt(KEY_CHIME_DELAY_SECONDS, 3))
    val chimeDelayFlow: StateFlow<Int> = _chimeDelayFlow.asStateFlow()

    private val _dimnessFlow = MutableStateFlow(readDimness())
    val dimnessFlow: StateFlow<Float> = _dimnessFlow.asStateFlow()

    private val _overlayTextFlow = MutableStateFlow(readOverlayText())
    val overlayTextFlow: StateFlow<String> = _overlayTextFlow.asStateFlow()

    private val _is24HourFlow = MutableStateFlow(readIs24Hour())
    val is24HourFlow: StateFlow<Boolean> = _is24HourFlow.asStateFlow()

    private val _caregiverPhoneFlow = MutableStateFlow(readCaregiverPhone())
    val caregiverPhoneFlow: StateFlow<String> = _caregiverPhoneFlow.asStateFlow()

    private val _caregiverEnabledFlow = MutableStateFlow(readCaregiverEnabled())
    val caregiverEnabledFlow: StateFlow<Boolean> = _caregiverEnabledFlow.asStateFlow()

    var theme: AppTheme
        get() = _themeFlow.value
        set(value) {
            prefs.edit().putString(KEY_THEME, value.name).apply()
            _themeFlow.value = value
        }

    fun setThemePreference(value: AppTheme) {
        theme = value
    }

    var initialDimness: Float
        get() = _dimnessFlow.value
        set(value) {
            val clamped = value.coerceIn(10f, 95f)
            prefs.edit().putFloat(KEY_DIMNESS, clamped).apply()
            _dimnessFlow.value = clamped
        }

    fun setDimnessPreference(value: Float) {
        initialDimness = value
    }

    var isChimeEnabled: Boolean
        get() = _chimeFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CHIME_ENABLED, value).apply()
            _chimeFlow.value = value
        }

    fun setChimePreference(value: Boolean) {
        isChimeEnabled = value
    }

    var chimeOnOverlay: Boolean
        get() = _chimeOnOverlayFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CHIME_ON_OVERLAY, value).apply()
            _chimeOnOverlayFlow.value = value
        }

    fun setChimeOnOverlayPreference(value: Boolean) {
        chimeOnOverlay = value
    }

    var chimeOnNotification: Boolean
        get() = _chimeOnNotificationFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CHIME_ON_NOTIFICATION, value).apply()
            _chimeOnNotificationFlow.value = value
        }

    fun setChimeOnNotificationPreference(value: Boolean) {
        chimeOnNotification = value
    }

    var overlayText: String
        get() = _overlayTextFlow.value
        set(value) {
            prefs.edit().putString(KEY_OVERLAY_TEXT, value).apply()
            _overlayTextFlow.value = value
        }

    fun setOverlayTextPreference(value: String) {
        overlayText = value
    }

    var is24HourFormat: Boolean
        get() = _is24HourFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_IS_24_HOUR, value).apply()
            _is24HourFlow.value = value
        }

    fun setIs24HourPreference(value: Boolean) {
        is24HourFormat = value
    }

    var caregiverPhone: String
        get() = _caregiverPhoneFlow.value
        set(value) {
            prefs.edit().putString(KEY_CAREGIVER_PHONE, value).apply()
            _caregiverPhoneFlow.value = value
        }

    fun setCaregiverPhonePreference(value: String) {
        caregiverPhone = value
    }

    var caregiverEnabled: Boolean
        get() = _caregiverEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_CAREGIVER_ENABLED, value).apply()
            _caregiverEnabledFlow.value = value
        }

    fun setCaregiverEnabledPreference(value: Boolean) {
        caregiverEnabled = value
    }

    var chimeDelaySeconds: Int
        get() = _chimeDelayFlow.value
        set(value) {
            val clamped = value.coerceIn(0, 15)
            prefs.edit().putInt(KEY_CHIME_DELAY_SECONDS, clamped).apply()
            _chimeDelayFlow.value = clamped
        }

    fun setChimeDelayPreference(value: Int) {
        chimeDelaySeconds = value
    }

    var doubleTapAction: GestureAction
        get() = runCatching { GestureAction.fromString(prefs.getString(KEY_DOUBLE_TAP_ACTION, GestureAction.UNLOCK_PHONE.name)!!) }.getOrDefault(GestureAction.UNLOCK_PHONE)
        set(value) = prefs.edit().putString(KEY_DOUBLE_TAP_ACTION, value.name).apply()

    var tripleTapAction: GestureAction
        get() = runCatching { GestureAction.fromString(prefs.getString(KEY_TRIPLE_TAP_ACTION, GestureAction.MARK_TAKEN.name)!!) }.getOrDefault(GestureAction.MARK_TAKEN)
        set(value) = prefs.edit().putString(KEY_TRIPLE_TAP_ACTION, value.name).apply()

    var wheelMin: Int
        get() = prefs.getInt(KEY_WHEEL_MIN, 0)
        set(value) = prefs.edit().putInt(KEY_WHEEL_MIN, value).apply()

    var wheelMax: Int
        get() = prefs.getInt(KEY_WHEEL_MAX, 60)
        set(value) = prefs.edit().putInt(KEY_WHEEL_MAX, value).apply()

    var wheelStep: Int
        get() = prefs.getInt(KEY_WHEEL_STEP, 5)
        set(value) = prefs.edit().putInt(KEY_WHEEL_STEP, value).apply()

    var isNightShiftEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_SHIFT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_SHIFT_ENABLED, value).apply()

    var nightShiftStartHour: Int
        get() = prefs.getInt(KEY_NIGHT_SHIFT_START_HOUR, 0) // 12:00 AM
        set(value) = prefs.edit().putInt(KEY_NIGHT_SHIFT_START_HOUR, value).apply()

    var nightShiftStartMin: Int
        get() = prefs.getInt(KEY_NIGHT_SHIFT_START_MIN, 0)
        set(value) = prefs.edit().putInt(KEY_NIGHT_SHIFT_START_MIN, value).apply()

    var nightShiftEndHour: Int
        get() = prefs.getInt(KEY_NIGHT_SHIFT_END_HOUR, 2) // 2:00 AM
        set(value) = prefs.edit().putInt(KEY_NIGHT_SHIFT_END_HOUR, value).apply()

    var nightShiftEndMin: Int
        get() = prefs.getInt(KEY_NIGHT_SHIFT_END_MIN, 0)
        set(value) = prefs.edit().putInt(KEY_NIGHT_SHIFT_END_MIN, value).apply()

    var nightShiftQuietCancelOffsetHours: Int
        get() = prefs.getInt(KEY_NIGHT_SHIFT_QUIET_CANCEL_HOURS, 1)
        set(value) = prefs.edit().putInt(KEY_NIGHT_SHIFT_QUIET_CANCEL_HOURS, value).apply()

    var isOverlayEnabled: Boolean
        get() = _overlayEnabledFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()
            _overlayEnabledFlow.value = value
        }

    fun setOverlayEnabledPreference(value: Boolean) {
        isOverlayEnabled = value
    }

    var brightenOnScroll: Boolean
        get() = _brightenOnScrollFlow.value
        set(value) {
            prefs.edit().putBoolean(KEY_BRIGHTEN_ON_SCROLL, value).apply()
            _brightenOnScrollFlow.value = value
        }

    fun setBrightenOnScrollPreference(value: Boolean) {
        brightenOnScroll = value
    }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()
            _geminiApiKeyFlow.value = value
        }
    private val _geminiApiKeyFlow = MutableStateFlow(prefs.getString(KEY_GEMINI_API_KEY, "") ?: "")
    val geminiApiKeyFlow: StateFlow<String> = _geminiApiKeyFlow.asStateFlow()

    fun setGeminiApiKeyPreference(value: String) {
        geminiApiKey = value
    }

    var isInitialSeedDone: Boolean
        get() = prefs.getBoolean(KEY_INITIAL_SEED_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_INITIAL_SEED_DONE, value).apply()

    fun recordMedicineDeleted(id: Long, name: String) {
        val currentSet = prefs.getStringSet("KEY_DELETED_MEDICINES", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(id.toString())
        val cleanName = name.lowercase().trim()
        if (cleanName.isNotBlank()) {
            currentSet.add("name:$cleanName")
        }
        prefs.edit().putStringSet("KEY_DELETED_MEDICINES", currentSet).apply()
    }

    fun isMedicineDeleted(id: Long, name: String): Boolean {
        val currentSet = prefs.getStringSet("KEY_DELETED_MEDICINES", emptySet()) ?: emptySet()
        if (currentSet.contains(id.toString())) return true
        val cleanName = name.lowercase().trim()
        if (cleanName.isNotBlank() && currentSet.contains("name:$cleanName")) return true
        return false
    }

    fun isMedicineReminderEnabled(medId: Long): Boolean {
        return prefs.getBoolean("KEY_MED_REMINDER_$medId", true)
    }

    fun setMedicineReminderEnabled(medId: Long, enabled: Boolean) {
        prefs.edit().putBoolean("KEY_MED_REMINDER_$medId", enabled).apply()
    }

    var authMode: String?
        get() = prefs.getString(KEY_AUTH_MODE, null)
        set(value) {
            prefs.edit().putString(KEY_AUTH_MODE, value).apply()
            if (value == "developer") {
                if (geminiApiKey.isBlank() && DEVELOPER_GEMINI_KEY.isNotBlank()) {
                    geminiApiKey = DEVELOPER_GEMINI_KEY
                }
            } else if (value == "guest") {
                if (geminiApiKey == DEVELOPER_GEMINI_KEY || DEVELOPER_GEMINI_KEY.isBlank()) {
                    geminiApiKey = ""
                }
            }
        }

    val isDeveloperMode: Boolean
        get() = authMode == "developer"

    var lastSeenVersionCode: Long
        get() = prefs.getLong(KEY_LAST_SEEN_VERSION_CODE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SEEN_VERSION_CODE, value).apply()

    var lastSeenVersionName: String
        get() = prefs.getString(KEY_LAST_SEEN_VERSION_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SEEN_VERSION_NAME, value).apply()
}
