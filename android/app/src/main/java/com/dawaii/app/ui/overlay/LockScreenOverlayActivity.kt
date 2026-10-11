package com.dawaii.app.ui.overlay

import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dawaii.app.R
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.DoseLog
import com.dawaii.app.data.local.entity.DoseStatus
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.preferences.AppTheme
import com.dawaii.app.data.preferences.GestureAction
import com.dawaii.app.receiver.AlarmReceiver
import com.dawaii.app.util.MedicationNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.dawaii.app.receiver.MedicationAlarmScheduler

class LockScreenOverlayActivity : ComponentActivity() {

    companion object {
        private const val TAG = "LockScreenOverlayAct"
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_FORCE_TEST = "extra_force_test"
    }

    private lateinit var preferences: AppPreferences
    private lateinit var database: AppDatabase
    private var isActivated = false
    private var selectedPostponeMinutes: Int = 15
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var currentMedication: Medicine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = AppPreferences.getInstance(this)
        database = AppDatabase.getInstance(this)

        val isForceTest = intent.getBooleanExtra(EXTRA_FORCE_TEST, false)
        if (!preferences.isOverlayEnabled && !isForceTest) {
            Log.d(TAG, "Overlay disabled in user settings and not a test preview. Finishing LockScreenOverlayActivity.")
            finish()
            return
        }

        // 1. Android OS Alignment: Allow activity to wake screen and display above keyguard
        setupLockScreenFlags()

        // 2. Acquire hardware wake lock to physically wake display from sleep
        acquireWakeLock()

        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        val isSakura = preferences.theme == AppTheme.SAKURA
        val layoutRes = if (isSakura) R.layout.layout_overlay_sakura else R.layout.layout_overlay_normal
        setContentView(layoutRes)

        initViews(medicationId, scheduledTime, isSakura)

