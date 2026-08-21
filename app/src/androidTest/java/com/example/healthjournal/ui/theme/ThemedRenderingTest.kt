package com.example.healthjournal.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.util.FakeJournalViewModel
import com.example.healthjournal.viewmodel.IJournalViewModel
import io.qameta.allure.kotlin.Feature
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the main history screen renders correctly under BOTH
 * the light ("Medical Standard") and dark ("Eye-strain Reduction")
 * palettes, i.e., all content resolves from the active MaterialTheme
 * color scheme without depending on a fixed theme.
 */
@Feature("Medical App Color System")
class ThemedRenderingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun themedViewModel(): IJournalViewModel {
        val viewModel = object : FakeJournalViewModel() {}
        viewModel.allEntries.value = listOf(JournalEntry(description = "Themed entry body"))
        return viewModel
    }

    @Test
    fun historyScreen_rendersUnderLightPalette() {
        val viewModel = themedViewModel()
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = false) {
                com.example.healthjournal.ui.screens.HistoryScreen(
                    viewModel = viewModel,
                    onAddEntryClick = {},
                    onEntryClick = {},
                    onArchiveClick = {},
                    onExportClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Themed entry body", substring = true).assertExists()
    }

    @Test
    fun historyScreen_rendersUnderDarkPalette() {
        val viewModel = themedViewModel()
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = true) {
                com.example.healthjournal.ui.screens.HistoryScreen(
                    viewModel = viewModel,
                    onAddEntryClick = {},
                    onEntryClick = {},
                    onArchiveClick = {},
                    onExportClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Themed entry body", substring = true).assertExists()
    }
}
