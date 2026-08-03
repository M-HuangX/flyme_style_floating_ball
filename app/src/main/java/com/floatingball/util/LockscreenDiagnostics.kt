package com.floatingball.util

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Display

/**
 * Diagnostic logging for lock screen detection research.
 * Tag: LockscreenDiag — easy to filter from adb logcat.
 */
object LockscreenDiagnostics {

    private const val TAG = "LockscreenDiag"

    fun logTrigger(source: String, detail: String = "") {
        Log.i(TAG, "[TRIGGER] src=$source uptime=${SystemClock.uptimeMillis()} elapsed=${SystemClock.elapsedRealtime()} $detail")
    }

    fun sampleState(context: Context, label: String) {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager ?: return
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager ?: return

        val k1 = km.isKeyguardLocked
        // isDeviceLocked needs API 23+ — fine for us (minSdk 26)
        val deviceLocked = km.isDeviceLocked
        val k2 = km.isKeyguardLocked
        val interactive = pm.isInteractive
        val defaultDisplay = wm.defaultDisplay
        val displayState = defaultDisplay?.state ?: -1

        val stateStr = when (displayState) {
            Display.STATE_ON -> "ON"
            Display.STATE_OFF -> "OFF"
            Display.STATE_DOZE -> "DOZE"
            Display.STATE_DOZE_SUSPEND -> "DOZE_SUSPEND"
            else -> "UNKNOWN($displayState)"
        }

        Log.i(TAG, "[SAMPLE] label=$label k1=$k1 k2=$k2 devLocked=$deviceLocked interactive=$interactive display=$stateStr uptime=${SystemClock.uptimeMillis()}")
    }

    fun logLifecycle(event: String) {
        Log.i(TAG, "[LIFECYCLE] $event uptime=${SystemClock.uptimeMillis()} elapsed=${SystemClock.elapsedRealtime()}")
    }

    fun logAccessibility(eventType: Int, className: String?, packageName: String?, changeTypes: String = "") {
        if (eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            Log.i(TAG, "[A11Y] type=$eventType cls=$className pkg=$packageName changes=$changeTypes uptime=${SystemClock.uptimeMillis()}")
        }
    }

    fun dumpWindows(context: Context, label: String) {
        val a11yService = context as? android.accessibilityservice.AccessibilityService ?: return
        val windows = a11yService.windows
        if (windows.isNullOrEmpty()) {
            Log.i(TAG, "[WINDOWS] label=$label count=0")
            return
        }
        Log.i(TAG, "[WINDOWS] label=$label count=${windows.size}")
        for ((i, w) in windows.withIndex()) {
            val r = android.graphics.Rect()
            w.getBoundsInScreen(r)
            Log.i(TAG, "[WINDOWS]   [$i] type=${w.type} active=${w.isActive} focused=${w.isFocused} layer=${w.layer} bounds=$r")
        }
    }
}
