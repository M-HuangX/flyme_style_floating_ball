package com.floatingball.gesture

import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs

class BallGestureDetector(
    private val view: View,
    private val callback: GestureCallback
) {
    interface GestureCallback {
        fun onClick()
        fun onDoubleClick()
        fun onSwipeUp()
        fun onSwipeDown()
        fun onSwipeLeft()
        fun onSwipeRight()
        /** Called during swipe motion for real-time inner circle offset */
        fun onSwipeMove(dx: Float, dy: Float)
        /** Called when swipe ends for spring-back animation */
        fun onSwipeRelease()
        fun onLongPressStart()
        fun onDrag(deltaX: Int, deltaY: Int)
        fun onDragEnd()
        fun onTouchDown()
        fun onTouchUp()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop: Int = ViewConfiguration.get(view.context).scaledTouchSlop
    private val swipeMinDistance: Int = (30 * view.context.resources.displayMetrics.density).toInt()

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var isLongPressTriggered: Boolean = false
    private var isDragging: Boolean = false
    private var isSwiping: Boolean = false
    private var hasMovedBeyondSlop: Boolean = false
    private var totalDeltaX: Float = 0f
    private var totalDeltaY: Float = 0f

    private val gestureDetector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!isLongPressTriggered && !hasMovedBeyondSlop) {
                callback.onClick()
                return true
            }
            return false
        }
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isLongPressTriggered && !hasMovedBeyondSlop) {
                callback.onDoubleClick()
                return true
            }
            return false
        }
    })

    private val longPressRunnable = Runnable {
        if (!hasMovedBeyondSlop) {
            isLongPressTriggered = true
            isDragging = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            callback.onLongPressStart()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX; downY = event.rawY
                lastX = event.rawX; lastY = event.rawY
                isLongPressTriggered = false; isDragging = false; isSwiping = false
                hasMovedBeyondSlop = false
                totalDeltaX = 0f; totalDeltaY = 0f
                callback.onTouchDown()
                handler.postDelayed(longPressRunnable, 300)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val cx = event.rawX; val cy = event.rawY
                totalDeltaX = cx - downX; totalDeltaY = cy - downY

                if (isDragging) {
                    val dx = (cx - lastX).toInt(); val dy = (cy - lastY).toInt()
                    if (dx != 0 || dy != 0) callback.onDrag(dx, dy)
                    lastX = cx; lastY = cy
                } else if (!isLongPressTriggered && !hasMovedBeyondSlop) {
                    if (abs(totalDeltaX) > touchSlop || abs(totalDeltaY) > touchSlop) {
                        hasMovedBeyondSlop = true
                        isSwiping = true
                        handler.removeCallbacks(longPressRunnable)
                    }
                }

                if (isSwiping) {
                    // Calculate swipe offset and clamp
                    val dx = totalDeltaX; val dy = totalDeltaY
                    val absDx = abs(dx); val absDy = abs(dy)
                    if (absDx > absDy) {
                        callback.onSwipeMove(dx.coerceIn(-200f, 200f), 0f)
                    } else {
                        callback.onSwipeMove(0f, dy.coerceIn(-200f, 200f))
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRunnable)

                if (isDragging) {
                    isDragging = false
                    callback.onDragEnd()
                } else if (isSwiping) {
                    isSwiping = false
                    callback.onSwipeRelease()

                    val absDx = abs(totalDeltaX); val absDy = abs(totalDeltaY)
                    if (absDx > absDy && absDx > swipeMinDistance) {
                        if (totalDeltaX < 0) callback.onSwipeLeft() else callback.onSwipeRight()
                    } else if (absDy > absDx && absDy > swipeMinDistance) {
                        if (totalDeltaY < 0) callback.onSwipeUp() else callback.onSwipeDown()
                    }
                } else if (isLongPressTriggered) {
                    // long press without movement — no action
                }

                isLongPressTriggered = false; isDragging = false; isSwiping = false
                hasMovedBeyondSlop = false
                totalDeltaX = 0f; totalDeltaY = 0f
                callback.onTouchUp()
                return true
            }
        }
        return false
    }

    fun destroy() { handler.removeCallbacks(longPressRunnable) }
}
