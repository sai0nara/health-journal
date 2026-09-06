package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ActivityScenario
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.MainActivity
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.export.ExportViewModel
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.local.JournalDatabase
import org.junit.Rule
import org.junit.Test

class ExportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @org.junit.Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("TEST_MODE", true)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    @Test
    fun testExportScreen_RendersCorrectly() {
        // Navigate to export screen from HistoryScreen (which is the start destination)
        composeTestRule.onNodeWithContentDescription("Export Data").performClick()

        // Check if screen elements are present
        composeTestRule.onNodeWithText("Export Data").assertExists()
        composeTestRule.onNodeWithText("Choose the date range and format for your export.").assertExists()
        composeTestRule.onNodeWithText("PDF (Medical Report)").assertExists()
        composeTestRule.onNodeWithText("ZIP (Raw Data & Media)").assertExists()
        composeTestRule.onNodeWithText("Generate Export").assertExists()
    }

    @Test
    fun testDateRangePicker_hiddenForZip_showsFullBackupNotice() {
        composeTestRule.onNodeWithContentDescription("Export Data").performClick()

        // Date range picker visible with PDF (default)
        composeTestRule.onNodeWithText("Start Date").assertExists()
        composeTestRule.onNodeWithText("End Date").assertExists()

        // Switch to ZIP - picker hidden, clear full-backup notice shown
        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithText("Start Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("End Date").assertDoesNotExist()
        composeTestRule.onNodeWithText("ZIP exports your full backup", substring = true).assertExists()
    }

    @Test
    fun testEncryptToggle_onlyShownForZip() {
        composeTestRule.onNodeWithContentDescription("Export Data").performClick()

        // Encrypt option not present for PDF
        composeTestRule.onNodeWithText("Encrypt backup").assertDoesNotExist()

        // Switch to ZIP - encrypt toggle appears
        composeTestRule.onNodeWithTag("format_zip").performClick()
        composeTestRule.onNodeWithText("Encrypt backup").assertExists()

        // Passphrase field hidden until toggle is enabled
        composeTestRule.onNodeWithText("Enter passphrase").assertDoesNotExist()

        // Enable toggle - passphrase field appears
        composeTestRule.onNodeWithTag("encrypt_backup").performClick()
        composeTestRule.onNodeWithText("Enter passphrase").assertExists()
    }
}
