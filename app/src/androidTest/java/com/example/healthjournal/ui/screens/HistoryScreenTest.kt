package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.viewmodel.IJournalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import android.app.PendingIntent
import android.content.Context

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        
        override fun addEntry(description: String, timestamp: Long) {}
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {}
        override fun signOut() {}
    }

    private val viewModel = MockJournalViewModel()

    @Test
    fun testHistoryScreen_DisplaysEntries() {
        val entries = listOf(
            JournalEntry(description = "Morning jog"),
            JournalEntry(description = "Healthy lunch")
        )
        viewModel.allEntries.value = entries

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onAddEntryClick = {}
            )
        }

        // Verify that entries are displayed
        composeTestRule.onNodeWithText("Morning jog").assertExists()
        composeTestRule.onNodeWithText("Healthy lunch").assertExists()
    }

    @Test
    fun testHistoryScreen_FabCallsOnAddEntryClick() {
        var addEntryClicked = false

        composeTestRule.setContent {
            HistoryScreen(
                viewModel = viewModel,
                onAddEntryClick = { addEntryClicked = true }
            )
        }

        // Click the FAB
        composeTestRule.onNodeWithContentDescription("Add Entry")
            .performClick()

        // Verify onAddEntryClick was called
        assert(addEntryClicked)
    }
}
