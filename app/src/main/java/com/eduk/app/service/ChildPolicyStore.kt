package com.eduk.app.service

import android.content.Context
import com.eduk.app.cloud.CloudAppRule
import com.eduk.app.cloud.CloudSchedule
import com.eduk.app.cloud.StudentPolicyResponse
import com.google.gson.Gson
import java.util.Calendar

/**
 * Keeps the last server-authorized control policy available when the child
 * device temporarily has no network connection. It never stores credentials.
 */
class ChildPolicyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(response: StudentPolicyResponse) {
        preferences.edit()
            .putString(KEY_POLICY_JSON, gson.toJson(response))
            .putLong(KEY_SYNCED_AT, System.currentTimeMillis())
            .apply()
    }

    fun grantAccess(minutes: Int) {
        val current = preferences.getLong(KEY_ACCESS_UNTIL, 0L)
        val base = maxOf(System.currentTimeMillis(), current)
        preferences.edit().putLong(KEY_ACCESS_UNTIL, base + minutes * 60_000L).apply()
    }

    fun clearAccess() {
        preferences.edit().putLong(KEY_ACCESS_UNTIL, 0L).apply()
    }

    fun isAccessCurrentlyEarned(): Boolean = System.currentTimeMillis() < preferences.getLong(KEY_ACCESS_UNTIL, 0L)

    fun shouldGate(packageName: String): Boolean {
        if (packageName == "com.eduk.app") return false
        val policy = load() ?: return DEFAULT_GATED_PACKAGES.contains(packageName)
        if (!policy.policy.isBlockingEnabled) return false
        if (isAccessCurrentlyEarned()) return false
        if (hasDeviceLockSchedule(policy.schedules)) return true
        val matchingRule = policy.appRules.firstOrNull { it.isEnabled && it.packageName == packageName }
        return when (matchingRule?.accessMode) {
            "allow" -> false
            "block", "learning_gate" -> true
            else -> hasEntertainmentSchedule(policy.schedules) && matchingRule?.category != "Education"
        }
    }

    private fun load(): StudentPolicyResponse? = preferences.getString(KEY_POLICY_JSON, null)
        ?.let { raw -> runCatching { gson.fromJson(raw, StudentPolicyResponse::class.java) }.getOrNull() }

    private fun hasDeviceLockSchedule(schedules: List<CloudSchedule>): Boolean = schedules.any { it.isEnabled && it.mode == "device_lock" && isActiveNow(it) }

    private fun hasEntertainmentSchedule(schedules: List<CloudSchedule>): Boolean = schedules.any { it.isEnabled && it.mode == "block_entertainment" && isActiveNow(it) }

    private fun isActiveNow(schedule: CloudSchedule): Boolean {
        val calendar = Calendar.getInstance()
        val appDay = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val validDays = schedule.daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }
        if (appDay !in validDays) return false
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (schedule.startMinuteOfDay < schedule.endMinuteOfDay) {
            minute in schedule.startMinuteOfDay until schedule.endMinuteOfDay
        } else {
            minute >= schedule.startMinuteOfDay || minute < schedule.endMinuteOfDay
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "eduk_child_policy"
        private const val KEY_POLICY_JSON = "policy_json"
        private const val KEY_SYNCED_AT = "policy_synced_at"
        private const val KEY_ACCESS_UNTIL = "earned_access_until"
        private val DEFAULT_GATED_PACKAGES = setOf(
            "com.zhiliaoapp.musically",
            "com.google.android.youtube",
            "com.instagram.android",
            "com.facebook.katana",
            "com.twitter.android"
        )
    }
}
