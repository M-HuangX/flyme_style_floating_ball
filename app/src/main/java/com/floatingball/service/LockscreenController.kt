package com.floatingball.service

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.util.Log
import android.view.Display

/**
 * Lock screen detection for HyperOS.
 *
 * Strategy (backed by diagnostic data from target device):
 * - isKeyguardLocked() is 100% reliable on HyperOS 3
 * - USER_PRESENT fires reliably with RECEIVER_EXPORTED
 * - Hide immediately on any "locked" signal
 * - Show only after stable unlock confirmation
 */
class LockscreenController(
    private val context: Context,
    private val onHide: () -> Unit,
    private val onShow: () -> Unit
) {
    companion object {
        private const val TAG = "LockscreenCtrl"
    }

    private val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var isHidden = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) {}
        override fun onDisplayRemoved(id: Int) {}
        override fun onDisplayChanged(id: Int) {
            if (id == Display.DEFAULT_DISPLAY) reconcile("display")
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "SCREEN_OFF → hide immediately")
                    hideImmediate()
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    reconcile(intent.action!!)
                }
            }
        }
    }

    fun start() {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        dm.registerDisplayListener(displayListener, null)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        context.registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)

        // Check initial state
        reconcile("start")
    }

    fun stop() {
        context.unregisterReceiver(screenReceiver)
    }

    private fun hideImmediate() {
        if (!isHidden) {
            isHidden = true
            onHide()
            Log.d(TAG, "Hidden")
        }
    }

    private fun reconcile(source: String) {
        // Check if feature is enabled
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        if (!prefs.getBoolean("hide_on_lockscreen", true)) return

        val locked = km.isKeyguardLocked
        val interactive = pm.isInteractive
        val displayOn = context.getSystemService(Context.DISPLAY_SERVICE)
            ?.let { it as? DisplayManager }?.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON

        Log.d(TAG, "reconcile src=$source locked=$locked interactive=$interactive displayOn=$displayOn")

        if (locked || !interactive || !displayOn) {
            hideImmediate()
            return
        }

        // All three conditions met: definitely unlocked
        if (isHidden) {
            isHidden = false
            onShow()
            Log.d(TAG, "Shown (unlocked)")
        }
    }

}
