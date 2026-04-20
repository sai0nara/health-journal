package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.local.JournalEntry
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

@Feature("Add Entry")
class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries: StateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
        override val isUserSignedIn: StateFlow<Boolean> = MutableStateFlow(false)
        override val syncStatus: StateFlow<String?> = MutableStateFlow(null)
        
        var addEntryCalledWith: Pair<String, Long>? = null
        
        override fun addEntry(description: String, timestamp: Long) {
            addEntryCalledWith = description to timestamp
        }
        
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {}
        override fun signOut() {}
    }

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
            allureScreenshot("add_entry_screen_opened")
        }

        val testDescription = "I feel great!"
        step("Enter description: $testDescription") {
            composeTestRule.onNodeWithText("How are you feeling today?")
                .performTextInput(testDescription)
            allureScreenshot("description_entered")
        }

        step("Click Save button") {
            composeTestRule.onNodeWithText("Save Entry")
                .performClick()
            allureScreenshot("save_clicked")
        }

        step("Verify entry was saved and screen closed") {
            allureScreenshot("verification_save_success")
            assert(viewModel.addEntryCalledWith?.first == testDescription)
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
            allureScreenshot("add_entry_screen_opened")
        }

        step("Click Back button") {
            composeTestRule.onNodeWithContentDescription("Back")
                .performClick()
            allureScreenshot("back_clicked")
        }

        step("Verify back was called") {
            allureScreenshot("verification_back_success")
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_DatePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
        }

        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        step("Click Date button ($currentDate)") {
            composeTestRule.onNodeWithText(currentDate).performClick()
            allureScreenshot("date_picker_opened")
        }

        step("Verify Date Picker is visible") {
            allureScreenshot("verification_date_picker_visible")
            // Material 3 DatePicker header usually contains "Select date"
            composeTestRule.onNodeWithText("SELECT DATE", ignoreCase = true).assertExists()
        }
    }

    @Test
    fun testAddEntryScreen_TimePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        step("Click Time button ($currentTime)") {
            composeTestRule.onNodeWithText(currentTime).performClick()
            allureScreenshot("time_picker_opened")
        }

        step("Verify Time Picker is visible") {
            allureScreenshot("verification_time_picker_visible")
            composeTestRule.onNodeWithText("Select Time").assertExists()
        }
    }

    @Test
    fun testAddEntryScreen_EmptyDescriptionDoesNotSave() {
        var backCalled = false
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = { backCalled = true })
            }
        }

        step("Click Save with empty description") {
            composeTestRule.onNodeWithText("Save Entry").performClick()
            allureScreenshot("save_attempt_empty")
        }

        step("Verify no save occurred") {
            allureScreenshot("verification_no_save_occurred")
            assert(viewModel.addEntryCalledWith == null)
            assert(!backCalled)
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
