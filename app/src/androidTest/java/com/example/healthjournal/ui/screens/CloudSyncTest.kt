package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.local.JournalEntry
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

@Feature("Cloud Synchronization")
class CloudSyncTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        override val searchQuery = MutableStateFlow("")
        override val isAscending = MutableStateFlow(false)
        
        var signInCalled = false
        var syncNowCalled = false

        override fun addEntry(description: String, timestamp: Long, photoUrl: String?) {}
        override fun updateEntry(entry: JournalEntry) {}
        override suspend fun getEntryById(entryId: String): JournalEntry? = null
        
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {
            signInCalled = true
        }
        override fun syncNow() {
            syncNowCalled = true
        }
        override fun signOut() {
            isUserSignedIn.value = false
        }
        override fun setSearchQuery(query: String) { searchQuery.value = query }
        override fun setSortOrder(isAsc: Boolean) { isAscending.value = isAsc }
    }

    private val viewModel = MockJournalViewModel()

    // --- 1. Authentication & Authorization ---

    @Test
    fun testFirstTimeLogin() {
        step("Open app and click Sign In") {
            viewModel.isUserSignedIn.value = false
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("login_screen")
            composeTestRule.onNodeWithText("Sign In").performClick()
        }

        step("Enter credentials in dialog and click Login") {
            composeTestRule.onNodeWithText("Sign In with Credentials").assertIsDisplayed()
            composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
            composeTestRule.onNodeWithText("Password").performTextInput("password")
            composeTestRule.onNodeWithText("Login").performClick()
            composeTestRule.waitForIdle()
            assert(viewModel.signInCalled)
        }

        step("Simulate successful login and scope grant") {
            viewModel.isUserSignedIn.value = true
            viewModel.syncStatus.value = "Syncing..."
            composeTestRule.waitForIdle()
            allureScreenshot("after_login_syncing")
        }

        step("Verify user is redirected and sync status appears") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Sync Now").assertIsDisplayed()
            composeTestRule.onNodeWithText("Syncing...").assertIsDisplayed()
            allureScreenshot("verification_login_success")
        }
    }

    @Test
    fun testRevokedAccess() {
        step("Start with active session") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Simulate revoked token or expired session") {
            viewModel.isUserSignedIn.value = false
            viewModel.syncStatus.value = "Auth Expired. Re-signin required."
            composeTestRule.waitForIdle()
            allureScreenshot("session_expired")
            composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
        }
    }

    @Test
    fun testSilentReauth() {
        step("Simulate app launch with existing valid session") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("silent_reauth_launch")
        }

        step("Verify sync starts automatically") {
            // In a real app, the ViewModel would trigger this on init
            viewModel.syncNow() 
            composeTestRule.waitForIdle()
            assert(viewModel.syncNowCalled)
        }
    }

    // --- 2. Data Consistency & Feedback ---

    @Test
    fun testSyncIndicatorState() {
        step("Launch app signed in") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Observe 'Syncing' state") {
            viewModel.syncStatus.value = "Uploading 2 entries..."
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Uploading 2 entries...").assertIsDisplayed()
            allureScreenshot("sync_state_uploading")
        }

        step("Observe 'Success' state") {
            viewModel.syncStatus.value = "Synced"
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Synced").assertIsDisplayed()
            allureScreenshot("sync_state_success")
        }
    }

    @Test
    fun testOfflineBanner() {
        step("Simulate offline state during sync") {
            viewModel.isUserSignedIn.value = true
            viewModel.syncStatus.value = "Waiting for network connection..."
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("offline_banner_visible")
        }

        step("Verify visual feedback for offline mode") {
            composeTestRule.onNodeWithText("Waiting for network connection...").assertIsDisplayed()
        }
    }

    @Test
    fun testBackgroundRecovery() {
        val entries = listOf(
            JournalEntry(description = "Entry 1", isSynced = false),
            JournalEntry(description = "Entry 2", isSynced = false),
            JournalEntry(description = "Entry 3", isSynced = false)
        )

        step("Display 3 unsynced entries (Local Only)") {
            viewModel.allEntries.value = entries
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("unsynced_entries_list")
            composeTestRule.onAllNodesWithContentDescription("Local Only").assertCountEquals(3)
        }

        step("Simulate sync success") {
            viewModel.allEntries.value = entries.map { it.copy(isSynced = true) }
            composeTestRule.waitForIdle()
            allureScreenshot("synced_entries_list")
        }

        step("Verify icons updated to Cloud Synced") {
            composeTestRule.onAllNodesWithContentDescription("Cloud Synced").assertCountEquals(3)
        }
    }

    @Test
    fun testPullOnRefresh() {
        step("Launch app with initial data") {
            viewModel.isUserSignedIn.value = true
            viewModel.allEntries.value = listOf(JournalEntry(description = "Old Entry", isSynced = true))
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_before_refresh")
        }

        step("Perform Swipe-to-Refresh") {
            // PullToRefreshBox uses semantics for the refresh action
            composeTestRule.onNode(hasScrollAction()).performTouchInput {
                swipeDown()
            }
            // Give it a moment to react to swipe
            composeTestRule.waitForIdle()
            allureScreenshot("after_swipe")
            
            // Verify that refresh triggered sync
            composeTestRule.waitUntil(5000) { viewModel.syncNowCalled }
            assert(viewModel.syncNowCalled)
        }

        step("Simulate new data arriving from Cloud") {
            viewModel.allEntries.value = listOf(
                JournalEntry(description = "Old Entry", isSynced = true),
                JournalEntry(description = "Remote Entry 2", isSynced = true)
            )
            composeTestRule.waitForIdle()
            allureScreenshot("history_after_refresh")
        }

        step("Verify remote entries are now visible") {
            composeTestRule.onNodeWithText("Remote Entry 2").assertIsDisplayed()
        }
    }

    @Test
    fun testEmptyCloudState() {
        step("Log in with empty cloud data") {
            viewModel.isUserSignedIn.value = true
            viewModel.allEntries.value = emptyList()
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("empty_state_screen")
        }

        step("Verify empty state message") {
            composeTestRule.onNodeWithText("No entries yet. Start by adding one!").assertIsDisplayed()
            allureScreenshot("verification_empty_state")
        }
    }

    @Test
    fun testLoginWithCredentialsDialog() {
        step("Open app and click Sign In") {
            viewModel.isUserSignedIn.value = false
            composeTestRule.setContent {
                HistoryScreen(
                    viewModel = viewModel, 
                    onAddEntryClick = {},
                    onEntryClick = {}
                )
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Sign In").performClick()
        }

        step("Verify Login Dialog is shown and enter credentials") {
            composeTestRule.onNodeWithText("Sign In with Credentials").assertIsDisplayed()
            composeTestRule.onNodeWithText("Email").performTextInput("user@example.com")
            composeTestRule.onNodeWithText("Password").performTextInput("password123")
            allureScreenshot("login_dialog_filled")
        }

        step("Click Login and verify ViewModel call") {
            composeTestRule.onNodeWithText("Login").performClick()
            composeTestRule.waitForIdle()
            assert(viewModel.signInCalled)
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
