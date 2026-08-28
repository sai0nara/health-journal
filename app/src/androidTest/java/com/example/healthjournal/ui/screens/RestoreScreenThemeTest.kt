package com.example.healthjournal.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.export.RestoreResult
import com.example.healthjournal.export.RestoreUiState
import com.example.healthjournal.export.RestoreViewModel
import com.example.healthjournal.ui.theme.HealthJournalTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreScreenThemeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createHarness(initial: RestoreUiState, darkTheme: Boolean) {
        val flow = MutableStateFlow(initial)
        val viewModel = mockk<RestoreViewModel>(relaxed = true)
        every { viewModel.uiState } returns flow
        every { viewModel.selectBackup(any()) } just runs
        every { viewModel.submitPassphrase(any()) } just runs
        every { viewModel.confirmRestore() } just runs
        every { viewModel.reset() } just runs
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = darkTheme) {
                RestoreScreen(viewModel = viewModel)
            }
        }
    }

    @Test
    fun idle_rendersInLightTheme() {
        createHarness(RestoreUiState.Idle, darkTheme = false)
        composeTestRule.onNodeWithText("Select Backup File").assertIsDisplayed()
    }

    @Test
    fun idle_rendersInDarkTheme() {
        createHarness(RestoreUiState.Idle, darkTheme = true)
        composeTestRule.onNodeWithText("Select Backup File").assertIsDisplayed()
    }

    @Test
    fun success_rendersInLightTheme() {
        createHarness(RestoreUiState.Success(RestoreResult(1, 2, 3, 4, 5, 6)), darkTheme = false)
        composeTestRule.onNodeWithText("Restore Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun success_rendersInDarkTheme() {
        createHarness(RestoreUiState.Success(RestoreResult(1, 2, 3, 4, 5, 6)), darkTheme = true)
        composeTestRule.onNodeWithText("Restore Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun error_rendersInLightTheme() {
        createHarness(
            RestoreUiState.Error(com.example.healthjournal.export.RestoreError.CorruptedFile("corrupt")),
            darkTheme = false
        )
        composeTestRule.onNodeWithText("Restore Failed").assertIsDisplayed()
    }

    @Test
    fun error_rendersInDarkTheme() {
        createHarness(
            RestoreUiState.Error(com.example.healthjournal.export.RestoreError.CorruptedFile("corrupt")),
            darkTheme = true
        )
        composeTestRule.onNodeWithText("Restore Failed").assertIsDisplayed()
    }
}
