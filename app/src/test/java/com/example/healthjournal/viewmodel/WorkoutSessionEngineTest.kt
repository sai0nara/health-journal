package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.domain.WorkoutIntervalPhase
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.health.FakeWorkoutHealthDataSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the Phase 3 Active-session engine extensions: the 3-2-1
 * countdown, manual HIIT interval advance (phase/round tracking with haptic
 * cue), the strength set matrix (exercise/set adds with rest timer), summary
 * composition (tonnage + interval counts), and crash recovery of sets and
 * intervals.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionEngineTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var repository: WorkoutRepository
    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val haptics = mutableListOf<WorkoutHaptic>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeWorkoutSessionDao()
        repository = WorkoutRepository(dao)
        haptics.clear()
        coEvery { journalRepository.insert(any()) } returns Unit
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): WorkoutViewModel = WorkoutViewModel(
        repository = repository,
        healthSource = FakeWorkoutHealthDataSource(),
        journalRepository = journalRepository,
        dispatcher = dispatcher,
        onHaptic = { haptics.add(it) },
        clock = { 1_000L }
    )

    /** Selects a type with valid target and completes the countdown. */
    private suspend fun startActive(type: WorkoutType, target: String): WorkoutViewModel {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(type)
        vm.updateTarget(target)
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        tickCountdown(vm)
        return vm
    }

    private fun tickCountdown(vm: WorkoutViewModel) {
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun startSession_entersCountdown_beforeActive() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.FITNESS)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Countdown)
        assertEquals(WorkoutViewModel.COUNTDOWN_SECONDS, (state as WorkoutUiState.Countdown).secondsRemaining)
        assertEquals(listOf(WorkoutHaptic.START), haptics)
    }

    @Test
    fun countdown_countsDownPerTick_thenBecomesActive() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.YOGA)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, (vm.uiState.value as WorkoutUiState.Countdown).secondsRemaining)

        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, (vm.uiState.value as WorkoutUiState.Countdown).secondsRemaining)

        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()
        val active = vm.uiState.value
        assertTrue(active is WorkoutUiState.Active)
        assertEquals(0L, (active as WorkoutUiState.Active).session.elapsedSeconds)
    }

    @Test
    fun countdown_doesNotPersistSessionOnTicks() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.FITNESS)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        val sessionId = (vm.uiState.value as WorkoutUiState.Countdown).session.session_id

        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, dao.getSessionById(sessionId)!!.elapsedSeconds)
    }

    @Test
    fun advanceInterval_startsWorkIntervalThenTogglesPhase() = runTest {
        val vm = startActive(WorkoutType.HIIT, "20")
        assertTrue(vm.uiState.value is WorkoutUiState.Active)

        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        assertEquals(WorkoutIntervalPhase.REST, state.session.intervalState!!.phase)
        assertEquals(1, state.session.intervalState!!.rounds)
        assertEquals(1, state.session.intervalState!!.intervals)
        assertEquals(listOf(WorkoutHaptic.START, WorkoutHaptic.INTERVAL), haptics)
    }

    @Test
    fun advanceInterval_backToWork_keepsRoundCount() = runTest {
        val vm = startActive(WorkoutType.HIIT, "20")
        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        assertEquals(WorkoutIntervalPhase.WORK, state.session.intervalState!!.phase)
        assertEquals(1, state.session.intervalState!!.rounds)
        assertEquals(2, state.session.intervalState!!.intervals)
    }

    @Test
    fun advanceInterval_persistsStateForCrashRecovery() = runTest {
        val vm = startActive(WorkoutType.HIIT, "20")

        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        val persisted = dao.getSessionById((vm.uiState.value as WorkoutUiState.Active).session.session_id)!!
        assertNotNull(persisted.intervalState)
        assertEquals(WorkoutIntervalPhase.REST, persisted.intervalState!!.phase)
    }

    @Test
    fun advanceInterval_ignoredForNonHiitType() = runTest {
        val vm = startActive(WorkoutType.RUN, "5")

        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Active)
        assertNull((state as WorkoutUiState.Active).session.intervalState)
        assertEquals(listOf(WorkoutHaptic.START), haptics)
    }

    @Test
    fun addExercise_addsToMatrixAndPersists() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")

        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        assertEquals(listOf("Squat"), state.session.setMatrix?.map { it.name })
        assertNull(state.setMatrixError)
        val persisted = dao.getSessionById(state.session.session_id)!!
        assertEquals("Squat", persisted.setMatrix!!.single().name)
    }

    @Test
    fun addExercise_blankName_setsInlineError() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")

        vm.addExercise("   ")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        assertEquals("Exercise name is required", state.setMatrixError)
    }

    @Test
    fun addSet_appendsValidatedSetAndStartsRestTimer() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        val exerciseId = (vm.uiState.value as WorkoutUiState.Active).session.setMatrix!!.single().id

        vm.addSet(exerciseId, kg = 60.0, reps = 10)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        val set = state.session.setMatrix!!.single().sets.single()
        assertEquals(60.0, set.kg, 0.0)
        assertEquals(10, set.reps)
        assertEquals(600.0, set.kg * set.reps, 0.0)
        assertEquals(WorkoutViewModel.DEFAULT_REST_SECONDS, state.restSeconds)
        assertNull(state.setMatrixError)
        assertEquals(1, dao.getSessionById(state.session.session_id)!!.setMatrix!!.single().sets.size)
    }

    @Test
    fun addSet_invalidValues_setsInlineErrorWithoutAppending() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        val exerciseId = (vm.uiState.value as WorkoutUiState.Active).session.setMatrix!!.single().id

        vm.addSet(exerciseId, kg = 0.0, reps = 10)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Active
        assertEquals("Weight must be greater than zero", state.setMatrixError)
        assertTrue(state.session.setMatrix!!.single().sets.isEmpty())
    }

    @Test
    fun addSet_ignoredForNonFitnessType() = runTest {
        val vm = startActive(WorkoutType.RUN, "5")

        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Active)
        assertNull((state as WorkoutUiState.Active).session.setMatrix)
    }

    @Test
    fun advanceTime_decrementsRestTimer() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        val exerciseId = (vm.uiState.value as WorkoutUiState.Active).session.setMatrix!!.single().id
        vm.addSet(exerciseId, kg = 30.0, reps = 12)
        dispatcher.scheduler.advanceUntilIdle()

        vm.advanceTime(10)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            WorkoutViewModel.DEFAULT_REST_SECONDS - 10,
            (vm.uiState.value as WorkoutUiState.Active).restSeconds
        )
    }

    @Test
    fun finishSession_summaryCarriesTonnage() = runTest {
        val vm = startActive(WorkoutType.FITNESS, "30")
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        vm.addExercise("Bench")
        dispatcher.scheduler.advanceUntilIdle()
        val matrix = (vm.uiState.value as WorkoutUiState.Active).session.setMatrix!!
        vm.addSet(matrix[0].id, kg = 60.0, reps = 10)
        dispatcher.scheduler.advanceUntilIdle()
        vm.addSet(matrix[0].id, kg = 70.0, reps = 8)
        dispatcher.scheduler.advanceUntilIdle()
        vm.addSet(matrix[1].id, kg = 50.0, reps = 5)
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = vm.uiState.value as WorkoutUiState.Summary
        assertEquals(600.0 + 560.0 + 250.0, summary.tonnageKg!!, 0.0)
    }

    @Test
    fun finishSession_summaryCarriesIntervalCounts() = runTest {
        val vm = startActive(WorkoutType.HIIT, "20")
        repeat(3) {
            vm.advanceInterval()
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = vm.uiState.value as WorkoutUiState.Summary
        assertEquals(2, summary.intervalRounds)
        assertEquals(3, summary.intervalIntervals)
    }

    @Test
    fun resumeRecovery_restoresSetsAndIntervals() = runTest {
        val unfinished = WorkoutSession(
            status = com.example.healthjournal.data.local.WorkoutStatus.PAUSED.name,
            elapsedSeconds = 120L,
            setMatrix = listOf(
                StrengthExercise(
                    name = "Squat",
                    sets = listOf(StrengthSet(kg = 40.0, reps = 12))
                )
            ),
            intervalState = com.example.healthjournal.domain.WorkoutIntervalSession(
                phase = WorkoutIntervalPhase.REST,
                rounds = 2,
                intervals = 3
            )
        )
        repository.saveSession(unfinished)

        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)

        vm.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Active)
        val session = (state as WorkoutUiState.Active).session
        assertEquals("Squat", session.setMatrix!!.single().name)
        assertEquals(WorkoutIntervalPhase.REST, session.intervalState!!.phase)
        assertEquals(3, session.intervalState!!.intervals)
    }
}