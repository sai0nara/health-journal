package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.viewmodel.JournalViewModel
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val viewModel: JournalViewModel = mockk(relaxed = true)

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
        composeTestRule.onNodeWithText("How are you feeling today?")
            .performTextInput("I feel great!")

        // Click the save button
        composeTestRule.onNodeWithText("Save Entry")
            .performClick()

        // Verify viewModel.addEntry was called
        coVerify { viewModel.addEntry("I feel great!") }
        
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
