package com.eduk.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.eduk.app.ui.MainActivity

/**
 * Legitimate Accessibility Service to monitor app transitions.
 * When a restricted app is opened, it redirects the user to the Eduk Question Screen.
 */
class AppMonitoringService : AccessibilityService() {

    private val restrictedApps = mutableSetOf("com.zhiliaoapp.musically", "com.google.android.youtube", "com.instagram.android")
    private var isUnlocked = false
    private var unlockUntil = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            if (restrictedApps.contains(packageName)) {
                checkAccess(packageName)
            }
        }
    }

    private fun checkAccess(packageName: String) {
        val currentTime = System.currentTimeMillis()
        if (!isUnlocked || currentTime > unlockUntil) {
            Log.d("EdukMonitor", "Blocking access to $packageName")
            isUnlocked = false
            
            // Redirect to Eduk Question Screen
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("RESTRICTED_APP", packageName)
                putExtra("TRIGGER_QUESTION", true)
            }
            startActivity(intent)
        } else {
            Log.d("EdukMonitor", "Access granted to $packageName. Time left: ${(unlockUntil - currentTime) / 1000}s")
        }
    }

    override fun onInterrupt() {
        Log.e("EdukMonitor", "Service Interrupted")
    }

    // This would be called by the ViewModel when a question is answered correctly
    fun grantAccess(minutes: Int) {
        isUnlocked = true
        unlockUntil = System.currentTimeMillis() + (minutes * 60 * 1000)
    }
}
