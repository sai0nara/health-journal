package com.example.healthjournal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthjournal.domain.DatePreset
import com.example.healthjournal.domain.HistoryDateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class HistoryCalendarPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun calendarPanel_rendersMonthAndDaysWithDots() {
        val nov2026 = YearMonth.of(2026, 11)
        val dayWithEntry = LocalDate.of(2026, 11, 10)
        val daysWithEntries = setOf(dayWithEntry)

        var selectedDate: LocalDate? = null

        composeTestRule.setContent {
            MaterialTheme {
                HistoryCalendarPanel(
                    visibleMonth = nov2026,
                    dateFilter = HistoryDateFilter.All,
                    daysWithEntries = daysWithEntries,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onDateSelected = { selectedDate = it },
                    onPresetSelected = {},
                    onClearDateFilter = {}
                )
            }
        }

        // Verify month title is displayed
        composeTestRule.onNodeWithTag("calendar_month_title").assertIsDisplayed()

        // Verify day with entry has dot
        composeTestRule.onNodeWithTag("event_dot_$dayWithEntry", useUnmergedTree = true).assertExists()

        // Verify tapping a date cell triggers callback
        composeTestRule.onNodeWithTag("calendar_day_$dayWithEntry").performClick()
        assertEquals(dayWithEntry, selectedDate)
    }

    @Test
    fun calendarPanel_presetSelection_andActiveFilterChip() {
        val nov2026 = YearMonth.of(2026, 11)
        var selectedPreset: DatePreset? = null
        var clearClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                HistoryCalendarPanel(
                    visibleMonth = nov2026,
                    dateFilter = HistoryDateFilter.SingleDay(LocalDate.of(2026, 11, 10)),
                    daysWithEntries = emptySet(),
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onDateSelected = {},
                    onPresetSelected = { selectedPreset = it },
                    onClearDateFilter = { clearClicked = true }
                )
            }
        }

        // Tap Today preset chip
        composeTestRule.onNodeWithTag("preset_chip_TODAY").performClick()
        assertEquals(DatePreset.TODAY, selectedPreset)

        // Verify active filter chip exists and clear can be tapped
        composeTestRule.onNodeWithTag("history_date_filter_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_date_filter_icon", useUnmergedTree = true).performClick()
        assertTrue(clearClicked)
    }
}
