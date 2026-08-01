package com.floatingball.service

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager

/**
 * Manages auto-hide blacklist: hides the floating ball when
 * certain fullscreen apps (camera, video players, etc.) are foreground.
 */
object HideAppManager {

    private val defaultPackages = setOf(
        "com.android.camera", "com.android.camera2",
        "com.google.android.GoogleCamera",
        "com.google.android.youtube",
        "com.android.gallery3d",
        "com.miui.gallery", "com.miui.video",
        "com.google.android.apps.photos",
        "com.netflix.mediaclient",
        "com.tencent.qqlive",    // Tencent Video
        "com.qiyi.video",        // iQiyi
        "com.bilibili.app.in",   // Bilibili
        "com.ss.android.ugc.aweme", // TikTok
        "com.zhiliaoapp.musically", // TikTok Lite
    )

    fun isEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean("auto_hide_enabled", true)
    }

    fun shouldHide(context: Context, packageName: String?): Boolean {
        if (packageName == null) return false
        if (!isEnabled(context)) return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val customSet = prefs.getStringSet("auto_hide_packages", emptySet()) ?: emptySet()
        val allPackages = defaultPackages + customSet
        return allPackages.contains(packageName)
    }

    /**
     * Check if the device is in landscape orientation.
     * Video watching and camera typically use landscape = ball would be in the way.
     */
    fun isLandscape(service: android.accessibilityservice.AccessibilityService): Boolean {
        return service.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
    }

    /** Get all installed apps that can be hidden */
    fun getInstalledLaunchableApps(context: Context): Map<String, String> {
        val pm = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val activities = pm.queryIntentActivities(intent, 0)
        val result = mutableMapOf<String, String>()
        for (ri in activities) {
            val pkg = ri.activityInfo.packageName
            val label = ri.loadLabel(pm).toString()
            result[pkg] = label
        }
        return result.toSortedMap(compareBy { result[it] ?: it })
    }
}
