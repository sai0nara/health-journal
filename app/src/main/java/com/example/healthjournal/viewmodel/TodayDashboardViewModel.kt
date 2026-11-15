package com.example.healthjournal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthjournal.data.ActiveProfileProvider
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Slide state for one Vitals parameter in Today's Overview carousel. */
data class TodayVitalSlide(
    val tab: VitalMetricTab,
    val latestValueDisplay: String? = null,
    val secondaryValueDisplay: String? = null,
    val timestampMillis: Long? = null,
    val sparklinePoints: List<Float> = emptyList(),
    val isEmptyState: Boolean = false
)

/** Summary state for the half-width Workouts card in Today's Overview. */
data class TodayWorkoutsCardState(
    val latestWorkoutName: String? = null,
    val latestTimestampMillis: Long? = null,
    val completedThisWeek: Int = 0,
    val weeklyTarget: Int = GoalsRepository.DEFAULT_WORKOUT_WEEKLY_TARGET,
    val progressFraction: Float = 0f,
    val isEmptyState: Boolean = true
)

/** Summary state for the half-width Measurements card in Today's Overview. */
data class TodayMeasurementsCardState(
    val latestWeightDisplay: String? = null,
    val latestTimestampMillis: Long? = null,
    val weeklyDeltaDisplay: String? = null,
    val sparklinePoints: List<Float> = emptyList(),
    val isEmptyState: Boolean = true
)

enum class TodayActivityCategory {
    VITALS,
    WORKOUT,
    MEASUREMENT,
    JOURNAL
}

/** One row in the Recent Activity timeline (capped at 5 newest items). */
data class TodayActivityItem(
    val id: String,
    val category: TodayActivityCategory,
    val summary: String,
    val timestampMillis: Long,
    val isToday: Boolean,
    val isYesterday: Boolean,
    val formattedTime: String?,
    val vitalTab: VitalMetricTab? = null
)

