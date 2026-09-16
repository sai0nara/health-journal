package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.FakeExerciseCatalogDao
import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.health.FakeWorkoutHealthDataSource
import com.example.healthjournal.health.HealthExerciseType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

/**
 * Unit tests for preset-driven routine execution in WorkoutViewModel: starting
 * a routine from a preset, per-set completion (weight/reps/RPE/checkbox) with
 * automatic rest timer and haptics, mid-routine exercise swaps, and crash
 * recovery that resumes at the last completed set.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutineExecutionTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var catalogDao: FakeExerciseCatalogDao
    private lateinit var repository: WorkoutRepository
    private lateinit var healthSource: FakeWorkoutHealthDataSource
    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val haptics = mutableListOf<WorkoutHaptic>()
    private var nowMillis = 1_700_000_000_000L

    private val squatItem = ExerciseCatalogItem(
        id = "barbell-squat",
        name = "Barbell Squat",
        muscleCategory = "Quads"
    )
    private val pressItem = ExerciseCatalogItem(
        id = "barbell-bench-press",
        name = "Barbell Bench Press",
        muscleCategory = "Chest"
    )
    private val legDayPreset = WorkoutPreset(
        id = "p1",
        name = "Leg Day",
        scheduledDay = ScheduledDay.ANY.name,
        exercises = listOf(
            PresetExercise(
                exerciseId = "barbell-squat",
                targetSets = 3,
                defaultReps = 5,
                defaultWeightKg = 60.0,
                restSeconds = 90
            )
        ),
        lastModified = 1_700_000_000_000L
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeWorkoutSessionDao()
        presetDao = FakeWorkoutPresetDao()
        catalogDao = FakeExerciseCatalogDao()
        repository = WorkoutRepository(dao)
        healthSource = FakeWorkoutHealthDataSource()
        haptics.clear()
        nowMillis = 1_700_000_000_000L
        coEvery { journalRepository.insert(any()) } returns Unit
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(
        health: FakeWorkoutHealthDataSource = healthSource,
        clock: () -> Long = { nowMillis }
    ): WorkoutViewModel = WorkoutViewModel(
        repository = repository,
        healthSource = health,
        journalRepository = journalRepository,
        dispatcher = dispatcher,
        onHaptic = { haptics.add(it) },
        clock = clock,
        presetRepository = PresetRepository(presetDao),
        catalogDao = catalogDao
    )

    /** Advances a configured session past the 3-2-1 countdown into Active. */
    private fun startActiveSessionCooldown(): WorkoutViewModel {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.FITNESS)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        return vm
    }

    private suspend fun startRoutineFor(presetId: String = legDayPreset.id): WorkoutViewModel {
        with(presetDao) {
            insertAll(listOf(legDayPreset))
        }
        with(catalogDao) {
            insertAll(listOf(squatItem, pressItem))
        }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset(presetId)
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        return vm
    }

    private fun activeState(vm: WorkoutViewModel): WorkoutUiState.Active =
        vm.uiState.value as WorkoutUiState.Active

    @Test
    fun startPreset_createsRoutineSessionWithPlannedMatrix() = runTest {
        val vm = startRoutineFor()

        val active = activeState(vm)
        assertTrue(active.isRoutine)
        assertTrue(active.keepScreenOn)
        assertEquals(WorkoutType.FITNESS.name, active.session.type)
        val exercise = active.session.setMatrix!!.single()
        assertEquals("Barbell Squat", exercise.name)
        assertEquals("barbell-squat", exercise.exerciseId)
        assertEquals(3, exercise.targetSets)
        assertEquals(5, exercise.targetReps)
        assertEquals(60.0, exercise.targetWeightKg!!, 0.0)
        assertEquals(90, exercise.restSeconds)
        assertEquals(3, exercise.sets.size)
        assertTrue(exercise.sets.all { !it.completed && it.kg == 60.0 && it.reps == 5 })
    }

    @Test
    fun startPreset_withUnfinishedSession_forcesRecovery() = runTest {
        val first = startActiveSessionCooldown()
        val sessionId = activeState(first).session.session_id

        with(presetDao) { insertAll(listOf(legDayPreset)) }
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset(legDayPreset.id)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)
        assertEquals(sessionId, dao.getSessionById(sessionId)!!.session_id)
    }

    @Test
    fun toggleSetCompleted_marksCompleteStartsRestTimerAndHaptics() = runTest {
        val vm = startRoutineFor()

        vm.toggleSetCompleted(exerciseIndex = 0, setIndex = 0)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        assertTrue(active.session.setMatrix!!.single().sets[0].completed)
        assertEquals(90, active.restSeconds)
        assertTrue(haptics.contains(WorkoutHaptic.SET_COMPLETE))
    }

    @Test
    fun toggleSetCompleted_finalSetOfExercise_emitsSuccessHaptic() = runTest {
        val vm = startRoutineFor()

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.toggleSetCompleted(0, 1)
        dispatcher.scheduler.advanceUntilIdle()
        vm.toggleSetCompleted(0, 2)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        assertTrue(active.session.setMatrix!!.single().sets.all { it.completed })
        assertTrue(haptics.contains(WorkoutHaptic.EXERCISE_COMPLETE))
    }

    @Test
    fun toggleSetCompleted_copiesPerformedWeightAndRepsToNextPristineSet() = runTest {
        val vm = startRoutineFor()
        vm.updateRoutineSet(0, 0, 100.0, 6)
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        val sets = activeState(vm).session.setMatrix!!.single().sets
        assertTrue(sets[0].completed)
        assertEquals(100.0, sets[1].kg, 0.0)
        assertEquals(6, sets[1].reps)
        assertFalse(sets[1].completed)
    }

    @Test
    fun toggleSetCompleted_doesNotOverwriteUserEditedNextSet() = runTest {
        val vm = startRoutineFor()
        vm.updateRoutineSet(0, 0, 100.0, 6)
        vm.updateRoutineSet(0, 1, 110.0, 5)
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        val sets = activeState(vm).session.setMatrix!!.single().sets
        assertEquals(110.0, sets[1].kg, 0.0)
        assertEquals(5, sets[1].reps)
    }

    @Test
    fun toggleSetCompleted_uncheckStopsRestTimer() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, activeState(vm).restSeconds)

        vm.advanceTime(15)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(75, activeState(vm).restSeconds)

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, activeState(vm).restSeconds)
    }

    @Test
    fun toggleSetCompleted_uncheckPersistsIncompleteSet() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        assertFalse(active.session.setMatrix!!.single().sets[0].completed)
        val stored = dao.getSessionById(active.session.session_id)!!
        assertFalse(stored.setMatrix!!.single().sets[0].completed)
    }

    @Test
    fun updateRoutineSet_editsWeightAndReps() = runTest {
        val vm = startRoutineFor()

        vm.updateRoutineSet(exerciseIndex = 0, setIndex = 0, kg = 65.0, reps = 6)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        val set = active.session.setMatrix!!.single().sets[0]
        assertEquals(65.0, set.kg, 0.0)
        assertEquals(6, set.reps)
        assertNull(active.setMatrixError)
    }

    @Test
    fun updateRoutineSet_invalidValues_surfaceInlineErrorWithoutMutation() = runTest {
        val vm = startRoutineFor()

        vm.updateRoutineSet(exerciseIndex = 0, setIndex = 0, kg = 0.0, reps = 0)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        assertTrue(active.setMatrixError != null)
        assertEquals(60.0, active.session.setMatrix!!.single().sets[0].kg, 0.0)
        assertEquals(5, active.session.setMatrix!!.single().sets[0].reps)
    }

    @Test
    fun swapRoutineExercise_resetsSetsToPlannedDefaults() = runTest {
        val vm = startRoutineFor()
        vm.updateRoutineSet(0, 0, 100.0, 6)
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        vm.swapRoutineExercise(exerciseIndex = 0, exerciseId = pressItem.id, name = pressItem.name)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        val exercise = active.session.setMatrix!!.single()
        assertEquals(pressItem.name, exercise.name)
        assertEquals(pressItem.id, exercise.exerciseId)
        assertEquals(3, exercise.targetSets)
        assertEquals(90, exercise.restSeconds)
        assertEquals(3, exercise.sets.size)
        assertTrue(exercise.sets.all { !it.completed && it.rpe == null })
        assertTrue(exercise.sets.all { it.kg == 60.0 && it.reps == 5 })
    }

    @Test
    fun swapRoutineExercise_stopsRestTimer() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(15)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(75, activeState(vm).restSeconds)

        vm.swapRoutineExercise(exerciseIndex = 0, exerciseId = pressItem.id, name = pressItem.name)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, activeState(vm).restSeconds)
    }

    @Test
    fun addRoutineSet_appendsPlannedSetAndIncrementsTargets() = runTest {
        val vm = startRoutineFor()

        vm.addRoutineSet(exerciseIndex = 0)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        val exercise = active.session.setMatrix!!.single()
        assertEquals(4, exercise.targetSets)
        assertEquals(4, exercise.sets.size)
        val added = exercise.sets.last()
        assertFalse(added.completed)
        assertEquals(60.0, added.kg, 0.0)
        assertEquals(5, added.reps)
        val stored = dao.getSessionById(active.session.session_id)!!
        assertEquals(4, stored.setMatrix!!.single().sets.size)
    }

    @Test
    fun addRoutineSet_onNonPlannedExercise_isNoOp() = runTest {
        val vm = startActiveSessionCooldown()
        val before = activeState(vm).session.setMatrix

        vm.addRoutineSet(exerciseIndex = 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(before, activeState(vm).session.setMatrix)
    }

    @Test
    fun restTimer_crossingZero_emitsRestEndedHaptic() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, activeState(vm).restSeconds)

        vm.advanceTime(89)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(haptics.contains(WorkoutHaptic.REST_ENDED))

        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, activeState(vm).restSeconds)
        assertTrue(haptics.contains(WorkoutHaptic.REST_ENDED))
    }

    @Test
    fun crashRecovery_routine_resumesAtLastCompletedSet() = runTest {
        val first = startRoutineFor()
        first.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        first.toggleSetCompleted(0, 1)
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(recovered.uiState.value is WorkoutUiState.RecoveryRequired)

        recovered.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(recovered)
        assertTrue(active.isRoutine)
        val exercise = active.session.setMatrix!!.single()
        assertTrue(exercise.sets[0].completed)
        assertTrue(exercise.sets[1].completed)
        assertFalse(exercise.sets[2].completed)
    }

    @Test
    fun finishSession_routine_tonnageCountsCompletedSetsOnly() = runTest {
        val vm = startRoutineFor()
        vm.updateRoutineSet(0, 0, 100.0, 5)
        dispatcher.scheduler.advanceUntilIdle()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = vm.uiState.value as WorkoutUiState.Summary
        assertEquals(500.0, summary.tonnageKg!!, 0.0)
        assertEquals(
            HealthExerciseType.STRENGTH_TRAINING,
            healthSource.storedRecords().single().exerciseType
        )
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Tonnage: 500 kg"))
        }) }
    }

    @Test
    fun restTimer_dozeCatchesUp_duringRoutine() = runTest {
        var wall = 10_000L
        val vm = newViewModel(clock = { wall })
        with(presetDao) { insertAll(listOf(legDayPreset)) }
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset(legDayPreset.id)
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(90, activeState(vm).restSeconds)

        // Doze past the whole rest window; one late tick must catch the timer up.
        wall += 90_000L
        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, activeState(vm).restSeconds)
    }
}