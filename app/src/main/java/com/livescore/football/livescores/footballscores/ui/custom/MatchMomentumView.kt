package com.livescore.football.livescores.footballscores.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

class MatchMomentumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // List of pressure points: positive for Home team, negative for Away team
    // Range: -100 (Away max pressure) to +100 (Home max pressure)
    private var momentumData = listOf<Float>()

    private val homePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }

    private val awayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2530")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A3342")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2")
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var animationProgress = 1f

    init {
        // Mock default data if empty
        setMomentumData(listOf(
            15f, 25f, -10f, -40f, 10f, 50f, 75f, 30f, -20f, -60f,
            -80f, -10f, 40f, 60f, -15f, 20f, 45f, 85f, 90f, 10f,
            -25f, -45f, -95f, -30f, 5f, 15f, -20f, 40f, 65f, 80f
        ))
    }

    fun setMomentumData(data: List<Float>) {
        momentumData = data
        animateEntrance()
    }

    private fun animateEntrance() {
        val anim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                animationProgress = interpolatedTime
                invalidate()
            }
        }
        anim.duration = 800
        startAnimation(anim)
    }

    override fun onDraw(canvas: Canvas) {
        if (momentumData.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f

        // Draw Center Axis
        canvas.drawLine(0f, centerY, w, centerY, linePaint)

        // Draw half-time indicator at center of data using dashed paint
        canvas.drawLine(w / 2f, 0f, w / 2f, h, dashedPaint)
        canvas.drawText("HT", w / 2f, 32f, textPaint)

        val barSpacing = w / (momentumData.size + 1)
        val maxBarHeight = centerY * 0.8f // Leave 20% margin top/bottom

        for (i in momentumData.indices) {
            val pressure = momentumData[i] * animationProgress
            val x = (i + 1) * barSpacing
            val barHeight = (Math.abs(pressure) / 100f) * maxBarHeight

            if (pressure >= 0) {
                // Home team pressure (Top)
                val top = centerY - barHeight
                val rect = RectF(x - barSpacing/3, top, x + barSpacing/3, centerY)
                canvas.drawRoundRect(rect, 4f, 4f, homePaint)
            } else {
                // Away team pressure (Bottom)
                val bottom = centerY + barHeight
                val rect = RectF(x - barSpacing/3, centerY, x + barSpacing/3, bottom)
                canvas.drawRoundRect(rect, 4f, 4f, awayPaint)
            }
        }
    }
}
