package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.util.FakeJournalViewModel
import com.example.healthjournal.viewmodel.BodyMeasurementViewModel
import io.mockk.coVerify
import io.mockk.mockk
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import org.junit.Rule
import org.junit.Test

@Feature("Body Measurements Capture")
class MeasurementEntrySheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    private val measurementRepository: BodyMeasurementRepository = mockk(relaxed = true)

    private val measurementViewModel = BodyMeasurementViewModel(measurementRepository)

    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }

    private fun setContent(darkTheme: Boolean = false) {
        composeTestRule.setContent {
            com.example.healthjournal.ui.theme.HealthJournalTheme(darkTheme = darkTheme) {
                HistoryScreen(
                    viewModel = FakeJournalViewModel(),
                    measurementViewModelFactory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            measurementViewModel as T
                    },
                    onAddEntryClick = {},
                    onEntryClick = {},
                    onArchiveClick = {},
                    onExportClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun openSheet(darkTheme: Boolean = false) {
        setContent(darkTheme)
        step("Open the measurement capture sheet via the tape-measure FAB") {
            composeTestRule
                .onNodeWithContentDescription("Add body measurements")
                .performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun captureSheet_rendersUnderLightPalette() {
        openSheet(darkTheme = false)

        step("Verify sheet renders under light palette") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Body measurements").assertExists()
            composeTestRule.onNodeWithTag("bm_save").assertExists()
        }
    }

    @Test
    fun captureSheet_rendersUnderDarkPalette() {
        openSheet(darkTheme = true)

        step("Verify sheet renders under dark palette") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Body measurements").assertExists()
            composeTestRule.onNodeWithTag("bm_save").assertExists()
        }
    }

    @Test
    fun secondaryFab_opensMeasurementSheet() {
        openSheet()

        step("Verify sheet content appears") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Body measurements").assertExists()
            composeTestRule.onNodeWithTag("bm_field_WAIST").assertExists()
            composeTestRule.onNodeWithTag("bm_save").assertIsNotEnabled()
        }
    }

    @Test
    fun partialEntry_savesAndDismissesSheet() {
        openSheet()

        step("Enter waist only and save") {
            composeTestRule.onNodeWithTag("bm_field_WAIST").performTextInput("85")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("bm_save").assertIsEnabled()
            composeTestRule.onNodeWithTag("bm_save").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify insert persisted and sheet dismissed") {
            coVerify {
                measurementRepository.insert(
                    match { it.waist_cm == 85.0 && it.weight_kg == null }
                )
            }
            composeTestRule.onNodeWithTag("bm_field_WAIST").assertDoesNotExist()
        }
    }

    @Test
    fun invalidInput_showsInlineErrorAndRetainsTypedValue() {
        openSheet()

        step("Enter malformed weight") {
            composeTestRule.onNodeWithTag("bm_field_WEIGHT").performTextInput("abc")
            composeTestRule.waitForIdle()
        }

        step("Verify inline error, retained text and disabled save") {
            composeTestRule.onNodeWithText("Invalid decimal format").assertExists()
            composeTestRule.onNodeWithTag("bm_save").assertIsNotEnabled()
            composeTestRule.onNodeWithText("abc").assertExists()
        }
    }

    @Test
    fun typedValue_survivesSheetReopen() {
        openSheet()
        step("Type value then close the sheet") {
            composeTestRule.onNodeWithTag("bm_field_WAIST").performTextInput("86.5")
            composeTestRule.waitForIdle()
            composeTestRule
                .onNodeWithContentDescription("Close measurements sheet")
                .performClick()
            composeTestRule.waitForIdle()
        }

        step("Reopen and verify state kept") {
            composeTestRule
                .onNodeWithContentDescription("Add body measurements")
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("86.5").assertExists()
        }
    }
}
