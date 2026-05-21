package com.livescore.app.myapplication.livescore.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

class FootballPitchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141922")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E2530")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val playerHomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }

    private val playerAwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.FILL
    }

    private val playerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B0E13")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        style = Paint.Style.FILL
    }

    private val attackOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Positions representing players and ball
    private var ballX = 0.5f // 0 to 1
    private var ballY = 0.5f // 0 to 1

    private var activeAttackSide = 0 // 0 = none, 1 = Home (attacking right), 2 = Away (attacking left)
    private var attackProgress = 0f

    init {
        startAttackAnimation()
    }

    fun updateBallPosition(x: Float, y: Float) {
        ballX = x.coerceIn(0f, 1f)
        ballY = y.coerceIn(0f, 1f)
        invalidate()
    }

    fun triggerHomeAttack() {
        activeAttackSide = 1
        invalidate()
    }

    fun triggerAwayAttack() {
        activeAttackSide = 2
        invalidate()
    }

    fun clearAttack() {
        activeAttackSide = 0
        invalidate()
    }

    private fun startAttackAnimation() {
        val anim = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                attackProgress = interpolatedTime
                invalidate()
            }
        }
        anim.duration = 2000
        anim.repeatCount = Animation.INFINITE
        anim.repeatMode = Animation.RESTART
        startAnimation(anim)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // 1. Draw Pitch Background
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // 2. Draw Attack Overlay Animations
        if (activeAttackSide == 1) {
            // Home team attacking right -> Green gradient expanding to the right
            val waveWidth = w * 0.4f
            val startX = w * 0.4f + (w * 0.2f * attackProgress)
            val endX = startX + waveWidth
            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(Color.parseColor("#0000C853"), Color.parseColor("#3300C853"), Color.parseColor("#0000C853")),
                null, Shader.TileMode.CLAMP
            )
            attackOverlayPaint.shader = shader
            canvas.drawRect(startX, 0f, w, h, attackOverlayPaint)
        } else if (activeAttackSide == 2) {
            // Away team attacking left -> Green gradient expanding to the left
            val waveWidth = w * 0.4f
            val startX = w * 0.6f - (w * 0.2f * attackProgress)
            val endX = startX - waveWidth
            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(Color.parseColor("#0000C853"), Color.parseColor("#3300C853"), Color.parseColor("#0000C853")),
                null, Shader.TileMode.CLAMP
            )
            attackOverlayPaint.shader = shader
            canvas.drawRect(0f, 0f, startX, h, attackOverlayPaint)
        }

        // 3. Draw Pitch Markings (Lines)
        // Outer border with some padding
        val pad = 20f
        val rect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRect(rect, linePaint)

        // Center line
        val midX = w / 2f
        canvas.drawLine(midX, pad, midX, h - pad, linePaint)

        // Center Circle
        canvas.drawCircle(midX, h / 2f, h / 6f, linePaint)
        canvas.drawCircle(midX, h / 2f, 6f, linePaint) // Kickoff spot

        // Left Penalty Box
        val penWidth = w / 6f
        val penHeight = h / 2f
        val penTop = h / 4f
        canvas.drawRect(pad, penTop, pad + penWidth, penTop + penHeight, linePaint)

        // Left Goal Box
        val goalWidth = w / 18f
        val goalHeight = h / 4f
        val goalTop = h * (3f / 8f)
        canvas.drawRect(pad, goalTop, pad + goalWidth, goalTop + goalHeight, linePaint)

        // Right Penalty Box
        canvas.drawRect(w - pad - penWidth, penTop, w - pad, penTop + penHeight, linePaint)

        // Right Goal Box
        canvas.drawRect(w - pad - goalWidth, goalTop, w - pad, goalTop + goalHeight, linePaint)

        // 4. Draw Ball
        val drawBallX = pad + (ballX * (w - 2 * pad))
        val drawBallY = pad + (ballY * (h - 2 * pad))
        canvas.drawCircle(drawBallX, drawBallY, 12f, ballPaint)
        canvas.drawCircle(drawBallX, drawBallY, 12f, playerBorderPaint)

        // 5. Draw a few mock player dots around to look alive
        // Home Players
        drawPlayer(canvas, midX - 100f, h / 2f, playerHomePaint)
        drawPlayer(canvas, midX - 300f, h / 3f, playerHomePaint)
        drawPlayer(canvas, midX - 300f, h * 2 / 3f, playerHomePaint)
        
        // Away Players
        drawPlayer(canvas, midX + 100f, h / 2f, playerAwayPaint)
        drawPlayer(canvas, midX + 300f, h / 3f, playerAwayPaint)
        drawPlayer(canvas, midX + 300f, h * 2 / 3f, playerAwayPaint)
    }

    private fun drawPlayer(canvas: Canvas, x: Float, y: Float, paint: Paint) {
        canvas.drawCircle(x, y, 16f, paint)
        canvas.drawCircle(x, y, 16f, playerBorderPaint)
    }
}