data class TodayDashboardUiState(
    val activeProfileId: String = SELF_PROFILE_ID,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val vitalsSlides: List<TodayVitalSlide> = listOf(TodayVitalSlide(tab = VitalMetricTab.BLOOD_PRESSURE, isEmptyState = true)),
    val workoutsCard: TodayWorkoutsCardState = TodayWorkoutsCardState(),
    val measurementsCard: TodayMeasurementsCardState = TodayMeasurementsCardState(),
    val recentActivity: List<TodayActivityItem> = emptyList(),
    val quickLogPresets: List<WorkoutPreset> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * Aggregates active-profile metrics from Room repositories into glanceable
 * overview cards, sparklines, and a capped Recent Activity timeline.
 */
class TodayDashboardViewModel(
    journalRepository: JournalRepository,
    workoutRepository: WorkoutRepository,
    measurementRepository: BodyMeasurementRepository,
    goalsRepository: GoalsRepository,
    presetRepository: PresetRepository,
    activeProfileProvider: ActiveProfileProvider,
    private val unitSystemFlow: StateFlow<UnitSystem>,
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val nowMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val localeProvider: () -> Locale = { Locale.getDefault() }
) : ViewModel() {

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<TodayDashboardUiState> = combine(
        activeProfileProvider.activeProfileId,
        unitSystemFlow,
        journalRepository.vitalsEntries,
        journalRepository.allEntries,
        workoutRepository.completedSessions,
        measurementRepository.allEntries,
        goalsRepository.workoutWeeklyTarget,
        presetRepository.presets
    ) { args ->
        val profileId = args[0] as String
        val unitSystem = args[1] as UnitSystem
        val vitals = args[2] as List<JournalEntry>
        val allEntries = args[3] as List<JournalEntry>
        val sessions = args[4] as List<WorkoutSession>
        val measurements = args[5] as List<BodyMeasurementEntry>
        val weeklyTarget = args[6] as Int
        val presets = args[7] as List<WorkoutPreset>

        buildUiState(
            profileId = profileId,
            unitSystem = unitSystem,
            vitals = vitals,
            allEntries = allEntries,
            sessions = sessions,
            measurements = measurements,
            weeklyTarget = weeklyTarget,
            presets = presets
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayDashboardUiState(isLoading = true)
    )

    private fun buildUiState(
        profileId: String,
        unitSystem: UnitSystem,
        vitals: List<JournalEntry>,
        allEntries: List<JournalEntry>,
        sessions: List<WorkoutSession>,
        measurements: List<BodyMeasurementEntry>,
        weeklyTarget: Int,
        presets: List<WorkoutPreset>
    ): TodayDashboardUiState {
        val zone = zoneIdProvider()
        val nowMillis = nowMillisProvider()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val weekStartMillis = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

        return TodayDashboardUiState(
            activeProfileId = profileId,
            unitSystem = unitSystem,
            vitalsSlides = buildVitalsSlides(vitals, unitSystem),
            workoutsCard = buildWorkoutsCard(sessions, weeklyTarget, weekStartMillis),
            measurementsCard = buildMeasurementsCard(measurements, unitSystem, weekStartMillis),
            recentActivity = buildRecentActivity(allEntries, sessions, measurements, unitSystem, today, zone),
            quickLogPresets = presets.sortedByDescending { it.lastModified }.take(MAX_QUICK_LOG_PRESETS),
            isLoading = false
        )
    }

    private fun buildVitalsSlides(
        vitals: List<JournalEntry>,
        unitSystem: UnitSystem
    ): List<TodayVitalSlide> {
        val sortedDesc = vitals.sortedByDescending { it.timestamp }
        val slides = VitalMetricTab.entries.mapNotNull { tab ->
            buildSlideForTab(tab, sortedDesc, unitSystem)
        }
        return slides.ifEmpty {
            listOf(TodayVitalSlide(tab = VitalMetricTab.BLOOD_PRESSURE, isEmptyState = true))
        }
    }

    private fun buildSlideForTab(
        tab: VitalMetricTab,
        sortedDesc: List<JournalEntry>,
        unitSystem: UnitSystem
    ): TodayVitalSlide? {
        val matching = sortedDesc.filter { entryHasMetric(it, tab) }
        if (matching.isEmpty()) return null
        val latest = matching.first()
        val sparkline = matching.take(MAX_SPARKLINE_POINTS).reversed().mapNotNull {
            metricPlotValue(it, tab, unitSystem)?.toFloat()
        }
        val (primary, secondary) = formatVitalValues(latest, tab, unitSystem)
        return TodayVitalSlide(
            tab = tab,
            latestValueDisplay = primary,
            secondaryValueDisplay = secondary,
            timestampMillis = latest.timestamp,
            sparklinePoints = sparkline,
            isEmptyState = false
        )
    }

    private fun entryHasMetric(entry: JournalEntry, tab: VitalMetricTab): Boolean = when (tab) {
        VitalMetricTab.BLOOD_PRESSURE -> entry.bp_systolic != null && entry.bp_diastolic != null
        VitalMetricTab.BLOOD_GLUCOSE -> entry.blood_glucose_mmol != null
        VitalMetricTab.HEART_RATE -> entry.heart_rate_avg != null
        VitalMetricTab.BODY_TEMPERATURE -> entry.body_temperature_c != null
        VitalMetricTab.SPO2 -> entry.spo2_percent != null
    }

    private fun metricPlotValue(entry: JournalEntry, tab: VitalMetricTab, unitSystem: UnitSystem): Double? =
        when (tab) {
            VitalMetricTab.BLOOD_PRESSURE -> entry.bp_systolic
            VitalMetricTab.BLOOD_GLUCOSE -> entry.blood_glucose_mmol?.let {
                if (unitSystem == UnitSystem.IMPERIAL) it * 18.0182 else it
            }
            VitalMetricTab.HEART_RATE -> entry.heart_rate_avg?.toDouble()
            VitalMetricTab.BODY_TEMPERATURE -> entry.body_temperature_c?.let {
                if (unitSystem == UnitSystem.IMPERIAL) (it * 9.0 / 5.0) + 32.0 else it
            }
            VitalMetricTab.SPO2 -> entry.spo2_percent?.toDouble()
        }

    private fun formatVitalValues(
        entry: JournalEntry,
        tab: VitalMetricTab,
        unitSystem: UnitSystem
    ): Pair<String, String?> = when (tab) {
        VitalMetricTab.BLOOD_PRESSURE -> {
            val sys = entry.bp_systolic?.roundToInt() ?: 0
            val dia = entry.bp_diastolic?.roundToInt() ?: 0
            val hr = entry.heart_rate_avg?.let { "$it bpm" }
            "$sys/$dia mmHg" to hr
        }
        VitalMetricTab.BLOOD_GLUCOSE -> {
            val raw = entry.blood_glucose_mmol ?: 0.0
            val display = if (unitSystem == UnitSystem.IMPERIAL) {
                "${(raw * 18.0182).roundToInt()} mg/dL"
            } else {
                "${UnitConverter.formatDouble(raw)} mmol/L"
            }
            display to null
        }
        VitalMetricTab.HEART_RATE -> "${entry.heart_rate_avg ?: 0} bpm" to null
        VitalMetricTab.BODY_TEMPERATURE -> {
            val raw = entry.body_temperature_c ?: 0.0
            val display = if (unitSystem == UnitSystem.IMPERIAL) {
                "${UnitConverter.formatDouble((raw * 9.0 / 5.0) + 32.0)} °F"
            } else {
                "${UnitConverter.formatDouble(raw)} °C"
            }
            display to null
        }
        VitalMetricTab.SPO2 -> "${entry.spo2_percent ?: 0}%" to null
    }

    private fun buildWorkoutsCard(
        sessions: List<WorkoutSession>,
        weeklyTarget: Int,
        weekStartMillis: Long
    ): TodayWorkoutsCardState {
        val safeTarget = weeklyTarget.coerceIn(
            GoalsRepository.MIN_WORKOUT_WEEKLY_TARGET,
            GoalsRepository.MAX_WORKOUT_WEEKLY_TARGET
        )
        val sortedDesc = sessions.sortedByDescending { it.startTimestamp }
        val completedThisWeek = sortedDesc.count { it.startTimestamp >= weekStartMillis }
        val fraction = (completedThisWeek.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
        val latest = sortedDesc.firstOrNull()
        return TodayWorkoutsCardState(
            latestWorkoutName = latest?.let { it.routineName?.takeIf { n -> n.isNotBlank() } ?: it.type },
            latestTimestampMillis = latest?.startTimestamp,
            completedThisWeek = completedThisWeek,
            weeklyTarget = safeTarget,
            progressFraction = fraction,
            isEmptyState = latest == null
        )
    }

    private fun buildMeasurementsCard(
        measurements: List<BodyMeasurementEntry>,
        unitSystem: UnitSystem,
        weekStartMillis: Long
    ): TodayMeasurementsCardState {
        val weightEntriesDesc = measurements
            .filter { it.weight_kg != null }
            .sortedByDescending { it.timestamp }
        val latest = weightEntriesDesc.firstOrNull()
            ?: return TodayMeasurementsCardState(isEmptyState = true)

        val unitSuffix = if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
        fun convertWeight(kg: Double): Double =
            if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.kgToLbs(kg) else kg

        val latestDisplay = "${UnitConverter.formatDouble(convertWeight(latest.weight_kg!!))} $unitSuffix"
        val inWeek = weightEntriesDesc.filter { it.timestamp >= weekStartMillis }
        val deltaDisplay = if (inWeek.size >= 2) {
            val delta = convertWeight(inWeek.first().weight_kg!!) - convertWeight(inWeek.last().weight_kg!!)
            val sign = if (delta > 0) "+" else ""
            "$sign${UnitConverter.formatDouble(delta)} $unitSuffix"
        } else {
            null
        }
        val sparkline = weightEntriesDesc.take(MAX_SPARKLINE_POINTS).reversed()
            .map { convertWeight(it.weight_kg!!).toFloat() }

        return TodayMeasurementsCardState(
            latestWeightDisplay = latestDisplay,
            latestTimestampMillis = latest.timestamp,
            weeklyDeltaDisplay = deltaDisplay,
            sparklinePoints = sparkline,
            isEmptyState = false
        )
    }

    private fun buildRecentActivity(
        allEntries: List<JournalEntry>,
        sessions: List<WorkoutSession>,
        measurements: List<BodyMeasurementEntry>,
        unitSystem: UnitSystem,
        today: LocalDate,
        zone: ZoneId
    ): List<TodayActivityItem> {
        val yesterday = today.minusDays(1)
        val timeFormatter = if (localeProvider().language == "ru") {
            DateTimeFormatter.ofPattern("HH:mm", localeProvider())
        } else {
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        }

        val items = mutableListOf<TodayActivityItem>()

        allEntries.forEach { entry ->
            val firstVitalTab = VitalMetricTab.entries.firstOrNull { entryHasMetric(entry, it) }
            val category = if (firstVitalTab != null) TodayActivityCategory.VITALS else TodayActivityCategory.JOURNAL
            val summary = if (firstVitalTab != null) {
                formatVitalValues(entry, firstVitalTab, unitSystem).first
            } else {
                entry.description.trim().ifEmpty { "Journal" }
            }
            val dateTime = Instant.ofEpochMilli(entry.timestamp).atZone(zone)
            val isToday = dateTime.toLocalDate() == today
            items += TodayActivityItem(
                id = "j_${entry.entry_id}",
                category = category,
                summary = summary,
                timestampMillis = entry.timestamp,
                isToday = isToday,
                isYesterday = dateTime.toLocalDate() == yesterday,
                formattedTime = if (isToday) timeFormatter.format(dateTime) else null,
                vitalTab = firstVitalTab
            )
        }

        sessions.forEach { session ->
            val dateTime = Instant.ofEpochMilli(session.startTimestamp).atZone(zone)
            val isToday = dateTime.toLocalDate() == today
            val mins = (session.elapsedSeconds / 60L).coerceAtLeast(1L)
            val name = session.routineName?.takeIf { it.isNotBlank() } ?: session.type
            items += TodayActivityItem(
                id = "w_${session.session_id}",
                category = TodayActivityCategory.WORKOUT,
                summary = "$name - $mins min",
                timestampMillis = session.startTimestamp,
                isToday = isToday,
                isYesterday = dateTime.toLocalDate() == yesterday,
                formattedTime = if (isToday) timeFormatter.format(dateTime) else null
            )
        }

        val unitSuffix = if (unitSystem == UnitSystem.IMPERIAL) "lb" else "kg"
        measurements.filter { it.weight_kg != null }.forEach { m ->
            val dateTime = Instant.ofEpochMilli(m.timestamp).atZone(zone)
            val isToday = dateTime.toLocalDate() == today
            val displayVal = if (unitSystem == UnitSystem.IMPERIAL) UnitConverter.kgToLbs(m.weight_kg!!) else m.weight_kg!!
            items += TodayActivityItem(
                id = "m_${m.entry_id}",
                category = TodayActivityCategory.MEASUREMENT,
                summary = "${UnitConverter.formatDouble(displayVal)} $unitSuffix",
                timestampMillis = m.timestamp,
                isToday = isToday,
                isYesterday = dateTime.toLocalDate() == yesterday,
                formattedTime = if (isToday) timeFormatter.format(dateTime) else null
            )
        }

        return items.sortedByDescending { it.timestampMillis }.take(MAX_RECENT_ACTIVITY)
    }

    class Factory(
        private val journalRepository: JournalRepository,
        private val workoutRepository: WorkoutRepository,
        private val measurementRepository: BodyMeasurementRepository,
        private val goalsRepository: GoalsRepository,
        private val presetRepository: PresetRepository,
        private val activeProfileProvider: ActiveProfileProvider,
        private val unitSystemFlow: StateFlow<UnitSystem>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TodayDashboardViewModel::class.java)) {
                return TodayDashboardViewModel(
                    journalRepository = journalRepository,
                    workoutRepository = workoutRepository,
                    measurementRepository = measurementRepository,
                    goalsRepository = goalsRepository,
                    presetRepository = presetRepository,
                    activeProfileProvider = activeProfileProvider,
                    unitSystemFlow = unitSystemFlow
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        const val MAX_SPARKLINE_POINTS = 7
        const val MAX_RECENT_ACTIVITY = 5
        const val MAX_QUICK_LOG_PRESETS = 3
    }
}
