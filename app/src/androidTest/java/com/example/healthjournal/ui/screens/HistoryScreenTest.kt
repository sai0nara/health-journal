package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

@Feature("History")
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : IJournalViewModel {
        override val allEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
        override val isUserSignedIn = MutableStateFlow(false)
        override val syncStatus = MutableStateFlow<String?>(null)
        
        var syncNowCalled = false

        override fun addEntry(description: String, timestamp: Long) {}
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {
            syncNowCalled = true
        }
        override fun signOut() {}
    }

    private val viewModel = MockJournalViewModel()

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
                    onAddEntryClick = {}
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
                    onAddEntryClick = { addEntryClicked = true }
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
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
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
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
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
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_sync_status")
        }

        step("Verify sync status text is displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_sync_status_shown")
            composeTestRule.onNodeWithText(status).assertIsDisplayed()
        }
    }

    @Test
    fun testHistoryScreen_SyncButtonClickTriggersViewModel() {
        step("Set signed in state and open History Screen") {
            viewModel.isUserSignedIn.value = true
            composeTestRule.setContent {
                HistoryScreen(viewModel = viewModel, onAddEntryClick = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Click Sync button") {
            composeTestRule.onNodeWithContentDescription("Sync Now").performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("sync_clicked")
        }

        step("Verify syncNow was called") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_sync_triggered")
            assert(viewModel.syncNowCalled)
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
