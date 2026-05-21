package com.livescore.app.myapplication.livescore.ui.custom

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
        color = Color.parseColor("#1E2530")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val eventPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0B0E13")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private var currentMatchMinute = 78 // Mock current match progress

    init {
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
        val padding = 40f
        val centerY = h / 2f
        val timelineWidth = w - (2 * padding)

        // 1. Draw central gray timeline line (0 to 90')
        canvas.drawLine(padding, centerY, w - padding, centerY, linePaint)

        // 2. Draw active progress line based on current minute
        val progressRatio = (currentMatchMinute.toFloat() / 90f).coerceIn(0f, 1f)
        val progressX = padding + (progressRatio * timelineWidth)
        canvas.drawLine(padding, centerY, progressX, centerY, progressPaint)

        // 3. Draw Minute markings
        canvas.drawText("0'", padding, centerY + 35f, textPaint)
        canvas.drawText("90'", w - padding, centerY + 35f, textPaint)
        canvas.drawText("HT 45'", padding + (0.5f * timelineWidth), centerY + 35f, textPaint)

        // 4. Plot Events
        for (event in events) {
            val eventRatio = (event.minute.toFloat() / 90f).coerceIn(0f, 1f)
            val x = padding + (eventRatio * timelineWidth)
            val yOffset = if (event.isHome) -25f else 25f

            // Draw event point line linking to timeline
            canvas.drawLine(x, centerY, x, centerY + yOffset, linePaint)

            // Setup color based on type
            when (event.type) {
                "GOAL" -> {
                    eventPaint.color = Color.parseColor("#00C853") // Green dot for Goal
                    canvas.drawCircle(x, centerY + yOffset, 12f, eventPaint)
                }
                "CARD_YELLOW" -> {
                    eventPaint.color = Color.parseColor("#FFD600") // Yellow card rect
                    canvas.drawRect(x - 8f, centerY + yOffset - 12f, x + 8f, centerY + yOffset + 12f, eventPaint)
                }
                "CARD_RED" -> {
                    eventPaint.color = Color.parseColor("#DD2C00") // Red card rect
                    canvas.drawRect(x - 8f, centerY + yOffset - 12f, x + 8f, centerY + yOffset + 12f, eventPaint)
                }
                "SUBST" -> {
                    eventPaint.color = Color.parseColor("#FFFFFF") // White sub dot
                    canvas.drawCircle(x, centerY + yOffset, 10f, eventPaint)
                }
            }
            canvas.drawCircle(x, centerY + yOffset, 12f, borderPaint)

            // Draw small minute text above/below the dot
            val textY = if (event.isHome) centerY + yOffset - 18f else centerY + yOffset + 34f
            canvas.drawText("${event.minute}'", x, textY, textPaint)
        }
    }
}
