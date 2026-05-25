package com.livescore.football.livescores.footballscores.ui.custom

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

    // Alternating green lawn colors
    private val stripePaintA = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#38652A")
        style = Paint.Style.FILL
    }

    private val stripePaintB = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#407330")
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D0FFFFFF") // Premium semi-transparent white lines
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val playerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B0E13")
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90FFFFFF")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) // Sleek dashed pointer line
    }

    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CC121620") // Sleek translucent dark header background
        style = Paint.Style.FILL
    }

    private val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600") // Neon yellow badge text
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val teamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val situationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    // Dynamic state parameters
    private var ballX = 0.5f // 0 to 1
    private var ballY = 0.5f // 0 to 1

    private var activeAttackSide = 0 // 0 = none, 1 = Home (attacking right), 2 = Away (attacking left)
    private var attackProgress = 0f

    // Team names and match state strings (English defaults)
    private var homeTeamName = "Home Team"
    private var awayTeamName = "Away Team"
    private var matchStatus = "1st Half | 00:00"
    private var matchStatusExtra = "0'"
    private var situationText = "Safe Possession"

    init {
        startAttackAnimation()
    }

    fun setTeamNames(home: String, away: String) {
        homeTeamName = home
        awayTeamName = away
        invalidate()
    }

    fun setMatchStatus(status: String, extra: String) {
        matchStatus = status
        matchStatusExtra = extra
        invalidate()
    }

    fun setSituationText(situation: String) {
        situationText = situation
        invalidate()
    }

    fun updateBallPosition(x: Float, y: Float) {
        ballX = x.coerceIn(0.05f, 0.95f)
        ballY = y.coerceIn(0.1f, 0.9f)
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

        // 1. Draw Striped Green Lawn
        val stripesCount = 15
        val stripeWidth = w / stripesCount
        for (i in 0 until stripesCount) {
            val startX = i * stripeWidth
            val paint = if (i % 2 == 0) stripePaintA else stripePaintB
            canvas.drawRect(startX, 0f, startX + stripeWidth, h, paint)
        }

        // 2. Draw Attack Overlay / Pressure Shading
        if (activeAttackSide == 1) {
            // Home attacking right: Draw translucent light green pressure field on the right
            val startX = w / 2f
            val endX = w - 20f
            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(Color.parseColor("#004CAF50"), Color.parseColor("#334CAF50")),
                null, Shader.TileMode.CLAMP
            )
            val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.shader = shader
            }
            canvas.drawRect(startX, 20f, endX, h - 20f, overlayPaint)
        } else if (activeAttackSide == 2) {
            // Away attacking left: Draw translucent light green pressure field on the left
            val startX = w / 2f
            val endX = 20f
            val shader = LinearGradient(
                startX, 0f, endX, 0f,
                intArrayOf(Color.parseColor("#004CAF50"), Color.parseColor("#334CAF50")),
                null, Shader.TileMode.CLAMP
            )
            val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                this.shader = shader
            }
            canvas.drawRect(endX, 20f, startX, h - 20f, overlayPaint)
        }

        // 3. Draw Pitch Markings (White Lines)
        val pad = 20f
        val rect = RectF(pad, pad, w - pad, h - pad)
        canvas.drawRect(rect, linePaint)

        // Center line
        val midX = w / 2f
        canvas.drawLine(midX, pad, midX, h - pad, linePaint)

        // Center Circle & kick off spot
        canvas.drawCircle(midX, h / 2f, h / 6f, linePaint)
        canvas.drawCircle(midX, h / 2f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })

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

        // Penalty spots
        canvas.drawCircle(pad + w / 9f, h / 2f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })
        canvas.drawCircle(w - pad - w / 9f, h / 2f, 4f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL })

        // 4. Calculate actual ball drawing coordinates
        val drawBallX = pad + (ballX * (w - 2 * pad))
        val drawBallY = pad + (ballY * (h - 2 * pad))

        // 5. Draw Situation Text & Possessing Team (Centered in active half)
        val textX = if (activeAttackSide == 1) w * 0.52f else if (activeAttackSide == 2) w * 0.15f else w * 0.33f
        val textY = h / 2f - 25f

        // Draw colored vertical bar (representing active side color)
        val verticalLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (activeAttackSide == 1) Color.parseColor("#E91E63") else if (activeAttackSide == 2) Color.WHITE else Color.parseColor("#FFD600")
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(textX, textY, textX, textY + 54f, verticalLinePaint)

        // Determine current possessor and situation text dynamically (All English)
        val activeTeam = if (activeAttackSide == 1) homeTeamName else if (activeAttackSide == 2) awayTeamName else "Contested Ball"
        val activeSituation = when {
            activeAttackSide == 0 -> "Midfield Play"
            activeAttackSide == 1 && ballX > 0.8f -> "Dangerous Attack 🔥"
            activeAttackSide == 1 && ballX > 0.65f -> "Attack in Box"
            activeAttackSide == 2 && ballX < 0.2f -> "Dangerous Attack 🔥"
            activeAttackSide == 2 && ballX < 0.35f -> "Attack in Box"
            else -> situationText
        }

        // Draw Text labels
        canvas.drawText(activeTeam, textX + 16f, textY + 22f, teamPaint)
        canvas.drawText(activeSituation, textX + 16f, textY + 46f, situationPaint)

        // 6. Draw glowing dashed pointer line connecting text box to ball position
        if (activeAttackSide != 0) {
            val startPX = if (activeAttackSide == 1) textX + 180f else textX + 150f
            val startPY = textY + 30f
            canvas.drawLine(startPX, startPY, drawBallX, drawBallY, pointerPaint)
        }

        // 7. Draw vector Soccer Ball ⚽ emoji (centered on coordinate)
        canvas.drawText("⚽", drawBallX, drawBallY + 12f, emojiPaint)

        // 8. Draw Top Match Status Pill Header
        val headerWidth = w * 0.44f
        val headerHeight = 36f
        val headerX = w / 2f - headerWidth / 2f
        val headerY = pad + 10f
        val headerRect = RectF(headerX, headerY, headerX + headerWidth, headerY + headerHeight)
        canvas.drawRoundRect(headerRect, 18f, 18f, headerBgPaint)
        canvas.drawText(matchStatus, w / 2f, headerY + 24f, headerTextPaint)

        // Draw extra elapsed badge next to match status (e.g. ⏱ 6')
        if (matchStatusExtra.isNotEmpty()) {
            val badgeWidth = w * 0.12f
            val badgeX = w / 2f + headerWidth / 2f + 8f
            val badgeRect = RectF(badgeX, headerY, badgeX + badgeWidth, headerY + headerHeight)
            canvas.drawRoundRect(badgeRect, 18f, 18f, headerBgPaint)
            canvas.drawText("⏱ $matchStatusExtra", badgeX + badgeWidth / 2f, headerY + 24f, badgeTextPaint)
        }
    }
}
