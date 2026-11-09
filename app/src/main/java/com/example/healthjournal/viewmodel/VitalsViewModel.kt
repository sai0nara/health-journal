package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.ActiveProfileProvider
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

enum class VitalMetricTab(val label: String, val metricUnit: String, val imperialUnit: String = metricUnit) {
    BLOOD_PRESSURE("Blood Pressure", "mmHg", "mmHg"),
    BLOOD_GLUCOSE("Blood Glucose", "mmol/L", "mg/dL"),
    HEART_RATE("Heart Rate", "bpm", "bpm"),
    BODY_TEMPERATURE("Temperature", "°C", "°F"),
    SPO2("Oxygen Saturation", "%", "%");

    fun unit(unitSystem: UnitSystem): String =
        if (unitSystem == UnitSystem.IMPERIAL) imperialUnit else metricUnit
}

enum class VitalsTimeWindow(val label: String, val durationMs: Long?) {
    DAYS_7("7D", 7L * 86_400_000L),
    DAYS_30("30D", 30L * 86_400_000L),
    DAYS_90("90D", 90L * 86_400_000L),
    YEAR_1("1Y", 365L * 86_400_000L),
    ALL("All", null);

    val isLongRange: Boolean get() = this == DAYS_90 || this == YEAR_1 || this == ALL
}

data class VitalPoint(
    val timestamp: Long,
    val value: Double,
    val secondaryValue: Double? = null,
    val count: Int = 1,
    val min: Double? = null,
    val max: Double? = null,
    val entryId: String? = null,
    val rawEntry: JournalEntry? = null
)

data class VitalSummaryStats(
    val latestValue: String = "--",
    val latestTimestamp: Long? = null,
    val meanValue: String = "--",
    val minValue: String = "--",
    val maxValue: String = "--",
    val statusBadge: String? = null
)

data class VitalsUiState(
    val selectedMetric: VitalMetricTab = VitalMetricTab.BLOOD_PRESSURE,
    val selectedWindow: VitalsTimeWindow = VitalsTimeWindow.DAYS_30,
    val points: List<VitalPoint> = emptyList(),
    val historyEntries: List<JournalEntry> = emptyList(),
    val stats: VitalSummaryStats = VitalSummaryStats(),
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val isLoading: Boolean = false,
    val isEmpty: Boolean = true,
    val isUndoActive: Boolean = false,
    val lastDeletedEntry: JournalEntry? = null,
    val activeProfileId: String = SELF_PROFILE_ID
)

