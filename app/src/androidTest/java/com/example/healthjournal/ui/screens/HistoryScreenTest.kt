package com.example.healthjournal.ui.screens

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
        
        override fun addEntry(description: String, timestamp: Long) {}
        override fun signIn(activityContext: Context, onResolutionRequired: (PendingIntent) -> Unit) {}
        override fun syncNow() {}
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
            allureScreenshot("history_screen_with_entries")
        }

        step("Verify that entries are displayed") {
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
            allureScreenshot("history_screen_opened")
        }

        step("Click the Add Entry FAB") {
            composeTestRule.onNodeWithContentDescription("Add Entry")
                .performClick()
            allureScreenshot("fab_clicked")
        }

        step("Verify onAddEntryClick was called") {
            assert(addEntryClicked)
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
