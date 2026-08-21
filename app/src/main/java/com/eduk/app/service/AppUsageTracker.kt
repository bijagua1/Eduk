package com.eduk.app.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/** Tracks foreground time locally so a server-authorized per-app limit can still work while offline. */
internal class AppUsageTracker(context: Context) {
    private val preferences = context.getSharedPreferences("eduk_app_usage", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun beginForeground(packageName: String) {
        val now = System.currentTimeMillis()
        resetIfNewDay(now)
        flushActiveUsage(now)
        preferences.edit()
            .putString(KEY_ACTIVE_PACKAGE, packageName)
            .putLong(KEY_ACTIVE_STARTED_AT, now)
            .apply()
    }

    fun currentForegroundPackage(): String? = preferences.getString(KEY_ACTIVE_PACKAGE, null)

    fun remainingMillis(packageName: String, dailyLimitMinutes: Int): Long {
        val now = System.currentTimeMillis()
        resetIfNewDay(now)
        val used = usageMillis(packageName, now)
        return (dailyLimitMinutes.coerceAtLeast(0) * 60_000L - used).coerceAtLeast(0L)
    }

    private fun usageMillis(packageName: String, now: Long): Long {
        val persisted = usageMap()[packageName] ?: 0L
        val activePackage = preferences.getString(KEY_ACTIVE_PACKAGE, null)
        val activeStartedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT, now)
        val activeDelta = if (activePackage == packageName) (now - activeStartedAt).coerceAtLeast(0L) else 0L
        return persisted + activeDelta
    }

    private fun flushActiveUsage(now: Long) {
        val activePackage = preferences.getString(KEY_ACTIVE_PACKAGE, null) ?: return
        val startedAt = preferences.getLong(KEY_ACTIVE_STARTED_AT, now)
        val elapsed = (now - startedAt).coerceAtLeast(0L)
        if (elapsed == 0L) return
        val usage = usageMap()
        usage[activePackage] = (usage[activePackage] ?: 0L) + elapsed
        preferences.edit()
            .putString(KEY_USAGE_JSON, gson.toJson(usage))
            .putLong(KEY_ACTIVE_STARTED_AT, now)
            .apply()
    }

    private fun resetIfNewDay(now: Long) {
        val today = dayKey(now)
        if (preferences.getInt(KEY_DAY, -1) != today) {
            preferences.edit()
                .putInt(KEY_DAY, today)
                .remove(KEY_USAGE_JSON)
                .remove(KEY_ACTIVE_PACKAGE)
                .remove(KEY_ACTIVE_STARTED_AT)
                .apply()
        }
    }

    private fun usageMap(): MutableMap<String, Long> {
        val type = object : TypeToken<MutableMap<String, Long>>() {}.type
        return runCatching { gson.fromJson<MutableMap<String, Long>>(preferences.getString(KEY_USAGE_JSON, null), type) }
            .getOrNull() ?: mutableMapOf()
    }

    private fun dayKey(timestamp: Long): Int = Calendar.getInstance().run {
        timeInMillis = timestamp
        get(Calendar.YEAR) * 400 + get(Calendar.DAY_OF_YEAR)
    }

    private companion object {
        const val KEY_DAY = "usage_day"
        const val KEY_USAGE_JSON = "usage_json"
        const val KEY_ACTIVE_PACKAGE = "active_package"
        const val KEY_ACTIVE_STARTED_AT = "active_started_at"
    }
}
