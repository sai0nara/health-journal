package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.viewmodel.IJournalViewModel
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import android.app.PendingIntent
import android.content.Context

@Feature("Add Entry")
class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.END)

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
        
        composeTestRule.setContent {
            AddEntryScreen(
                viewModel = viewModel,
                onBack = { backCalled = true }
            )
        }

        // Fill in the description
        val testDescription = "I feel great!"
        composeTestRule.onNodeWithText("How are you feeling today?")
            .performTextInput(testDescription)

        // Click the save button
        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        // Verify viewModel.addEntry was called
        assert(viewModel.addEntryCalledWith?.first == testDescription)
        
        // Verify onBack was called
        assert(backCalled)
    }

    @Test
    fun testAddEntryScreen_BackButtonCallsOnBack() {
        var backCalled = false
        
        composeTestRule.setContent {
            AddEntryScreen(
                viewModel = viewModel,
                onBack = { backCalled = true }
            )
        }

        // Click the back button
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()

        // Verify onBack was called
        assert(backCalled)
    }
}
