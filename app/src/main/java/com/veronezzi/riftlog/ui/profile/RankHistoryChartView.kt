package com.veronezzi.riftlog.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.rifttracker.designsystem.R as DesignR

/**
 * Minimal step/line chart of rank progression over time. No chart library dependency - a
 * handful of points doesn't need one, just a Canvas.
 *
 * The caller must not feed this view fewer than [MIN_POINTS_TO_RENDER] values: there's no line
 * to draw between zero or one point, so the empty/placeholder state belongs at the call site
 * (see ProfileFragment), not here.
 */
class RankHistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var values: List<Int> = emptyList()

    private val density = context.resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, DesignR.color.rift_accent)
        style = Paint.Style.STROKE
        strokeWidth = density * 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = linePaint.color
        style = Paint.Style.FILL
    }
    private val dotRadius = density * 3f
    private val verticalInset = density * 8f

    fun setValues(newValues: List<Int>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = values
        if (points.size < MIN_POINTS_TO_RENDER || width == 0 || height == 0) return

        val min = points.min()
        val max = points.max()
        // Flat history (rank hasn't moved at all): still draw a straight horizontal line rather
        // than dividing by zero.
        val range = (max - min).coerceAtLeast(1)
        val top = verticalInset
        val bottom = height - verticalInset
        val usableHeight = (bottom - top).coerceAtLeast(1f)
        val stepX = width.toFloat() / (points.size - 1)

        fun yFor(value: Int) = bottom - ((value - min).toFloat() / range) * usableHeight

        var previousX = 0f
        var previousY = yFor(points[0])
        for (i in 1 until points.size) {
            val x = i * stepX
            val y = yFor(points[i])
            canvas.drawLine(previousX, previousY, x, y, linePaint)
            previousX = x
            previousY = y
        }
        points.forEachIndexed { index, value ->
            canvas.drawCircle(index * stepX, yFor(value), dotRadius, dotPaint)
        }
    }

    companion object {
        const val MIN_POINTS_TO_RENDER = 2
    }
}
