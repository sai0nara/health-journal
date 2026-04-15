package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.viewmodel.JournalViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel: JournalViewModel = mockk(relaxed = true)

    @Test
    fun testHistoryScreen_DisplaysEntries() {
        val entries = listOf(
            JournalEntry(description = "Morning jog"),
            JournalEntry(description = "Healthy lunch")
        )
        val entriesFlow = MutableStateFlow(entries)
        every { viewModel.allEntries } returns entriesFlow

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
        every { viewModel.allEntries } returns MutableStateFlow(emptyList())

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
