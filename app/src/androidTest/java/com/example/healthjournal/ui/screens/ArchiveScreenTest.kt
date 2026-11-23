package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.viewmodel.IJournalViewModel
import com.example.healthjournal.ui.theme.HealthJournalTheme
import io.qameta.allure.android.allureScreenshot
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import android.app.PendingIntent

@Feature("Archive")
class ArchiveScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : com.example.healthjournal.util.FakeJournalViewModel()

    private val viewModel = MockJournalViewModel()

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }

    @Test
    fun testArchiveScreen_BatchDeletionFlow() {
        val entry1 = JournalEntry(entry_id = "1", description = "Archived 1", isArchived = true)
        val entry2 = JournalEntry(entry_id = "2", description = "Archived 2", isArchived = true)
        
        step("Open Archive Screen with multiple entries") {
            viewModel.reactiveArchivedEntries.value = listOf(entry1, entry2)
            composeTestRule.setContent {
                HealthJournalTheme {
                    ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
                }
            }
            composeTestRule.waitForIdle()
            allureScreenshot("archive_screen_batch_delete_start")
        }

        step("Long press entry 1 to enter selection mode") {
            composeTestRule.onNodeWithTag("archive_entry_1").performTouchInput {
                down(center)
            }
            composeTestRule.mainClock.advanceTimeBy(1000)
            composeTestRule.onNodeWithTag("archive_entry_1").performTouchInput {
                up()
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("1 Selected").assertExists()
        }

        step("Select entry 2") {
            composeTestRule.onNodeWithTag("archive_entry_2").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("archive_screen_both_selected")
        }

        step("Click Delete Selected and Confirm") {
            composeTestRule.onNodeWithContentDescription("Delete Selected").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("archive_screen_delete_confirm_dialog")
            
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()
        }
        
        step("Verify entries were deleted") {
            assert(viewModel.deletedEntriesIds?.containsAll(listOf("1", "2")) == true)
        }
    }

    @Test
    fun testArchiveScreen_EmptyArchiveFlow() {
        val entry1 = JournalEntry(entry_id = "1", description = "Archived 1", isArchived = true)
        
        step("Open Archive Screen") {
            viewModel.reactiveArchivedEntries.value = listOf(entry1)
            viewModel.archivedEntries.value = listOf(entry1)
            composeTestRule.setContent {
                HealthJournalTheme {
                    ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Click Empty Archive and Confirm") {
            composeTestRule.onNodeWithContentDescription("Empty Archive").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("archive_screen_empty_confirm_bottom_sheet")
            
            composeTestRule.onNodeWithText("Permanently Delete All").performClick()
            composeTestRule.waitForIdle()
        }
        
        step("Verify empty archive was called") {
            assert(viewModel.emptyArchiveCalled)
        }
    }

    @Test
    fun testArchiveScreen_SearchFunctionality() {
        val entry1 = JournalEntry(entry_id = "1", description = "Morning jog", isArchived = true)
        val entry2 = JournalEntry(entry_id = "2", description = "Evening walk", isArchived = true)
        
        step("Open Archive Screen") {
            viewModel.reactiveArchivedEntries.value = listOf(entry1, entry2)
            composeTestRule.setContent {
                HealthJournalTheme {
                    ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Type into search bar") {
            composeTestRule.onNodeWithText("Search archive...").performTextInput("walk")
            composeTestRule.waitForIdle()
        }
        
        step("Verify search query is updated in ViewModel") {
            assert(viewModel.archiveSearchQuery.value == "walk")
        }
    }

    @Test
    fun testArchiveScreen_selectItemsAndScroll_withMultiParagraphRichText_doesNotCrash() {
        val wrappingParagraphThenBulletHtml =
            "<p>First paragraph with plenty of words so that it wraps across at least two lines inside the journal entry card on mobile screens.</p>" +
                "<p>Second paragraph occupying the third visible line.</p>" +
                "<ul><li>Bullet item on paragraph index two beyond max lines</li></ul>"
        val wrappingBulletListHtml =
            "<ul>" +
                "<li>First bullet item with plenty of words so that it wraps across at least two lines inside the journal entry card when selection mode narrows the width</li>" +
                "<li>Second bullet item taking up the third visible line</li>" +
                "<li>Third bullet item truncated past max lines</li>" +
                "</ul>"
        val wrappingOrderedListHtml =
            "<ol>" +
                "<li>First ordered step with plenty of words so that it wraps across at least two lines inside the journal entry card on mobile screens</li>" +
                "<li>Second ordered step occupying the third visible line</li>" +
                "<li>Third ordered step truncated past max lines</li>" +
                "</ol>"

        val entries = (1..12).map { index ->
            val description = when (index % 3) {
                0 -> wrappingOrderedListHtml
                1 -> wrappingParagraphThenBulletHtml
                else -> wrappingBulletListHtml
            }
            JournalEntry(
                entry_id = index.toString(),
                description = description,
                timestamp = 1_700_000_000_000L - index * 60_000L,
                isArchived = true
            )
        }

        viewModel.reactiveArchivedEntries.value = entries
        composeTestRule.setContent {
            HealthJournalTheme {
                ArchiveScreen(viewModel = viewModel, onBack = {}, onEntryClick = {})
            }
        }
        composeTestRule.waitForIdle()

        // Long-press first item to enter selection mode, then select second item
        composeTestRule.onNodeWithTag("archive_entry_1").performTouchInput {
            down(center)
        }
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.onNodeWithTag("archive_entry_1").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("archive_entry_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 Selected").assertIsDisplayed()

        // Scroll through the list while items are selected (triggers layout at narrowed width)
        composeTestRule.onNodeWithTag("archive_list")
            .performScrollToNode(hasTestTag("archive_entry_12"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("archive_entry_12").assertIsDisplayed()

        composeTestRule.onNodeWithTag("archive_list")
            .performScrollToNode(hasTestTag("archive_entry_1"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2 Selected").assertIsDisplayed()
    }
}
