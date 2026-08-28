package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.export.RestoreError
import com.example.healthjournal.export.RestoreResult
import com.example.healthjournal.export.RestoreUiState
import com.example.healthjournal.export.RestoreViewModel
import com.example.healthjournal.ui.theme.HealthJournalTheme
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createHarness(initial: RestoreUiState): RestoreViewModel {
        val flow = MutableStateFlow(initial)
        val viewModel = mockk<RestoreViewModel>(relaxed = true)
        every { viewModel.uiState } returns flow
        every { viewModel.selectBackup(any()) } just runs
        every { viewModel.submitPassphrase(any()) } just runs
        every { viewModel.confirmRestore() } just runs
        every { viewModel.reset() } just runs
        composeTestRule.setContent {
            HealthJournalTheme {
                RestoreScreen(viewModel = viewModel)
            }
        }
        return viewModel
    }

    @Test
    fun idle_showsSelectBackupButton() {
        createHarness(RestoreUiState.Idle)
        composeTestRule.onNodeWithText("Select Backup File").assertIsDisplayed()
    }

    @Test
    fun validating_showsBusyIndicator() {
        createHarness(RestoreUiState.Validating)
        composeTestRule.onNodeWithText("Working on your backup...").assertIsDisplayed()
    }

    @Test
    fun confirmationRequired_showsMetadataAndConfirm_callsViewModel() {
        val viewModel = createHarness(
            RestoreUiState.ConfirmationRequired(
                fileUri = "file:///tmp/backup.zip",
                isEncrypted = true,
                schemaVersion = 12,
                backupTimestamp = 1700000000000L
            )
        )
        composeTestRule.onNodeWithText("Confirm Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Encrypted").assertIsDisplayed()
        composeTestRule.onNodeWithText("v12").assertIsDisplayed()

        composeTestRule.onNodeWithText("Restore").performClick()
        verify { viewModel.confirmRestore() }
    }

    @Test
    fun passphraseRequired_showsPassphraseField() {
        createHarness(
            RestoreUiState.PassphraseRequired(fileUri = "file:///tmp/backup.zip")
        )
        composeTestRule.onNodeWithText("Encrypted Backup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passphrase").assertIsDisplayed()
    }

    @Test
    fun success_showsCompleteAndCounts() {
        createHarness(
            RestoreUiState.Success(
                RestoreResult(1, 2, 3, 4, 5, 6)
            )
        )
        composeTestRule.onNodeWithText("Restore Complete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Journal entries").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun error_showsMessage() {
        createHarness(
            RestoreUiState.Error(RestoreError.CorruptedFile("The backup file is corrupted."))
        )
        composeTestRule.onNodeWithText("Restore Failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("The backup file is corrupted.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose Another Backup").assertIsDisplayed()
    }

    @Test
    fun wrongPassphrase_error_requestsPassphraseAgain() {
        createHarness(
            RestoreUiState.Error(RestoreError.WrongPassphrase(), requestPassphrase = true)
        )
        composeTestRule.onNodeWithText("Wrong Passphrase").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
