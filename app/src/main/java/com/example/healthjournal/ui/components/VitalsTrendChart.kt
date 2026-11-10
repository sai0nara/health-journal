package com.example.healthjournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.ui.theme.ColorTransparent
import com.example.healthjournal.ui.theme.VitalsDiastolicColor
import com.example.healthjournal.ui.theme.VitalsReferenceBandColor
import com.example.healthjournal.ui.theme.VitalsReferenceLineColor
import com.example.healthjournal.ui.theme.VitalsSystolicColor
import com.example.healthjournal.viewmodel.VitalMetricTab
import com.example.healthjournal.viewmodel.VitalPoint
import com.example.healthjournal.viewmodel.VitalsTimeWindow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun VitalsTrendChart(
    metric: VitalMetricTab,
    window: VitalsTimeWindow,
    points: List<VitalPoint>,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .testTag("vitals_trend_chart_empty"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.vitals_chart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var touchX by remember { mutableFloatStateOf(-1f) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val sortedPoints = remember(points) { points.sortedBy { it.timestamp } }

    val referenceBands = remember(metric, unitSystem) {
        getReferenceBands(metric, unitSystem)
    }
    val referenceLines = remember(metric, unitSystem) {
        getReferenceLines(metric, unitSystem)
    }

    val chartCd = stringResource(
        R.string.vitals_chart_content_description,
        metric.label,
        points.size
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("vitals_trend_chart")
            .semantics {
                contentDescription = chartCd
            }
            .pointerInput(sortedPoints) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                    }
                )
            }
            .pointerInput(sortedPoints) {
                detectDragGestures(
                    onDragStart = { offset -> touchX = offset.x },
                    onDrag = { change, _ ->
                        change.consume()
                        touchX = change.position.x
                    },
                    onDragEnd = { },
                    onDragCancel = { }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            val horizontalPad = with(density) { 32.dp.toPx() }
            val verticalPad = with(density) { 24.dp.toPx() }
            val drawableWidth = (size.width - horizontalPad * 2).coerceAtLeast(1f)
            val drawableHeight = (size.height - verticalPad * 2).coerceAtLeast(1f)

            val times = sortedPoints.map { it.timestamp }
            val minTime = times.min()
            val maxTime = times.max()
            val timeSpan = (maxTime - minTime).takeIf { it > 0L } ?: 1L

            // Determine Y-range including reference lines and bands
            val allYValues = mutableListOf<Double>()
            sortedPoints.forEach { pt ->
                allYValues.add(pt.value)
                pt.secondaryValue?.let { allYValues.add(it) }
                pt.min?.let { allYValues.add(it) }
                pt.max?.let { allYValues.add(it) }
            }
            referenceBands.forEach {
                allYValues.add(it.first)
                allYValues.add(it.second)
            }
            referenceLines.forEach { allYValues.add(it) }

            val rawMinY = allYValues.minOrNull() ?: 0.0
            val rawMaxY = allYValues.maxOrNull() ?: 100.0
            val valSpan = (rawMaxY - rawMinY).takeIf { it > 0.0 } ?: 1.0
            val yPadding = valSpan * 0.15
            val minY = rawMinY - yPadding
            val maxY = rawMaxY + yPadding
            val ySpan = (maxY - minY).coerceAtLeast(1.0)

            fun yFor(v: Double): Float =
                (verticalPad + drawableHeight * (1f - ((v - minY) / ySpan).toFloat()))
                    .coerceIn(0f, size.height)

            fun xFor(t: Long): Float =
                if (minTime == maxTime) {
                    size.width / 2f
                } else {
                    horizontalPad + drawableWidth * ((t - minTime).toFloat() / timeSpan)
                }

            // Draw reference bands (subtle tinted rects)
            referenceBands.forEach { (bandMin, bandMax) ->
                val topY = yFor(bandMax)
                val bottomY = yFor(bandMin)
                val height = abs(bottomY - topY)
                drawRect(
                    color = VitalsReferenceBandColor,
                    topLeft = Offset(horizontalPad, topY),
                    size = Size(drawableWidth, height)
                )
            }

            // Draw reference dashed lines
            referenceLines.forEach { refVal ->
                val refY = yFor(refVal)
                drawLine(
                    color = VitalsReferenceLineColor.copy(alpha = 0.5f),
                    start = Offset(horizontalPad, refY),
                    end = Offset(size.width - horizontalPad, refY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }

            // Map points to canvas coordinates
            val primaryOffsets = sortedPoints.map { Offset(xFor(it.timestamp), yFor(it.value)) }
            val secondaryOffsets = if (metric == VitalMetricTab.BLOOD_PRESSURE) {
                sortedPoints.mapNotNull { pt ->
                    pt.secondaryValue?.let { Offset(xFor(pt.timestamp), yFor(it)) }
                }
            } else emptyList()

            // Find closest point to touch
            if (touchX >= 0f) {
                var closestIdx = 0
                var minDiff = Float.MAX_VALUE
                primaryOffsets.forEachIndexed { i, offset ->
                    val diff = abs(offset.x - touchX)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestIdx = i
                    }
                }
                if (selectedPointIndex != closestIdx) {
                    selectedPointIndex = closestIdx
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }

            if (metric == VitalMetricTab.BLOOD_PRESSURE) {
                // Systolic line
                if (primaryOffsets.size > 1) {
                    val sysPath = Path().apply {
                        moveTo(primaryOffsets.first().x, primaryOffsets.first().y)
                        primaryOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(sysPath, color = VitalsSystolicColor, style = Stroke(width = 6f))
                }
                primaryOffsets.forEach {
                    drawCircle(color = surfaceColor, radius = 7f, center = it)
                    drawCircle(color = VitalsSystolicColor, radius = 5f, center = it)
                }

                // Diastolic line
                if (secondaryOffsets.size > 1) {
                    val diaPath = Path().apply {
                        moveTo(secondaryOffsets.first().x, secondaryOffsets.first().y)
                        secondaryOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(diaPath, color = VitalsDiastolicColor, style = Stroke(width = 6f))
                }
                secondaryOffsets.forEach {
                    drawCircle(color = surfaceColor, radius = 7f, center = it)
                    drawCircle(color = VitalsDiastolicColor, radius = 5f, center = it)
                }
            } else {
                // Single-value metric curve with gradient fill
                if (primaryOffsets.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(primaryOffsets.first().x, primaryOffsets.first().y)
                        primaryOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                        lineTo(primaryOffsets.last().x, size.height - verticalPad)
                        lineTo(primaryOffsets.first().x, size.height - verticalPad)
                        close()
                    }
                    drawPath(
                        fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.22f), ColorTransparent),
                            startY = verticalPad,
                            endY = size.height - verticalPad
                        )
                    )

                    val strokePath = Path().apply {
                        moveTo(primaryOffsets.first().x, primaryOffsets.first().y)
                        primaryOffsets.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(strokePath, color = primaryColor, style = Stroke(width = 6f))
                }

                primaryOffsets.forEach {
                    drawCircle(color = surfaceColor, radius = 7f, center = it)
                    drawCircle(color = primaryColor, radius = 5f, center = it)
                }
            }

            // Scrubber cursor
            selectedPointIndex?.let { idx ->
                if (idx in primaryOffsets.indices) {
                    val targetX = primaryOffsets[idx].x
                    drawLine(
                        color = onSurfaceVariant.copy(alpha = 0.8f),
                        start = Offset(targetX, verticalPad / 2f),
                        end = Offset(targetX, size.height - verticalPad / 2f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // Target highlight circles
                    drawCircle(
                        color = if (metric == VitalMetricTab.BLOOD_PRESSURE) VitalsSystolicColor else primaryColor,
                        radius = 11f,
                        center = primaryOffsets[idx],
                        style = Stroke(width = 3f)
                    )

                    if (metric == VitalMetricTab.BLOOD_PRESSURE && idx in secondaryOffsets.indices) {
                        drawCircle(
                            color = VitalsDiastolicColor,
                            radius = 11f,
                            center = secondaryOffsets[idx],
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        }

        // Tooltip display
        selectedPointIndex?.let { idx ->
            if (idx in sortedPoints.indices) {
                val point = sortedPoints[idx]
                ChartTooltip(
                    point = point,
                    metric = metric,
                    unitSystem = unitSystem,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                )
            }
        }

        // Legend row for dual series Blood Pressure
        if (metric == VitalMetricTab.BLOOD_PRESSURE) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VitalsSystolicColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.vitals_field_systolic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VitalsDiastolicColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.vitals_field_diastolic),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartTooltip(
    point: VitalPoint,
    metric: VitalMetricTab,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("vitals_chart_tooltip"),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dateStr = remember(point.timestamp) {
                Instant.ofEpochMilli(point.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
            }

            Text(
                text = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val valueStr = when (metric) {
                VitalMetricTab.BLOOD_PRESSURE -> {
                    "${point.value.roundToInt()} / ${point.secondaryValue?.roundToInt() ?: 0} mmHg"
                }
                VitalMetricTab.HEART_RATE, VitalMetricTab.SPO2 -> {
                    "${point.value.roundToInt()} ${metric.unit(unitSystem)}"
                }
                VitalMetricTab.BLOOD_GLUCOSE, VitalMetricTab.BODY_TEMPERATURE -> {
                    val formatted = BigDecimal(point.value)
                        .setScale(1, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString()
                    "$formatted ${metric.unit(unitSystem)}"
                }
            }

            Text(
                text = valueStr,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (point.count > 1) {
                val minStr = point.min?.let { formatOneDec(it) } ?: "--"
                val maxStr = point.max?.let { formatOneDec(it) } ?: "--"
                Text(
                    text = stringResource(
                        R.string.vitals_chart_daily_mean,
                        point.count,
                        minStr,
                        maxStr
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatOneDec(v: Double): String =
    BigDecimal(v).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

private fun getReferenceBands(
    metric: VitalMetricTab,
    unitSystem: UnitSystem
): List<Pair<Double, Double>> = when (metric) {
    VitalMetricTab.BLOOD_GLUCOSE -> {
        if (unitSystem == UnitSystem.IMPERIAL) listOf(70.0 to 99.0) else listOf(3.9 to 5.6)
    }
    VitalMetricTab.HEART_RATE -> listOf(60.0 to 100.0)
    VitalMetricTab.BODY_TEMPERATURE -> {
        if (unitSystem == UnitSystem.IMPERIAL) listOf(97.0 to 99.0) else listOf(36.1 to 37.2)
    }
    else -> emptyList()
}

private fun getReferenceLines(
    metric: VitalMetricTab,
    unitSystem: UnitSystem
): List<Double> = when (metric) {
    VitalMetricTab.BLOOD_PRESSURE -> listOf(120.0, 80.0)
    VitalMetricTab.SPO2 -> listOf(95.0)
    else -> emptyList()
}
