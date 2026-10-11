package com.dawaii.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.dawaii.app.ui.overlay.LockScreenOverlayActivity
import com.dawaii.app.util.MedicationNotificationHelper

class DawaiiApp : Application() {

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize high-priority medication notification channel
        MedicationNotificationHelper.createNotificationChannel(this)

        // Track whether the user is actively using the app
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var activeActivities = 0

            override fun onActivityStarted(activity: Activity) {
                if (activity !is LockScreenOverlayActivity) {
                    activeActivities++
                    isAppInForeground = activeActivities > 0
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity !is LockScreenOverlayActivity) {
                    activeActivities = (activeActivities - 1).coerceAtLeast(0)
                    isAppInForeground = activeActivities > 0
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
