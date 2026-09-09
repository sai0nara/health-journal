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
import com.example.healthjournal.util.FakeWorkoutHealthDataSource
import com.example.healthjournal.util.FakeWorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutStatus
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
}
