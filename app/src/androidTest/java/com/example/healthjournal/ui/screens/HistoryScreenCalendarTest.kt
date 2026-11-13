package com.example.healthjournal.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.domain.HistoryDateFilter
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeJournalViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HistoryScreenCalendarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun calendarToggle_expandsAndCollapsesCalendarPanel() {
        val fakeViewModel = FakeJournalViewModel()

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(
                    viewModel = fakeViewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Initially collapsed
        composeTestRule.onNodeWithTag("history_calendar_panel").assertDoesNotExist()

        // Tap toggle button -> expands
        composeTestRule.onNodeWithTag("history_calendar_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_calendar_panel").assertIsDisplayed()

        // Tap toggle button again -> collapses
        composeTestRule.onNodeWithTag("history_calendar_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_calendar_panel").assertDoesNotExist()
    }

    @Test
    fun dateFilter_rendersContextualEmptyState_andClearCTA() {
        val fakeViewModel = FakeJournalViewModel()
        val targetDate = LocalDate.of(2026, 11, 10)
        fakeViewModel.setDateFilter(HistoryDateFilter.SingleDay(targetDate))
        fakeViewModel.allEntries.value = emptyList()

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(
                    viewModel = fakeViewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify contextual date empty state appears
        composeTestRule.onNodeWithTag("history_date_empty_state").assertIsDisplayed()

        // Tap Clear CTA
        composeTestRule.onNodeWithTag("clear_date_filter_cta").performClick()
        composeTestRule.waitForIdle()
        assertEquals(HistoryDateFilter.All, fakeViewModel.dateFilter.value)
    }

    @Test
    fun activeDateFilter_displaysEntries_andActiveFilterChip() {
        val fakeViewModel = FakeJournalViewModel()
        val targetDate = LocalDate.of(2026, 11, 10)
        fakeViewModel.setDateFilter(HistoryDateFilter.SingleDay(targetDate))
        fakeViewModel.allEntries.value = listOf(
            JournalEntry(entry_id = "e1", description = "Target Day Entry", timestamp = 1700000000000L)
        )

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(
                    viewModel = fakeViewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Active filter chip is rendered
        composeTestRule.onNodeWithTag("history_date_filter_chip").assertIsDisplayed()

        // Entry is displayed
        composeTestRule.onNodeWithText("Target Day Entry").assertIsDisplayed()

        // Clear icon on chip resets filter to All
        composeTestRule.onNodeWithTag("clear_date_filter_icon", useUnmergedTree = true).performClick()
        composeTestRule.waitForIdle()
        assertEquals(HistoryDateFilter.All, fakeViewModel.dateFilter.value)
    }

    @Test
    fun tc028_tc029_contextualAddEntryFab_invokedAcrossFilterModes() {
        val fakeViewModel = FakeJournalViewModel()
        var addEntryClicked = false

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(
                    viewModel = fakeViewModel,
                    onAddEntryClick = { addEntryClicked = true },
                    onEntryClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // TC-031: When dateFilter is All, chip is not displayed
        composeTestRule.onNodeWithTag("history_date_filter_chip").assertDoesNotExist()

        // Tap Add Entry FAB
        composeTestRule.onNodeWithContentDescription("Add Entry", substring = true, ignoreCase = true).performClick()
        composeTestRule.waitForIdle()
        assertTrue(addEntryClicked)
    }

    @Test
    fun calendarApplyButton_closesExpandedCalendarPanel() {
        val fakeViewModel = FakeJournalViewModel()

        composeTestRule.setContent {
            HealthJournalTheme {
                HistoryScreen(
                    viewModel = fakeViewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Expand calendar
        composeTestRule.onNodeWithTag("history_calendar_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_calendar_panel").assertIsDisplayed()

        // Tap Apply button -> collapses calendar panel
        composeTestRule.onNodeWithTag("calendar_apply_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("history_calendar_panel").assertDoesNotExist()
    }
}
