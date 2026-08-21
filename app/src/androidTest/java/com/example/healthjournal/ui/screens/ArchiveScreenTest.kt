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
}