        // Play audio chime if configured
        if (preferences.isChimeEnabled && preferences.chimeOnOverlay) {
            val delayMs = preferences.chimeDelaySeconds * 1000L
            handler.postDelayed({ playAudioChime() }, delayMs)
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "dawaii:lockscreen_activity_wake"
            )
            wakeLock?.acquire(15000L) // 15 seconds
            Log.d(TAG, "Screen bright wake lock acquired")
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire wake lock: ${e.message}")
        }
    }

    private fun initViews(medicationId: Long, scheduledTime: Long, isSakura: Boolean) {
        val root = findViewById<View>(R.id.overlayRoot)
        val dimnessFactor = (preferences.initialDimness / 100f).coerceIn(0.10f, 0.95f)
        root.alpha = dimnessFactor

        // Date Display
        val tvDate = findViewById<TextView?>(R.id.tvOverlayDate)
        val dateFormat = SimpleDateFormat("MMMM d", Locale.US)
        tvDate?.text = dateFormat.format(Date()).uppercase()

        // Prompt / Medicine Info
        val tvPrompt = findViewById<TextView?>(R.id.tvOverlayPrompt)
        tvPrompt?.text = preferences.overlayText.ifBlank { "TIME TO TAKE MEDICINE" }

        lifecycleScope.launch(Dispatchers.IO) {
            val medicine = database.medicineDao().getMedicineById(medicationId)
            currentMedication = medicine
            if (medicine != null) {
                withContext(Dispatchers.Main) {
                    val promptCustom = preferences.overlayText.ifBlank { "TIME TO TAKE" }
                    tvPrompt?.text = "${medicine.name.uppercase()} • ${medicine.dosage}\n$promptCustom"
                }
            }
        }

        // Clock configuration (12h vs 24h)
        val clockTime = findViewById<TextClock?>(R.id.digitalClockTime)
        val clockAmPm = findViewById<TextClock?>(R.id.digitalClockAmPm)
        if (preferences.is24HourFormat) {
            clockTime?.format12Hour = "HH:mm"
            clockTime?.format24Hour = "HH:mm"
            clockAmPm?.visibility = View.GONE
        } else {
            clockTime?.format12Hour = "h:mm"
            clockTime?.format24Hour = "h:mm"
            clockAmPm?.visibility = View.VISIBLE
        }

        // Foliage Animation for Sakura Theme
        if (isSakura) {
            animateSakuraFoliage()
        }

        // Horizontal Snap Scrollwheel
        val rvWheel = findViewById<RecyclerView>(R.id.rvScrollWheel)
        setupScrollWheel(rvWheel, medicationId)

        if (preferences.brightenOnScroll) {
            rvWheel.isEnabled = true
            rvWheel.setOnTouchListener { _, event ->
                if (!isActivated && (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE)) {
                    handleSingleTap(root, rvWheel)
                }
                false
            }
        } else {
            rvWheel.isEnabled = false
            rvWheel.setOnTouchListener { _, _ -> !isActivated }
        }

        // Muscle-Memory Gestures
        val gestureDetector = OverlayGestureDetector(
            onSingleTap = { handleSingleTap(root, rvWheel) },
            onDoubleTap = { handleAction(preferences.doubleTapAction, medicationId, scheduledTime) },
            onTripleTap = { handleAction(preferences.tripleTapAction, medicationId, scheduledTime) }
        )
        root.setOnTouchListener(gestureDetector)
    }

    private fun animateSakuraFoliage() {
        val ivTopLeft = findViewById<ImageView?>(R.id.ivFoliageTopLeft)
        val ivBottomRight = findViewById<ImageView?>(R.id.ivFoliageBottomRight)
        val density = resources.displayMetrics.density
        val restYTop = -70f * density
        val restXTop = 0f * density
        val restYBottom = 70f * density
        val restXBottom = 0f * density

        ivTopLeft?.apply {
            translationX = restXTop
            translationY = restYTop - (30f * density)
            alpha = 0f
            animate()
                .translationX(restXTop)
                .translationY(restYTop)
                .alpha(0.95f)
                .setDuration(950)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        ivBottomRight?.apply {
            translationX = restXBottom
            translationY = restYBottom + (30f * density)
            alpha = 0f
            animate()
                .translationX(restXBottom)
                .translationY(restYBottom)
                .alpha(0.95f)
                .setDuration(950)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun setupScrollWheel(recyclerView: RecyclerView, medicationId: Long) {
        val min = preferences.wheelMin
        val max = preferences.wheelMax
        val step = preferences.wheelStep
        val numbers = (min..max step step).toList()

        selectedPostponeMinutes = if (numbers.contains(15)) 15 else (numbers.firstOrNull() ?: 15)

        val adapter = WheelAdapter(numbers, preferences.theme) { tappedValue ->
            selectedPostponeMinutes = tappedValue
            postponeDose(medicationId, tappedValue)
        }
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        val screenWidth = resources.displayMetrics.widthPixels
        val itemWidth = (90 * resources.displayMetrics.density).toInt()
        val sidePadding = (screenWidth / 2) - (itemWidth / 2)
        recyclerView.setPadding(sidePadding, 0, sidePadding, 0)

        val snapHelper = LinearSnapScaleHelper { centerValue ->
            selectedPostponeMinutes = centerValue
        }
        snapHelper.attachToRecyclerView(recyclerView)

        val initialIdx = numbers.indexOf(selectedPostponeMinutes)
        if (initialIdx >= 0) {
            recyclerView.scrollToPosition(initialIdx)
        }

        recyclerView.post {
            snapHelper.applyScaling(recyclerView)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                if (dx != 0 && preferences.brightenOnScroll && !isActivated) {
                    val root = findViewById<View>(R.id.overlayRoot)
                    if (root != null) handleSingleTap(root, rv)
                }
            }
        })

        recyclerView.setOnClickListener {
            postponeDose(medicationId, selectedPostponeMinutes)
        }
    }

    private fun handleSingleTap(root: View, recyclerView: RecyclerView) {
        if (!isActivated) {
            isActivated = true
            val animator = ValueAnimator.ofFloat(root.alpha, 1.0f)
            animator.duration = 350
            animator.addUpdateListener { root.alpha = it.animatedValue as Float }
            animator.start()

            recyclerView.isEnabled = true
            recyclerView.setOnTouchListener(null)
        }
    }

    private fun handleAction(action: GestureAction, medicationId: Long, scheduledTime: Long) {
        when (action) {
            GestureAction.UNLOCK_PHONE -> finish()
            GestureAction.MARK_TAKEN -> markDoseTaken(medicationId, scheduledTime)
            GestureAction.NONE -> {}
        }
    }

    private fun postponeDose(medicationId: Long, snoozeMinutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val cal = Calendar.getInstance().apply { timeInMillis = triggerAtMillis }
        val newHour = cal.get(Calendar.HOUR_OF_DAY)
        val newMinute = cal.get(Calendar.MINUTE)

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULED_TIME, triggerAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)

        lifecycleScope.launch(Dispatchers.IO) {
            val med = database.medicineDao().getMedicineById(medicationId)
            if (med != null) {
                val updatedMed = med.copy(scheduledHour = newHour, scheduledMinute = newMinute)
                database.medicineDao().updateMedicine(updatedMed)
                MedicationAlarmScheduler.scheduleMedicineAlarm(applicationContext, updatedMed)
            }

            database.doseLogDao().insertLog(
                DoseLog(
                    medicationId = medicationId,
                    scheduledTimeMillis = triggerAtMillis,
                    actualTriggerTimeMillis = System.currentTimeMillis(),
                    status = DoseStatus.SNOOZED,
                    snoozeMinutes = snoozeMinutes,
                    triggerType = "OVERLAY_WHEEL"
                )
            )
        }

        finish()
    }

    private fun markDoseTaken(medicationId: Long, scheduledTime: Long) {
        MedicationNotificationHelper.markDoseTaken(
            context = applicationContext,
            medicationId = medicationId,
            scheduledTime = scheduledTime,
            triggerType = "LOCKSCREEN_GESTURE"
        )
        finish()
    }

    private fun playAudioChime() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(this, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
            } catch (_: Exception) {}
        }
    }
}
