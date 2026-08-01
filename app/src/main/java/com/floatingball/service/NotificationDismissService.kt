package com.floatingball.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Auto-dismisses the system "displaying over other apps" notification.
 */
class NotificationDismissService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifDismiss"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName
        val title = sbn.notification.extras.getString("android.title") ?: ""
        val text = sbn.notification.extras.getString("android.text") ?: ""

        // Log all notifications for debugging
        Log.d(TAG, "Notif: pkg=$pkg title=$title text=$text")

        // The overlay notification comes from "android" system package
        // and contains "display" / "over" / the app name
        val combined = "$title $text".lowercase()
        val isOverlayNotif = (pkg == "android" || pkg == "com.android.systemui") &&
                (combined.contains("display") ||
                 combined.contains("floating ball") ||
                 combined.contains("over other"))

        if (isOverlayNotif) {
            Log.d(TAG, "Dismissing overlay notification: $title")
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
