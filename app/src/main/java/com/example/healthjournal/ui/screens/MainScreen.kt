package com.example.healthjournal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.ui.components.QuickLogBottomSheet
import com.example.healthjournal.viewmodel.TodayActivityCategory
import com.example.healthjournal.viewmodel.TodayActivityItem
import com.example.healthjournal.viewmodel.TodayDashboardUiState
import com.example.healthjournal.viewmodel.TodayMeasurementsCardState
import com.example.healthjournal.viewmodel.TodayVitalSlide
import com.example.healthjournal.viewmodel.TodayWorkoutsCardState
import com.example.healthjournal.viewmodel.VitalMetricTab
import com.example.healthjournal.ui.theme.ColorTransparent
import com.example.healthjournal.ui.theme.TodayFabGradientEnd as FabTealEnd
import com.example.healthjournal.ui.theme.TodayFabGradientStart as FabTealStart
import com.example.healthjournal.ui.theme.TodayMeasurementsAccent as MeasurementsSky
import com.example.healthjournal.ui.theme.TodayVitalsAccent as VitalsCoral
import com.example.healthjournal.ui.theme.TodayWorkoutsAccent as WorkoutsEmerald
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Glanceable Today Dashboard launch screen: live Overview cards (Vitals carousel,
 * Workouts weekly progress ring, Measurements sparkline), Recent Activity timeline,
 * `+ Log` Quick-Log FAB, and persistent bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: TodayDashboardUiState = TodayDashboardUiState(),
    onOpenVitals: (VitalMetricTab) -> Unit = {},
    onOpenWorkouts: () -> Unit = {},
    onOpenMeasurements: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenPresets: () -> Unit = {},
    onStartPresetWorkout: (WorkoutPreset) -> Unit = {},
    onNewJournalEntry: () -> Unit = {},
    onActivityClick: (TodayActivityItem) -> Unit = {},
    profileSwitcher: @Composable (enabled: Boolean, onSwitch: (String) -> Unit) -> Unit =
        { _, _ -> },
    onSwitchProfile: (String) -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    var showQuickLogSheet by remember { mutableStateOf(false) }

    if (showQuickLogSheet) {
        QuickLogBottomSheet(
            presets = state.quickLogPresets,
            onDismiss = { showQuickLogSheet = false },
            onPresetSelected = onStartPresetWorkout,
            onManagePresets = onOpenPresets,
            onLogVitals = { onOpenVitals(VitalMetricTab.BLOOD_PRESSURE) },
            onStartWorkout = onOpenWorkouts,
            onLogMeasurement = onOpenMeasurements,
            onNewJournalEntry = onNewJournalEntry
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(32.dp)
                            .testTag("app_logo")
                    )
                },
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    profileSwitcher(true, onSwitchProfile)
                }
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            val fabCd = stringResource(R.string.today_fab_log)
            FloatingActionButton(
                onClick = { if (!showQuickLogSheet) showQuickLogSheet = true },
                shape = CircleShape,
                containerColor = ColorTransparent,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(FabTealStart, FabTealEnd)),
                        shape = CircleShape
                    )
                    .testTag("today_quick_log_fab")
                    .semantics { contentDescription = fabCd }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = fabCd,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.today_overview_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                VitalsCarouselSection(
                    slides = state.vitalsSlides,
                    onSlideClick = { slide -> onOpenVitals(slide.tab) }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkoutsSummaryCard(
                        card = state.workoutsCard,
                        onClick = onOpenWorkouts,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    MeasurementsSummaryCard(
                        card = state.measurementsCard,
                        onClick = onOpenMeasurements,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }

            item {
                RecentActivitySection(
                    items = state.recentActivity,
                    onViewAll = onOpenHistory,
                    onItemClick = onActivityClick
                )
            }

            item {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun VitalsCarouselSection(
    slides: List<TodayVitalSlide>,
    onSlideClick: (TodayVitalSlide) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { slides.size.coerceAtLeast(1) })
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vitals_carousel_pager")
        ) { pageIndex ->
            val slide = slides.getOrElse(pageIndex) {
                TodayVitalSlide(tab = VitalMetricTab.BLOOD_PRESSURE, isEmptyState = true)
            }
            VitalsSlideCard(
                slide = slide,
                onClick = { onSlideClick(slide) }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            slides.forEachIndexed { index, _ ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (active) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                        .testTag("vitals_pager_dot")
                )
            }
        }
    }
}

