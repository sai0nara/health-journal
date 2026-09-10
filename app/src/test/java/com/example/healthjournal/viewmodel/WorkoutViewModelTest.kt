package com.example.healthjournal.viewmodel

import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.health.FakeWorkoutHealthDataSource
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

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dao = FakeWorkoutSessionDao()
        repository = WorkoutRepository(dao)
        healthSource = FakeWorkoutHealthDataSource()
        haptics.clear()
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
        onHaptic = { haptics.add(it) }
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
}
