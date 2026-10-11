package com.dawaii.app.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import com.dawaii.app.R
import com.dawaii.app.data.local.AppDatabase
import com.dawaii.app.data.local.entity.Medicine
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.preferences.AppTheme
import com.dawaii.app.data.preferences.GestureAction
import com.dawaii.app.service.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsActivity : Activity() {

    private lateinit var prefs: AppPreferences
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = AppPreferences(this)
        setupSeedMedication()
        bindViews()
    }

    private fun setupSeedMedication() {
        val db = AppDatabase.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            val existing = db.medicineDao().getMedicineById(1L)
            if (existing == null) {
                db.medicineDao().insertMedicine(
                    Medicine(
                        id = 1L,
                        doctorId = 1L,
                        name = "Evening Dose",
                        dosage = "1 Tablet",
                        frequency = "Daily at 11:30 PM",
                        scheduledHour = 23,
                        scheduledMinute = 30
                    )
                )
            }
        }
    }

    private fun bindViews() {
        val btnTest = findViewById<Button>(R.id.btnTestOverlay)
        val rgTheme = findViewById<RadioGroup>(R.id.rgTheme)
        val rbSakura = findViewById<RadioButton>(R.id.rbSakura)
        val rbNormal = findViewById<RadioButton>(R.id.rbNormal)
        val sbDimness = findViewById<SeekBar>(R.id.sbDimness)
        val tvDimness = findViewById<TextView>(R.id.tvDimnessLabel)
        val switchChime = findViewById<Switch>(R.id.switchChime)
        val sbChimeDelay = findViewById<SeekBar>(R.id.sbChimeDelay)
        val tvChimeDelay = findViewById<TextView>(R.id.tvChimeDelayLabel)
        val spDoubleTap = findViewById<Spinner>(R.id.spDoubleTap)
        val spTripleTap = findViewById<Spinner>(R.id.spTripleTap)
        val switchNightShift = findViewById<Switch>(R.id.switchNightShift)

        // Theme
        if (prefs.theme == AppTheme.SAKURA) rbSakura.isChecked = true else rbNormal.isChecked = true
        rgTheme.setOnCheckedChangeListener { _, checkedId ->
            prefs.theme = if (checkedId == R.id.rbSakura) AppTheme.SAKURA else AppTheme.NORMAL
        }

        // Overlay & Brighten on Scroll Switches
        val switchEnableOverlay = findViewById<Switch>(R.id.switchEnableOverlay)
        val switchEnableNotifications = findViewById<Switch>(R.id.switchEnableNotifications)
        val switchBrightenOnScroll = findViewById<Switch>(R.id.switchBrightenOnScroll)

        switchEnableOverlay.isChecked = prefs.isOverlayEnabled
        switchEnableOverlay.setOnCheckedChangeListener { _, isChecked ->
            prefs.isOverlayEnabled = isChecked
        }

        switchEnableNotifications.isChecked = prefs.isNotificationsEnabled
        switchEnableNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.isNotificationsEnabled = isChecked
        }

        switchBrightenOnScroll.isChecked = prefs.brightenOnScroll
        switchBrightenOnScroll.setOnCheckedChangeListener { _, isChecked ->
            prefs.brightenOnScroll = isChecked
        }

        // Dimness
        val dimInt = (prefs.initialDimness * 100).toInt()
        sbDimness.progress = dimInt
        tvDimness.text = "Initial Dimness Opacity: $dimInt%"
        sbDimness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val clamped = progress.coerceAtLeast(10)
                tvDimness.text = "Initial Dimness Opacity: $clamped%"
                if (fromUser) prefs.initialDimness = clamped / 100f
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // Audio Chime
        switchChime.isChecked = prefs.isChimeEnabled
        switchChime.setOnCheckedChangeListener { _, isChecked -> prefs.isChimeEnabled = isChecked }

        sbChimeDelay.progress = prefs.chimeDelaySeconds
        tvChimeDelay.text = "Chime Delay: ${prefs.chimeDelaySeconds} seconds"
        sbChimeDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvChimeDelay.text = "Chime Delay: $progress seconds"
                if (fromUser) prefs.chimeDelaySeconds = progress
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        // Gestures
        val gestureActions = GestureAction.values().map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gestureActions)
        spDoubleTap.adapter = adapter
        spTripleTap.adapter = adapter

        spDoubleTap.setSelection(gestureActions.indexOf(prefs.doubleTapAction.name))
        spTripleTap.setSelection(gestureActions.indexOf(prefs.tripleTapAction.name))

        spDoubleTap.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                prefs.doubleTapAction = GestureAction.valueOf(gestureActions[pos])
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        spTripleTap.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, pos: Int, p3: Long) {
                prefs.tripleTapAction = GestureAction.valueOf(gestureActions[pos])
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Night Shift
        switchNightShift.isChecked = prefs.isNightShiftEnabled
        switchNightShift.setOnCheckedChangeListener { _, isChecked -> prefs.isNightShiftEnabled = isChecked }

        // Trigger Test Button
        btnTest.setOnClickListener {
            val intent = Intent(this, com.dawaii.app.ui.overlay.LockScreenOverlayActivity::class.java).apply {
                putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_MEDICATION_ID, 1L)
                putExtra(com.dawaii.app.ui.overlay.LockScreenOverlayActivity.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            Toast.makeText(this, "Spawning DAWAII!! Lock Screen Overlay...", Toast.LENGTH_SHORT).show()
        }
    }
}
