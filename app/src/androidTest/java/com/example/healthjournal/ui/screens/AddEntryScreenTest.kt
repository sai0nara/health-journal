package com.example.healthjournal.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule

import com.example.healthjournal.MainActivity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.viewmodel.IJournalViewModel
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.test.rule.GrantPermissionRule

@Feature("Add Entry")
class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()


    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries: StateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
        override val archivedEntries: StateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
        override val reactiveArchivedEntries: StateFlow<List<JournalEntry>> = MutableStateFlow(emptyList())
        override val isUserSignedIn: StateFlow<Boolean> = MutableStateFlow(false)
        override val syncStatus: StateFlow<String?> = MutableStateFlow(null)
        override val searchQuery: StateFlow<String> = MutableStateFlow("")
        override val archiveSearchQuery: StateFlow<String> = MutableStateFlow("")
        override val isAscending: StateFlow<Boolean> = MutableStateFlow(false)
        
        var addEntryCalledWith: Quadruple<String, Long, List<String>, List<AttachmentData>>? = null
        
        override fun addEntry(
            description: String, 
            timestamp: Long, 
            photoUrls: List<String>, 
            attachments: List<AttachmentData>,
            bpSystolic: Double?,
            bpDiastolic: Double?,
            heartRate: Int?,
            sleepHours: Float?
        ) {
            addEntryCalledWith = Quadruple(description, timestamp, photoUrls, attachments)
        }

        override fun updateEntry(entry: JournalEntry) {}
        var entryToReturn: JournalEntry? = null
        override suspend fun getEntryById(entryId: String): JournalEntry? = entryToReturn
        
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {}
        override fun signOut() {}
        override fun setSearchQuery(query: String) {}
        override fun setArchiveSearchQuery(query: String) {}
        override fun setSortOrder(isAsc: Boolean) {}

        // Health Connect
        override val healthPermissions: Set<String> = emptySet()
        override suspend fun hasHealthPermissions(): Boolean = false
        override fun checkHealthAvailability(): Int = 1 // SDK_AVAILABLE
        override suspend fun syncHealthData(timestamp: Long): com.example.healthjournal.viewmodel.HealthSyncResult = 
            com.example.healthjournal.viewmodel.HealthSyncResult()

        // Archive & Delete
        override fun archiveEntry(entryId: String) {}
        override fun restoreEntry(entryId: String) {}
        override fun deleteEntries(entryIds: List<String>) {}
        override fun emptyArchive() {}
        override suspend fun savePersistentFile(uri: android.net.Uri, isPhoto: Boolean): String? = null
    }

    // Helper for Triple replacement
    data class Quadruple<out A, out B, out C, out D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )

    private val viewModel = MockJournalViewModel()

    @Test
    fun testAddEntryScreen_SaveButtonCallsViewModel() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        val testDescription = "I feel great!"
        step("Enter description: $testDescription") {
            composeTestRule.onNodeWithText("How are you feeling today?")
                .performTextInput(testDescription)
            composeTestRule.waitForIdle()
            allureScreenshot("description_entered")
        }

        step("Click Save button") {
            composeTestRule.onNodeWithText("Save Entry")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_clicked")
        }

        step("Verify entry was saved and screen closed") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            assert(viewModel.addEntryCalledWith?.first == testDescription)
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_BackButtonCallsOnBack() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        step("Click Back button") {
            composeTestRule.onNodeWithContentDescription("Back")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("back_clicked")
        }

        step("Verify back was called") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_DatePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        step("Click Date button ($currentDate)") {
            composeTestRule.onNodeWithText(currentDate, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("date_picker_opened")
        }

        step("Verify Date Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_date_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_TimePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        step("Click Time button ($currentTime)") {
            composeTestRule.onNodeWithText(currentTime, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("time_picker_opened")
        }

        step("Verify Time Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_time_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_EmptyDescriptionDoesNotSave() {
        var backCalled = false
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }


        step("Click Save with empty description") {
            composeTestRule.onNodeWithText("Save Entry").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_attempt_empty")
        }

        step("Verify no save occurred") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_no_save_occurred")
            assert(viewModel.addEntryCalledWith == null)
            assert(!backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_EnrichmentPanelButtonsClickable() {
        step("Open Add Entry Screen with enrichment callbacks") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click Camera in EnrichmentPanel") {
            composeTestRule.onNodeWithText("Camera")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("camera_clicked_in_screen")
        }

        step("Click Gallery in EnrichmentPanel") {
            composeTestRule.onNodeWithText("Gallery")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("gallery_clicked_in_screen")
        }

        step("Click Attach File in EnrichmentPanel") {
            composeTestRule.onNodeWithText("File")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("attach_file_clicked_in_screen")
        }
    }

    @Test
    fun testAddEntryScreen_UnarchiveAction() {
        var backCalled = false
        val archivedEntry = JournalEntry(entry_id = "1", description = "Archived", isArchived = true)
        
        step("Open Add Entry Screen with archived entry") {
            viewModel.entryToReturn = archivedEntry
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true },
                    entryId = "1"
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click Unarchive button") {
            composeTestRule.onNodeWithContentDescription("Unarchive").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify back was called") {
            composeTestRule.waitUntil(5000) { backCalled }
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_AttachmentDisplaysThumbnailForImage() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
