package com.floatingball.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.preference.PreferenceManager as AndroidXPreferenceManager
import com.floatingball.ActionType
import com.floatingball.R
import com.floatingball.ui.MainActivity
import com.floatingball.util.OneHandedModeHelper
import com.floatingball.view.FloatingBallView

class FloatingBallService : AccessibilityService() {

    companion object {
        private const val TAG = "FloatingBallService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_ball_channel"

        fun isServiceEnabled(context: android.content.Context): Boolean {
            val svc = "${context.packageName}/${FloatingBallService::class.java.name}"
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            return enabled?.contains(svc) == true
        }
    }

    private var windowManager: WindowManager? = null
    private var ballView: FloatingBallView? = null
    private var savedPosX: Int = 0
    private var savedPosY: Int = 0
    private var preCompensationY: Int = 0
    private var oneHandedCompensated: Boolean = false
    private var oneHandedObserver: android.database.ContentObserver? = null
    private var isBallHidden: Boolean = false
    private var orientationListener: android.view.OrientationEventListener? = null

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        Log.d(TAG, "Preference changed: $key")
        val view = ballView ?: return@OnSharedPreferenceChangeListener
        when (key) {
            "ball_size_dp" -> {
                view.setBallSizeDp(prefs.getInt(key, 38))
                updateWindowSize()
            }
            "ball_outer_opacity" -> view.setOuterOpacity(prefs.getInt(key, 55))
            "ball_inner_opacity" -> view.setInnerOpacity(prefs.getInt(key, 92))
            "ball_radius_ratio" -> view.setRadiusRatio(prefs.getInt(key, 73))
        }
    }

    // ---- Lifecycle ----

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "onServiceConnected")

        // We don't need any accessibility events (auto-hide uses OrientationEventListener,
        // all gestures use performGlobalAction/dispatchGesture). But the framework requires
        // at least one event type to bind the service. TYPE_ANNOUNCEMENT fires so rarely
        // it's effectively never — minimal power impact.
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_ANNOUNCEMENT
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }

        createNotificationChannel()
        startForegroundNotification()

        AndroidXPreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(prefListener)

        // Schedule periodic health checks to auto-restart if killed
        HealthCheckReceiver.schedule(this)

        // Orientation-based auto-hide (event-driven, zero polling)
        orientationListener = object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (!HideAppManager.isEnabled(this@FloatingBallService)) return
                val isLandscape = orientation in 80..100 || orientation in 260..280
                if (isLandscape && !isBallHidden) {
                    isBallHidden = true
                    removeBallFromWindow()
                    Log.d(TAG, "Auto-hidden (landscape)")
                } else if (!isLandscape && isBallHidden) {
                    isBallHidden = false
                    addBallToWindow()
                    Log.d(TAG, "Auto-shown (portrait)")
                }
            }
        }
        orientationListener?.enable()

        createFloatingBall()

        // ContentObserver for external one-handed mode exit
        oneHandedObserver = object : android.database.ContentObserver(
            android.os.Handler(android.os.Looper.getMainLooper())
        ) {
            override fun onChange(selfChange: Boolean) {
                if (selfChange) return // ignore our own writes
                val nowActive = try {
                    Settings.Secure.getInt(contentResolver, "one_handed_mode_activated", 0) == 1
                } catch (_: Exception) { false }
                Log.d(TAG, "Observer: nowActive=$nowActive compensated=$oneHandedCompensated")
                if (!nowActive && oneHandedCompensated) {
                    Log.d(TAG, "External exit via ContentObserver")
                    OneHandedModeHelper.forceReset()
                    animateCompensation(false)
                }
            }
        }
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor("one_handed_mode_activated"),
            false, oneHandedObserver!!
        )
        Log.d(TAG, "ContentObserver registered for one_handed_mode_activated")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() = Unit

    private fun removeBallFromWindow() {
        val v = ballView ?: return
        try { windowManager?.removeView(v) } catch (_: Exception) {}
    }

    private fun addBallToWindow() {
        val v = ballView ?: return
        val wm = windowManager ?: return
        val prefs = AndroidXPreferenceManager.getDefaultSharedPreferences(this)
        val sizeDp = getIntPref(prefs, "ball_size_dp", 38)
        val ballPx = (sizeDp * resources.displayMetrics.density).toInt()
        val extra = (ballPx * 0.2f).toInt()
        val windowPx = ballPx + extra * 2

        val params = WindowManager.LayoutParams(
            windowPx, windowPx,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedPosX
            y = savedPosY
        }
        try { wm.addView(v, params) } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        oneHandedObserver?.let { contentResolver.unregisterContentObserver(it) }
        orientationListener?.disable()
        AndroidXPreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        removeFloatingBall()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        AndroidXPreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        removeFloatingBall()
        // Return true so the system will rebind us if we get killed
        return true
    }

    // ---- Ball ----

    private fun createFloatingBall() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = AndroidXPreferenceManager.getDefaultSharedPreferences(this)

        val sizeDp = getIntPref(prefs, "ball_size_dp", 38)
        val outerOp = getIntPref(prefs, "ball_outer_opacity", 55)
        val innerOp = getIntPref(prefs, "ball_inner_opacity", 92)
        val ratio = getIntPref(prefs, "ball_radius_ratio", 73)

        ballView = FloatingBallView(this).apply {
            setBallSizeDp(sizeDp)
            setOuterOpacity(outerOp)
            setInnerOpacity(innerOp)
            setRadiusRatio(ratio)

            gestureCallback = object : com.floatingball.gesture.BallGestureDetector.GestureCallback {
                override fun onClick() = performAction(readAction(prefs, "action_click", ActionType.BACK))
                override fun onDoubleClick() = performAction(readAction(prefs, "action_double_click", ActionType.ONE_HANDED))
                override fun onSwipeUp() = performAction(readAction(prefs, "action_swipe_up", ActionType.HOME))
                override fun onSwipeDown() = performAction(readAction(prefs, "action_swipe_down", ActionType.NOTIFICATIONS))
                override fun onSwipeLeft() = performAction(readAction(prefs, "action_swipe_left", ActionType.RECENTS))
                override fun onSwipeRight() = performAction(readAction(prefs, "action_swipe_right", ActionType.QUICK_SETTINGS))

                override fun onLongPressStart() {}
                override fun onSwipeMove(dx: Float, dy: Float) {
                    ballView?.applySwipeOffset(dx, dy)
                }
                override fun onSwipeRelease() {
                    ballView?.releaseSwipeOffset()
                }
                override fun onDrag(dx: Int, dy: Int) {
                    moveBall(dx, dy)
                }
                override fun onDragEnd() {
                    savePosition()
                }
                override fun onTouchDown() {}
                override fun onTouchUp() {}
            }
        }

        val dm = resources.displayMetrics
        val ballPx = (sizeDp * dm.density).toInt()
        // Add extra space for inner circle offset animation (40% of ball radius = 20% of diameter)
        val extraPx = (ballPx * 0.2f).toInt()
        val windowPx = ballPx + extraPx * 2
        savedPosX = getIntPref(prefs, "ball_pos_x", -1)
        savedPosY = getIntPref(prefs, "ball_pos_y", -1)
        if (savedPosX < 0 || savedPosY < 0) {
            savedPosX = dm.widthPixels - windowPx - 32
            savedPosY = dm.heightPixels / 2 - windowPx / 2
        }

        // TYPE_ACCESSIBILITY_OVERLAY: does NOT trigger "displaying over other apps"
        // notification because the window is part of the accessibility service.
        // Fall back to TYPE_APPLICATION_OVERLAY if not available.
        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            // On Android 10+, TYPE_ACCESSIBILITY_OVERLAY is deprecated but still works
            // and avoids the overlay notification when used by an accessibility service
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        val params = WindowManager.LayoutParams(
            windowPx, windowPx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedPosX
            y = savedPosY
        }

        try { windowManager?.addView(ballView, params) }
        catch (e: SecurityException) { Log.e(TAG, "Overlay permission not granted", e) }
    }

    private fun removeFloatingBall() {
        try { ballView?.let { windowManager?.removeView(it) } }
        catch (_: Exception) {}
        ballView = null; windowManager = null
    }

    private fun moveBall(dx: Int, dy: Int) {
        val v = ballView ?: return; val w = windowManager ?: return
        try {
            val p = v.layoutParams as WindowManager.LayoutParams
            p.x += dx; p.y += dy
            savedPosX = p.x; savedPosY = p.y
            w.updateViewLayout(v, p)
        } catch (_: Exception) {}
    }

    private fun savePosition() {
        AndroidXPreferenceManager.getDefaultSharedPreferences(this)
            .edit().putInt("ball_pos_x", savedPosX).putInt("ball_pos_y", savedPosY).apply()
    }

    private fun updateWindowSize() {
        val v = ballView ?: return; val w = windowManager ?: return
        val prefs = AndroidXPreferenceManager.getDefaultSharedPreferences(this)
        val ballPx = (getIntPref(prefs, "ball_size_dp", 38) * resources.displayMetrics.density).toInt()
        val extra = (ballPx * 0.2f).toInt()
        val windowPx = ballPx + extra * 2
        try {
            val p = v.layoutParams as WindowManager.LayoutParams
            p.width = windowPx; p.height = windowPx
            w.updateViewLayout(v, p)
        } catch (_: Exception) {}
    }

    // ---- Actions ----

    private fun performAction(action: ActionType) {
        Log.d(TAG, "performAction: $action")
        when (action) {
            ActionType.BACK -> performGlobalAction(GLOBAL_ACTION_BACK)
            ActionType.HOME -> {
                if (OneHandedModeHelper.isModeActive()) {
                    OneHandedModeHelper.toggle(this, this,
                        onEnter = {}, onExit = { animateCompensation(false) })
                }
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
            ActionType.RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            ActionType.NOTIFICATIONS -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            ActionType.QUICK_SETTINGS -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            ActionType.SCREENSHOT -> performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            ActionType.LOCK_SCREEN -> performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            ActionType.ONE_HANDED -> OneHandedModeHelper.toggle(
                this, this,
                onEnter = { animateCompensation(true) },
                onExit = { animateCompensation(false) }
            )
            ActionType.NONE -> {}
        }
    }

    private fun readAction(prefs: SharedPreferences, key: String, default: ActionType): ActionType {
        return try {
            ActionType.fromString(prefs.getString(key, default.name) ?: default.name)
        } catch (_: ClassCastException) { default }
    }

    // ---- Notification ----

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun startForegroundNotification() {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE).setContentIntent(pi).build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceCompat.startForeground(this, NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, n)
    }

    private fun getIntPref(prefs: SharedPreferences, key: String, default: Int): Int {
        return try { prefs.getInt(key, default) }
        catch (_: ClassCastException) { prefs.getString(key, default.toString())?.toIntOrNull() ?: default }
    }

    private fun getOffsetPct(): Int {
        val prefs = AndroidXPreferenceManager.getDefaultSharedPreferences(this)
        return try { prefs.getInt("one_handed_offset_pct", 40) }
        catch (_: ClassCastException) { prefs.getString("one_handed_offset_pct", "40")?.toIntOrNull() ?: 40 }
    }

    private fun animateCompensation(enter: Boolean) {
        val v = ballView ?: return
        val wm = windowManager ?: return
        val params = v.layoutParams as? WindowManager.LayoutParams ?: return

        val screenH = resources.displayMetrics.heightPixels
        val offset = (screenH * getOffsetPct() / 100f).toInt()

        if (enter) {
            val ballCenterY = params.y + v.height / 2
            if (ballCenterY <= screenH / 2) return
            preCompensationY = params.y
            val targetY = (params.y - offset).coerceIn(80, screenH - v.height - 32)
            Log.d(TAG, "Compensate UP: ${params.y} -> $targetY (offset=$offset)")
            animateBallY(v, wm, params, targetY)
            oneHandedCompensated = true
        } else {
            if (!oneHandedCompensated) return // already restored by observer
            val targetY = preCompensationY
            preCompensationY = 0
            oneHandedCompensated = false
            Log.d(TAG, "Restore DOWN: ${params.y} -> $targetY")
            animateBallY(v, wm, params, targetY)
        }
    }

    private fun animateBallY(v: FloatingBallView, wm: WindowManager, params: WindowManager.LayoutParams, targetY: Int) {
        val startY = params.y
        android.animation.ValueAnimator.ofInt(startY, targetY).apply {
            duration = 250
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                params.y = it.animatedValue as Int
                try { wm.updateViewLayout(v, params) } catch (_: Exception) {}
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    savedPosY = params.y
                    savePosition()
                }
            })
            start()
        }
    }

}
