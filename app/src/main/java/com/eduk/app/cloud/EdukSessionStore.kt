package com.eduk.app.cloud

import android.content.Context
import android.os.Build
import android.provider.Settings

class EdukSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("eduk_cloud_session", Context.MODE_PRIVATE)

    fun saveParentSession(token: String, familyId: String) {
        preferences.edit()
            .putString(KEY_PARENT_TOKEN, token)
            .putString(KEY_FAMILY_ID, familyId)
            .apply()
    }

    fun parentToken(): String? = preferences.getString(KEY_PARENT_TOKEN, null)

    fun saveStudentSession(token: String, childId: String) {
        preferences.edit()
            .putString(KEY_STUDENT_TOKEN, token)
            .putString(KEY_CHILD_ID, childId)
            .apply()
    }

    fun studentToken(): String? = preferences.getString(KEY_STUDENT_TOKEN, null)

    fun deviceId(): String = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        ?: "eduk-${Build.MODEL.hashCode()}"

    fun deviceLabel(): String = listOf(Build.MANUFACTURER, Build.MODEL)
        .joinToString(" ")
        .trim()
        .ifBlank { "Student Android device" }

    companion object {
        private const val KEY_PARENT_TOKEN = "parent_token"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_STUDENT_TOKEN = "student_token"
        private const val KEY_CHILD_ID = "child_id"
    }
}
