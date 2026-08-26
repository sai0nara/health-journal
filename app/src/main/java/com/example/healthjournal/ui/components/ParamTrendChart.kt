package com.example.healthjournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

/**
 * Minimal dependency-free per-parameter trend chart (Compose Canvas):
 * plots the series left-to-right with automatic min/max scaling that
 * includes the goal target, so the dashed goal line is always visible.
 *
 * When [goalTarget] is set, a dashed horizontal line marks the goal and the
 * region between the polyline and the goal line is filled with a translucent
 * tint (FR3/FR4), giving an at-a-glance sense of distance from target.
 */
@Composable
fun ParamTrendChart(
    series: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    goalTarget: Double? = null,
    unitLabel: String? = null
) {
    if (series.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val goalColor = MaterialTheme.colorScheme.tertiary

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val times = series.map { it.first }
            val values = series.map { it.second }
            // Include the goal in scaling so the line never leaves the canvas.
            val scaledValues = if (goalTarget != null) values + goalTarget else values
            val minTime = times.min()
            val maxTime = times.max()
            val minValue = scaledValues.min()
            val maxValue = scaledValues.max()

            val horizontalPadding = 24f
            val verticalPadding = 24f
            val drawableWidth = size.width - horizontalPadding * 2
            val drawableHeight = size.height - verticalPadding * 2
            val timeSpan = (maxTime - minTime).coerceAtLeast(1L)
            // Flat series still renders a mid-height line instead of dividing by zero.
            val valueSpan = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

            fun yFor(value: Double): Float =
                verticalPadding + drawableHeight *
                    (1f - ((value - minValue).toFloat() / valueSpan.toFloat()))

            val points = series.map { (time, value) ->
                Offset(
                    x = horizontalPadding + drawableWidth *
                        ((time - minTime).toFloat() / timeSpan),
                    y = yFor(value)
                )
            }
            val goalY = goalTarget?.let { yFor(it) }

            if (goalY != null) {
                // Delta area between the series and the goal line (FR4).
                val areaPath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, goalY)
                    lineTo(points.first().x, goalY)
                    close()
                }
                drawPath(areaPath, color = fillColor)
            }

            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color = lineColor, style = Stroke(width = 6f))
            points.forEach { drawCircle(color = pointColor, radius = 8f, center = it) }

            if (goalY != null) {
                drawLine(
                    color = goalColor,
                    start = Offset(horizontalPadding, goalY),
                    end = Offset(size.width - horizontalPadding, goalY),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            }
        }

        if (goalTarget != null && unitLabel != null) {
            Text(
                text = "Goal ${formatGoalValue(goalTarget)} $unitLabel",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = goalColor,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 24.dp)
            )
        }
    }
}

/** Trims trailing zeros so labels read "80 cm" not "80.0 cm". */
private fun formatGoalValue(target: Double): String =
    if (target == target.toLong().toDouble()) target.toLong().toString() else target.toString()
