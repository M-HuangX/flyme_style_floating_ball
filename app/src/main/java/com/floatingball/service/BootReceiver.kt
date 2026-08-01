package com.floatingball.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Schedules health checks after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent?.action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            Log.d(TAG, "Boot completed, scheduling health checks")
            HealthCheckReceiver.schedule(context)
        }
    }
}
