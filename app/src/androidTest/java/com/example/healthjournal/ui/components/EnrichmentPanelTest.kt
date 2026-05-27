package com.example.healthjournal.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import org.junit.Rule
import org.junit.Test

@Feature("Enrichment Panel")
class EnrichmentPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    @Test
    fun enrichmentPanel_displaysButtons() {
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Attach Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync Health").assertIsDisplayed()
    }

    @Test
    fun enrichmentPanel_attachPhotoClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = { clicked = true },
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Attach Photo").performClick()
        assert(clicked)
    }

    @Test
    fun enrichmentPanel_syncHealthClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = {},
                onSyncHealthClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Sync Health").performClick()
        assert(clicked)
    }
}
