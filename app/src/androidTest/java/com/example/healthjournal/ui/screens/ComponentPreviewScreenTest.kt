package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.R
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.targetString
import org.junit.Rule
import org.junit.Test

class ComponentPreviewScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun previewScreen_rendersExternalizedStrings() {
        composeTestRule.setContent {
            HealthJournalTheme {
                ComponentPreviewScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText(targetString(R.string.preview_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(targetString(R.string.preview_enrichment)).assertIsDisplayed()
        composeTestRule.onNodeWithText(targetString(R.string.preview_toolbar)).assertIsDisplayed()
    }
}
