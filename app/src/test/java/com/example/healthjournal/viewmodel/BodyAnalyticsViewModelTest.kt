package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.domain.MeasurementField
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BodyAnalyticsViewModel: tab selection state, per-parameter
 * series derivation (ascending), and goal-target projection from the
 * goals repository feed.
 */
class BodyAnalyticsViewModelTest {

    private val entriesFlow = MutableStateFlow(emptyList<BodyMeasurementEntry>())
    private val goalsFlow = MutableStateFlow(emptyList<GoalEntity>())
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope runs on Main: install the unconfined test dispatcher
        // so stateIn's upstream collects eagerly under runTest.
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Awaits the first real combine output past stateIn's emptyList seed. */
    private suspend fun BodyAnalyticsViewModel.awaitSeries(): List<Pair<Long, Double>> =
        withTimeout(1_000) { series.filter { it.isNotEmpty() }.first() }

    private fun createViewModel(): BodyAnalyticsViewModel {
        val measurementRepository: BodyMeasurementRepository = mockk {
            every { allEntries } returns entriesFlow
        }
        val goalsRepository: GoalsRepository = mockk {
            every { goals } returns goalsFlow
        }
        return BodyAnalyticsViewModel(measurementRepository, goalsRepository, testDispatcher)
    }

    @Test
    fun defaultTab_isWeight() {
        assertEquals(MeasurementField.WEIGHT, createViewModel().uiState.value.selectedTab)
    }

    @Test
    fun onTabSelected_updatesSelectedTab() {
        val viewModel = createViewModel()

        viewModel.onTabSelected(MeasurementField.WAIST)

        assertEquals(MeasurementField.WAIST, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun series_derivesAscendingPointsForActiveTab() = runTest {
        val viewModel = createViewModel()
        entriesFlow.value = listOf(
            BodyMeasurementEntry(entry_id = "a", timestamp = 2_000L, weight_kg = 78.0),
            BodyMeasurementEntry(entry_id = "b", timestamp = 1_000L, weight_kg = 80.0),
            // Girth-only row must be excluded from the weight series.
            BodyMeasurementEntry(entry_id = "c", timestamp = 3_000L, waist_cm = 85.0)
        )

        val series = viewModel.awaitSeries()

        assertEquals(listOf(1_000L to 80.0, 2_000L to 78.0), series)
    }

    @Test
    fun series_followsTabSwitch() = runTest {
        val viewModel = createViewModel()
        entriesFlow.value = listOf(
            BodyMeasurementEntry(entry_id = "c", timestamp = 3_000L, waist_cm = 85.5)
        )

        viewModel.onTabSelected(MeasurementField.WAIST)

        assertEquals(listOf(3_000L to 85.5), viewModel.awaitSeries())
    }

    @Test
    fun uiState_projectsGoalTargetsByParameterId() {
        val viewModel = createViewModel()
        goalsFlow.value = listOf(
            GoalEntity("WAIST", 82.0, 1_000L),
            GoalEntity("WEIGHT", 75.0, 2_000L)
        )

        val targets = viewModel.uiState.value.goalTargets

        assertEquals(82.0, targets["WAIST"]!!, 0.001)
        assertEquals(75.0, targets["WEIGHT"]!!, 0.001)
    }
}