class VitalsViewModel(
    private val journalRepository: JournalRepository,
    private val activeProfileProvider: ActiveProfileProvider,
    private val unitSystemFlow: StateFlow<UnitSystem>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {

    private val _selectedMetric = MutableStateFlow(VitalMetricTab.BLOOD_PRESSURE)
    private val _selectedWindow = MutableStateFlow(VitalsTimeWindow.DAYS_30)
    private val _pendingDeletedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _lastDeletedEntry = MutableStateFlow<JournalEntry?>(null)
    private val _isUndoActive = MutableStateFlow(false)

    val uiState: StateFlow<VitalsUiState> = combine(
        journalRepository.vitalsEntries,
        activeProfileProvider.activeProfileId,
        unitSystemFlow,
        _selectedMetric,
        _selectedWindow,
        _pendingDeletedIds,
        _isUndoActive,
        _lastDeletedEntry
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val allEntries = args[0] as List<JournalEntry>
        val activeProfile = args[1] as String
        val unitSystem = args[2] as UnitSystem
        val metric = args[3] as VitalMetricTab
        val window = args[4] as VitalsTimeWindow
        @Suppress("UNCHECKED_CAST")
        val pendingDeleted = args[5] as Set<String>
        val isUndoActive = args[6] as Boolean
        val lastDeleted = args[7] as JournalEntry?

        buildUiState(
            allEntries = allEntries,
            activeProfile = activeProfile,
            unitSystem = unitSystem,
            metric = metric,
            window = window,
            pendingDeleted = pendingDeleted,
            isUndoActive = isUndoActive,
            lastDeleted = lastDeleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = VitalsUiState()
    )

    fun selectMetricTab(tab: VitalMetricTab) {
        _selectedMetric.value = tab
    }

    fun selectTimeWindow(window: VitalsTimeWindow) {
        _selectedWindow.value = window
    }

    fun logVital(vitals: EntryVitals, timestamp: Long = clock()) {
        viewModelScope.launch(dispatcher) {
            val profileId = activeProfileProvider.currentProfileId()
            val entry = JournalEntry(
                entry_id = UUID.randomUUID().toString(),
                profileId = profileId,
                description = "",
                timestamp = timestamp,
                bp_systolic = vitals.bpSystolic,
                bp_diastolic = vitals.bpDiastolic,
                heart_rate_avg = vitals.heartRate,
                blood_glucose_mmol = vitals.bloodGlucoseMmol,
                body_temperature_c = vitals.bodyTemperatureC,
                spo2_percent = vitals.spo2Percent
            )
            journalRepository.insert(profileId, entry)
        }
    }

    fun deleteVital(entry: JournalEntry) {
        val entryId = entry.entry_id
        _pendingDeletedIds.update { it + entryId }
        _lastDeletedEntry.value = entry
        _isUndoActive.value = true

        viewModelScope.launch(dispatcher) {
            val profileId = activeProfileProvider.currentProfileId()
            journalRepository.delete(profileId, entryId)
        }
    }

    fun undoDelete() {
        val entryToRestore = _lastDeletedEntry.value ?: return
        _pendingDeletedIds.update { it - entryToRestore.entry_id }
        _lastDeletedEntry.value = null
        _isUndoActive.value = false

        viewModelScope.launch(dispatcher) {
            val profileId = activeProfileProvider.currentProfileId()
            journalRepository.insert(profileId, entryToRestore)
        }
    }

    fun dismissUndo() {
        _lastDeletedEntry.value = null
        _isUndoActive.value = false
    }

    private fun buildUiState(
        allEntries: List<JournalEntry>,
        activeProfile: String,
        unitSystem: UnitSystem,
        metric: VitalMetricTab,
        window: VitalsTimeWindow,
        pendingDeleted: Set<String>,
        isUndoActive: Boolean,
        lastDeleted: JournalEntry?
    ): VitalsUiState {
        val now = clock()
        val windowCutoff = window.durationMs?.let { now - it } ?: Long.MIN_VALUE

        val eligibleEntries = allEntries
            .filter { it.profileId == activeProfile || activeProfile.isEmpty() }
            .filter { it.entry_id !in pendingDeleted }
            .filter { it.timestamp >= windowCutoff }
            .sortedBy { it.timestamp }

        // Filter and map to VitalPoint per metric
        val rawPoints = eligibleEntries.mapNotNull { entry ->
            when (metric) {
                VitalMetricTab.BLOOD_PRESSURE -> {
                    val sys = entry.bp_systolic
                    val dia = entry.bp_diastolic
                    if (sys != null && dia != null) {
                        VitalPoint(
                            timestamp = entry.timestamp,
                            value = sys,
                            secondaryValue = dia,
                            count = 1,
                            min = sys,
                            max = sys,
                            entryId = entry.entry_id,
                            rawEntry = entry
                        )
                    } else null
                }
                VitalMetricTab.BLOOD_GLUCOSE -> {
                    val mmol = entry.blood_glucose_mmol
                    if (mmol != null) {
                        val displayVal = if (unitSystem == UnitSystem.IMPERIAL) {
                            UnitConverter.mmolToMgdl(mmol)
                        } else mmol
                        VitalPoint(
                            timestamp = entry.timestamp,
                            value = displayVal,
                            count = 1,
                            min = displayVal,
                            max = displayVal,
                            entryId = entry.entry_id,
                            rawEntry = entry
                        )
                    } else null
                }
                VitalMetricTab.HEART_RATE -> {
                    val hr = entry.heart_rate_avg
                    if (hr != null) {
                        VitalPoint(
                            timestamp = entry.timestamp,
                            value = hr.toDouble(),
                            count = 1,
                            min = hr.toDouble(),
                            max = hr.toDouble(),
                            entryId = entry.entry_id,
                            rawEntry = entry
                        )
                    } else null
                }
                VitalMetricTab.BODY_TEMPERATURE -> {
                    val c = entry.body_temperature_c
                    if (c != null) {
                        val displayVal = if (unitSystem == UnitSystem.IMPERIAL) {
                            UnitConverter.celsiusToFahrenheit(c)
                        } else c
                        VitalPoint(
                            timestamp = entry.timestamp,
                            value = displayVal,
                            count = 1,
                            min = displayVal,
                            max = displayVal,
                            entryId = entry.entry_id,
                            rawEntry = entry
                        )
                    } else null
                }
                VitalMetricTab.SPO2 -> {
                    val spo2 = entry.spo2_percent
                    if (spo2 != null) {
                        VitalPoint(
                            timestamp = entry.timestamp,
                            value = spo2.toDouble(),
                            count = 1,
                            min = spo2.toDouble(),
                            max = spo2.toDouble(),
                            entryId = entry.entry_id,
                            rawEntry = entry
                        )
                    } else null
                }
            }
        }

        // Apply same-day decimation if in a long-range window
        val points = if (window.isLongRange && rawPoints.isNotEmpty()) {
            aggregateSameDayPoints(rawPoints)
        } else {
            rawPoints
        }

        val history = eligibleEntries
            .filter { entryHasMetric(it, metric) }
            .sortedByDescending { it.timestamp }

        val stats = computeStats(points, rawPoints, metric, unitSystem)

        return VitalsUiState(
            selectedMetric = metric,
            selectedWindow = window,
            points = points,
            historyEntries = history,
            stats = stats,
            unitSystem = unitSystem,
            isLoading = false,
            isEmpty = points.isEmpty(),
            isUndoActive = isUndoActive,
            lastDeletedEntry = lastDeleted,
            activeProfileId = activeProfile
        )
    }

    private fun entryHasMetric(entry: JournalEntry, metric: VitalMetricTab): Boolean =
        when (metric) {
            VitalMetricTab.BLOOD_PRESSURE -> entry.bp_systolic != null && entry.bp_diastolic != null
            VitalMetricTab.BLOOD_GLUCOSE -> entry.blood_glucose_mmol != null
            VitalMetricTab.HEART_RATE -> entry.heart_rate_avg != null
            VitalMetricTab.BODY_TEMPERATURE -> entry.body_temperature_c != null
            VitalMetricTab.SPO2 -> entry.spo2_percent != null
        }

    private fun aggregateSameDayPoints(points: List<VitalPoint>): List<VitalPoint> {
        val zone = ZoneId.systemDefault()
        return points
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
            .map { (_, dayPoints) ->
                if (dayPoints.size == 1) {
                    dayPoints.first()
                } else {
                    val meanVal = dayPoints.map { it.value }.average()
                    val meanSecVal = if (dayPoints.any { it.secondaryValue != null }) {
                        dayPoints.mapNotNull { it.secondaryValue }.average()
                    } else null
                    val minVal = dayPoints.minOf { it.min ?: it.value }
                    val maxVal = dayPoints.maxOf { it.max ?: it.value }
                    VitalPoint(
                        timestamp = dayPoints.first().timestamp,
                        value = meanVal,
                        secondaryValue = meanSecVal,
                        count = dayPoints.size,
                        min = minVal,
                        max = maxVal,
                        entryId = null,
                        rawEntry = null
                    )
                }
            }
            .sortedBy { it.timestamp }
    }

    private fun computeStats(
        points: List<VitalPoint>,
        rawPoints: List<VitalPoint>,
        metric: VitalMetricTab,
        unitSystem: UnitSystem
    ): VitalSummaryStats {
        if (points.isEmpty()) return VitalSummaryStats()

        val latestPoint = rawPoints.maxByOrNull { it.timestamp } ?: points.last()

        return when (metric) {
            VitalMetricTab.BLOOD_PRESSURE -> {
                val latestSys = latestPoint.value.roundToInt()
                val latestDia = (latestPoint.secondaryValue ?: 0.0).roundToInt()
                val meanSys = rawPoints.map { it.value }.average().roundToInt()
                val meanDia = rawPoints.mapNotNull { it.secondaryValue }.average().roundToInt()
                val minSys = rawPoints.minOf { it.value }.roundToInt()
                val minDia = rawPoints.mapNotNull { it.secondaryValue }.minOrNull()?.roundToInt() ?: 0
                val maxSys = rawPoints.maxOf { it.value }.roundToInt()
                val maxDia = rawPoints.mapNotNull { it.secondaryValue }.maxOrNull()?.roundToInt() ?: 0

                val badge = when {
                    latestSys < 120 && latestDia < 80 -> "Normal"
                    latestSys in 120..129 && latestDia < 80 -> "Elevated"
                    latestSys in 130..139 || latestDia in 80..89 -> "Stage 1"
                    latestSys >= 180 || latestDia >= 120 -> "Crisis"
                    else -> "Stage 2"
                }

                VitalSummaryStats(
                    latestValue = "$latestSys/$latestDia",
                    latestTimestamp = latestPoint.timestamp,
                    meanValue = "$meanSys/$meanDia",
                    minValue = "$minSys/$minDia",
                    maxValue = "$maxSys/$maxDia",
                    statusBadge = badge
                )
            }
            VitalMetricTab.BLOOD_GLUCOSE -> {
                val latestFormatted = formatDecimal(latestPoint.value, scale = 1)
                val meanFormatted = formatDecimal(rawPoints.map { it.value }.average(), scale = 1)
                val minFormatted = formatDecimal(rawPoints.minOf { it.value }, scale = 1)
                val maxFormatted = formatDecimal(rawPoints.maxOf { it.value }, scale = 1)

                val latestMmol = if (unitSystem == UnitSystem.IMPERIAL) {
                    UnitConverter.mgdlToMmol(latestPoint.value)
                } else latestPoint.value

                val badge = when {
                    latestMmol < 3.9 -> "Low"
                    latestMmol in 3.9..5.6 -> "Normal"
                    latestMmol in 5.6..6.9 -> "Elevated"
                    else -> "High"
                }

                VitalSummaryStats(
                    latestValue = latestFormatted,
                    latestTimestamp = latestPoint.timestamp,
                    meanValue = meanFormatted,
                    minValue = minFormatted,
                    maxValue = maxFormatted,
                    statusBadge = badge
                )
            }
            VitalMetricTab.HEART_RATE -> {
                val latest = latestPoint.value.roundToInt()
                val mean = rawPoints.map { it.value }.average().roundToInt()
                val min = rawPoints.minOf { it.value }.roundToInt()
                val max = rawPoints.maxOf { it.value }.roundToInt()

                val badge = when {
                    latest < 60 -> "Low"
                    latest in 60..100 -> "Normal"
                    else -> "High"
                }

                VitalSummaryStats(
                    latestValue = latest.toString(),
                    latestTimestamp = latestPoint.timestamp,
                    meanValue = mean.toString(),
                    minValue = min.toString(),
                    maxValue = max.toString(),
                    statusBadge = badge
                )
            }
            VitalMetricTab.BODY_TEMPERATURE -> {
                val latestFormatted = formatDecimal(latestPoint.value, scale = 1)
                val meanFormatted = formatDecimal(rawPoints.map { it.value }.average(), scale = 1)
                val minFormatted = formatDecimal(rawPoints.minOf { it.value }, scale = 1)
                val maxFormatted = formatDecimal(rawPoints.maxOf { it.value }, scale = 1)

                val latestC = if (unitSystem == UnitSystem.IMPERIAL) {
                    UnitConverter.fahrenheitToCelsius(latestPoint.value)
                } else latestPoint.value

                val badge = when {
                    latestC < 36.1 -> "Low"
                    latestC in 36.1..37.2 -> "Normal"
                    latestC in 37.2..38.0 -> "Elevated"
                    else -> "High"
                }

                VitalSummaryStats(
                    latestValue = latestFormatted,
                    latestTimestamp = latestPoint.timestamp,
                    meanValue = meanFormatted,
                    minValue = minFormatted,
                    maxValue = maxFormatted,
                    statusBadge = badge
                )
            }
            VitalMetricTab.SPO2 -> {
                val latest = latestPoint.value.roundToInt()
                val mean = rawPoints.map { it.value }.average().roundToInt()
                val min = rawPoints.minOf { it.value }.roundToInt()
                val max = rawPoints.maxOf { it.value }.roundToInt()

                val badge = when {
                    latest >= 95 -> "Normal"
                    latest in 90..94 -> "Low"
                    else -> "Critical"
                }

                VitalSummaryStats(
                    latestValue = latest.toString(),
                    latestTimestamp = latestPoint.timestamp,
                    meanValue = mean.toString(),
                    minValue = min.toString(),
                    maxValue = max.toString(),
                    statusBadge = badge
                )
            }
        }
    }

    private fun formatDecimal(value: Double, scale: Int): String =
        BigDecimal(value)
            .setScale(scale, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
}

class VitalsViewModelFactory(
    private val journalRepository: JournalRepository,
    private val activeProfileProvider: ActiveProfileProvider,
    private val unitSystemFlow: StateFlow<UnitSystem>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VitalsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VitalsViewModel(
                journalRepository = journalRepository,
                activeProfileProvider = activeProfileProvider,
                unitSystemFlow = unitSystemFlow,
                dispatcher = dispatcher
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class ${modelClass.name}")
    }
}
