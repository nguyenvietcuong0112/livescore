package com.livescore.app.myapplication.livescore.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

class LivePulseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }

    private val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private var pulseRadiusRatio = 0f
    private var pulseAlpha = 255

    init {
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        val anim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                pulseRadiusRatio = interpolatedTime
                pulseAlpha = (255 * (1 - interpolatedTime)).toInt()
                invalidate()
            }
        }
        anim.duration = 1500
        anim.repeatCount = Animation.INFINITE
        anim.repeatMode = Animation.RESTART
        startAnimation(anim)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = Math.min(width, height) / 4f

        // Draw outer pulsing ring
        pulsePaint.alpha = pulseAlpha
        canvas.drawCircle(cx, cy, baseRadius + (baseRadius * pulseRadiusRatio), pulsePaint)

        // Draw solid inner dot
        canvas.drawCircle(cx, cy, baseRadius, dotPaint)
    }
}
