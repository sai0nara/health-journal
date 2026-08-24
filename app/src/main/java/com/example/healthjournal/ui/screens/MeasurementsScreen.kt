package com.example.healthjournal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.domain.toSummary
import com.example.healthjournal.domain.toWeightTrend
import com.example.healthjournal.viewmodel.BodyMeasurementViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Chronological list of saved body measurements with a weight trend chart
 * and undo-protected deletion (Product Guidelines: Safety Nets & Undo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementsScreen(
    viewModel: BodyMeasurementViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body measurements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val trend = entries.toWeightTrend()
            if (trend.isNotEmpty()) {
                WeightTrendChart(
                    trend = trend,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("bm_trend_chart")
                )
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No body measurements yet",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("bm_empty_state")
                        )
                        Text(
                            text = "Tap the ruler button on the History screen to log your first entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.entry_id }) { entry ->
                        MeasurementCard(
                            entry = entry,
                            dateLabel = dateFormat.format(Date(entry.timestamp)),
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteEntry(entry.entry_id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Measurement deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    entry: BodyMeasurementEntry,
    dateLabel: String,
    onDelete: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.toSummary(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete measurement",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Minimal dependency-free line chart (Compose Canvas): plots the weight
 * series left-to-right with automatic min/max scaling.
 */
@Composable
fun WeightTrendChart(
    trend: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    if (trend.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val times = trend.map { it.first }
        val values = trend.map { it.second }
        val minTime = times.min()
        val maxTime = times.max()
        val minValue = values.min()
        val maxValue = values.max()

        val horizontalPadding = 24f
        val verticalPadding = 24f
        val drawableWidth = size.width - horizontalPadding * 2
        val drawableHeight = size.height - verticalPadding * 2
        val timeSpan = (maxTime - minTime).coerceAtLeast(1L)
        // Flat series still renders a mid-height line instead of dividing by zero.
        val valueSpan = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

        val points = trend.map { (time, value) ->
            Offset(
                x = horizontalPadding + drawableWidth *
                    ((time - minTime).toFloat() / timeSpan),
                y = verticalPadding + drawableHeight *
                    (1f - ((value - minValue).toFloat() / valueSpan.toFloat()))
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 6f))
        points.forEach { drawCircle(color = pointColor, radius = 8f, center = it) }
    }
}
