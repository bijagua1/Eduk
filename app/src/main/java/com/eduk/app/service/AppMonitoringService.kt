package com.eduk.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.eduk.app.ui.MainActivity

class AppMonitoringService : AccessibilityService() {

    companion object {
        private var instance: AppMonitoringService? = null
        
        fun grantAccess(minutes: Int) {
            instance?.performGrant(minutes)
        }

        fun setBlockingEnabled(enabled: Boolean) {
            instance?.applyBlockingState(enabled)
        }
        
        fun isServiceRunning(): Boolean = instance != null
    }

    private val restrictedApps = mutableSetOf(
        "com.zhiliaoapp.musically", 
        "com.google.android.youtube", 
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android"
    )
    
    private var isUnlocked = false
    private var unlockUntil = 0L
    private var isBlockingEnabled = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("EdukMonitor", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // Don't block our own app
            if (packageName == "com.eduk.app") return
            
            if (restrictedApps.contains(packageName)) {
                checkAccess(packageName)
            }
        }
    }

    private fun checkAccess(packageName: String) {
        if (!isBlockingEnabled) return
        val currentTime = System.currentTimeMillis()
        if (!isUnlocked || currentTime > unlockUntil) {
            Log.d("EdukMonitor", "Blocking access to $packageName")
            isUnlocked = false
            
            // Redirect to Eduk Question Screen
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("RESTRICTED_APP", packageName)
                putExtra("TRIGGER_QUESTION", true)
            }
            startActivity(intent)
        }
    }

    private fun performGrant(minutes: Int) {
        isUnlocked = true
        unlockUntil = System.currentTimeMillis() + (minutes * 60 * 1000)
        Log.d("EdukMonitor", "Access granted for $minutes minutes")
    }

    private fun applyBlockingState(enabled: Boolean) {
        isBlockingEnabled = enabled
        if (!enabled) {
            isUnlocked = true
            unlockUntil = Long.MAX_VALUE
        }
        Log.d("EdukMonitor", "Remote blocking state updated: $enabled")
    }

    override fun onInterrupt() {
        Log.e("EdukMonitor", "Service Interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
