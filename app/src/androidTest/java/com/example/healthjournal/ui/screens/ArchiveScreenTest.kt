package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.MainActivity
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
import android.content.Context

@Feature("Archive")
class ArchiveScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    @org.junit.Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("TEST_MODE", true)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val archivedEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val reactiveArchivedEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        override val searchQuery = MutableStateFlow("")
        override val archiveSearchQuery = MutableStateFlow("")
        override val isAscending = MutableStateFlow(false)
        
        var restoredEntryId: String? = null
        var deletedEntriesIds: List<String>? = null
        var emptyArchiveCalled = false

        override fun addEntry(
            description: String, timestamp: Long, photoUrls: List<String>, attachments: List<AttachmentData>,
            bpSystolic: Double?, bpDiastolic: Double?, heartRate: Int?, sleepHours: Float?
        ) {}
        override fun updateEntry(entry: JournalEntry) {}
        override suspend fun getEntryById(entryId: String): JournalEntry? = null
        
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {}
        override fun signOut() {}
        override fun setSearchQuery(query: String) {}
        override fun setArchiveSearchQuery(query: String) {
            archiveSearchQuery.value = query
        }
        override fun setSortOrder(isAsc: Boolean) {}

        override val healthPermissions: Set<String> = emptySet()
        override suspend fun hasHealthPermissions(): Boolean = false
        override fun checkHealthAvailability(): Int = 1
        override suspend fun syncHealthData(timestamp: Long): com.example.healthjournal.viewmodel.HealthSyncResult = 
            com.example.healthjournal.viewmodel.HealthSyncResult()

        override fun archiveEntry(entryId: String) {}
        override fun restoreEntry(entryId: String) {
            restoredEntryId = entryId
        }
        override fun deleteEntries(entryIds: List<String>) {
            deletedEntriesIds = entryIds
        }
        override fun emptyArchive() {
            emptyArchiveCalled = true
        }
        override suspend fun savePersistentFile(uri: android.net.Uri, isPhoto: Boolean): String? = null
    }

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
            composeTestRule.onNodeWithTag("archive_entry_1").performTouchInput { longClick() }
            composeTestRule.waitForIdle()
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
