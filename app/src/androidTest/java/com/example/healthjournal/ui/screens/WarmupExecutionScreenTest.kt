package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.domain.WarmupOutcome
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeExerciseCatalogDao
import com.example.healthjournal.util.FakeWorkoutHealthDataSource
import com.example.healthjournal.util.FakeWorkoutPresetDao
import com.example.healthjournal.util.FakeWorkoutSessionDao
import com.example.healthjournal.viewmodel.WorkoutUiState
import com.example.healthjournal.viewmodel.WorkoutViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WarmupExecutionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var sessionDao: FakeWorkoutSessionDao
    private lateinit var viewModel: WorkoutViewModel

    private val warmupPreset = WorkoutPreset(
        id = "warmup-preset",
        name = "Leg Day",
        exercises = listOf(
            PresetExercise(
                exerciseId = "squat",
                targetSets = 2,
                defaultReps = 5,
                defaultWeightKg = 60.0,
                restSeconds = 90
            )
        ),
        warmupName = "Rowing",
        warmupDurationSeconds = 60
    )
    private val plainPreset = WorkoutPreset(
        id = "plain-preset",
        name = "Push Day",
        exercises = listOf(
            PresetExercise(
                exerciseId = "squat",
                targetSets = 1,
                defaultReps = 5,
                defaultWeightKg = 40.0,
                restSeconds = 60
            )
        )
    )

    @Before
    fun setup() {
        sessionDao = FakeWorkoutSessionDao()
        val presetDao = FakeWorkoutPresetDao()
        val catalogDao = FakeExerciseCatalogDao()
        val journalRepository: JournalRepository = mockk(relaxed = true)
        coEvery { journalRepository.insert(any()) } returns Unit
        UnitSettings.write(
            InstrumentationRegistry.getInstrumentation().targetContext,
            UnitSystem.METRIC
        )
        runBlocking {
            presetDao.insertAll(listOf(warmupPreset, plainPreset))
            catalogDao.insertAll(
                listOf(ExerciseCatalogItem(id = "squat", name = "Squat", muscleCategory = "Quads"))
            )
        }
        viewModel = WorkoutViewModel(
            repository = WorkoutRepository(sessionDao),
            healthSource = FakeWorkoutHealthDataSource(),
            journalRepository = journalRepository,
            presetRepository = PresetRepository(presetDao),
            catalogDao = catalogDao
        )
    }

    private fun openRoutine(presetId: String) {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {}, onPresetsClick = {})
            }
        }
        composeTestRule.waitForIdle()
        viewModel.startPreset(presetId)
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()
    }

    private fun activeSessionId(): String =
        (viewModel.uiState.value as WorkoutUiState.Active).session.session_id

    @Test
    fun warmup_showsPanelWithNameTimerSkip_andHidesWorkSets() {
        openRoutine(warmupPreset.id)

        composeTestRule.onNodeWithTag("warmup_panel").assertExists()
        composeTestRule.onNodeWithText("Rowing").assertExists()
        composeTestRule.onNodeWithTag("warmup_timer").assertTextContains("01:00")
        composeTestRule.onNodeWithTag("warmup_skip_button").assertExists()
        composeTestRule.onNodeWithTag("routine_exercise_0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("workout_pause_button").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Finish").assertIsEnabled()
    }

    @Test
    fun skipWarmup_revealsWorkSetsAndRecordsSkipped() {
        openRoutine(warmupPreset.id)

        composeTestRule.onNodeWithTag("warmup_skip_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("warmup_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
        val persisted = runBlocking { sessionDao.getSessionById(activeSessionId())!! }
        assertEquals(WarmupOutcome.SKIPPED.name, persisted.warmupOutcome)
    }

    @Test
    fun warmupCountdownToZero_revealsWorkSetsAndRecordsCompleted() {
        openRoutine(warmupPreset.id)

        viewModel.advanceTime(60)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("warmup_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
        val persisted = runBlocking { sessionDao.getSessionById(activeSessionId())!! }
        assertEquals(WarmupOutcome.COMPLETED.name, persisted.warmupOutcome)
        assertEquals(60, persisted.warmupElapsedSeconds)
    }

    @Test
    fun finishDuringWarmup_reachesSummaryAndRecordsSkipped() {
        openRoutine(warmupPreset.id)
        val sessionId = activeSessionId()

        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Workout Complete").assertExists()
        val completed = runBlocking { sessionDao.getSessionById(sessionId)!! }
        assertEquals(WarmupOutcome.SKIPPED.name, completed.warmupOutcome)
    }

    @Test
    fun presetWithoutWarmup_hasNoPanelAndPauseWorks() {
        openRoutine(plainPreset.id)

        composeTestRule.onNodeWithTag("warmup_panel").assertDoesNotExist()
        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
        composeTestRule.onNodeWithTag("workout_pause_button").assertIsEnabled()

        composeTestRule.onNodeWithTag("workout_pause_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Paused").assertExists()
    }
}
