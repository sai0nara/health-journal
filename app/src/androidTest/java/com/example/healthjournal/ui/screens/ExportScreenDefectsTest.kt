package com.example.healthjournal.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.healthjournal.export.ExportState
import com.example.healthjournal.export.ExportViewModel
import com.example.healthjournal.export.RestoreUiState
import com.example.healthjournal.export.RestoreViewModel
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
class ExportScreenDefectsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun createHarness() {
        val exportFlow = MutableStateFlow<ExportState>(ExportState.Idle)
        val exportViewModel = mockk<ExportViewModel>(relaxed = true)
        every { exportViewModel.exportState } returns exportFlow
        every { exportViewModel.exportData(any(), any(), any()) } just runs

        val restoreFlow = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
        val restoreViewModel = mockk<RestoreViewModel>(relaxed = true)
        every { restoreViewModel.uiState } returns restoreFlow

        composeTestRule.setContent {
            ExportScreen(
                viewModel = exportViewModel,
                restoreViewModel = restoreViewModel,
                onBack = {}
            )
        }
    }

    @Test
    fun selectedZipFormat_survivesConfigurationChange() {
        val stateRestorationTester = StateRestorationTester(composeTestRule)
        stateRestorationTester.setContent {
            val exportFlow = MutableStateFlow<ExportState>(ExportState.Idle)
            val exportViewModel = mockk<ExportViewModel>(relaxed = true)
            every { exportViewModel.exportState } returns exportFlow
            every { exportViewModel.exportData(any(), any(), any()) } just runs

            val restoreFlow = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
            val restoreViewModel = mockk<RestoreViewModel>(relaxed = true)
            every { restoreViewModel.uiState } returns restoreFlow

            ExportScreen(
                viewModel = exportViewModel,
                restoreViewModel = restoreViewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("format_pdf").assertIsSelected()
        composeTestRule.onNodeWithTag("format_zip").assertIsNotSelected()

        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithTag("format_zip").assertIsSelected()
        composeTestRule.onNodeWithTag("format_pdf").assertIsNotSelected()

        stateRestorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag("format_zip").assertIsSelected()
        composeTestRule.onNodeWithTag("format_pdf").assertIsNotSelected()
    }

    @Test
    fun generateExportButton_isReachableViaScroll() {
        createHarness()
        composeTestRule.onNodeWithText("Generate Export").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun formatLabels_renderOnOwnRows() {
        createHarness()
        composeTestRule.onNodeWithText("PDF (Medical Report)").assertIsDisplayed()
        composeTestRule.onNodeWithText("ZIP (Raw Data & Media)").assertIsDisplayed()
    }

    @Test
    fun dateCard_onlyShownForPdfFormat() {
        createHarness()
        composeTestRule.onNodeWithText("Start Date").assertIsDisplayed()

        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithText("Start Date").assertDoesNotExist()

        composeTestRule.onNodeWithTag("format_pdf").performClick()
        composeTestRule.onNodeWithText("Start Date").assertIsDisplayed()
    }
}
