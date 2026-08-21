package com.eduk.app.cloud

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant

/**
 * Holds only signed session tokens and opaque family/device identifiers.
 * Values are encrypted at rest with an Android Keystore-backed MasterKey.
 */
class EdukSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPreferences = appContext.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val preferences: SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        SECURE_PREFERENCES_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        migrateLegacySessionOnce()
    }

    fun saveParentSession(token: String, familyId: String, expiresAt: String? = null) {
        preferences.edit()
            .putString(KEY_PARENT_TOKEN, token)
            .putString(KEY_FAMILY_ID, familyId)
            .putString(KEY_PARENT_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun parentToken(): String? = preferences.getString(KEY_PARENT_TOKEN, null)

    fun replaceParentToken(token: String, expiresAt: String? = null) {
        preferences.edit().putString(KEY_PARENT_TOKEN, token).putString(KEY_PARENT_EXPIRES_AT, expiresAt).apply()
    }

    fun shouldRefreshParentSession(withinMillis: Long = 10 * 60 * 1000L) = expiresSoon(KEY_PARENT_EXPIRES_AT, withinMillis)

    fun familyId(): String? = preferences.getString(KEY_FAMILY_ID, null)

    fun clearParentSession() {
        preferences.edit()
            .remove(KEY_PARENT_TOKEN)
            .remove(KEY_FAMILY_ID)
            .remove(KEY_PARENT_EXPIRES_AT)
            .apply()
    }

    fun saveStudentSession(token: String, childId: String, expiresAt: String? = null) {
        preferences.edit()
            .putString(KEY_STUDENT_TOKEN, token)
            .putString(KEY_CHILD_ID, childId)
            .putString(KEY_STUDENT_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun studentToken(): String? = preferences.getString(KEY_STUDENT_TOKEN, null)

    fun replaceStudentToken(token: String, expiresAt: String? = null) {
        preferences.edit().putString(KEY_STUDENT_TOKEN, token).putString(KEY_STUDENT_EXPIRES_AT, expiresAt).apply()
    }

    fun shouldRefreshStudentSession(withinMillis: Long = 24 * 60 * 60 * 1000L) = expiresSoon(KEY_STUDENT_EXPIRES_AT, withinMillis)

    fun studentChildId(): String? = preferences.getString(KEY_CHILD_ID, null)

    fun clearStudentSession() {
        preferences.edit()
            .remove(KEY_STUDENT_TOKEN)
            .remove(KEY_CHILD_ID)
            .remove(KEY_STUDENT_EXPIRES_AT)
            .apply()
    }

    fun deviceId(): String = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "eduk-${Build.MODEL.hashCode()}"

    fun deviceLabel(): String = listOf(Build.MANUFACTURER, Build.MODEL)
        .joinToString(" ")
        .trim()
        .ifBlank { "Student Android device" }

    private fun migrateLegacySessionOnce() {
        if (preferences.getBoolean(KEY_MIGRATION_COMPLETE, false)) return
        preferences.edit().apply {
            legacyPreferences.getString(KEY_PARENT_TOKEN, null)?.let { putString(KEY_PARENT_TOKEN, it) }
            legacyPreferences.getString(KEY_FAMILY_ID, null)?.let { putString(KEY_FAMILY_ID, it) }
            legacyPreferences.getString(KEY_STUDENT_TOKEN, null)?.let { putString(KEY_STUDENT_TOKEN, it) }
            legacyPreferences.getString(KEY_CHILD_ID, null)?.let { putString(KEY_CHILD_ID, it) }
            putBoolean(KEY_MIGRATION_COMPLETE, true)
            apply()
        }
        legacyPreferences.edit().clear().apply()
    }

    private fun expiresSoon(key: String, withinMillis: Long): Boolean {
        val expiry = preferences.getString(key, null) ?: return false
        val expiryMillis = runCatching { Instant.parse(expiry).toEpochMilli() }.getOrNull() ?: return false
        return expiryMillis <= System.currentTimeMillis() + withinMillis
    }

    companion object {
        private const val LEGACY_PREFERENCES_NAME = "eduk_cloud_session"
        private const val SECURE_PREFERENCES_NAME = "eduk_cloud_session_secure"
        private const val KEY_MIGRATION_COMPLETE = "secure_migration_complete"
        private const val KEY_PARENT_TOKEN = "parent_token"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_PARENT_EXPIRES_AT = "parent_expires_at"
        private const val KEY_STUDENT_TOKEN = "student_token"
        private const val KEY_CHILD_ID = "child_id"
        private const val KEY_STUDENT_EXPIRES_AT = "student_expires_at"
    }
}
