package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.FakeExerciseCatalogDao
import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.health.FakeWorkoutHealthDataSource
import com.example.healthjournal.health.HealthExerciseType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    fun startPreset_recordsRoutineNameOnSession() = runTest {
        val vm = startRoutineFor()

        assertEquals("Leg Day", activeState(vm).session.routineName)
        val stored = dao.getSessionById(activeState(vm).session.session_id)!!
        assertEquals("Leg Day", stored.routineName)
    }

    @Test
    fun finishSession_routine_historyCardNamesRoutineAndExercises() = runTest {
        val vm = startRoutineFor()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Workout: Fitness"))
            assertTrue(entry.description.contains("Leg Day"))
            assertTrue(entry.description.contains("Barbell Squat"))
            assertTrue(entry.description.contains("3 sets"))
        }) }
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
    fun toggleSetCompleted_skippedPreviousSet_isIgnored() = runTest {
        val vm = startRoutineFor()

        vm.toggleSetCompleted(exerciseIndex = 0, setIndex = 1)
        dispatcher.scheduler.advanceUntilIdle()

        val active = activeState(vm)
        val sets = active.session.setMatrix!!.single().sets
        assertFalse(sets[1].completed)
        assertFalse(sets[0].completed)
        assertEquals(0, active.restSeconds)
        assertFalse(haptics.contains(WorkoutHaptic.SET_COMPLETE))
    }

    @Test
    fun toggleSetCompleted_previousSetsCompleted_allowsCompletingCurrent() = runTest {
        val vm = startRoutineFor()

        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.toggleSetCompleted(0, 1)
        dispatcher.scheduler.advanceUntilIdle()

        val sets = activeState(vm).session.setMatrix!!.single().sets
        assertTrue(sets[0].completed)
        assertTrue(sets[1].completed)
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
    fun swapRoutineExercise_resetsSetsToCatalogDefaults() = runTest {
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
        // After swap the exercise adopts the catalog default plan for the new
        // movement: Barbell Bench Press defaults to 3×5 @ 30 kg, rest 120s.
        assertEquals(3, exercise.targetSets)
        assertEquals(120, exercise.restSeconds)
        assertEquals(3, exercise.sets.size)
        assertTrue(exercise.sets.all { !it.completed && it.rpe == null })
        assertTrue(exercise.sets.all { it.kg == 30.0 && it.reps == 5 })
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
            assertTrue(entry.description.contains("Leg Day"))
            assertTrue(entry.description.contains("Barbell Squat"))
            assertTrue(entry.description.contains("3 sets"))
        }) }
    }

    @Test
    fun finishSession_routine_whilePaused_completesAndJournals() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.pauseSession()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Paused)

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = vm.uiState.value as WorkoutUiState.Summary
        assertEquals(WorkoutStatus.COMPLETED.name, summary.session.status)
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Leg Day"))
        }) }
        assertEquals(
            HealthExerciseType.STRENGTH_TRAINING,
            healthSource.storedRecords().single().exerciseType
        )
    }

    @Test
    fun finishSession_routine_healthWriteThrows_stillCompletesUnsynced() = runTest {
        val offline = FakeWorkoutHealthDataSource()
        offline.writeFailure = RuntimeException("no connection")
        with(presetDao) {
            insertAll(listOf(legDayPreset))
        }
        with(catalogDao) {
            insertAll(listOf(squatItem, pressItem))
        }
        val vm = newViewModel(health = offline)
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset(legDayPreset.id)
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = vm.uiState.value as WorkoutUiState.Summary
        assertEquals(false, summary.healthSynced)
        assertTrue(offline.storedRecords().isEmpty())
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Leg Day"))
        }) }
    }

    @Test
    fun finishSession_routine_doubleTap_writesExactlyOneJournalEntry() = runTest {
        val vm = startRoutineFor()
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()
        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { journalRepository.insert(any()) }
        assertEquals(1, healthSource.storedRecords().size)
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

    @Test
    fun startPreset_missingPreset_surfacesErrorWithoutStartingSession() = runTest {
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.startPreset("does-not-exist")
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as WorkoutUiState.Error
        assertEquals("Preset not found", state.message)
        assertTrue(dao.getAllSessions().first().isEmpty())

        vm.dismissError()
        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun startPreset_unknownOrZeroSetExercises_areSkipped() = runTest {
        val mixedDay = WorkoutPreset(
            id = "p2",
            name = "Mixed Day",
            scheduledDay = ScheduledDay.ANY.name,
            exercises = listOf(
                PresetExercise("barbell-squat", 3, 5, 60.0, 90),
                PresetExercise("no-such-exercise", 3, 5, 60.0, 90),
                PresetExercise("barbell-bench-press", 0, 6, 65.0, 120)
            ),
            lastModified = 1_700_000_000_000L
        )
        with(presetDao) { insertAll(listOf(mixedDay)) }
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset("p2")
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }

        // Unknown exercises (no catalog row) and zero-target-set plans are
        // omitted; the matrix holds exactly the resolvable movements.
        val matrix = activeState(vm).session.setMatrix!!
        assertEquals(1, matrix.size)
        assertEquals("Barbell Squat", matrix.single().name)
        assertTrue(matrix.single().isPlanned)
    }

    @Test
    fun startPreset_allUnresolvable_stillOpensEmptyRoutine() = runTest {
        val mysteryDay = WorkoutPreset(
            id = "p3",
            name = "Mystery Day",
            scheduledDay = ScheduledDay.ANY.name,
            exercises = listOf(
                PresetExercise("ghost-lift", 3, 5, 60.0, 90),
                PresetExercise("phantom-press", 2, 8, 40.0, 90)
            ),
            lastModified = 1_700_000_000_000L
        )
        with(presetDao) { insertAll(listOf(mysteryDay)) }
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.startPreset("p3")
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }

        val active = activeState(vm)
        assertTrue(vm.uiState.value is WorkoutUiState.Active)
        assertTrue(active.session.setMatrix.orEmpty().isEmpty())
        assertEquals("Mystery Day", active.session.routineName)
    }

    @Test
    fun startPreset_doubleTap_createsExactlyOneSessionWithoutDuplicates() = runTest {
        with(presetDao) { insertAll(listOf(legDayPreset)) }
        with(catalogDao) { insertAll(listOf(squatItem, pressItem)) }
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.startPreset(legDayPreset.id)
        vm.startPreset(legDayPreset.id)
        dispatcher.scheduler.advanceUntilIdle()

        // A double-submit must never fork a second session row or duplicate
        // the planned matrix: the store keeps a single session.
        val sessions = dao.getAllSessions().first()
        assertEquals(1, sessions.size)
        assertEquals(3, sessions.single().setMatrix!!.single().sets.size)
    }

    @Test
    fun pauseSession_freezesMutationsUntilResume() = runTest {
        val vm = startRoutineFor()
        vm.pauseSession()
        dispatcher.scheduler.advanceUntilIdle()
        val sessionId = (vm.uiState.value as WorkoutUiState.Paused).session.session_id

        // While frozen the routine-seeded mutations are accepted by the state
        // machine but must not touch the stored matrix or fire haptics.
        vm.updateRoutineSet(0, 0, 99.0, 9)
        vm.toggleSetCompleted(0, 0)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Paused)
        val stored = dao.getSessionById(sessionId)!!
        assertEquals(WorkoutStatus.PAUSED.name, stored.status)
        assertEquals(60.0, stored.setMatrix!!.single().sets[0].kg, 0.0)
        assertEquals(5, stored.setMatrix!!.single().sets[0].reps)
        assertFalse(stored.setMatrix!!.single().sets[0].completed)
        assertFalse(haptics.contains(WorkoutHaptic.SET_COMPLETE))

        // The frozen session does not accumulate elapsed time while paused.
        vm.advanceTime(30)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, (vm.uiState.value as WorkoutUiState.Paused).session.elapsedSeconds)
    }

    @Test
    fun finishSession_fromRecoveryRequired_isNoOp() = runTest {
        val first = startRoutineFor()
        val sessionId = activeState(first).session.session_id
        first.pauseSession()
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(recovered.uiState.value is WorkoutUiState.RecoveryRequired)
        val before = dao.getSessionById(sessionId)!!

        recovered.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        // A recovery prompt must be resolved first; Finish is a no-op there.
        assertTrue(recovered.uiState.value is WorkoutUiState.RecoveryRequired)
        assertEquals(before, dao.getSessionById(sessionId))
        coVerify(exactly = 0) { journalRepository.insert(any<JournalEntry>()) }
    }

    @Test
    fun swapRoutineExercise_unknownExercise_fallsBackToGenericDefaults() = runTest {
        val vm = startRoutineFor()

        vm.swapRoutineExercise(0, "mystery-movement", "Mystery Movement")
        dispatcher.scheduler.advanceUntilIdle()

        val exercise = activeState(vm).session.setMatrix!!.single()
        assertEquals("Mystery Movement", exercise.name)
        assertEquals("mystery-movement", exercise.exerciseId)
        assertEquals(3, exercise.targetSets)
        assertEquals(10, exercise.targetReps)
        assertEquals(20.0, exercise.targetWeightKg!!, 0.0)
        assertEquals(90, exercise.restSeconds)
        assertEquals(3, exercise.sets.size)
        assertTrue(exercise.sets.all { !it.completed && it.kg == 20.0 && it.reps == 10 })
    }

    @Test
    fun finishSession_plannedMatrixWithoutRoutineName_omitsNameLine() = runTest {
        val legacy = WorkoutSession(
            type = WorkoutType.FITNESS.name,
            status = WorkoutStatus.ACTIVE.name,
            startTimestamp = 1_700_000_000_000L,
            setMatrix = listOf(
                StrengthExercise(
                    name = "Barbell Squat",
                    sets = List(3) { StrengthSet(kg = 60.0, reps = 5) },
                    targetSets = 3,
                    targetReps = 5,
                    targetWeightKg = 60.0,
                    restSeconds = 90,
                    exerciseId = "barbell-squat"
                )
            ),
            routineName = null
        )
        dao.insertAll(listOf(legacy))
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)
        vm.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        // Pre-routine-name sessions carry a planned matrix but no routine
        // name: the exercise line renders and no blank/null name line appears.
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Workout: Fitness"))
            assertTrue(entry.description.contains("Barbell Squat: 3 sets"))
            assertFalse(entry.description.contains("null"))
        }) }
    }

    @Test
    fun restTimer_springForwardPastMidnight_catchesUpWithoutGoingNegative() = runTest {
        var wall = 1_700_000_000_000L
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

        // A spring-forward DST jump (01:59 -> 03:00) shifts the wall clock an
        // hour mid-rest; the next tick catches the timer up to zero and the
        // countdown never drifts negative afterwards.
        wall += 3_600_000L
        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, activeState(vm).restSeconds)
        assertTrue(haptics.contains(WorkoutHaptic.REST_ENDED))

        vm.advanceTime(5)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(0, activeState(vm).restSeconds)
    }
}