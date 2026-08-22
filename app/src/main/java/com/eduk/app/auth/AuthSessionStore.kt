package com.eduk.app.auth

import android.content.Context

class AuthSessionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("eduk_auth", Context.MODE_PRIVATE)

    val token: String?
        get() = preferences.getString(KEY_TOKEN, null)

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).apply()
    }

    val role: String?
        get() = preferences.getString(KEY_ROLE, null)

    fun saveRole(role: String) {
        preferences.edit().putString(KEY_ROLE, role).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_TOKEN).remove(KEY_ROLE).apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ROLE = "user_role"
    }
}
