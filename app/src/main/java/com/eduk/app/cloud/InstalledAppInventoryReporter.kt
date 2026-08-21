package com.eduk.app.cloud

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object InstalledAppInventoryReporter {
    fun collectVisibleLaunchableApps(context: Context): List<InstalledAppReport> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return activities
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                val applicationInfo = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
                    ?: return@mapNotNull null
                val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
                InstalledAppReport(
                    packageName = packageName,
                    displayName = resolveInfo.loadLabel(packageManager).toString().trim().ifBlank { packageName },
                    versionName = versionName,
                    isSystemApp = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.displayName.lowercase() }
            .toList()
    }
}
