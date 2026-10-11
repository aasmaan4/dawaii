package com.dawaii.app.ui.overlay

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class LinearSnapScaleHelper(
    private val onSnapSettled: (Int) -> Unit
) : LinearSnapHelper() {

    private var recyclerView: RecyclerView? = null
    private val minScale = 0.70f
    private val maxScale = 1.35f
    private val minAlpha = 0.25f
    private val maxAlpha = 1.00f

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        this.recyclerView = recyclerView

        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                applyScaling(rv)
            }

            override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
                super.onScrollStateChanged(rv, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = rv.layoutManager ?: return
                    val snapView = findSnapView(layoutManager) ?: return
                    val position = layoutManager.getPosition(snapView)
                    val adapter = rv.adapter as? WheelAdapter
                    val value = adapter?.getItemAt(position)
                    if (value != null) {
                        onSnapSettled(value)
                    }
                }
            }
        })
    }

    fun applyScaling(rv: RecyclerView) {
        val midX = rv.width / 2.0f
        if (midX <= 0f) return

        for (i in 0 until rv.childCount) {
            val child: View = rv.getChildAt(i)
            val childMidX = (child.left + child.right) / 2.0f
            val distanceFromCenter = abs(midX - childMidX)
            val normalizedDistance = (distanceFromCenter / midX).coerceIn(0.0f, 1.0f)

            // Interpolate scale & alpha
            val scale = maxScale - normalizedDistance * (maxScale - minScale)
            val alpha = maxAlpha - normalizedDistance * (maxAlpha - minAlpha)

            child.scaleX = scale
            child.scaleY = scale
            child.alpha = alpha
        }
    }
}
