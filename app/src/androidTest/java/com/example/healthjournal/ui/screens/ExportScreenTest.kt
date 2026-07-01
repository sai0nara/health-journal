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
}
