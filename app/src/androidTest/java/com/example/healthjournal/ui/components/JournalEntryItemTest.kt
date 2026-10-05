package com.example.healthjournal.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class JournalEntryItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entryItem_rendersTagChipsAndReportsTap() {
        var tapped: String? = null
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Evening walk"),
                    onClick = {},
                    onPhotoClick = {},
                    tags = listOf("recovery", "Fitness"),
                    onTagClick = { tapped = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_tag_recovery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_tag_Fitness").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_tag_recovery").performClick()
        assertEquals("recovery", tapped)
    }

    @Test
    fun entryItem_noTags_rendersNoTagRow() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Plain entry"),
                    onClick = {},
                    onPhotoClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_tag_row").assertDoesNotExist()
    }
}
