package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.ActiveProfileProvider
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class TodayDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val zoneId: ZoneId = ZoneId.of("UTC")
    private val fixedToday: LocalDate = LocalDate.of(2026, 11, 15)
    private val fixedNowMillis: Long =
        fixedToday.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

    private val vitalsFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val allEntriesFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val sessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val measurementsFlow = MutableStateFlow<List<BodyMeasurementEntry>>(emptyList())
    private val weeklyTargetFlow = MutableStateFlow(GoalsRepository.DEFAULT_WORKOUT_WEEKLY_TARGET)
    private val presetsFlow = MutableStateFlow<List<WorkoutPreset>>(emptyList())
    private val profileIdFlow = MutableStateFlow(SELF_PROFILE_ID)
    private val unitSystemFlow = MutableStateFlow(UnitSystem.METRIC)

    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val workoutRepository: com.example.healthjournal.data.WorkoutRepository = mockk(relaxed = true)
    private val measurementRepository: BodyMeasurementRepository = mockk(relaxed = true)
    private val goalsRepository: GoalsRepository = mockk(relaxed = true)
    private val presetRepository: PresetRepository = mockk(relaxed = true)
    private val activeProfileProvider: ActiveProfileProvider = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { journalRepository.vitalsEntries } returns vitalsFlow
        every { journalRepository.allEntries } returns allEntriesFlow
        every { workoutRepository.completedSessions } returns sessionsFlow
        every { measurementRepository.allEntries } returns measurementsFlow
        every { goalsRepository.workoutWeeklyTarget } returns weeklyTargetFlow
        every { presetRepository.presets } returns presetsFlow
        every { activeProfileProvider.activeProfileId } returns profileIdFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(clockMillis: () -> Long = { fixedNowMillis }): TodayDashboardViewModel =
        TodayDashboardViewModel(
            journalRepository = journalRepository,
            workoutRepository = workoutRepository,
            measurementRepository = measurementRepository,
            goalsRepository = goalsRepository,
            presetRepository = presetRepository,
            activeProfileProvider = activeProfileProvider,
            unitSystemFlow = unitSystemFlow,
            zoneIdProvider = { zoneId },
            nowMillisProvider = clockMillis
        )

    private fun ts(daysAgo: Long, hour: Int = 8, minute: Int = 30): Long =
        fixedToday.minusDays(daysAgo).atTime(LocalTime.of(hour, minute)).atZone(zoneId).toInstant().toEpochMilli()

    @Test
    fun `vitals empty state renders single slide targeting Blood Pressure tab 0`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        advanceUntilIdle()

        val slides = vm.uiState.value.vitalsSlides
        assertEquals(1, slides.size)
        assertTrue(slides.first().isEmptyState)
        assertEquals(VitalMetricTab.BLOOD_PRESSURE, slides.first().tab)
    }

    @Test
    fun `vitals carousel hides unlogged categories and preserves canonical order`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vitalsFlow.value = listOf(
            JournalEntry(
                entry_id = "v1",
                timestamp = ts(0, 8, 30),
                description = "",
                bp_systolic = 118.0,
                bp_diastolic = 76.0,
                heart_rate_avg = 64,
                spo2_percent = 98
            )
        )
        advanceUntilIdle()

        val slides = vm.uiState.value.vitalsSlides
        assertEquals(listOf(VitalMetricTab.BLOOD_PRESSURE, VitalMetricTab.HEART_RATE, VitalMetricTab.SPO2), slides.map { it.tab })
        assertFalse(slides.any { it.isEmptyState })
        assertEquals(1, slides.first().sparklinePoints.size)
    }

    @Test
    fun `vitals carousel renders all 5 slides when all 5 parameters are logged`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vitalsFlow.value = listOf(
            JournalEntry(
                entry_id = "all",
                timestamp = ts(0),
                description = "",
                bp_systolic = 120.0,
                bp_diastolic = 80.0,
                heart_rate_avg = 65,
                blood_glucose_mmol = 5.4,
                body_temperature_c = 36.6,
                spo2_percent = 99
            )
        )
        advanceUntilIdle()

        assertEquals(5, vm.uiState.value.vitalsSlides.size)
    }

    @Test
    fun `vitals sparkline caps at last 7 readings`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        vitalsFlow.value = (0L until 10L).map { idx ->
            JournalEntry(
                entry_id = "hr_$idx",
                timestamp = ts(idx),
                description = "",
                heart_rate_avg = (60 + idx).toInt()
            )
        }
        advanceUntilIdle()

        val hrSlide = vm.uiState.value.vitalsSlides.first { it.tab == VitalMetricTab.HEART_RATE }
        assertEquals(7, hrSlide.sparklinePoints.size)
    }

    @Test
    fun `workouts card counts rolling 7-day sessions and caps progress fraction at 1 when X exceeds Y`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        weeklyTargetFlow.value = 5
        sessionsFlow.value = (0L until 7L).map { idx ->
            WorkoutSession(
                session_id = "w_$idx",
                type = "STRENGTH",
                routineName = "Strength Training",
                status = WorkoutStatus.COMPLETED.name,
                startTimestamp = ts(idx),
                endTimestamp = ts(idx) + 2_700_000L,
                elapsedSeconds = 2700L
            )
        } + WorkoutSession(
            session_id = "w_old",
            type = "RUN",
            status = WorkoutStatus.COMPLETED.name,
            startTimestamp = ts(7),
            endTimestamp = ts(7) + 1_800_000L,
            elapsedSeconds = 1800L
        )
        advanceUntilIdle()

        val card = vm.uiState.value.workoutsCard
        assertFalse(card.isEmptyState)
        assertEquals(7, card.completedThisWeek)
        assertEquals(5, card.weeklyTarget)
        assertEquals(1f, card.progressFraction, 0.001f)
    }

    @Test
    fun `measurements card computes weekly delta only when at least 2 entries in 7-day window and converts to imperial`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        measurementsFlow.value = listOf(
            BodyMeasurementEntry(entry_id = "m1", timestamp = ts(0), weight_kg = 82.4)
        )
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.measurementsCard.weeklyDeltaDisplay)

        measurementsFlow.value = listOf(
            BodyMeasurementEntry(entry_id = "m1", timestamp = ts(0), weight_kg = 82.4),
            BodyMeasurementEntry(entry_id = "m2", timestamp = ts(4), weight_kg = 83.0)
        )
        advanceUntilIdle()
        assertTrue(vm.uiState.value.measurementsCard.latestWeightDisplay!!.contains("82.4 kg"))
        assertTrue(vm.uiState.value.measurementsCard.weeklyDeltaDisplay!!.contains("-0.6 kg"))

        unitSystemFlow.value = UnitSystem.IMPERIAL
        advanceUntilIdle()
        assertTrue(vm.uiState.value.measurementsCard.latestWeightDisplay!!.contains("lb"))
        assertTrue(vm.uiState.value.measurementsCard.weeklyDeltaDisplay!!.contains("lb"))
    }

    @Test
    fun `recent activity caps at 5 newest items across categories and sets time pill only for today`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        allEntriesFlow.value = listOf(
            JournalEntry(entry_id = "j_today", timestamp = ts(0, 8, 30), description = "Morning note", bp_systolic = 118.0, bp_diastolic = 76.0),
            JournalEntry(entry_id = "j_yest", timestamp = ts(1, 9, 0), description = "Yesterday note"),
            JournalEntry(entry_id = "j_old1", timestamp = ts(2, 10, 0), description = "Old 1"),
            JournalEntry(entry_id = "j_old2", timestamp = ts(3, 10, 0), description = "Old 2")
        )
        sessionsFlow.value = listOf(
            WorkoutSession(session_id = "w1", type = "RUN", status = WorkoutStatus.COMPLETED.name, startTimestamp = ts(1, 18, 0), elapsedSeconds = 2700L)
        )
        measurementsFlow.value = listOf(
            BodyMeasurementEntry(entry_id = "m1", timestamp = ts(2, 7, 0), weight_kg = 82.4)
        )
        advanceUntilIdle()

        val activity = vm.uiState.value.recentActivity
        assertEquals(5, activity.size)
        assertTrue(activity.first().isToday)
        assertFalse(activity[1].isToday)
        assertNull(activity[1].formattedTime)
    }

    @Test
    fun `presets in quick log sheet cap at 3 newest`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        presetsFlow.value = (1..5).map { idx ->
            WorkoutPreset(id = "p$idx", name = "Preset $idx", lastModified = idx * 1_000L)
        }
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.quickLogPresets.size)
    }

    @Test
    fun `profile switch updates activeProfileId and isolates data`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        allEntriesFlow.value = listOf(JournalEntry(entry_id = "j1", timestamp = ts(0), description = "Me entry"))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.recentActivity.size)

        profileIdFlow.value = "sibling_2"
        allEntriesFlow.value = emptyList()
        vitalsFlow.value = emptyList()
        sessionsFlow.value = emptyList()
        measurementsFlow.value = emptyList()
        advanceUntilIdle()

        assertEquals("sibling_2", vm.uiState.value.activeProfileId)
        assertTrue(vm.uiState.value.recentActivity.isEmpty())
        assertTrue(vm.uiState.value.vitalsSlides.first().isEmptyState)
    }

    @Test
    fun `recent activity strips html markup and entities from journal summaries`() = runTest {
        val vm = createViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }

        allEntriesFlow.value = listOf(
            JournalEntry(entry_id = "html1", timestamp = ts(0, 9, 0), description = "<p>Text</><br>"),
            JournalEntry(entry_id = "html2", timestamp = ts(0, 8, 0), description = "<p><b>Morning</b> walk</p><p>Felt &amp; slept well&nbsp;!</p>"),
            JournalEntry(entry_id = "html3", timestamp = ts(0, 7, 30), description = "<p>Run &num;Fitness&excl;</p>"),
            JournalEntry(entry_id = "html_empty", timestamp = ts(0, 7, 0), description = "<p><br></p>&nbsp;")
        )
        advanceUntilIdle()

        val activity = vm.uiState.value.recentActivity
        assertEquals(4, activity.size)
        assertEquals("Text", activity[0].summary)
        assertEquals("Morning walk Felt & slept well !", activity[1].summary)
        assertEquals("Run #Fitness!", activity[2].summary)
        assertEquals("Journal", activity[3].summary)
    }
}
