package com.eduk.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudentPolicyResponse
import com.eduk.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var refreshInFlight = false
    private var lastPolicyRefreshAt = 0L
    private var lastGateLaunchAt = 0L
    private val policyRefreshRunnable = object : Runnable {
        override fun run() {
            refreshRemotePolicy(force = true)
            handler.postDelayed(this, POLICY_REFRESH_INTERVAL_MILLIS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        policyStore = ChildPolicyStore(this)
        instance = this
        Log.d("EdukMonitor", "Service connected with persisted child policy")
        refreshRemotePolicy(force = true)
        handler.post(policyRefreshRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !::policyStore.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        if (policyStore.currentForegroundPackage() != packageName) {
            policyStore.beginForeground(packageName)
        }
        refreshRemotePolicy()
        evaluateForeground(packageName)
    }

    private fun evaluateForeground(packageName: String) {
        if (policyStore.shouldGate(packageName)) checkAccess(packageName)
        else scheduleGateCheck(packageName)
    }

    private fun refreshRemotePolicy(force: Boolean = false) {
        if (!::policyStore.isInitialized || refreshInFlight) return
        val now = System.currentTimeMillis()
        if (!force && now - lastPolicyRefreshAt < POLICY_REFRESH_INTERVAL_MILLIS) return
        val sessionStore = EdukSessionStore(this)
        val storedToken = sessionStore.studentToken() ?: return
        refreshInFlight = true
        lastPolicyRefreshAt = now
        serviceScope.launch {
            val activeToken = if (sessionStore.shouldRefreshStudentSession()) {
                runCatching { EdukCloudRepository.refreshStudentSession(storedToken) }
                    .onSuccess { sessionStore.replaceStudentToken(it.token, it.expiresAt) }
                    .getOrNull()?.token ?: storedToken
            } else storedToken
            runCatching { EdukCloudRepository.getStudentPolicy(activeToken) }
                .onSuccess { policy ->
                    policyStore.save(policy)
                    withContext(Dispatchers.Main) {
                        policyStore.currentForegroundPackage()?.let(::evaluateForeground)
                    }
                }
                .onFailure { Log.w("EdukMonitor", "Remote policy refresh failed; using last authorized policy") }
            refreshInFlight = false
        }
    }

    private fun scheduleGateCheck(packageName: String) {
        val delay = policyStore.nextGateCheckDelayMillis(packageName) ?: return
        if (delay <= 0L) return
        handler.postDelayed({
            if (::policyStore.isInitialized && policyStore.currentForegroundPackage() == packageName && policyStore.shouldGate(packageName)) {
                checkAccess(packageName)
            }
        }, delay + 350L)
    }

    private fun checkAccess(packageName: String) {
        if (policyStore.isAccessCurrentlyEarned()) return
        val now = System.currentTimeMillis()
        if (now - lastGateLaunchAt < 1_500L) return
        lastGateLaunchAt = now
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

    private fun reloadPolicy() {
        if (!::policyStore.isInitialized) return
        policyStore.currentForegroundPackage()?.let { packageName ->
            evaluateForeground(packageName)
        }
    }

    override fun onInterrupt() {
        Log.e("EdukMonitor", "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        serviceScope.cancel()
        instance = null
    }

    private companion object {
        const val POLICY_REFRESH_INTERVAL_MILLIS = 15_000L
    }
}
