package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.ActiveProfileProvider
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VitalsViewModelTest {

    private lateinit var viewModel: VitalsViewModel
    private val journalRepository: JournalRepository = mockk()
    private val activeProfileProvider: ActiveProfileProvider = mockk()
    private val vitalsFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
    private val profileIdFlow = MutableStateFlow(SELF_PROFILE_ID)
    private val unitSystemFlow = MutableStateFlow(UnitSystem.METRIC)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { journalRepository.vitalsEntries } returns vitalsFlow
        every { journalRepository.currentProfileId() } returns SELF_PROFILE_ID
        coEvery { journalRepository.insert(any<String>(), any()) } returns Unit
        coEvery { journalRepository.delete(any<String>(), any<String>()) } returns Unit
        every { activeProfileProvider.activeProfileId } returns profileIdFlow
        every { activeProfileProvider.currentProfileId() } returns SELF_PROFILE_ID

        viewModel = VitalsViewModel(
            journalRepository = journalRepository,
            activeProfileProvider = activeProfileProvider,
            unitSystemFlow = unitSystemFlow,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_defaultsToBloodPressure_30Days_andEmptyWhenNoData() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(VitalMetricTab.BLOOD_PRESSURE, state.selectedMetric)
        assertEquals(VitalsTimeWindow.DAYS_30, state.selectedWindow)
        assertTrue(state.isEmpty)
        assertTrue(state.points.isEmpty())
    }

    @Test
    fun populatedEntries_populatesPoints_andComputesStats() = runTest {
        val now = System.currentTimeMillis()
        val entry1 = JournalEntry(
            entry_id = "e1",
            description = "",
            timestamp = now - 2 * 86400000L,
            bp_systolic = 118.0,
            bp_diastolic = 76.0
        )
        val entry2 = JournalEntry(
            entry_id = "e2",
            description = "",
            timestamp = now - 86400000L,
            bp_systolic = 124.0,
            bp_diastolic = 82.0
        )
        vitalsFlow.value = listOf(entry1, entry2)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(2, state.points.size)
        assertEquals("124/82", state.stats.latestValue)
        assertEquals("121/79", state.stats.meanValue)
        assertEquals("118/76", state.stats.minValue)
        assertEquals("124/82", state.stats.maxValue)
    }

    @Test
    fun tabSwitching_filtersPointsForSelectedMetric() = runTest {
        val now = System.currentTimeMillis()
        val bpEntry = JournalEntry(
            entry_id = "e1",
            description = "",
            timestamp = now - 86400000L,
            bp_systolic = 120.0,
            bp_diastolic = 80.0
        )
        val glucoseEntry = JournalEntry(
            entry_id = "e2",
            description = "",
            timestamp = now - 86400000L,
            blood_glucose_mmol = 5.5
        )
        vitalsFlow.value = listOf(bpEntry, glucoseEntry)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectMetricTab(VitalMetricTab.BLOOD_GLUCOSE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(VitalMetricTab.BLOOD_GLUCOSE, state.selectedMetric)
        assertEquals(1, state.points.size)
        assertEquals(5.5, state.points.first().value, 0.01)
        assertEquals("5.5", state.stats.latestValue)
    }

    @Test
    fun timeWindowFiltering_excludesOlderReadings() = runTest {
        val now = System.currentTimeMillis()
        val freshEntry = JournalEntry(
            entry_id = "e1",
            description = "",
            timestamp = now - 2 * 86400000L, // 2 days old
            heart_rate_avg = 72
        )
        val staleEntry = JournalEntry(
            entry_id = "e2",
            description = "",
            timestamp = now - 15 * 86400000L, // 15 days old
            heart_rate_avg = 80
        )
        vitalsFlow.value = listOf(staleEntry, freshEntry)
        viewModel.selectMetricTab(VitalMetricTab.HEART_RATE)
        testDispatcher.scheduler.advanceUntilIdle()

        // In 30D, both are included
        assertEquals(2, viewModel.uiState.value.points.size)

        // Switch to 7D: staleEntry (15 days old) must be excluded
        viewModel.selectTimeWindow(VitalsTimeWindow.DAYS_7)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.points.size)
        assertEquals(72.0, state.points.first().value, 0.01)
    }

    @Test
    fun sameDayReadings_inLongRangeWindow_areAggregatedIntoDailyMean() = runTest {
        // Base day timestamp (1 day ago)
        val dayBase = System.currentTimeMillis() - 86400000L
        val reading1 = JournalEntry(
            entry_id = "e1",
            description = "",
            timestamp = dayBase,
            body_temperature_c = 36.4
        )
        val reading2 = JournalEntry(
            entry_id = "e2",
            description = "",
            timestamp = dayBase + 3600000L, // 1 hour later
            body_temperature_c = 37.0
        )
        vitalsFlow.value = listOf(reading1, reading2)
        viewModel.selectMetricTab(VitalMetricTab.BODY_TEMPERATURE)
        viewModel.selectTimeWindow(VitalsTimeWindow.DAYS_90)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // Aggregated into 1 daily point with mean 36.7 and count 2
        assertEquals(1, state.points.size)
        val point = state.points.first()
        assertEquals(36.7, point.value, 0.05)
        assertEquals(2, point.count)
        assertEquals(36.4, point.min!!, 0.01)
        assertEquals(37.0, point.max!!, 0.01)
    }

    @Test
    fun unitSystemImperial_convertsGlucoseAndTemperatureDisplay() = runTest {
        val now = System.currentTimeMillis()
        val entry = JournalEntry(
            entry_id = "e1",
            description = "",
            timestamp = now,
            blood_glucose_mmol = 5.5,
            body_temperature_c = 37.0
        )
        vitalsFlow.value = listOf(entry)
        unitSystemFlow.value = UnitSystem.IMPERIAL
        viewModel.selectMetricTab(VitalMetricTab.BLOOD_GLUCOSE)
        testDispatcher.scheduler.advanceUntilIdle()

        // 5.5 mmol/L * 18.0182 ~ 99.1 mg/dL
        val glucoseState = viewModel.uiState.value
        assertEquals(UnitSystem.IMPERIAL, glucoseState.unitSystem)
        assertEquals(99.1, glucoseState.points.first().value, 0.5)

        // Temperature: 37.0 C ~ 98.6 F
        viewModel.selectMetricTab(VitalMetricTab.BODY_TEMPERATURE)
        testDispatcher.scheduler.advanceUntilIdle()
        val tempState = viewModel.uiState.value
        assertEquals(98.6, tempState.points.first().value, 0.2)
    }

    @Test
    fun quickLogVital_persistsToRepository_underActiveProfile() = runTest {
        val vitals = EntryVitals(
            bpSystolic = 120.0,
            bpDiastolic = 80.0,
            heartRate = 72
        )

        viewModel.logVital(vitals, timestamp = 1762700000000L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            journalRepository.insert(
                SELF_PROFILE_ID,
                match {
                    it.bp_systolic == 120.0 &&
                        it.bp_diastolic == 80.0 &&
                        it.heart_rate_avg == 72 &&
                        it.timestamp == 1762700000000L
                }
            )
        }
    }

    @Test
    fun deleteVital_removesFromUi_activatesUndo_andRestoresOnUndo() = runTest {
        val entry = JournalEntry(
            entry_id = "del-1",
            description = "",
            timestamp = System.currentTimeMillis(),
            spo2_percent = 98
        )
        vitalsFlow.value = listOf(entry)
        viewModel.selectMetricTab(VitalMetricTab.SPO2)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEmpty)

        // Delete item
        viewModel.deleteVital(entry)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isUndoActive)
        assertEquals(entry, viewModel.uiState.value.lastDeletedEntry)
        assertTrue(viewModel.uiState.value.isEmpty)

        // Undo delete
        viewModel.undoDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUndoActive)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun dismissUndo_clearsUndoActiveState() = runTest {
        val entry = JournalEntry(
            entry_id = "del-2",
            description = "",
            timestamp = System.currentTimeMillis(),
            spo2_percent = 97
        )
        vitalsFlow.value = listOf(entry)
        viewModel.selectMetricTab(VitalMetricTab.SPO2)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteVital(entry)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isUndoActive)

        viewModel.dismissUndo()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isUndoActive)
        assertEquals(null, viewModel.uiState.value.lastDeletedEntry)
    }
}
