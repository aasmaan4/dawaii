package com.dawaii.app.ui.overlay

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View

class OverlayGestureDetector(
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onTripleTap: () -> Unit
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var tapCount = 0
    private val multiTapTimeout = 320L // Time window to accumulate multiple taps

    private val tapRunnable = Runnable {
        when (tapCount) {
            1 -> onSingleTap()
            2 -> onDoubleTap()
            3 -> onTripleTap()
        }
        tapCount = 0
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            tapCount++
            handler.removeCallbacks(tapRunnable)

            if (tapCount >= 3) {
                onTripleTap()
                tapCount = 0
            } else {
                handler.postDelayed(tapRunnable, multiTapTimeout)
            }
            v?.performClick()
            return true
        }
        return true
    }

    fun reset() {
        handler.removeCallbacks(tapRunnable)
        tapCount = 0
    }
}
