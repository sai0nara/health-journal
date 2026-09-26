package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Rule
import org.junit.Test

class ComponentPreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun previewScreen_rendersExternalizedEnglishStrings() {
        composeTestRule.setContent {
            HealthJournalTheme {
                ComponentPreviewScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Component Preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("EnrichmentPanel Preview:").assertIsDisplayed()
        composeTestRule.onNodeWithText("RichTextToolbar Preview:").assertIsDisplayed()
    }
}
