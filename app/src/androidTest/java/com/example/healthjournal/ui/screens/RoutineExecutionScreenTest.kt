package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
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
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PresetRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.data.local.ExerciseCatalogItem
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
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
        // The quick pad reads the persisted unit preference; tests assume the
        // default metric step unless a test explicitly switches to imperial.
        UnitSettings.write(InstrumentationRegistry.getInstrumentation().targetContext, UnitSystem.METRIC)
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
        composeTestRule.onNodeWithTag("routine_set_rpe_0_0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("routine_set_done_0_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("routine_swap_0").assertExists()
    }

    @Test
    fun routineSet_editWeightAndReps_persists() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("65")
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextInput("6")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("65")
        composeTestRule.onNodeWithTag("routine_set_reps_0_0")
            .assertEditableText("6")
        val matrix = persistedMatrix()
        assertEquals(65.0, matrix.single().sets[0].kg, 0.0)
        assertEquals(6, matrix.single().sets[0].reps)
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
    fun routineSet_cannotCheckSetWhilePreviousIsPending() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_done_0_1").performClick()
        composeTestRule.waitForIdle()

        // The skipped earlier set stays pending and the later set cannot complete.
        assertFalse(persistedMatrix().single().sets[1].completed)
        assertFalse(persistedMatrix().single().sets[0].completed)
        assertFalse(haptics.contains(WorkoutHaptic.SET_COMPLETE))
    }

    @Test
    fun routineSet_checkingBoxDisabledWhilePreviousIsPending_preventsClick() {
        openRoutine()

        // Later sets' checkboxes are disabled until every earlier set is done.
        composeTestRule.onNodeWithTag("routine_set_done_0_1").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").assertIsNotEnabled()

        // Completing Set 1 alone only unlocks Set 2.
        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_done_0_1").assertIsEnabled()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").assertIsNotEnabled()

        // Once the earlier set is complete the later checkbox re-enables.
        composeTestRule.onNodeWithTag("routine_set_done_0_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_done_0_2").assertIsEnabled()
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
    fun routineQuickPad_actsOnLastEditedSet_notFirstUncompleted() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_reps_0_1").performClick()
        composeTestRule.onNodeWithTag("routine_set_reps_0_1").performTextInput("5")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()

        val matrix = persistedMatrix()
        assertEquals(60.0, matrix.single().sets[0].kg, 0.0)
        assertEquals(61.25, matrix.single().sets[1].kg, 0.0)
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("60")
        composeTestRule.onNodeWithTag("routine_set_kg_0_1")
            .assertEditableText("61.25")
    }

    @Test
    fun routineQuickPad_actsWhileFieldFocused_updatesDisplayedValue() {
        openRoutine()

        // Put the cursor in the weight field of Set 1, then nudge via the pad.
        composeTestRule.onNodeWithTag("routine_set_kg_0_1").performClick()
        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()

        val matrix = persistedMatrix()
        assertEquals(61.25, matrix.single().sets[1].kg, 0.0)
        composeTestRule.onNodeWithTag("routine_set_kg_0_1")
            .assertEditableText("61.25")

        // Same for the reps field while focused.
        composeTestRule.onNodeWithTag("routine_set_reps_0_1").performClick()
        composeTestRule.onNodeWithTag("routine_reps_plus").performClick()
        composeTestRule.waitForIdle()

        assertEquals(6, persistedMatrix().single().sets[1].reps)
        composeTestRule.onNodeWithTag("routine_set_reps_0_1")
            .assertEditableText("6")
    }

    @Test
    fun routineQuickPad_actsWhileFieldFocused_weightAndRepsAdjust() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("61.25")

        composeTestRule.onNodeWithTag("routine_weight_minus").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("60")

        composeTestRule.onNodeWithTag("routine_reps_plus").performClick()
        composeTestRule.waitForIdle()
        assertEquals(6, persistedMatrix().single().sets[0].reps)

        composeTestRule.onNodeWithTag("routine_reps_minus").performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, persistedMatrix().single().sets[0].reps)
    }

    @Test
    fun routineQuickPad_metricStepIsOnePointTwoFiveKg() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()

        // Four 1.25kg taps make a clean 5kg jump from the 60kg default.
        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("65")
    }

    @Test
    fun routineQuickPad_rapidTaps_applyEveryIncrementWithoutLostUpdates() {
        openRoutine()

        // Anchor the set at 20.0kg, then fire five + taps with no idle between.
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("20")
        composeTestRule.waitForIdle()
        assertEquals(20.0, persistedMatrix().single().sets[0].kg, 0.0)

        repeat(5) {
            composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        }
        composeTestRule.waitForIdle()

        // 20.0 + 5 x 1.25 = 26.25; every tap must land, and no duplicate rows.
        assertEquals(20.0 + 5 * 1.25, persistedMatrix().single().sets[0].kg, 0.0)
        assertEquals(1, persistedMatrix().size)
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("26.25")
    }

    @Test
    fun routineSwap_stopsRestTimer() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("workout_rest_timer").assertIsDisplayed()

        composeTestRule.onNodeWithTag("routine_swap_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Barbell Bench Press").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workout_rest_timer").assertDoesNotExist()
    }

    @Test
    fun routineAddSet_appendsExtraPlannedSet() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_add_set_0").performClick()
        composeTestRule.waitForIdle()

        val matrix = persistedMatrix()
        assertEquals(4, matrix.single().sets.size)
        assertEquals(4, matrix.single().targetSets)
        val added = matrix.single().sets[3]
        assertEquals(60.0, added.kg, 0.0)
        assertEquals(5, added.reps)
        composeTestRule.onNodeWithTag("routine_set_label_0_3").assertTextEquals("Set 4")
    }

    @Test
    fun routineManySets_finishButtonStaysReachable() {
        openRoutine()

        // Push the routine well past the first screenful of sets so the
        // scrollable area must give way rather than clip the CTA row.
        repeat(5) {
            composeTestRule.onNodeWithTag("routine_add_set_0").performClick()
            composeTestRule.waitForIdle()
        }
        assertEquals(8, persistedMatrix().single().sets.size)

        composeTestRule.onNodeWithText("Finish").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
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

    @Test
    fun countdown_showsGetReady_andIsNotSkippable() {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {}, onPresetsClick = {})
            }
        }
        composeTestRule.waitForIdle()
        viewModel.startPreset(legDay.id)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workout_countdown").assertExists()
        composeTestRule.onNodeWithText("Get ready").assertExists()
        // No skip/cancel affordances: the countdown runs to zero on its own.
        composeTestRule.onNodeWithText("Skip", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Cancel", useUnmergedTree = true).assertDoesNotExist()

        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_exercise_0").assertExists()
    }

    @Test
    fun routineQuickPad_focusedFieldAtRepsOne_decrementIsNoOp() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performClick()
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_reps_0_0").performTextInput("1")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_reps_minus").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_set_reps_0_0")
            .assertEditableText("1")
        assertEquals(1, persistedMatrix().single().sets[0].reps)
        composeTestRule.onNodeWithTag("set_matrix_error").assertDoesNotExist()
    }

    @Test
    fun routineQuickPad_unfocusedFieldAtRepsOne_decrementIsNoOp() {
        openRoutine()

        // Seed the reps floor without ever focusing a field; the pad still
        // targets the first uncompleted set and its decrement is a no-op.
        viewModel.updateRoutineSet(0, 0, 60.0, 1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_reps_minus").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_set_reps_0_0")
            .assertEditableText("1")
        assertEquals(1, persistedMatrix().single().sets[0].reps)
        composeTestRule.onNodeWithTag("set_matrix_error").assertDoesNotExist()
    }

    @Test
    fun routineQuickPad_imperialStepIsFivePounds() {
        UnitSettings.write(InstrumentationRegistry.getInstrumentation().targetContext, UnitSystem.IMPERIAL)
        openRoutine()

        composeTestRule.onNodeWithText("+5 lb").assertExists()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("20")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_weight_plus").performClick()
        composeTestRule.waitForIdle()

        // Rows display lb now: anchoring "20" means 20 lb, and one + tap
        // lands on 25 lb while storage stays canonical kg.
        val expected = UnitConverter.lbsToKg(20.0) + UnitConverter.lbsToKg(5.0)
        assertEquals(expected, persistedMatrix().single().sets[0].kg, 0.0)
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("25")
    }

    @Test
    fun routineQuickPad_weightBelowLowerBound_showsInlineErrorAndKeepsValue() {
        openRoutine()

        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("1.25")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("routine_weight_minus").performClick()
        composeTestRule.waitForIdle()

        // A decrement that would cross zero is rejected inline: value and
        // stored row both stay at 1.25 kg.
        composeTestRule.onNodeWithTag("set_matrix_error").assertExists()
        composeTestRule.onNodeWithText("Weight must be greater than zero").assertExists()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("1.25")
        assertEquals(1.25, persistedMatrix().single().sets[0].kg, 0.0)
    }

    @Test
    fun routineSetRows_imperialShowLbAndParseToKg() {
        UnitSettings.write(InstrumentationRegistry.getInstrumentation().targetContext, UnitSystem.IMPERIAL)
        openRoutine()

        // 60 kg renders as 132.3 lb with an lb label.
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("132.3")
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertTextContains("lb", substring = true)

        // Typing lb parses back to canonical kg storage.
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextClearance()
        composeTestRule.onNodeWithTag("routine_set_kg_0_0").performTextInput("135")
        composeTestRule.waitForIdle()

        assertEquals(61.23, persistedMatrix().single().sets[0].kg, 0.0)
        composeTestRule.onNodeWithTag("routine_set_kg_0_0")
            .assertEditableText("135")
    }

    @Test
    fun routineHeaderAndTonnage_imperialShowLb() {
        UnitSettings.write(InstrumentationRegistry.getInstrumentation().targetContext, UnitSystem.IMPERIAL)
        openRoutine()

        composeTestRule.onNodeWithText("132.3 lb", substring = true).assertExists()

        composeTestRule.onNodeWithTag("routine_set_done_0_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        // One 60 kg × 5 set = 300 kg = 661.4 lb of tonnage.
        composeTestRule.onNodeWithText("Tonnage: 661.4 lb").assertExists()
    }
}