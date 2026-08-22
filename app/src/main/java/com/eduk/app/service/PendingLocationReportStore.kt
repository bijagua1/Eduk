package com.eduk.app.service

import android.content.Context
import com.eduk.app.cloud.StudentLocationReportRequest
import com.google.gson.Gson

/** Holds only the latest consented location until it is delivered or sharing is stopped. */
internal class PendingLocationReportStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("eduk_pending_location", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(report: StudentLocationReportRequest) {
        preferences.edit().putString(KEY_REPORT, gson.toJson(report)).apply()
    }

    fun read(): StudentLocationReportRequest? = preferences.getString(KEY_REPORT, null)
        ?.let { runCatching { gson.fromJson(it, StudentLocationReportRequest::class.java) }.getOrNull() }

    fun clear() {
        preferences.edit().remove(KEY_REPORT).apply()
    }

    private companion object {
        const val KEY_REPORT = "latest_consent_pending_report"
    }
}
