package com.eduk.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.StudentLocationReportRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationReporter(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    suspend fun sendCurrentLocation(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext Result.failure(SecurityException("Location permission is required."))

        val location = currentLocation()
            ?: return@withContext Result.failure(IllegalStateException("No location fix is available yet."))

        runCatching {
            val response = EdukCloudRepository.reportStudentLocation(
                studentToken = token,
                request = StudentLocationReportRequest(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyMeters = location.accuracy.toInt().coerceAtLeast(0),
                ),
            )
            check(response.accepted) { "Location server rejected the report." }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        val cached = providers.mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
        if (cached != null) return cached

        val provider = providers.firstOrNull { locationManager.isProviderEnabled(it) } ?: return null
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) continuation.resume(location)
                        locationManager.removeUpdates(this)
                    }
                }
                locationManager.requestLocationUpdates(provider, 1_000L, 1f, listener)
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            }
        }
    }
}