package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.AnnotatedString
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeExerciseCatalogDao
import com.example.healthjournal.util.FakeWorkoutHealthDataSource
import com.example.healthjournal.util.FakeWorkoutPresetDao
import com.example.healthjournal.util.FakeWorkoutSessionDao
import com.example.healthjournal.viewmodel.WorkoutHaptic
import com.example.healthjournal.viewmodel.WorkoutViewModel
import io.mockk.coEvery
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for preset-driven routine execution in the Active state:
 * per-set weight/reps/RPE fields and completion checkboxes, automatic rest
 * timer, haptic cues on set/exercise completion, mid-routine exercise swap
 * via dropdown, and the custom +/- quick-increment pad.
 */
class RoutineExecutionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var presetDao: FakeWorkoutPresetDao
    private lateinit var catalogDao: FakeExerciseCatalogDao
    private lateinit var journalRepository: JournalRepository
    private lateinit var viewModel: WorkoutViewModel
    private val haptics = CopyOnWriteArrayList<WorkoutHaptic>()

    private val squat = ExerciseCatalogItem(
        id = "barbell-squat",
        name = "Barbell Squat",
        muscleCategory = "Quads"
    )
    private val press = ExerciseCatalogItem(
        id = "barbell-bench-press",
        name = "Barbell Bench Press",
        muscleCategory = "Chest"
    )
    private val legDay = WorkoutPreset(
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
        )
    )

    @Before
    fun setup() {
        dao = FakeWorkoutSessionDao()
        presetDao = FakeWorkoutPresetDao()
        catalogDao = FakeExerciseCatalogDao()
        journalRepository = mockk(relaxed = true)
        coEvery { journalRepository.insert(any()) } returns Unit
        haptics.clear()
        viewModel = buildViewModel()
    }

    private fun buildViewModel(): WorkoutViewModel {
        runBlocking {
            presetDao.insertAll(listOf(legDay))
            catalogDao.insertAll(listOf(squat, press))
        }
        return WorkoutViewModel(
            repository = WorkoutRepository(dao),
            healthSource = FakeWorkoutHealthDataSource(),
            journalRepository = journalRepository,
            onHaptic = { haptics.add(it) },
            presetRepository = PresetRepository(presetDao),
            catalogDao = catalogDao
        )
    }

    /** Renders the screen and starts Leg Day past the 3-2-1 countdown. */
    private fun openRoutine() {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {}, onPresetsClick = {})
            }
        }
        composeTestRule.waitForIdle()
        viewModel.startPreset(legDay.id)
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()
    }

    /** The persisted set matrix of the running session, for crash-safety asserts. */
    private fun persistedMatrix(): List<com.example.healthjournal.domain.StrengthExercise> {
        val sessionId =
            (viewModel.uiState.value as com.example.healthjournal.viewmodel.WorkoutUiState.Active)
                .session.session_id
        return runBlocking { dao.getSessionById(sessionId)!!.setMatrix!! }
    }

    /** Asserts an editable field's current text (its EditableText semantics config). */
    private fun SemanticsNodeInteraction.assertEditableText(value: String) {
        assert(SemanticsMatcher.expectValue(
            SemanticsProperties.EditableText,
            AnnotatedString(value)
        ))
    }

    @Test
    fun hubRoutines_listsPresetsAndStartsRoutineFromIdle() {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {}, onPresetsClick = {})
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Leg Day").assertExists()
        composeTestRule.onNodeWithTag("routine_start_p1").assertExists()

        composeTestRule.onNodeWithTag("routine_start_p1").performClick()
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
    }

    @Test
    fun routineActive_showsExerciseCardsWithPerSetFields() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
        composeTestRule.onNodeWithText("Barbell Squat").assertExists()
        composeTestRule.onNodeWithTag("routine_set_label_0_0").assertTextEquals("Set 1")
        composeTestRule.onNodeWithTag("routine_set_label_0_1").assertTextEquals("Set 2")
        composeTestRule.onNodeWithTag("routine_set_label_0_2").assertTextEquals("Set 3")
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("60")
        composeTestRule.onNodeWithTag("routine_set_reps_0_0")
            .assertEditableText("5")
        composeTestRule.onNodeWithTag("routine_set_rpe_0_0").assertExists()
        composeTestRule.onNodeWithTag("routine_set_done_0_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("routine_swap_0").assertExists()
    }

    @Test
    fun routineSet_editWeightRepsAndRpe_persists() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("65")
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextInput("6")
        composeTestRule.onNodeWithTag("routine_set_rpe_0_0").performTextInput("8")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("65")
        composeTestRule.onNodeWithTag("routine_set_reps_0_0")
            .assertEditableText("6")
        composeTestRule.onNodeWithTag("routine_set_rpe_0_0")
            .assertEditableText("8")
        val matrix = persistedMatrix()
        assertEquals(65.0, matrix.single().sets[0].kg, 0.0)
        assertEquals(6, matrix.single().sets[0].reps)
        assertEquals(8, matrix.single().sets[0].rpe)
    }

    @Test
    fun routineSet_completion_startsRestTimerWithHaptic() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workout_rest_timer").assertIsDisplayed()
        assertTrue(haptics.contains(WorkoutHaptic.SET_COMPLETE))
        assertFalse(haptics.contains(WorkoutHaptic.EXERCISE_COMPLETE))
    }

    @Test
    fun routineSet_finalSet_emitsExerciseCompleteHaptic() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.onNodeWithTag("routine_set_done_0_1").performClick()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").performClick()
        composeTestRule.waitForIdle()

        assertTrue(haptics.contains(WorkoutHaptic.EXERCISE_COMPLETE))
    }

    @Test
    fun routineRestTimer_countsDownWhileResting() {
        openRoutine()
        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Rest 01:30").assertExists()

        viewModel.advanceTime(1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rest 01:29").assertExists()
    }

    @Test
    fun routineQuickPad_addsTwoAndHalfKgThenFivePounds() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_quick_2_5").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("62.5")

        composeTestRule.onNodeWithTag("routine_quick_5_lb").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("64.77")
    }

    @Test
    fun routineSwap_dropdownReplacesExercise() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_swap_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Barbell Bench Press").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Barbell Bench Press").assertExists()
        composeTestRule.onNodeWithText("Barbell Squat").assertDoesNotExist()
        assertEquals("Barbell Bench Press", persistedMatrix().single().name)
    }
}