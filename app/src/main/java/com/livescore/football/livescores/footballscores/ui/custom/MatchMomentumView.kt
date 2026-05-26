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

    private var momentumData = listOf<Float>()

    private val homeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val awayFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val homeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00C853")
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val awayStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2")
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
    }

    private val centerAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A3342") // Sleek charcoal
        strokeWidth = 2.5f
        style = Paint.Style.STROKE
    }

    private val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#171D26") // Subtle background grid
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 12f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4B5565")
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9AA4B2")
        textSize = 18f
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private var animationProgress = 0f

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        setMomentumData(emptyList())
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
        anim.duration = 1200
        startAnimation(anim)
    }

    override fun onDraw(canvas: Canvas) {
        if (momentumData.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val centerY = h / 2f
        val maxBarHeight = centerY * 0.75f // Leave 25% margin

        // 1. Draw horizontal background guidelines at +50 and -50 pressure
        val upperGridY = centerY - maxBarHeight * 0.5f
        val lowerGridY = centerY + maxBarHeight * 0.5f
        canvas.drawLine(0f, upperGridY, w, upperGridY, gridLinePaint)
        canvas.drawLine(0f, lowerGridY, w, lowerGridY, gridLinePaint)

        // Draw center axis (0 baseline)
        canvas.drawLine(0f, centerY, w, centerY, centerAxisPaint)

        // Draw HT (Halftime) indicator
        canvas.drawLine(w / 2f, 15f, w / 2f, h - 15f, gridLinePaint)
        canvas.drawText("HT", w / 2f, 25f, textPaint)
        canvas.drawText("0'", 25f, centerY + 30f, textPaint)
        canvas.drawText("90'", w - 25f, centerY + 30f, textPaint)

        // 2. Generate smooth points for top and bottom bezier curves
        val barSpacing = w / (momentumData.size + 1)
        
        val homePoints = ArrayList<PointF>()
        homePoints.add(PointF(0f, centerY))
        for (i in momentumData.indices) {
            val pressure = Math.max(0f, momentumData[i]) * animationProgress
            val x = (i + 1) * barSpacing
            val y = centerY - (pressure / 100f) * maxBarHeight
            homePoints.add(PointF(x, y))
        }
        homePoints.add(PointF(w, centerY))

        val awayPoints = ArrayList<PointF>()
        awayPoints.add(PointF(0f, centerY))
        for (i in momentumData.indices) {
            val pressure = Math.max(0f, -momentumData[i]) * animationProgress
            val x = (i + 1) * barSpacing
            val y = centerY + (pressure / 100f) * maxBarHeight
            awayPoints.add(PointF(x, y))
        }
        awayPoints.add(PointF(w, centerY))

        // 3. Render Home Team filled Area and glowing stroke line
        if (homePoints.size > 2) {
            val homeFillPath = Path()
            homeFillPath.smoothPathTo(homePoints)
            homeFillPath.lineTo(w, centerY)
            homeFillPath.lineTo(0f, centerY)
            homeFillPath.close()

            homeFillPaint.shader = LinearGradient(
                0f, centerY - maxBarHeight, 0f, centerY,
                intArrayOf(Color.parseColor("#4D00C853"), Color.parseColor("#0500C853")), // Rich translucent green gradient
                null, Shader.TileMode.CLAMP
            )
            canvas.drawPath(homeFillPath, homeFillPaint)

            val homeLinePath = Path()
            homeLinePath.smoothPathTo(homePoints)
            homeStrokePaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#00C853"))
            canvas.drawPath(homeLinePath, homeStrokePaint)
        }

        // 4. Render Away Team filled Area and glowing stroke line
        if (awayPoints.size > 2) {
            val awayFillPath = Path()
            awayFillPath.smoothPathTo(awayPoints)
            awayFillPath.lineTo(w, centerY)
            awayFillPath.lineTo(0f, centerY)
            awayFillPath.close()

            awayFillPaint.shader = LinearGradient(
                0f, centerY + maxBarHeight, 0f, centerY,
                intArrayOf(Color.parseColor("#4D9AA4B2"), Color.parseColor("#059AA4B2")), // Rich translucent grey gradient
                null, Shader.TileMode.CLAMP
            )
            canvas.drawPath(awayFillPath, awayFillPaint)

            val awayLinePath = Path()
            awayLinePath.smoothPathTo(awayPoints)
            awayStrokePaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#9AA4B2"))
            canvas.drawPath(awayLinePath, awayStrokePaint)
        }
    }

    /**
     * Extension function to construct smooth cubic bezier splines between coordinate points.
     */
    private fun Path.smoothPathTo(points: List<PointF>) {
        if (points.isEmpty()) return
        moveTo(points[0].x, points[0].y)
        if (points.size == 1) return
        if (points.size == 2) {
            lineTo(points[1].x, points[1].y)
            return
        }
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            
            // Cubic bezier control points to create highly natural, flowing curves
            val cp1X = p0.x + (p1.x - p0.x) / 3f
            val cp1Y = p0.y
            val cp2X = p0.x + 2f * (p1.x - p0.x) / 3f
            val cp2Y = p1.y
            
            cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1.x, p1.y)
        }
    }
}