@Composable
private fun VitalsSlideCard(
    slide: TodayVitalSlide,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = VitalsCoral.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("overview_vitals_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SemanticCategoryBadge(
                        icon = Icons.Default.Favorite,
                        accent = VitalsCoral,
                        contentDescription = stringResource(R.string.today_vitals_title)
                    )
                    Text(
                        text = if (slide.isEmptyState) {
                            stringResource(R.string.today_vitals_title)
                        } else {
                            vitalTabTitle(slide.tab)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                slide.timestampMillis?.let { ts ->
                    Text(
                        text = formatRelativeCardTimestamp(ts),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (slide.isEmptyState) {
                Text(
                    text = stringResource(R.string.today_empty_card_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val valueLine = buildString {
                        append(slide.latestValueDisplay.orEmpty())
                        if (!slide.secondaryValueDisplay.isNullOrBlank()) {
                            append(" • ")
                            append(slide.secondaryValueDisplay)
                        }
                    }
                    Text(
                        text = valueLine,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(12.dp))
                    MiniSparkline(
                        points = slide.sparklinePoints,
                        lineColor = VitalsCoral,
                        fillArea = false,
                        modifier = Modifier
                            .width(84.dp)
                            .height(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutsSummaryCard(
    card: TodayWorkoutsCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("overview_workouts_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SemanticCategoryBadge(
                        icon = Icons.Default.FitnessCenter,
                        accent = WorkoutsEmerald,
                        contentDescription = stringResource(R.string.today_workouts_title)
                    )
                    Text(
                        text = stringResource(R.string.today_workouts_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (card.isEmptyState) {
                    Text(
                        text = stringResource(R.string.today_empty_card_prompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val sub = buildString {
                        append(card.latestWorkoutName.orEmpty())
                        card.latestTimestampMillis?.let { ts ->
                            append(" • ")
                            append(formatRelativeDateOnly(ts))
                        }
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                Box(
                    modifier = Modifier.size(88.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 8.dp.toPx()
                        drawArc(
                            color = trackColor,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = WorkoutsEmerald,
                            startAngle = -90f,
                            sweepAngle = 360f * card.progressFraction.coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${card.completedThisWeek}/${card.weeklyTarget}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(
                                R.string.today_this_week,
                                card.completedThisWeek,
                                card.weeklyTarget
                            ).substringAfter(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementsSummaryCard(
    card: TodayMeasurementsCardState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag("overview_measurements_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SemanticCategoryBadge(
                    icon = Icons.Default.Straighten,
                    accent = MeasurementsSky,
                    contentDescription = stringResource(R.string.today_measurements_title)
                )
                Text(
                    text = stringResource(R.string.today_measurements_title),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (card.isEmptyState) {
                Text(
                    text = stringResource(R.string.today_empty_card_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val weightLine = buildString {
                        append(card.latestWeightDisplay.orEmpty())
                        card.latestTimestampMillis?.let { ts ->
                            append(" • ")
                            append(formatRelativeDateOnly(ts))
                        }
                    }
                    Text(
                        text = weightLine,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    MiniSparkline(
                        points = card.sparklinePoints,
                        lineColor = MeasurementsSky,
                        fillArea = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Text(
                        text = card.weeklyDeltaDisplay?.let {
                            stringResource(R.string.today_delta_this_week, it)
                        } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeasurementsSky,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivitySection(
    items: List<TodayActivityItem>,
    onViewAll: () -> Unit,
    onItemClick: (TodayActivityItem) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.today_recent_activity_header),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = onViewAll,
                modifier = Modifier.testTag("recent_activity_view_all")
            ) {
                Text(stringResource(R.string.today_view_all))
            }
        }

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.today_recent_activity_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            items.forEachIndexed { index, item ->
                RecentActivityRow(
                    item = item,
                    isLast = index == items.lastIndex,
                    index = index,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    item: TodayActivityItem,
    isLast: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    val dotColor = when (item.category) {
        TodayActivityCategory.VITALS -> VitalsCoral
        TodayActivityCategory.WORKOUT -> WorkoutsEmerald
        TodayActivityCategory.MEASUREMENT -> MeasurementsSky
        TodayActivityCategory.JOURNAL -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("recent_activity_row")
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .testTag("recent_activity_row_$index"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(top = 4.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatRelativeDateOnly(item.timestampMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item.formattedTime?.let { timeText ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SemanticCategoryBadge(
    icon: ImageVector,
    accent: Color,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.2f))
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MiniSparkline(
    points: List<Float>,
    lineColor: Color,
    fillArea: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas
        if (points.size == 1) {
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(size.width / 2f, size.height / 2f)
            )
            return@Canvas
        }
        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 0f
        val range = (maxVal - minVal).takeIf { it > 0.0001f } ?: 1f
        val stepX = size.width / (points.size - 1).toFloat()
        val coords = points.mapIndexed { idx, value ->
            val x = idx * stepX
            val normalizedY = (value - minVal) / range
            val y = size.height - (normalizedY * size.height * 0.8f) - size.height * 0.1f
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            coords.drop(1).forEach { lineTo(it.x, it.y) }
        }

        if (fillArea) {
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coords.last().x, size.height)
                lineTo(coords.first().x, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f))
                )
            )
        }

        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawCircle(
            color = lineColor,
            radius = 3.dp.toPx(),
            center = coords.last()
        )
    }
}

@Composable
private fun vitalTabTitle(tab: VitalMetricTab): String = when (tab) {
    VitalMetricTab.BLOOD_PRESSURE -> stringResource(R.string.vitals_tab_bp)
    VitalMetricTab.BLOOD_GLUCOSE -> stringResource(R.string.vitals_field_glucose)
    VitalMetricTab.HEART_RATE -> stringResource(R.string.vitals_field_hr)
    VitalMetricTab.BODY_TEMPERATURE -> stringResource(R.string.vitals_field_temperature)
    VitalMetricTab.SPO2 -> stringResource(R.string.vitals_field_spo2)
}

@Composable
private fun formatRelativeCardTimestamp(timestampMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(timestampMillis).atZone(zone)
    val dateLabel = formatRelativeDateOnly(timestampMillis)
    val locale = Locale.getDefault()
    val timePattern = if (locale.language == "ru") "HH:mm" else "hh:mm a"
    val timeStr = DateTimeFormatter.ofPattern(timePattern, locale).format(dateTime)
    return if (dateLabel == stringResource(R.string.history_preset_today)) {
        "$dateLabel, $timeStr"
    } else {
        dateLabel
    }
}

@Composable
private fun formatRelativeDateOnly(timestampMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(timestampMillis).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> stringResource(R.string.history_preset_today)
        today.minusDays(1) -> stringResource(R.string.history_preset_yesterday)
        else -> {
            val pattern = if (date.year == today.year) "d MMM" else "d MMM yyyy"
            DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
        }
    }
}
