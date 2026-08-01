package com.floatingball.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader

/**
 * Flyme-style 2-circle floating ball.
 * Outer circle: fixed, dark semi-transparent.
 * Inner circle: shifts during swipe gestures, spring-back on release.
 */
class BallRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    var ballRadius: Float = dpToPx(19f)
    var outerOpacity: Float = 0.55f
    var innerOpacity: Float = 0.92f
    var radiusRatio: Float = 0.73f

    /** Inner circle center offset during swipe (pixels) */
    var innerOffsetX: Float = 0f
    var innerOffsetY: Float = 0f

    private var bgShader: RadialGradient? = null
    private var lastShaderRadius: Float = -1f

    fun draw(canvas: Canvas) {
        val cx = canvas.width / 2f
        val cy = canvas.height / 2f

        // Layer 1: Outer circle (always centered, never moves)
        drawOuterCircle(canvas, cx, cy)

        // Layer 2: Inner circle (offset during swipe, spring-back)
        drawInnerCircle(canvas, cx + innerOffsetX, cy + innerOffsetY)
    }

    private fun drawOuterCircle(canvas: Canvas, cx: Float, cy: Float) {
        if (bgShader == null || ballRadius != lastShaderRadius) {
            val alpha = (outerOpacity * 255).toInt().coerceIn(0, 255)
            val centerColor = Color.argb(alpha, 200, 200, 200)
            val edgeColor = Color.argb((alpha * 0.55f).toInt(), 160, 160, 160)
            bgShader = RadialGradient(cx, cy, ballRadius, centerColor, edgeColor, Shader.TileMode.CLAMP)
            lastShaderRadius = ballRadius
        }
        bgPaint.shader = bgShader
        canvas.drawCircle(cx, cy, ballRadius, bgPaint)
    }

    private fun drawInnerCircle(canvas: Canvas, cx: Float, cy: Float) {
        val innerRadius = ballRadius * radiusRatio
        val alpha = (innerOpacity * 255).toInt().coerceIn(0, 255)
        innerPaint.color = Color.argb(alpha, 255, 255, 255)
        innerPaint.shader = null
        canvas.drawCircle(cx, cy, innerRadius, innerPaint)
    }

    companion object {
        fun dpToPx(dp: Float): Float = dp * 3f
    }
}
