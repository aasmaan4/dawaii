package com.dawaii.app.service

import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dawaii.app.R
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.DoseLog
import com.dawaii.app.data.local.entity.DoseStatus
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.preferences.AppTheme
import com.dawaii.app.data.preferences.GestureAction
import com.dawaii.app.receiver.AlarmReceiver
import com.dawaii.app.ui.overlay.LinearSnapScaleHelper
import com.dawaii.app.ui.overlay.OverlayGestureDetector
import com.dawaii.app.ui.overlay.WheelAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private lateinit var preferences: AppPreferences
    private lateinit var database: AppDatabase
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var isActivated = false

    companion object {
        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_FORCE_TEST = "extra_force_test"
        private const val NOTIFICATION_CHANNEL_ID = "dawaii_overlay_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences.getInstance(this)
        database = AppDatabase.getInstance(this)
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val medicationId = intent?.getLongExtra(EXTRA_MEDICATION_ID, -1L) ?: -1L
        val scheduledTime = intent?.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val forceTest = intent?.getBooleanExtra(EXTRA_FORCE_TEST, false) ?: false

        if (!preferences.isOverlayEnabled && !forceTest) {
            postStandardNotification()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val overlayIntent = Intent(this, com.dawaii.app.ui.overlay.LockScreenOverlayActivity::class.java).apply {
                putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_MEDICATION_ID, medicationId)
                putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, scheduledTime)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(overlayIntent)
        } catch (_: Exception) {
            postStandardNotification()
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun postStandardNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("DAWAII!! Reminder")
            .setContentText(preferences.overlayText.ifBlank { "Time for your scheduled medicine dose." })
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (preferences.isChimeEnabled && preferences.chimeOnNotification) {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            builder.setSound(notificationUri)
        }

        manager.notify(2001, builder.build())
    }

    private fun startForegroundNotification() {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medication Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("DAWAII!! Active")
            .setContentText("Lock screen reminder is active")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @Suppress("DEPRECATION")
    private fun setupOverlay(medicationId: Long, scheduledTime: Long) {
        if (overlayView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }

        val flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutParamsType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val inflater = LayoutInflater.from(this)
        val isSakura = preferences.theme == AppTheme.SAKURA
        val layoutRes = if (isSakura) R.layout.layout_overlay_sakura else R.layout.layout_overlay_normal
        overlayView = inflater.inflate(layoutRes, null)

        val root = overlayView!!.findViewById<View>(R.id.overlayRoot)
        val dimnessFactor = (preferences.initialDimness / 100f).coerceIn(0.10f, 0.95f)
        root.alpha = dimnessFactor

        // Set dynamic date (e.g. OCTOBER 1)
        val tvDate = overlayView!!.findViewById<TextView?>(R.id.tvOverlayDate)
        val dateFormat = SimpleDateFormat("MMMM d", Locale.US)
        tvDate?.text = dateFormat.format(Date()).uppercase()

        // Set dynamic middle text bound to DataStore/Preferences
        val tvPrompt = overlayView!!.findViewById<TextView?>(R.id.tvOverlayPrompt)
        tvPrompt?.text = preferences.overlayText.ifBlank { "DAWA!" }

        // Clock configuration according to 12hr / 24hr preference
        val clockTime = overlayView!!.findViewById<TextClock?>(R.id.digitalClockTime)
        val clockAmPm = overlayView!!.findViewById<TextClock?>(R.id.digitalClockAmPm)
        if (preferences.is24HourFormat) {
            clockTime?.format12Hour = "HH:mm"
            clockTime?.format24Hour = "HH:mm"
            clockAmPm?.visibility = View.GONE
        } else {
            clockTime?.format12Hour = "h:mm"
            clockTime?.format24Hour = "h:mm"
            clockAmPm?.visibility = View.VISIBLE
        }

        // Foliage Entrance Animation (Sakura Theme only)
        if (isSakura) {
            animateSakuraFoliage(overlayView!!)
        }

        // Horizontal Snap Scrollwheel setup & Tap Confirmation
        val rvWheel = overlayView!!.findViewById<RecyclerView>(R.id.rvScrollWheel)
        setupScrollWheel(rvWheel, medicationId)

        // Brighten when adjusting scrollwheel if enabled (default ON)
        if (preferences.brightenOnScroll) {
            rvWheel.isEnabled = true
            rvWheel.setOnTouchListener { _, event ->
                if (!isActivated && (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE)) {
                    handleSingleTap(root, rvWheel)
                }
                false
            }
        } else {
            // Lock scrollwheel initially until single tap
            rvWheel.isEnabled = false
            rvWheel.setOnTouchListener { _, _ -> !isActivated }
        }

        // Setup Muscle-Memory Gestures
        val gestureDetector = OverlayGestureDetector(
            onSingleTap = { handleSingleTap(root, rvWheel) },
            onDoubleTap = { handleAction(preferences.doubleTapAction, medicationId, scheduledTime) },
            onTripleTap = { handleAction(preferences.tripleTapAction, medicationId, scheduledTime) }
        )
        root.setOnTouchListener(gestureDetector)

        // Add View to WindowManager
        windowManager?.addView(overlayView, params)

        // Conditional Chime: Only play if master chime switch AND lock-screen overlay chime toggle are ON
        if (preferences.isChimeEnabled && preferences.chimeOnOverlay) {
            val delayMs = preferences.chimeDelaySeconds * 1000L
            handler.postDelayed({ playAudioChime() }, delayMs)
        }
    }

    private fun animateSakuraFoliage(view: View) {
        val ivTopLeft = view.findViewById<ImageView?>(R.id.ivFoliageTopLeft)
        val ivBottomRight = view.findViewById<ImageView?>(R.id.ivFoliageBottomRight)
        val density = view.resources.displayMetrics.density
        val restYTop = -70f * density
        val restXTop = 0f * density
        val restYBottom = 70f * density
        val restXBottom = 0f * density

        // Top vine: crowns top area, slides down into resting place (restXTop, restYTop)
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

        // Bottom vine: stretched across left/right edges, slides upward into resting place
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

    private var selectedPostponeMinutes: Int = 15

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

        // Scrolling selects the number visually; tapping confirms the postponing
        val snapHelper = LinearSnapScaleHelper { centerValue ->
            selectedPostponeMinutes = centerValue
        }
        snapHelper.attachToRecyclerView(recyclerView)

        // Initial scroll to 15 if present
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
                    val root = overlayView?.findViewById<View>(R.id.overlayRoot)
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
            GestureAction.UNLOCK_PHONE -> dismissOverlay()
            GestureAction.MARK_TAKEN -> markDoseTaken(medicationId, scheduledTime)
            GestureAction.NONE -> {}
        }
    }

    private fun postponeDose(medicationId: Long, snoozeMinutes: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

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

        serviceScope.launch(Dispatchers.IO) {
            database.doseLogDao().insertLog(
                DoseLog(
                    medicationId = medicationId,
                    scheduledTimeMillis = triggerAtMillis,
                    actualTriggerTimeMillis = System.currentTimeMillis(),
                    status = DoseStatus.SNOOZED,
                    snoozeMinutes = snoozeMinutes,
                    triggerType = "SNOOZED"
                )
            )
        }

        destroyOverlay()
    }

    private fun markDoseTaken(medicationId: Long, scheduledTime: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }

        serviceScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            database.doseLogDao().markActiveDoseTaken(medicationId, now)
            database.doseLogDao().insertLog(
                DoseLog(
                    medicationId = medicationId,
                    scheduledTimeMillis = scheduledTime,
                    actualTriggerTimeMillis = now,
                    actionTimeMillis = now,
                    status = DoseStatus.TAKEN,
                    triggerType = "GESTURE"
                )
            )
        }

        destroyOverlay()
    }

    private fun dismissOverlay() {
        destroyOverlay()
    }

    private fun playAudioChime() {
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(this, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    private fun destroyOverlay() {
        handler.removeCallbacksAndMessages(null)
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeViewImmediate(overlayView)
            } catch (e: Exception) {
                try {
                    windowManager?.removeView(overlayView)
                } catch (_: Exception) {}
            }
            overlayView = null
        }

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {}

        com.dawaii.app.receiver.MedicationAlarmScheduler.scheduleAllAlarms(applicationContext)

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyOverlay()
        serviceScope.cancel()
    }
}
