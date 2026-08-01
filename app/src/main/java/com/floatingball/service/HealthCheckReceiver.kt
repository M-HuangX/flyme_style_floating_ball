package com.floatingball.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log

/**
 * Periodic health check: verifies the accessibility service is running
 * and tries to recover if it was killed.
 *
 * Uses AlarmManager for scheduling — alarms survive process death
 * (though HyperOS may cancel them on "Clear All").
 */
class HealthCheckReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HealthCheckReceiver"
        private const val ACTION_CHECK = "com.floatingball.HEALTH_CHECK"
        private const val CHECK_INTERVAL_MS = 15 * 1000L // 15s: ~0.01% battery/day, near-instant recovery

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HealthCheckReceiver::class.java).apply {
                action = ACTION_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + CHECK_INTERVAL_MS,
                        pendingIntent
                    )
                } else {
                    @Suppress("DEPRECATION")
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + CHECK_INTERVAL_MS,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot set exact alarm, using inexact", e)
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + CHECK_INTERVAL_MS,
                    CHECK_INTERVAL_MS,
                    pendingIntent
                )
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Health check running")

        val isEnabled = FloatingBallService.isServiceEnabled(context)

        if (!isEnabled) {
            Log.w(TAG, "Service not enabled, skipping")
            return
        }

        // Schedule next check regardless
        schedule(context)
    }
}
