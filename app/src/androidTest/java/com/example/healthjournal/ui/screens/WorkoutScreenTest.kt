package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.WorkoutRepository
import com.example.healthjournal.domain.WorkoutType
import com.example.healthjournal.util.FakeWorkoutHealthDataSource
import com.example.healthjournal.util.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
import com.example.healthjournal.domain.StrengthExercise
import com.example.healthjournal.domain.StrengthSet
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.WorkoutViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WorkoutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var dao: FakeWorkoutSessionDao
    private lateinit var journalRepository: JournalRepository
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setup() {
        dao = FakeWorkoutSessionDao()
        journalRepository = mockk(relaxed = true)
        coEvery { journalRepository.insert(any()) } returns Unit
        viewModel = buildViewModel()
    }

    private fun buildViewModel(): WorkoutViewModel = WorkoutViewModel(
        repository = WorkoutRepository(dao),
        healthSource = FakeWorkoutHealthDataSource(),
        journalRepository = journalRepository,
        onHaptic = {}
    )

    private fun openScreen() {
        composeTestRule.setContent {
            HealthJournalTheme {
                WorkoutScreen(viewModel = viewModel, onBack = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun openLogDialogForRun() {
        openScreen()
        composeTestRule.onNodeWithText("Run").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Log past Run instead").performClick()
        composeTestRule.waitForIdle()
    }

    /** Starts a session of [type] and runs it past the 3-2-1 countdown. */
    private fun startActiveSession(type: WorkoutType, target: String) {
        openScreen()
        composeTestRule.onNodeWithText(type.label).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("workout_target_field").performTextInput(target)
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitForIdle()
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()
    }

    /** Opens the manual-log dialog for [type] without a target. */
    private fun openLogDialogForType(type: WorkoutType) {
        openScreen()
        composeTestRule.onNodeWithText(type.label).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Log past ${type.label} instead").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun idle_showsCategoriesWithoutHubLogButton() {
        openScreen()

        composeTestRule.onNodeWithText("Run").assertExists()
        composeTestRule.onNodeWithText("Fitness").assertExists()
        composeTestRule.onNodeWithText("Yoga").assertExists()
        // Type-less logging is gone: the log dialog opens per workout type.
        composeTestRule.onNodeWithText("Log Past Workout").assertDoesNotExist()
    }

    @Test
    fun idle_catalogScrollsToReachLastActivity() {
        openScreen()

        // All ten categories must be reachable: the catalog scrolls.
        composeTestRule.onNodeWithTag("workout_catalog")
            .performScrollToNode(hasText("Calisthenics"))
        composeTestRule.onNodeWithText("Calisthenics").assertIsDisplayed()
    }

    @Test
    fun configureScreen_offersLogPastWithTypeHeader() {
        openScreen()
        composeTestRule.onNodeWithText("Run").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Log past Run instead").performClick()
        composeTestRule.waitForIdle()

        // Header names the chosen activity; no redundant type buttons.
        composeTestRule.onNodeWithText("Log Past Run").assertExists()
        composeTestRule.onNodeWithText("Fitness").assertDoesNotExist()
        composeTestRule.onNodeWithText("Yoga").assertDoesNotExist()
    }

    @Test
    fun configureRun_showsDistanceTargetLabel() {
        openScreen()
        composeTestRule.onNodeWithText("Run").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Target distance (km)").assertExists()
    }

    @Test
    fun configureYoga_showsDurationTargetLabel() {
        openScreen()
        composeTestRule.onNodeWithText("Yoga").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Target duration (min)").assertExists()
    }

    @Test
    fun configureWalkingHiking_showsDistanceTargetLabel() {
        openScreen()
        composeTestRule.onNodeWithTag("workout_catalog")
            .performScrollToNode(hasText("Walking/Hiking"))
        composeTestRule.onNodeWithText("Walking/Hiking").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Target distance (km)").assertExists()
        composeTestRule.onNodeWithText("Target duration (min)").assertDoesNotExist()
    }

    @Test
    fun selectType_showsConfiguration() {
        openScreen()
        composeTestRule.onNodeWithText("Run").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workout_target_field").assertExists()
        composeTestRule.onNodeWithText("Start").assertExists()
        composeTestRule.onNodeWithText("Log past Run instead").assertExists()
    }

    @Test
    fun startSession_showsActiveTimerAndPause() {
        openScreen()
        composeTestRule.onNodeWithText("Yoga").performClick()
        composeTestRule.onNodeWithTag("workout_target_field").performTextInput("30")
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitForIdle()

        // Wait out the 3-2-1 countdown by advancing the engine's clock.
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("workout_timer").assertExists()
        composeTestRule.onNodeWithTag("workout_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertExists()
        composeTestRule.onNodeWithText("Finish").assertExists()
    }

    @Test
    fun manualLog_invalidInput_showsError() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("abc")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Invalid decimal format").assertExists()
    }

    @Test
    fun manualLog_invalidDateFormat_showsSameErrorAsDateOfBirth() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("30")
        // Digits survive the entry mask but month 13 is not a real date.
        composeTestRule.onNodeWithTag("manual_date_field").performTextInput("20241345")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Same message as Personal Card Date of Birth validation, shown inline
        // with the dialog kept open (not via a separate error dialog).
        composeTestRule.onNodeWithText("Invalid date format").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun manualLog_futureDate_showsSameErrorAsDateOfBirth() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("30")
        composeTestRule.onNodeWithTag("manual_date_field").performTextInput("2099-01-01")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Same message as Personal Card Date of Birth validation, shown inline
        // with the dialog kept open (not via a separate error dialog).
        composeTestRule.onNodeWithText("Date cannot be in the future").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun manualLog_lettersStrippedFromDurationLikeHeightWeight() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("3a0b")
        composeTestRule.waitForIdle()

        // Same sanitize rule as Personal Card Height/Weight: digits only.
        composeTestRule.onNodeWithTag("manual_duration_field").assertTextEquals("Duration (min)", "30")
    }

    @Test
    fun manualLog_impossibleDate_rejectedLikeInvalidFormat() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("30")
        // February 30th does not exist (2023 is not a leap year).
        composeTestRule.onNodeWithTag("manual_date_field").performTextInput("20230230")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Invalid date format").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun manualLog_invalidMonthDay_rejectedLikeInvalidFormat() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("30")
        // Month 13 and day 99 do not exist on any calendar.
        composeTestRule.onNodeWithTag("manual_date_field").performTextInput("20261399")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Invalid date format").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun manualLog_durationOverCap_rejected() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("2000")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Too long (max 1440 min)").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun manualLog_caloriesOverCap_rejected() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_duration_field").performTextInput("60")
        composeTestRule.onNodeWithTag("manual_calories_field").performTextInput("99999")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Too large (max 50000 kcal)").assertExists()
        composeTestRule.onNodeWithTag("manual_date_field").assertIsDisplayed()
    }

    @Test
    fun finishSession_showsSummaryWithCalories() {
        openScreen()
        composeTestRule.onNodeWithText("Run").performClick()
        composeTestRule.onNodeWithTag("workout_target_field").performTextInput("5")
        composeTestRule.onNodeWithText("Start").performClick()
        composeTestRule.waitForIdle()

        // Clear the 3-2-1 countdown first, then run the clock.
        viewModel.advanceTime(WorkoutViewModel.COUNTDOWN_SECONDS.toLong())
        composeTestRule.waitForIdle()
        viewModel.advanceTime(1_800)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("343 kcal", substring = true).assertExists()
        composeTestRule.onNodeWithText("Done").assertExists()
    }

    @Test
    fun unfinishedSession_showsRecoveryPrompt() {
        runBlocking {
            dao.upsertSession(WorkoutSession(status = WorkoutStatus.ACTIVE.name))
        }
        viewModel = buildViewModel()
        openScreen()

        composeTestRule.onNodeWithText("Resume").assertExists()
        composeTestRule.onNodeWithText("Discard").assertExists()
    }

    @Test
    fun hiitActive_showsPhaseIndicatorAndNextIntervalControl() {
        startActiveSession(WorkoutType.HIIT, "20")

        composeTestRule.onNodeWithTag("hiit_phase").assertIsDisplayed()
        composeTestRule.onNodeWithTag("next_interval_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next interval").assertExists()
    }

    @Test
    fun hiitActive_nextInterval_advancesPhaseAndCountsRound() {
        startActiveSession(WorkoutType.HIIT, "20")

        composeTestRule.onNodeWithText("Next interval").performClick()
        composeTestRule.waitForIdle()

        // Work -> Rest crossing completes the first round.
        composeTestRule.onNodeWithText("Rest").assertExists()
        composeTestRule.onNodeWithTag("hiit_rounds").assertExists()
    }

    @Test
    fun fitnessActive_showsSetMatrixEditor() {
        startActiveSession(WorkoutType.FITNESS, "30")

        composeTestRule.onNodeWithText("Add exercise").assertExists()
        composeTestRule.onNodeWithTag("exercise_name_field").assertExists()
        composeTestRule.onNodeWithText("No exercises yet").assertExists()
    }

    @Test
    fun fitnessActive_addExerciseAndSet_showsSetWithTonnageAndRestTimer() {
        startActiveSession(WorkoutType.FITNESS, "30")

        composeTestRule.onNodeWithTag("exercise_name_field").performTextInput("Squat")
        composeTestRule.onNodeWithText("Add exercise").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Squat").assertExists()
        composeTestRule.onNodeWithTag("set_kg_field_0").performTextInput("60")
        composeTestRule.onNodeWithTag("set_reps_field_0").performTextInput("10")
        composeTestRule.onNodeWithTag("add_set_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("60 kg × 10 reps").assertExists()
        composeTestRule.onNodeWithTag("workout_rest_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("90 kg").assertDoesNotExist()
    }

    @Test
    fun fitnessActive_secondExercise_doesNotMirrorInputAcrossCards() {
        startActiveSession(WorkoutType.FITNESS, "30")

        composeTestRule.onNodeWithTag("exercise_name_field").performTextInput("Squat")
        composeTestRule.onNodeWithText("Add exercise").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_name_field").performTextInput("Bench")
        composeTestRule.onNodeWithText("Add exercise").performClick()
        composeTestRule.waitForIdle()

        // Both cards render, but only one (the active) card owns the kg/reps
        // input. A second input row would share the same text state and mirror
        // whatever is typed into the first card.
        composeTestRule.onNodeWithText("Squat").assertExists()
        composeTestRule.onNodeWithText("Bench").assertExists()
        composeTestRule.onNodeWithTag("set_kg_field_0").assertExists()
        composeTestRule.onNodeWithTag("set_kg_field_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("set_reps_field_1").assertDoesNotExist()
    }

    @Test
    fun fitnessActive_addSet_invalidValues_showsInlineError() {
        startActiveSession(WorkoutType.FITNESS, "30")

        composeTestRule.onNodeWithTag("exercise_name_field").performTextInput("Squat")
        composeTestRule.onNodeWithText("Add exercise").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("set_kg_field_0").performTextInput("0")
        composeTestRule.onNodeWithTag("add_set_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Weight must be greater than zero").assertExists()
    }

    @Test
    fun fitnessSummary_showsTonnage() {
        startActiveSession(WorkoutType.FITNESS, "30")
        composeTestRule.onNodeWithTag("exercise_name_field").performTextInput("Squat")
        composeTestRule.onNodeWithText("Add exercise").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("set_kg_field_0").performTextInput("60")
        composeTestRule.onNodeWithTag("set_reps_field_0").performTextInput("10")
        composeTestRule.onNodeWithTag("add_set_button_0").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Tonnage: 600 kg", substring = true).assertExists()
    }

    @Test
    fun HIITSummary_showsRoundsAndIntervals() {
        startActiveSession(WorkoutType.HIIT, "20")
        composeTestRule.onNodeWithText("Next interval").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Finish").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Rounds: 1", substring = true).assertExists()
        composeTestRule.onNodeWithText("Intervals: 1", substring = true).assertExists()
    }

    @Test
    fun swimmingManualLog_showsLapsField() {
        openLogDialogForType(WorkoutType.SWIMMING)

        composeTestRule.onNodeWithTag("manual_laps_field").assertExists()
    }

    @Test
    fun calisthenicsManualLog_showsMovementCountsField() {
        openLogDialogForType(WorkoutType.CALISTHENICS)

        composeTestRule.onNodeWithTag("manual_movements_field").assertExists()
    }

    @Test
    fun runManualLog_hasNoLapsOrMovementsExtras() {
        openLogDialogForRun()

        composeTestRule.onNodeWithTag("manual_laps_field").assertDoesNotExist()
        composeTestRule.onNodeWithTag("manual_movements_field").assertDoesNotExist()
    }
}
