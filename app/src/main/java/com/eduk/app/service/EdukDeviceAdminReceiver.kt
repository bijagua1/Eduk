package com.eduk.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class EdukDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("EdukDeviceAdmin", "Device Admin Enabled")
        Toast.makeText(context, "Eduk Device Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d("EdukDeviceAdmin", "Device Admin Disabled")
        Toast.makeText(context, "Eduk Device Admin Disabled. Restrictions may not work.", Toast.LENGTH_LONG).show()
    }
}
