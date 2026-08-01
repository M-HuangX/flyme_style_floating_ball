package com.floatingball.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Helper to detect HyperOS/MIUI and open relevant system settings pages.
 */
object HyperOSGuideHelper {

    private const val TAG = "HyperOSGuideHelper"

    /**
     * Check if the device is running Xiaomi HyperOS or MIUI.
     */
    fun isXiaomiDevice(): Boolean {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        return brand.contains("xiaomi") || brand.contains("redmi") ||
                brand.contains("poco") || manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi")
    }

    /**
     * Open system Accessibility settings so user can enable our service.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Cannot open accessibility settings", e)
        }
    }

    /**
     * Open "Display over other apps" permission page for this app.
     */
    fun openOverlaySettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Cannot open overlay settings", e)
        }
    }

    /**
     * Open battery optimization page so user can set "No restrictions".
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback: open general battery settings
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: ActivityNotFoundException) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    /**
     * Try to open Xiaomi-specific Autostart settings page.
     * Falls back to app info page if the Xiaomi activity is not found.
     */
    fun openAutostartSettings(context: Context) {
        try {
            val intent = Intent().apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Xiaomi autostart activity not found, opening app info")
            openAppInfoSettings(context)
        }
    }

    /**
     * Open app info settings page.
     */
    fun openAppInfoSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open notification access settings so user can enable our NotificationListener
     * to auto-dismiss the "displaying over other apps" notification.
     */
    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openAppInfoSettings(context)
        }
    }
}
