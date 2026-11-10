package com.example.healthjournal.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthjournal.R
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.ui.components.VitalLogSheet
import com.example.healthjournal.ui.components.VitalsTrendChart
import com.example.healthjournal.viewmodel.VitalMetricTab
import com.example.healthjournal.viewmodel.VitalSummaryStats
import com.example.healthjournal.viewmodel.VitalsTimeWindow
import com.example.healthjournal.viewmodel.VitalsViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(
    viewModel: VitalsViewModel,
    onBack: () -> Unit,
    profileSwitcher: @Composable (enabled: Boolean, onSwitch: (String) -> Unit) -> Unit =
        { _, _ -> },
    onSwitchProfile: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()) }

    var showLogSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_tile_vitals)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = { profileSwitcher(true, onSwitchProfile) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showLogSheet = true },
                modifier = Modifier.testTag("vitals_fab_log")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.vitals_fab_log)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metric Tabs
            ScrollableTabRow(
                selectedTabIndex = state.selectedMetric.ordinal,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                VitalMetricTab.entries.forEach { tab ->
                    val tabTitle = when (tab) {
                        VitalMetricTab.BLOOD_PRESSURE -> stringResource(R.string.vitals_tab_bp)
                        VitalMetricTab.BLOOD_GLUCOSE -> stringResource(R.string.vitals_field_glucose)
                        VitalMetricTab.HEART_RATE -> stringResource(R.string.vitals_field_hr)
                        VitalMetricTab.BODY_TEMPERATURE -> stringResource(R.string.vitals_field_temperature)
                        VitalMetricTab.SPO2 -> stringResource(R.string.vitals_field_spo2)
                    }
                    Tab(
                        selected = state.selectedMetric == tab,
                        onClick = { viewModel.selectMetricTab(tab) },
                        text = { Text(tabTitle) },
                        modifier = Modifier.testTag("vitals_tab_${tab.name}")
                    )
                }
            }

            // Time Window Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VitalsTimeWindow.entries.forEach { window ->
                    val label = when (window) {
                        VitalsTimeWindow.DAYS_7 -> stringResource(R.string.vitals_window_7d)
                        VitalsTimeWindow.DAYS_30 -> stringResource(R.string.vitals_window_30d)
                        VitalsTimeWindow.DAYS_90 -> stringResource(R.string.vitals_window_90d)
                        VitalsTimeWindow.YEAR_1 -> stringResource(R.string.vitals_window_1y)
                        VitalsTimeWindow.ALL -> stringResource(R.string.vitals_window_all)
                    }
                    FilterChip(
                        selected = state.selectedWindow == window,
                        onClick = { viewModel.selectTimeWindow(window) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("vitals_window_${window.name}")
                    )
                }
            }

            // Summary Stats Card
            VitalsSummaryCard(
                stats = state.stats,
                metric = state.selectedMetric,
                unitSystem = state.unitSystem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Trend Chart
            VitalsTrendChart(
                metric = state.selectedMetric,
                window = state.selectedWindow,
                points = state.points,
                unitSystem = state.unitSystem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Empty state or History list
            if (state.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("vitals_empty_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.vitals_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.vitals_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showLogSheet = true },
                            modifier = Modifier.testTag("vitals_empty_cta")
                        ) {
                            Text(stringResource(R.string.vitals_empty_cta))
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.vitals_history_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.historyEntries, key = { it.entry_id }) { entry ->
                        VitalHistoryCard(
                            entry = entry,
                            metric = state.selectedMetric,
                            unitSystem = state.unitSystem,
                            dateLabel = dateFormat.format(Date(entry.timestamp)),
                            onDelete = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteVital(entry)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.vitals_snackbar_deleted),
                                        actionLabel = context.getString(R.string.history_undo),
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    } else {
                                        viewModel.dismissUndo()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLogSheet) {
        VitalLogSheet(
            initialMetric = state.selectedMetric,
            unitSystem = state.unitSystem,
            onDismiss = { showLogSheet = false },
            onSave = { vitals, timestamp ->
                viewModel.logVital(vitals, timestamp)
            }
        )
    }
}

@Composable
private fun VitalsSummaryCard(
    stats: VitalSummaryStats,
    metric: VitalMetricTab,
    unitSystem: UnitSystem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("vitals_summary_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.vitals_stats_latest),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stats.latestValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = metric.unit(unitSystem),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    stats.statusBadge?.let { badge ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.vitals_stats_average),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stats.meanValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${stringResource(R.string.vitals_stats_min)} / ${stringResource(R.string.vitals_stats_max)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stats.minValue} - ${stats.maxValue}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VitalHistoryCard(
    entry: JournalEntry,
    metric: VitalMetricTab,
    unitSystem: UnitSystem,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val valueDisplay = formatEntryMetricValue(entry, metric, unitSystem)
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("vitals_delete_${entry.entry_id}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatEntryMetricValue(
    entry: JournalEntry,
    metric: VitalMetricTab,
    unitSystem: UnitSystem
): String = when (metric) {
    VitalMetricTab.BLOOD_PRESSURE -> {
        val sys = entry.bp_systolic?.roundToInt() ?: "--"
        val dia = entry.bp_diastolic?.roundToInt() ?: "--"
        "$sys/$dia mmHg"
    }
    VitalMetricTab.BLOOD_GLUCOSE -> {
        val mmol = entry.blood_glucose_mmol
        if (mmol == null) "--"
        else if (unitSystem == UnitSystem.IMPERIAL) {
            "${UnitConverter.mmolToMgdl(mmol).roundToInt()} mg/dL"
        } else {
            "${formatOneDec(mmol)} mmol/L"
        }
    }
    VitalMetricTab.HEART_RATE -> {
        "${entry.heart_rate_avg ?: "--"} bpm"
    }
    VitalMetricTab.BODY_TEMPERATURE -> {
        val c = entry.body_temperature_c
        if (c == null) "--"
        else if (unitSystem == UnitSystem.IMPERIAL) {
            "${formatOneDec(UnitConverter.celsiusToFahrenheit(c))} °F"
        } else {
            "${formatOneDec(c)} °C"
        }
    }
    VitalMetricTab.SPO2 -> {
        "${entry.spo2_percent ?: "--"}%"
    }
}

private fun formatOneDec(value: Double): String =
    BigDecimal(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
