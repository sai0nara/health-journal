package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_rendersExternalizedEnglishStrings() {
        composeTestRule.setContent {
            HealthJournalTheme {
                SettingsScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithText("Metric (kg/cm)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Imperial (lbs/in)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }
}
