package com.example.healthjournal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.GoalValidator
import com.example.healthjournal.domain.MeasurementField
import com.example.healthjournal.domain.toParamTrend
import com.example.healthjournal.domain.valueFor
import com.example.healthjournal.ui.components.GoalSheet
import com.example.healthjournal.ui.components.ParamTrendChart
import com.example.healthjournal.viewmodel.BodyAnalyticsViewModel
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
    analyticsViewModel: BodyAnalyticsViewModel,
    onBack: () -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val analyticsState by analyticsViewModel.uiState.collectAsState()
    val measurementState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val pagerState = rememberPagerState(initialPage = 0) { MeasurementField.entries.size }

    // Two-way sync: swiping updates the ViewModel tab, tapping a tab animates the pager.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            analyticsViewModel.onTabSelected(MeasurementField.entries[page])
        }
    }

    // History cards render in the global display units.
    val prefUnits = UnitSettings.read(LocalContext.current)
    LaunchedEffect(prefUnits) {
        viewModel.onUnitSystemChanged(prefUnits)
    }

    var sheetField by remember { mutableStateOf<MeasurementField?>(null) }
    sheetField?.let { field ->
        GoalSheet(
            field = field,
            initialTarget = analyticsState.goalTargets[field.name],
            unitSystem = measurementState.unitSystem,
            onSave = { target ->
                analyticsViewModel.saveGoal(field, target)
                sheetField = null
            },
            onClear = {
                analyticsViewModel.clearGoal(field)
                sheetField = null
            },
            onDismiss = { sheetField = null }
        )
    }

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
            ScrollableTabRow(
                modifier = Modifier.testTag("bm_tabs"),
                selectedTabIndex = MeasurementField.entries.indexOf(analyticsState.selectedTab)
            ) {
                MeasurementField.entries.forEachIndexed { index, field ->
                    Tab(
                        selected = analyticsState.selectedTab == field,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(field.label) }
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                val field = MeasurementField.entries[page]
                val pageSeries = remember(entries, field) { entries.toParamTrend(field) }
                val goalTarget = analyticsState.goalTargets[field.name]
                val unitSystem = measurementState.unitSystem
                val isWeight = field == MeasurementField.WEIGHT
                // Charts plot display units; storage stays canonical metric.
                val displaySeries = remember(pageSeries, unitSystem, field) {
                    if (unitSystem == UnitSystem.IMPERIAL) {
                        pageSeries.map { (time, value) ->
                            time to if (isWeight) UnitConverter.kgToLbs(value)
                            else UnitConverter.cmToInches(value)
                        }
                    } else pageSeries
                }
                val displayGoal = remember(goalTarget, unitSystem, field) {
                    goalTarget?.let {
                        if (unitSystem == UnitSystem.IMPERIAL) {
                            if (isWeight) UnitConverter.kgToLbs(it) else UnitConverter.cmToInches(it)
                        } else it
                    }
                }

                // Column wrapper: header and chart must stack, not overlap
                // in the pager page's Box scope.
                Column(modifier = Modifier.fillMaxWidth()) {
                    ChartHeader(field, goalTarget, unitSystem) { sheetField = field }

                    if (pageSeries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No ${field.label} data yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.testTag("bm_param_empty_${field.name}")
                            )
                        }
                    } else {
                        ParamTrendChart(
                            series = displaySeries,
                            goalTarget = displayGoal,
                            unitLabel = GoalValidator.unitLabel(field, unitSystem),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("bm_chart_${field.name}")
                        )
                    }
                }
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
                            unitSystem = measurementState.unitSystem,
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

/** Chart section header: parameter name plus the Set Goal affordance (FR5). */
@Composable
private fun ChartHeader(
    field: MeasurementField,
    goalTarget: Double?,
    unitSystem: UnitSystem,
    onSetGoal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (goalTarget != null) {
                val isWeight = field == MeasurementField.WEIGHT
                "${field.label} · Goal ${
                    UnitConverter.formatMeasurement(goalTarget, unitSystem, isWeight)
                } ${GoalValidator.unitLabel(field, unitSystem)}"
            } else {
                field.label
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onSetGoal, modifier = Modifier.testTag("bm_set_goal")) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = "Set ${field.label} goal",
                tint = if (goalTarget != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeasurementCard(
    entry: BodyMeasurementEntry,
    dateLabel: String,
    unitSystem: UnitSystem,
    onDelete: () -> Unit
) {
    val circumferenceParams = MeasurementField.entries
        .filter { it != MeasurementField.WEIGHT }
        .mapNotNull { field ->
            entry.valueFor(field)?.let { field.label to it }
        }
    val weightUnit = if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
    val lengthUnit = if (unitSystem == UnitSystem.IMPERIAL) "in" else "cm"

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
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (entry.weight_kg != null) {
                    Text(
                        text = "${UnitConverter.formatMeasurement(entry.weight_kg, unitSystem, isWeight = true)} $weightUnit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (circumferenceParams.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        circumferenceParams.forEach { (label, value) ->
                            Text(
                                text = "$label ${UnitConverter.formatMeasurement(value, unitSystem, isWeight = false)} $lengthUnit",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.isSynced == true) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = "Cloud Synced",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.CloudSync,
                        contentDescription = "Local Only",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
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
}

