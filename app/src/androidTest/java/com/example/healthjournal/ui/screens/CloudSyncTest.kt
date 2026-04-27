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

@Feature("Cloud Backbone")
class CloudSyncTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        
        var signInCalled = false
        var syncNowCalled = false

        override fun addEntry(description: String, timestamp: Long) {}
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {
            signInCalled = true
        }
        override fun syncNow() {
            syncNowCalled = true
        }
        override fun signOut() {
            isUserSignedIn.value = false
        }
    }

    private val viewModel = MockJournalViewModel()

    // --- 1. Authentication & Authorization ---

    @Test
    fun testFirstTimeLogin() {
        step("Open app and click Sign In") {
            viewModel.isUserSignedIn.value = false
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("login_screen")
            composeTestRule.onNodeWithText("Sign In").performClick()
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
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Simulate revoked access/expired token") {
            viewModel.isUserSignedIn.value = false
            viewModel.syncStatus.value = "Session expired. Please sign in again."
            // Wait for recomposition
            composeTestRule.waitForIdle()
            allureScreenshot("access_revoked")
        }

        step("Verify sign-in required UI is shown") {
            // Wait for recomposition to ensure Sign In button replaces Sync button
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Sign In").fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Sign In").assertIsDisplayed()
            composeTestRule.onNodeWithText("Session expired", substring = true).assertIsDisplayed()
            allureScreenshot("verification_revoked_access_ui")
        }
    }

    @Test
    fun testSilentReauth() {
        step("Simulate app launch with existing valid session") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("silent_reauth_launch")
        }

        step("Verify Home Screen is shown directly without Login UI") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Sign In").assertDoesNotExist()
            composeTestRule.onNodeWithContentDescription("Sync Now").assertIsDisplayed()
            allureScreenshot("verification_silent_reauth")
        }
    }

    // --- 2. Synchronization Feedback ---

    @Test
    fun testSyncIndicatorState() {
        step("Launch app signed in") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Trigger sync and observe 'Syncing' state") {
            viewModel.syncStatus.value = "Syncing with Google Drive..."
            composeTestRule.waitForIdle()
            allureScreenshot("state_syncing")
            composeTestRule.onNodeWithText("Syncing with Google Drive...").assertIsDisplayed()
        }

        step("Complete sync and observe 'Synced' state") {
            viewModel.syncStatus.value = "All changes synced"
            composeTestRule.waitForIdle()
            allureScreenshot("state_synced")
            composeTestRule.onNodeWithText("All changes synced").assertIsDisplayed()
        }
    }

    @Test
    fun testOfflineBanner() {
        step("Simulate offline state during sync") {
            viewModel.isUserSignedIn.value = true
            viewModel.syncStatus.value = "Waiting for network connection..."
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("offline_banner_visible")
        }

        step("Verify offline banner text") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("Waiting for network", substring = true).assertIsDisplayed()
            allureScreenshot("verification_offline_banner")
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
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("unsynced_entries_list")
            composeTestRule.onAllNodesWithContentDescription("Local Only").assertCountEquals(3)
        }

        step("Simulate background sync completion") {
            viewModel.allEntries.value = entries.map { it.copy(isSynced = true) }
            composeTestRule.waitForIdle()
            allureScreenshot("synced_entries_list")
        }

        step("Verify all entries show 'Cloud Synced' icon") {
            composeTestRule.waitForIdle()
            composeTestRule.onAllNodesWithContentDescription("Cloud Synced").assertCountEquals(3)
            allureScreenshot("verification_recovery_success")
        }
    }

    // --- 3. Data Integrity ---

    @Test
    fun testPullOnRefresh() {
        step("Launch app with initial data") {
            viewModel.isUserSignedIn.value = true
            viewModel.allEntries.value = listOf(JournalEntry(description = "Old Entry", isSynced = true))
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
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
        }

        step("Simulate new data arriving from Cloud") {
            viewModel.allEntries.value = listOf(
                JournalEntry(description = "Old Entry", isSynced = true),
                JournalEntry(description = "New Cloud Entry", isSynced = true)
            )
            composeTestRule.waitForIdle()
            allureScreenshot("history_after_refresh")
        }

        step("Verify new entry is visible") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("New Cloud Entry").assertIsDisplayed()
            allureScreenshot("verification_refresh_success")
        }
    }

    @Test
    fun testEmptyCloudState() {
        step("Log in with empty cloud data") {
            viewModel.isUserSignedIn.value = true
            viewModel.allEntries.value = emptyList()
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("empty_state_screen")
        }

        step("Verify empty state message") {
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("No entries yet", substring = true).assertIsDisplayed()
            allureScreenshot("verification_empty_state")
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
