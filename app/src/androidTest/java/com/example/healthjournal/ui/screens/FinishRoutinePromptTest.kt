package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
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
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.WorkoutType
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for the end-of-routine confirmation: the dialog appears
 * only after the final planned set, Keep going leaves the session running,
 * and Finish reaches Summary.
 */
class FinishRoutinePromptTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var catalogDao: FakeExerciseCatalogDao
    private lateinit var journalRepository: JournalRepository
    private lateinit var viewModel: WorkoutViewModel

    private val squat = ExerciseCatalogItem(
        id = "barbell-squat",
        name = "Barbell Squat",
        muscleCategory = "Quads"
    )
    private val legDay = WorkoutPreset(
        id = "p1",
        name = "Leg Day",
        scheduledDay = ScheduledDay.ANY.name,
        exercises = listOf(
            PresetExercise(
                exerciseId = "barbell-squat",
                targetSets = 2,
                defaultReps = 5,
                defaultWeightKg = 60.0,
                restSeconds = 90
            )
        )
    )

    @Before
    fun setup() {
        dao = FakeWorkoutSessionDao()
        presetDao = FakeWorkoutPresetDao()
        catalogDao = FakeExerciseCatalogDao()
        journalRepository = mockk(relaxed = true)
        coEvery { journalRepository.insert(any()) } returns Unit
        UnitSettings.write(InstrumentationRegistry.getInstrumentation().targetContext, UnitSystem.METRIC)
        runBlocking {
            presetDao.insertAll(listOf(legDay))
            catalogDao.insertAll(listOf(squat))
        }
        viewModel = WorkoutViewModel(
            repository = WorkoutRepository(dao),
            healthSource = FakeWorkoutHealthDataSource(),
            journalRepository = journalRepository,
            presetRepository = PresetRepository(presetDao),
            catalogDao = catalogDao
        )
    }

    private fun renderScreen() {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {}, onPresetsClick = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun openRoutine() {
        renderScreen()
        viewModel.startPreset(legDay.id)
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()
    }

    private fun completeSet(setIndex: Int) {
        composeTestRule.onNodeWithTag("routine_set_done_0_$setIndex").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun finalSet_showsPrompt_andKeepGoingLeavesRoutineActive() {
        openRoutine()

        completeSet(0)
        composeTestRule.onNodeWithTag("finish_routine_prompt").assertDoesNotExist()

        completeSet(1)
        composeTestRule.onNodeWithTag("finish_routine_prompt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finish routine?").assertIsDisplayed()
        composeTestRule.onNodeWithText("All exercises are complete.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("finish_routine_keep_going").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finish_routine_prompt").assertDoesNotExist()
        val state = viewModel.uiState.value as WorkoutUiState.Active
        assertEquals(WorkoutStatus.ACTIVE.name, state.session.status)
        assertEquals(90, state.restSeconds)
    }

    @Test
    fun promptConfirm_finishesToSummary() {
        openRoutine()
        completeSet(0)
        completeSet(1)
        composeTestRule.onNodeWithTag("finish_routine_prompt").assertIsDisplayed()

        composeTestRule.onNodeWithTag("finish_routine_confirm").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
        val state = viewModel.uiState.value as WorkoutUiState.Summary
        assertEquals(WorkoutStatus.COMPLETED.name, state.session.status)
    }

    @Test
    fun manualFinishBeforeCompletion_bypassesPrompt() {
        openRoutine()
        completeSet(0)

        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finish_routine_prompt").assertDoesNotExist()
        assertTrue(viewModel.uiState.value is WorkoutUiState.Summary)
    }

    @Test
    fun manualStrengthSetCompletion_neverShowsPrompt() {
        renderScreen()
        viewModel.selectType(WorkoutType.FITNESS)
        viewModel.updateTarget("30")
        viewModel.startSession()
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()

        viewModel.addExercise("Squat")
        composeTestRule.waitForIdle()
        val exerciseId = (viewModel.uiState.value as WorkoutUiState.Active)
            .session.setMatrix!!.single().id
        viewModel.addSet(exerciseId, 60.0, 5)
        composeTestRule.waitForIdle()
        viewModel.toggleSetCompleted(0, 0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finish_routine_prompt").assertDoesNotExist()
        assertTrue(viewModel.uiState.value is WorkoutUiState.Active)
    }

    @Test
    fun hiitSession_neverShowsPrompt() {
        renderScreen()
        viewModel.selectType(WorkoutType.HIIT)
        viewModel.updateTarget("20")
        viewModel.startSession()
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()

        viewModel.toggleSetCompleted(0, 0)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("finish_routine_prompt").assertDoesNotExist()
        assertTrue(viewModel.uiState.value is WorkoutUiState.Active)
    }
}
