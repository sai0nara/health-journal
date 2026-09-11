package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.health.FakeWorkoutHealthDataSource
import com.example.healthjournal.health.HealthExerciseType
import com.example.healthjournal.domain.WorkoutIntervalPhase
import com.example.healthjournal.data.JournalRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WorkoutViewModel state transitions: selection,
 * configuration, timed sessions with batched persistence, summaries with
 * Health Connect sync, manual logging, and crash recovery.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var repository: WorkoutRepository
    private lateinit var healthSource: FakeWorkoutHealthDataSource
    private val journalRepository: JournalRepository = mockk(relaxed = true)
    private val haptics = mutableListOf<WorkoutHaptic>()
    private var nowMillis = 1_700_000_000_000L

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeWorkoutSessionDao()
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
        health: FakeWorkoutHealthDataSource = healthSource
    ): WorkoutViewModel = WorkoutViewModel(
        repository = repository,
        healthSource = health,
        journalRepository = journalRepository,
        dispatcher = dispatcher,
        onHaptic = { haptics.add(it) },
        clock = { nowMillis }
    )

    private fun newViewModelWithClock(
        clock: () -> Long
    ): WorkoutViewModel = WorkoutViewModel(
        repository = repository,
        healthSource = healthSource,
        journalRepository = journalRepository,
        dispatcher = dispatcher,
        onHaptic = { haptics.add(it) },
        clock = clock
    )

    /** Starts a configured session and runs it past the 3-2-1 countdown. */
    private fun startActiveSession(type: WorkoutType, target: String): WorkoutViewModel {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(type)
        vm.updateTarget(target)
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        return vm
    }

    @Test
    fun initialState_withoutUnfinishedSession_isIdle() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun initialState_withUnfinishedSession_requestsRecovery() = runTest {
        repository.saveSession(WorkoutSession(status = WorkoutStatus.ACTIVE.name))

        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)
    }

    @Test
    fun selectType_movesToConfiguring() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectType(WorkoutType.RUN)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Configuring)
        assertEquals(WorkoutType.RUN, (state as WorkoutUiState.Configuring).type)
    }

    @Test
    fun startSession_withoutConfiguring_isIgnored() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun startSession_withValidTarget_createsActiveSession() = runTest {
        val vm = startActiveSession(WorkoutType.RUN, "5")

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Active)
        val session = (state as WorkoutUiState.Active).session
        assertEquals(WorkoutStatus.ACTIVE.name, session.status)
        // Run targets are entered in kilometres and stored as metres.
        assertEquals(5_000.0, session.targetDistanceM!!, 0.0)
        assertEquals(listOf(WorkoutHaptic.START), haptics)
    }

    @Test
    fun startSession_walkingHiking_distanceCapable_UsesDistanceTarget() = runTest {
        val vm = startActiveSession(WorkoutType.WALKING_HIKING, "6")

        val session = (vm.uiState.value as WorkoutUiState.Active).session
        assertEquals(6_000.0, session.targetDistanceM!!, 0.0)
        assertEquals(null, session.targetDurationMin)
    }

    @Test
    fun startSession_cycling_distanceCapable_UsesDistanceTarget() = runTest {
        val vm = startActiveSession(WorkoutType.CYCLING, "25")

        val session = (vm.uiState.value as WorkoutUiState.Active).session
        assertEquals(25_000.0, session.targetDistanceM!!, 0.0)
        assertEquals(null, session.targetDurationMin)
    }

    @Test
    fun startSession_withUnfinishedSession_goesToRecoveryInstead() = runTest {
        repository.saveSession(WorkoutSession(status = WorkoutStatus.PAUSED.name))
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.RUN)
        vm.updateTarget("5")
        dispatcher.scheduler.advanceUntilIdle()

        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)
        assertEquals(0, haptics.size)
    }

    @Test
    fun startSession_withInvalidTarget_reportsInlineError() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.FITNESS)
        vm.updateTarget("0")
        dispatcher.scheduler.advanceUntilIdle()

        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Configuring)
        assertTrue((state as WorkoutUiState.Configuring).targetError != null)
    }

    @Test
    fun pauseAndResume_toggleActiveAndPaused() = runTest {
        val vm = startActiveSession(WorkoutType.YOGA, "30")

        vm.pauseSession()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Paused)

        vm.resumeSession()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Active)
        assertEquals(
            listOf(WorkoutHaptic.START, WorkoutHaptic.STOP, WorkoutHaptic.START),
            haptics
        )
    }

    @Test
    fun advanceTime_batchesPersistenceEveryFiveSeconds() = runTest {
        val vm = startActiveSession(WorkoutType.RUN, "5")

        vm.advanceTime(3)
        dispatcher.scheduler.advanceUntilIdle()
        val active = vm.uiState.value as WorkoutUiState.Active
        assertEquals(3L, active.session.elapsedSeconds)
        assertEquals(0L, dao.getSessionById(active.session.session_id)!!.elapsedSeconds)

        vm.advanceTime(2)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5L, dao.getSessionById(active.session.session_id)!!.elapsedSeconds)
    }

    @Test
    fun advanceTime_afterDoze_catchesUpElapsedFromInjectedClock() = runTest {
        var wall = 10_000L
        val vm = newViewModelWithClock { wall }
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.RUN)
        vm.updateTarget("5")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        assertTrue(vm.uiState.value is WorkoutUiState.Active)
        assertEquals(0L, (vm.uiState.value as WorkoutUiState.Active).session.elapsedSeconds)

        // The device dozes: no ticks fire, then the wall clock jumps five minutes ahead.
        wall += 300_000L
        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        val active = vm.uiState.value as WorkoutUiState.Active
        assertEquals(300L, active.session.elapsedSeconds)
    }

    @Test
    fun advanceTime_afterDoze_decrementsRestTimerFromInjectedClock() = runTest {
        var wall = 10_000L
        val vm = newViewModelWithClock { wall }
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.FITNESS)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        val exerciseId = (vm.uiState.value as WorkoutUiState.Active).session.setMatrix!!.single().id
        vm.addSet(exerciseId, kg = 40.0, reps = 10)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(WorkoutViewModel.DEFAULT_REST_SECONDS, (vm.uiState.value as WorkoutUiState.Active).restSeconds)

        // Doze for the remaining rest interval: one late tick must catch the rest timer up.
        wall += WorkoutViewModel.DEFAULT_REST_SECONDS * 1_000L
        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, (vm.uiState.value as WorkoutUiState.Active).restSeconds)
    }

    @Test
    fun advanceTime_pausedWallTimeDoesNotCountAfterResume() = runTest {
        var wall = 10_000L
        val vm = newViewModelWithClock { wall }
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.RUN)
        vm.updateTarget("5")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.advanceTime(120)
        dispatcher.scheduler.advanceUntilIdle()
        val sessionId = (vm.uiState.value as WorkoutUiState.Active).session.session_id

        vm.pauseSession()
        dispatcher.scheduler.advanceUntilIdle()

        // Ten minutes frozen in PAUSED: wall time must not accrue to the session.
        wall += 600_000L
        vm.resumeSession()
        dispatcher.scheduler.advanceUntilIdle()
        vm.advanceTime(1)
        dispatcher.scheduler.advanceUntilIdle()

        val active = vm.uiState.value as WorkoutUiState.Active
        assertEquals(121L, active.session.elapsedSeconds)
        // 121s is not a persistence boundary; the stored row reflects the resume snapshot
        // and must not have absorbed the ten paused minutes.
        assertEquals(120L, dao.getSessionById(sessionId)!!.elapsedSeconds)
    }

    @Test
    fun finishSession_savesCompletedJournalAndHealthSummary() = runTest {
        val vm = startActiveSession(WorkoutType.RUN, "5")
        vm.advanceTime(1_800)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Summary)
        val summary = state as WorkoutUiState.Summary
        assertEquals(343.0, summary.caloriesKcal, 0.01)
        assertTrue(summary.healthSynced)
        assertEquals(WorkoutStatus.COMPLETED.name, summary.session.status)
        assertEquals(
            WorkoutStatus.COMPLETED.name,
            dao.getSessionById(summary.session.session_id)!!.status
        )
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Run"))
            assertTrue(entry.description.contains("30"))
        }) }
        assertEquals(1, healthSource.storedRecords().size)
        assertTrue(haptics.contains(WorkoutHaptic.STOP))
    }

    @Test
    fun finishSession_healthDenied_stillSucceedsUnsynced() = runTest {
        val denied = FakeWorkoutHealthDataSource(permissionGranted = false)
        val vm = WorkoutViewModel(
            repository = repository,
            healthSource = denied,
            journalRepository = journalRepository,
            dispatcher = dispatcher,
            onHaptic = { haptics.add(it) }
        )
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.YOGA)
        vm.updateTarget("30")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Summary)
        assertEquals(false, (state as WorkoutUiState.Summary).healthSynced)
    }

    @Test
    fun finishSession_newTypes_healthDenied_stillCompleteUnsynced() = runTest {
        for (type in listOf(
            WorkoutType.HIIT, WorkoutType.CALISTHENICS, WorkoutType.SWIMMING,
            WorkoutType.WALKING_HIKING, WorkoutType.PILATES
        )) {
            val denied = FakeWorkoutHealthDataSource(permissionGranted = false)
            val vm = newViewModel(health = denied)
            dispatcher.scheduler.advanceUntilIdle()
            vm.selectType(type)
            vm.updateTarget("20")
            vm.startSession()
            dispatcher.scheduler.advanceUntilIdle()
            while (vm.uiState.value is WorkoutUiState.Countdown) {
                vm.advanceTime(1)
                dispatcher.scheduler.advanceUntilIdle()
            }
            vm.advanceTime(600)
            dispatcher.scheduler.advanceUntilIdle()

            vm.finishSession()
            dispatcher.scheduler.advanceUntilIdle()

            val state = vm.uiState.value
            assertTrue(state is WorkoutUiState.Summary)
            assertEquals(false, (state as WorkoutUiState.Summary).healthSynced)
            assertTrue(denied.storedRecords().isEmpty())
        }
    }

    @Test
    fun finishSession_airplaneMode_healthWriteThrows_stillCompletesUnsynced() = runTest {
        val offline = FakeWorkoutHealthDataSource()
        offline.writeFailure = RuntimeException("no connection")
        val vm = newViewModel(health = offline)
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.RUN)
        vm.updateTarget("5")
        vm.startSession()
        dispatcher.scheduler.advanceUntilIdle()
        while (vm.uiState.value is WorkoutUiState.Countdown) {
            vm.advanceTime(1)
            dispatcher.scheduler.advanceUntilIdle()
        }
        vm.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Summary)
        assertEquals(false, (state as WorkoutUiState.Summary).healthSynced)
        assertTrue(offline.storedRecords().isEmpty())
    }

    @Test
    fun saveManualLog_airplaneMode_healthWriteThrows_stillSucceeds() = runTest {
        val offline = FakeWorkoutHealthDataSource()
        offline.writeFailure = RuntimeException("no connection")
        val vm = newViewModel(health = offline)
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveManualLog(
            type = WorkoutType.CALISTHENICS,
            durationMinutes = "20",
            calories = "",
            timestamp = 1_000L
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
        assertTrue(offline.storedRecords().isEmpty())
    }

    @Test
    fun saveManualLog_valid_persistsCompletedSessionAndJournal() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveManualLog(
            type = WorkoutType.FITNESS,
            durationMinutes = "45",
            calories = "270",
            timestamp = 1_000L
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Fitness"))
        }) }
    }

    @Test
    fun saveManualLog_invalid_reportsErrorAndRecovers() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.saveManualLog(type = null, durationMinutes = "abc", calories = "", timestamp = 1_000L)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Error)

        vm.dismissError()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun discardRecovery_marksDiscardedAndGoesIdle() = runTest {
        val unfinished = WorkoutSession(status = WorkoutStatus.PAUSED.name)
        repository.saveSession(unfinished)
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.RecoveryRequired)

        vm.discardRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
        assertEquals(
            WorkoutStatus.DISCARDED.name,
            dao.getSessionById(unfinished.session_id)!!.status
        )
    }

    @Test
    fun resumeRecovery_restoresActiveSession() = runTest {
        val unfinished = WorkoutSession(
            status = WorkoutStatus.PAUSED.name,
            elapsedSeconds = 120L
        )
        repository.saveSession(unfinished)
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is WorkoutUiState.Active)
        assertEquals(unfinished.session_id, (state as WorkoutUiState.Active).session.session_id)
        assertEquals(120L, state.session.elapsedSeconds)
    }

    @Test
    fun recentSessions_emitsRepositoryHistoryNewestFirst() = runTest {
        repository.saveSession(
            WorkoutSession(status = WorkoutStatus.COMPLETED.name, startTimestamp = 1_000L)
        )
        repository.saveSession(
            WorkoutSession(status = WorkoutStatus.COMPLETED.name, startTimestamp = 3_000L)
        )
        repository.saveSession(
            WorkoutSession(status = WorkoutStatus.COMPLETED.name, startTimestamp = 2_000L)
        )

        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        val recent = vm.recentSessions.value
        assertEquals(3, recent.size)
        assertEquals(3_000L, recent[0].startTimestamp)
        assertEquals(2_000L, recent[1].startTimestamp)
        assertEquals(1_000L, recent[2].startTimestamp)
    }

    @Test
    fun closeSummary_returnsToIdle() = runTest {
        val vm = startActiveSession(WorkoutType.RUN, "5")
        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Summary)

        vm.closeSummary()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun cancelConfiguring_returnsToIdle() = runTest {
        val vm = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectType(WorkoutType.YOGA)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is WorkoutUiState.Configuring)

        vm.cancelConfiguring()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is WorkoutUiState.Idle)
    }

    @Test
    fun finishSession_journalDescriptionCarriesTonnage() = runTest {
        val vm = startActiveSession(WorkoutType.FITNESS, "30")
        vm.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        vm.addSet(exerciseId = (vm.uiState.value as WorkoutUiState.Active)
            .session.setMatrix!!.single().id, kg = 60.0, reps = 10)
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Tonnage: 600 kg"))
        }) }
    }

    @Test
    fun finishSession_journalDescriptionCarriesIntervalCounts() = runTest {
        val vm = startActiveSession(WorkoutType.HIIT, "20")
        vm.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        vm.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { journalRepository.insert(withArg { entry ->
            assertTrue(entry.description.contains("Rounds: 1 · Intervals: 1"))
        }) }
    }

    @Test
    fun finishSession_allTenTypes_produceSummaryJournalAndExerciseWrite() = runTest {
        val expectedTypes = mapOf(
            WorkoutType.RUN to HealthExerciseType.RUNNING,
            WorkoutType.FITNESS to HealthExerciseType.STRENGTH_TRAINING,
            WorkoutType.YOGA to HealthExerciseType.YOGA,
            WorkoutType.HIIT to HealthExerciseType.HIIT,
            WorkoutType.WALKING_HIKING to HealthExerciseType.HIKING,
            WorkoutType.CYCLING to HealthExerciseType.CYCLING,
            WorkoutType.STRETCHING_MOBILITY to HealthExerciseType.STRETCHING,
            WorkoutType.PILATES to HealthExerciseType.PILATES,
            WorkoutType.SWIMMING to HealthExerciseType.SWIMMING,
            WorkoutType.CALISTHENICS to HealthExerciseType.CALISTHENICS
        )
        for ((type, healthType) in expectedTypes) {
            val health = FakeWorkoutHealthDataSource()
            val vm = newViewModel(health = health)
            dispatcher.scheduler.advanceUntilIdle()
            vm.selectType(type)
            vm.updateTarget("30")
            vm.startSession()
            dispatcher.scheduler.advanceUntilIdle()
            while (vm.uiState.value is WorkoutUiState.Countdown) {
                vm.advanceTime(1)
                dispatcher.scheduler.advanceUntilIdle()
            }
            vm.advanceTime(600)
            dispatcher.scheduler.advanceUntilIdle()

            vm.finishSession()
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.uiState.value is WorkoutUiState.Summary)
            assertEquals(healthType, health.storedRecords().single().exerciseType)
        }
        coVerify(exactly = expectedTypes.size) { journalRepository.insert(any()) }
    }

    @Test
    fun crashRecovery_midSet_resumesFinishesWithTonnageAndStrengthWrite() = runTest {
        val first = startActiveSession(WorkoutType.FITNESS, "30")
        first.addExercise("Squat")
        dispatcher.scheduler.advanceUntilIdle()
        first.addSet(
            exerciseId = (first.uiState.value as WorkoutUiState.Active)
                .session.setMatrix!!.single().id,
            kg = 100.0,
            reps = 5
        )
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(recovered.uiState.value is WorkoutUiState.RecoveryRequired)

        recovered.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        val active = recovered.uiState.value as WorkoutUiState.Active
        assertEquals("Squat", active.session.setMatrix!!.single().name)
        assertEquals(100.0, active.session.setMatrix!!.single().sets.single().kg, 0.0)
        recovered.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()
        recovered.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = recovered.uiState.value as WorkoutUiState.Summary
        assertEquals(500.0, summary.tonnageKg!!, 0.0)
        assertEquals(
            HealthExerciseType.STRENGTH_TRAINING,
            healthSource.storedRecords().single().exerciseType
        )
    }

    @Test
    fun crashRecovery_midInterval_resumesFinishesWithIntervalCountsAndWrite() = runTest {
        val first = startActiveSession(WorkoutType.HIIT, "20")
        first.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()

        val recovered = newViewModel()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(recovered.uiState.value is WorkoutUiState.RecoveryRequired)

        recovered.resumeRecovery()
        dispatcher.scheduler.advanceUntilIdle()

        val active = recovered.uiState.value as WorkoutUiState.Active
        assertEquals(WorkoutIntervalPhase.REST, active.session.intervalState!!.phase)
        assertEquals(1, active.session.intervalState!!.intervals)
        recovered.advanceInterval()
        dispatcher.scheduler.advanceUntilIdle()
        recovered.advanceTime(600)
        dispatcher.scheduler.advanceUntilIdle()
        recovered.finishSession()
        dispatcher.scheduler.advanceUntilIdle()

        val summary = recovered.uiState.value as WorkoutUiState.Summary
        assertEquals(1, summary.intervalRounds)
        assertEquals(2, summary.intervalIntervals)
        assertEquals(HealthExerciseType.HIIT, healthSource.storedRecords().single().exerciseType)
    }
}
