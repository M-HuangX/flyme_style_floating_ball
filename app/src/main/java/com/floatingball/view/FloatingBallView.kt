package com.floatingball.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import kotlin.math.sqrt

class FloatingBallView(context: Context) : View(context) {

    val renderer = BallRenderer()
    var gestureCallback: com.floatingball.gesture.BallGestureDetector.GestureCallback? = null
        set(value) {
            field = value
            gestureDetector = value?.let {
                com.floatingball.gesture.BallGestureDetector(this, it)
            }
        }

    private var gestureDetector: com.floatingball.gesture.BallGestureDetector? = null
    private var snapAnimator: ValueAnimator? = null
    private var maxOffsetPx: Float = 0f

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setOval(0, 0, view.width, view.height)
            }
        }
    }

    fun setBallSizeDp(s: Int) {
        val px = dpToPx(s.toFloat())
        renderer.ballRadius = px / 2f
        maxOffsetPx = renderer.ballRadius * 0.4f // 40% of radius
        requestLayout()
        invalidate()
    }

    fun setOuterOpacity(p: Int) { renderer.outerOpacity = p / 100f; invalidate() }
    fun setInnerOpacity(p: Int) { renderer.innerOpacity = p / 100f; invalidate() }
    fun setRadiusRatio(p: Int) { renderer.radiusRatio = p / 100f; invalidate() }

    fun applySwipeOffset(dx: Float, dy: Float) {
        val mag = sqrt(dx * dx + dy * dy)
        if (mag > maxOffsetPx) {
            val scale = maxOffsetPx / mag
            renderer.innerOffsetX = dx * scale
            renderer.innerOffsetY = dy * scale
        } else {
            renderer.innerOffsetX = dx
            renderer.innerOffsetY = dy
        }
        invalidate()
    }

    fun releaseSwipeOffset() {
        snapAnimator?.cancel()
        val sx = renderer.innerOffsetX; val sy = renderer.innerOffsetY
        snapAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val f = it.animatedValue as Float
                renderer.innerOffsetX = sx * f; renderer.innerOffsetY = sy * f
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(a: android.animation.Animator) {
                    renderer.innerOffsetX = 0f; renderer.innerOffsetY = 0f
                    invalidate()
                }
            })
            start()
        }
    }

    override fun onMeasure(wms: Int, hms: Int) {
        // View must be large enough to show inner circle at max offset without clipping
        val ballDiam = (renderer.ballRadius * 2).toInt()
        val extra = (maxOffsetPx * 2).toInt() // padding on each side
        setMeasuredDimension(ballDiam + extra, ballDiam + extra)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer.draw(canvas)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        return gestureDetector?.onTouchEvent(event) ?: super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureDetector?.destroy()
        snapAnimator?.cancel()
    }

    private fun dpToPx(dp: Float): Float = dp * context.resources.displayMetrics.density
}
