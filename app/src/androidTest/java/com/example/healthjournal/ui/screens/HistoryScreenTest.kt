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
import org.junit.Rule
import org.junit.Test
import android.app.PendingIntent
import android.content.Context

@Feature("History")
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        override val searchQuery = MutableStateFlow("")
        override val isAscending = MutableStateFlow(false)
        
        var syncNowCalled = false

        override fun addEntry(
            description: String, 
            timestamp: Long, 
            photoUrls: List<String>, 
            attachments: List<AttachmentData>,
            bpSystolic: Double?,
            bpDiastolic: Double?,
            heartRate: Int?,
            sleepHours: Float?
        ) {}
        override fun updateEntry(entry: JournalEntry) {}
        override suspend fun getEntryById(entryId: String): JournalEntry? = null
        
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {
            syncNowCalled = true
        }
        override fun signOut() {}
        override fun setSearchQuery(query: String) { searchQuery.value = query }
        override fun setSortOrder(isAsc: Boolean) { isAscending.value = isAsc }

        // Health Connect
        override val healthPermissions: Set<String> = emptySet()
        override suspend fun hasHealthPermissions(): Boolean = false
        override fun checkHealthAvailability(): Int = 1 // SDK_AVAILABLE
        override suspend fun syncHealthData(timestamp: Long): com.example.healthjournal.viewmodel.HealthSyncResult = 
            com.example.healthjournal.viewmodel.HealthSyncResult()
    }

    private val viewModel = MockJournalViewModel()

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }

    @Test
    fun testHistoryScreen_DisplaysEntries() {
        val entries = listOf(
            JournalEntry(description = "Morning jog"),
            JournalEntry(description = "Healthy lunch")
        )
        
        step("Prepare entries and open History Screen") {
            viewModel.allEntries.value = entries

            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_screen_with_entries")
        }

        step("Verify that entries are displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_entries_displayed")
            composeTestRule.onNodeWithText("Morning jog").assertExists()
            composeTestRule.onNodeWithText("Healthy lunch").assertExists()
        }
    }

    @Test
    fun testHistoryScreen_FabCallsOnAddEntryClick() {
        var addEntryClicked = false

        step("Open History Screen") {
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel,
                    onAddEntryClick = { addEntryClicked = true },
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_screen_opened")
        }

        step("Click the Add Entry FAB") {
            composeTestRule.onNodeWithContentDescription("Add Entry")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("fab_clicked")
        }

        step("Verify onAddEntryClick was called") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_fab_click_success")
            assert(addEntryClicked)
        }
    }

    @Test
    fun testHistoryScreen_SignInButtonShownWhenLoggedOut() {
        step("Set signed out state and open History Screen") {
            viewModel.isUserSignedIn.value = false
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_signed_out")
        }

        step("Verify Sign In button is displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_sign_in_shown")
            composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        }
    }

    @Test
    fun testHistoryScreen_SyncButtonShownWhenLoggedIn() {
        step("Set signed in state and open History Screen") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_signed_in")
        }

        step("Verify Sync button is displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_sync_button_shown")
            composeTestRule.onNodeWithContentDescription("Sync Now").assertIsDisplayed()
        }
    }

    @Test
    fun testHistoryScreen_SyncStatusDisplayed() {
        val status = "Syncing with Google Drive..."
        step("Set sync status and open History Screen") {
            viewModel.syncStatus.value = status
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_sync_status")
        }

        step("Verify sync status text is displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_sync_status_shown")
            composeTestRule.onNodeWithText(status, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun testHistoryScreen_AboutDialogOpens() {
        step("Open History Screen") {
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel,
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click About App icon") {
            composeTestRule.onNodeWithContentDescription("About App")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("about_dialog_opened")
        }

        step("Verify About dialog content") {
            composeTestRule.onNodeWithText("About Health Journal", substring = true).assertIsDisplayed()
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }
}
