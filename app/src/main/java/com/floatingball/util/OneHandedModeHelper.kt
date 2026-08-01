package com.floatingball.util

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

object OneHandedModeHelper {

    private const val TAG = "OneHandedModeHelper"
    private var isActive: Boolean = false

    /**
     * Called when user triggers the ONE_HANDED action (e.g. double-click).
     * Toggles state: enter if off, exit if on.
     * @param onEnter called after entering (for ball compensation animation)
     * @param onExit  called after exiting (for ball restoration animation)
     */
    fun toggle(
        context: Context,
        service: AccessibilityService,
        onEnter: () -> Unit,
        onExit: () -> Unit
    ) {
        isActive = !isActive
        if (isActive) {
            doEnter(context, service)
            // Wait for system animation, then compensate
            Handler(Looper.getMainLooper()).postDelayed({ onEnter() }, 400)
        } else {
            doExit(context, service)
            // Restore immediately (exit animation is fast)
            Handler(Looper.getMainLooper()).postDelayed({ onExit() }, 50)
        }
    }

    /** Check if WE think one-handed mode is active */
    fun isModeActive(): Boolean = isActive
    fun forceReset() { isActive = false }

    private fun doEnter(context: Context, service: AccessibilityService) {
        if (tryWrite(context, 1)) {
            Log.d(TAG, "Entered via Settings")
            return
        }
        // Fallback: simulate bottom-bar swipe down
        val m = service.resources.displayMetrics
        val cx = m.widthPixels / 2f
        val path = Path().apply {
            moveTo(cx, m.heightPixels - 30f)
            lineTo(cx, m.heightPixels - 220f)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 300)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        Log.d(TAG, "Entered via gesture")
    }

    private fun doExit(context: Context, service: AccessibilityService) {
        if (tryWrite(context, 0)) {
            Log.d(TAG, "Exited via Settings")
            return
        }
        // Fallback: tap blank area at top
        val m = service.resources.displayMetrics
        val x = m.widthPixels / 2f
        val path = Path().apply { moveTo(x, 50f); lineTo(x, 50f) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        Log.d(TAG, "Exited via gesture")
    }

    private fun tryWrite(context: Context, value: Int): Boolean {
        return try {
            Settings.Secure.putInt(context.contentResolver, "one_handed_mode_activated", value)
            true
        } catch (_: Exception) { false }
    }
}
