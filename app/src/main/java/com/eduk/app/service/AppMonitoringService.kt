package com.eduk.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.eduk.app.cloud.StudentPolicyResponse
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

        fun applyRemotePolicy(context: Context, policy: StudentPolicyResponse) {
            ChildPolicyStore(context).save(policy)
            instance?.reloadPolicy()
        }

        fun isServiceRunning(): Boolean = instance != null
    }

    private lateinit var policyStore: ChildPolicyStore

    override fun onServiceConnected() {
        super.onServiceConnected()
        policyStore = ChildPolicyStore(this)
        instance = this
        Log.d("EdukMonitor", "Service connected with persisted child policy")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !::policyStore.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        if (policyStore.shouldGate(packageName)) checkAccess(packageName)
    }

    private fun checkAccess(packageName: String) {
        if (policyStore.isAccessCurrentlyEarned()) return
        Log.d("EdukMonitor", "Learning gate active for $packageName")
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("RESTRICTED_APP", packageName)
            putExtra("TRIGGER_QUESTION", true)
        }
        startActivity(intent)
    }

    private fun performGrant(minutes: Int) {
        if (!::policyStore.isInitialized) return
        policyStore.grantAccess(minutes.coerceAtLeast(0))
        Log.d("EdukMonitor", "Earned access granted for $minutes minutes")
    }

    private fun applyBlockingState(enabled: Boolean) {
        if (!enabled && ::policyStore.isInitialized) policyStore.grantAccess(24 * 60)
        if (enabled && ::policyStore.isInitialized) policyStore.clearAccess()
    }

    private fun reloadPolicy() = Unit

    override fun onInterrupt() {
        Log.e("EdukMonitor", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
