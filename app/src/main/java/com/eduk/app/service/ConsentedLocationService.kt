package com.eduk.app.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudentLocationReportRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground-only, visible location sharing. It starts only after the student
 * explicitly accepts sharing on a device whose parent enabled consent in Cloud.
 */
class ConsentedLocationService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionStore by lazy { EdukSessionStore(this) }
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val pendingReports by lazy { PendingLocationReportStore(this) }
    private val connectivityManager by lazy { getSystemService(ConnectivityManager::class.java) }
    private var connectivityRegistered = false
    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch { deliverPendingReport() }
        }
    }
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val token = sessionStore.studentToken() ?: return stopSharing()
            scope.launch {
                deliverReport(
                    StudentLocationReportRequest(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy.coerceAtLeast(0f).toInt(),
                        batteryPercent = null
                    )
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!sessionStore.isLocationSharingActive() || !hasLocationPermission()) {
            stopSharing()
            return START_NOT_STICKY
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification("Ready to share a consented location."))
        registerConnectivityCallback()
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS * 2)
            .build()
        requestUpdatesSafely(request)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        locationClient.removeLocationUpdates(callback)
        if (connectivityRegistered) runCatching { connectivityManager.unregisterNetworkCallback(connectivityCallback) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun requestUpdatesSafely(request: LocationRequest) {
        if (!hasLocationPermission()) return stopSharing()
        runCatching { locationClient.requestLocationUpdates(request, callback, mainLooper) }
            .onFailure { stopSharing() }
    }

    private fun stopSharing() {
        pendingReports.clear()
        sessionStore.setLocationSharingActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Eduk family location sharing", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows when Eduk is sharing the student device location with a parent."
            }
        )
    }

    private suspend fun deliverPendingReport() {
        val pendingReport = pendingReports.read() ?: return
        deliverReport(pendingReport)
    }

    private suspend fun deliverReport(report: StudentLocationReportRequest) {
        val token = sessionStore.studentToken() ?: return stopSharing()
        runCatching { EdukCloudRepository.reportStudentLocation(token, report) }
            .onSuccess {
                pendingReports.clear()
                updateNotification("Location shared with your family just now.")
            }
            .onFailure {
                pendingReports.save(report)
                updateNotification("Waiting to securely send your location when internet returns.")
            }
    }

    private fun registerConnectivityCallback() {
        if (connectivityRegistered) return
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(connectivityCallback)
            connectivityRegistered = true
        }
        scope.launch { deliverPendingReport() }
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(message))
    }

    private fun notification(message: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(com.eduk.app.R.mipmap.ic_eduk_launcher)
        .setContentTitle("Eduk location sharing is on")
        .setContentText(message)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .build()

    companion object {
        private const val CHANNEL_ID = "eduk_location_sharing"
        private const val NOTIFICATION_ID = 701
        private const val UPDATE_INTERVAL_MS = 15 * 60 * 1000L
        private const val MIN_UPDATE_INTERVAL_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ConsentedLocationService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConsentedLocationService::class.java))
        }
    }
}
