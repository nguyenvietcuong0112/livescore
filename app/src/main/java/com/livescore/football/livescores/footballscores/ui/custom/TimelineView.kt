package com.livescore.football.livescores.footballscores.ui.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

data class TimelineEvent(
    val minute: Int,
    val type: String, // "GOAL", "CARD_YELLOW", "CARD_RED", "SUBST"
    val isHome: Boolean
)

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var events = listOf<TimelineEvent>()

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#171D26") // Deep charcoal grid line
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val playheadGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3300C853")
        style = Paint.Style.FILL
    }

    private val playheadCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4B5565") // Elegant muted grey labels
        textSize = 21f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val minuteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2") // High-contrast grey
        textSize = 19f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val connectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D394C")
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private var currentMatchMinute = 78

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        // Mock events
        setEvents(listOf(
            TimelineEvent(12, "GOAL", true),
            TimelineEvent(34, "CARD_YELLOW", false),
            TimelineEvent(45, "SUBST", true),
            TimelineEvent(62, "GOAL", false),
            TimelineEvent(71, "CARD_RED", false)
        ))
    }

    fun setEvents(eventList: List<TimelineEvent>) {
        events = eventList
        invalidate()
    }

    fun setMatchMinute(minute: Int) {
        currentMatchMinute = minute
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 45f
        val centerY = h / 2f
        val timelineWidth = w - (2 * padding)

        // 1. Draw central structural grid segments (Ticks at 15, 30, 45, 60, 75)
        canvas.drawLine(padding, centerY, w - padding, centerY, linePaint)
        
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2D394C")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        for (i in 1..5) {
            val tickX = padding + (i * 15f / 90f) * timelineWidth
            canvas.drawLine(tickX, centerY - 8f, tickX, centerY + 8f, tickPaint)
        }

        // 2. Draw active progress line with gradient flow
        val progressRatio = (currentMatchMinute.toFloat() / 90f).coerceIn(0f, 1f)
        val progressX = padding + (progressRatio * timelineWidth)

        progressPaint.shader = LinearGradient(
            padding, centerY, progressX, centerY,
            intArrayOf(Color.parseColor("#00C853"), Color.parseColor("#00E676")),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawLine(padding, centerY, progressX, centerY, progressPaint)

        // Glowing active playhead marker
        if (currentMatchMinute in 1..89) {
            val auraRadius = 14f
            val pulseAura = (System.currentTimeMillis() % 1000) / 1000f
            playheadGlowPaint.alpha = (90 * (1f - pulseAura)).toInt()
            canvas.drawCircle(progressX, centerY, auraRadius + (8f * pulseAura), playheadGlowPaint)
            canvas.drawCircle(progressX, centerY, 5f, playheadCorePaint)
        }

        // 3. Main structural labels (0', HT, 90')
        canvas.drawText("00'", padding, centerY + 38f, textPaint)
        canvas.drawText("90'", w - padding, centerY + 38f, textPaint)
        canvas.drawText("HT 45'", padding + (0.5f * timelineWidth), centerY + 38f, textPaint)

        // 4. Draw detailed visual events
        for (event in events) {
            val eventRatio = (event.minute.toFloat() / 90f).coerceIn(0f, 1f)
            val x = padding + (eventRatio * timelineWidth)
            val yOffset = if (event.isHome) -36f else 36f

            // Connector link line
            canvas.drawLine(x, centerY, x, centerY + yOffset, connectorPaint)

            val eventY = centerY + yOffset
            when (event.type) {
                "GOAL" -> {
                    drawSoccerBall(canvas, x, eventY, 11f)
                }
                "CARD_YELLOW" -> {
                    drawCardNode(canvas, x, eventY, isRed = false)
                }
                "CARD_RED" -> {
                    drawCardNode(canvas, x, eventY, isRed = true)
                }
                "SUBST" -> {
                    drawSubstitutionNode(canvas, x, eventY, 11f)
                }
            }

            // Draw clean timestamp label
            val textY = if (event.isHome) eventY - 22f else eventY + 36f
            canvas.drawText("${event.minute}'", x, textY, minuteLabelPaint)
        }
    }

    /**
     * Programmatically constructs a high-fidelity vector soccer ball.
     */
    private fun drawSoccerBall(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121620")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val darkPanelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E2530")
            style = Paint.Style.FILL
        }

        canvas.drawCircle(cx, cy, radius, basePaint)

        // Pentagon core
        val pentagonPath = Path()
        val pentagonRadius = radius * 0.35f
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0 - 90.0))
            val px = cx + pentagonRadius * Math.cos(angle).toFloat()
            val py = cy + pentagonRadius * Math.sin(angle).toFloat()
            if (i == 0) {
                pentagonPath.moveTo(px, py)
            } else {
                pentagonPath.lineTo(px, py)
            }
        }
        pentagonPath.close()
        canvas.drawPath(pentagonPath, darkPanelPaint)
        canvas.drawPath(pentagonPath, borderPaint)

        // Radiating line segments
        for (i in 0 until 5) {
            val angle = Math.toRadians((i * 72.0 - 90.0))
            val px = cx + pentagonRadius * Math.cos(angle).toFloat()
            val py = cy + pentagonRadius * Math.sin(angle).toFloat()
            val bx = cx + radius * Math.cos(angle).toFloat()
            val by = cy + radius * Math.sin(angle).toFloat()
            canvas.drawLine(px, py, bx, by, borderPaint)
        }

        canvas.drawCircle(cx, cy, radius, borderPaint)
    }

    /**
     * Programmatically draws a 3D tilted warning card.
     */
    private fun drawCardNode(canvas: Canvas, cx: Float, cy: Float, isRed: Boolean) {
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isRed) Color.parseColor("#DD2C00") else Color.parseColor("#FFD600")
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#40000000"))
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121620")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(15f) // Subtle tilting rotation for organic 3D posture

        val cardW = 12f
        val cardH = 18f
        val cardRect = RectF(-cardW / 2f, -cardH / 2f, cardW / 2f, cardH / 2f)
        canvas.drawRoundRect(cardRect, 3f, 3f, cardPaint)
        canvas.drawRoundRect(cardRect, 3f, 3f, borderPaint)

        canvas.restore()
    }

    /**
     * Programmatically draws overlapping green (entering) and red (exiting) circles for substitutions.
     */
    private fun drawSubstitutionNode(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00C853")
            style = Paint.Style.FILL
        }
        val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DD2C00")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#121620")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        // Render overlapping indicator node structures
        val offset = radius * 0.35f
        val subRadius = radius * 0.72f

        canvas.drawCircle(cx - offset, cy - offset, subRadius, greenPaint)
        canvas.drawCircle(cx - offset, cy - offset, subRadius, borderPaint)

        canvas.drawCircle(cx + offset, cy + offset, subRadius, redPaint)
        canvas.drawCircle(cx + offset, cy + offset, subRadius, borderPaint)
    }
}
