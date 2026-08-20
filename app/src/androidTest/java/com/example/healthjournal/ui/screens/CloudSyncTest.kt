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

@Feature("Cloud Synchronization")
class CloudSyncTest {

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

    class MockJournalViewModel : com.example.healthjournal.util.FakeJournalViewModel()

    private val viewModel = MockJournalViewModel()

    @Test
    fun testSignInTriggered() {
        step("Open app and click Sign In") {
            viewModel.isUserSignedIn.value = false
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {},
                        onArchiveClick = {},
                        onExportClick = {}
                    )

                }
            }
            composeTestRule.waitForIdle()
            allureScreenshot("login_screen")
            composeTestRule.onNodeWithText("Sign In").performClick()
        }

        step("Verify Sign In was called on ViewModel") {
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
