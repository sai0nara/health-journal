package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.MainActivity

import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.viewmodel.IJournalViewModel
import io.qameta.allure.android.allureScreenshot
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import android.app.PendingIntent
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import androidx.test.rule.GrantPermissionRule

@Feature("Add Entry")
class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @org.junit.Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("TEST_MODE", true)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    class MockJournalViewModel : com.example.healthjournal.util.FakeJournalViewModel()

    private val viewModel = MockJournalViewModel()

    @Test
    fun testAddEntryScreen_SaveButtonCallsViewModel() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        val testDescription = "I feel great!"
        step("Enter description: $testDescription") {
            composeTestRule.onNodeWithText("How are you feeling today?")
                .performTextInput(testDescription)
            composeTestRule.waitForIdle()
            allureScreenshot("description_entered")
        }

        step("Click Save button") {
            composeTestRule.onNodeWithText("Save Entry")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_clicked")
        }

        step("Verify entry was saved and screen closed") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            // Substring check because RichTextState wraps in <p>
            assert(viewModel.addEntryCalledWith?.description?.contains(testDescription) == true)
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_BackButtonCallsOnBack() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        step("Click Back button") {
            composeTestRule.onNodeWithContentDescription("Back")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("back_clicked")
        }

        step("Verify back was called") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_DatePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        step("Click Date button ($currentDate)") {
            composeTestRule.onNodeWithText(currentDate, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("date_picker_opened")
        }

        step("Verify Date Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_date_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_TimePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        step("Click Time button ($currentTime)") {
            composeTestRule.onNodeWithText(currentTime, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("time_picker_opened")
        }

        step("Verify Time Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_time_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_EmptyDescriptionDoesNotSave() {
        var backCalled = false
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }


        step("Click Save with empty description") {
            composeTestRule.onNodeWithText("Save Entry").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_attempt_empty")
        }

        step("Verify no save occurred") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_no_save_occurred")
            assert(viewModel.addEntryCalledWith == null)
            assert(!backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_EnrichmentPanelButtonsClickable() {
        step("Open Add Entry Screen with enrichment callbacks") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click Camera in EnrichmentPanel") {
            composeTestRule.onNodeWithText("Camera")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("camera_clicked_in_screen")
        }

        step("Click Gallery in EnrichmentPanel") {
            composeTestRule.onNodeWithText("Gallery")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("gallery_clicked_in_screen")
        }

        step("Click Attach File in EnrichmentPanel") {
            composeTestRule.onNodeWithText("File")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("attach_file_clicked_in_screen")
        }
    }

    @Test
    fun testAddEntryScreen_UnarchiveAction() {
        val archivedEntry = JournalEntry(entry_id = "1", description = "Archived", isArchived = true)
        
        step("Open Add Entry Screen with archived entry") {
            viewModel.entryToReturn = archivedEntry
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { },
                    entryId = "1"
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click Unarchive button") {
            composeTestRule.onNodeWithContentDescription("Unarchive").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify toolbar is now visible (in Edit mode)") {
            // "header_button" is a tag in RichTextToolbar
            composeTestRule.onNodeWithTag("header_button").assertExists()
        }
    }

    @Test
    fun testAddEntryScreen_AttachmentDisplaysThumbnailForImage() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
