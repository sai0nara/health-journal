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
                onAttachFileClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Photo").assertIsDisplayed()
        composeTestRule.onNodeWithText("File").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health").assertIsDisplayed()
    }

    @Test
    fun enrichmentPanel_attachPhotoClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = { clicked = true },
                onAttachFileClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Photo").performClick()
        assert(clicked)
    }

    @Test
    fun enrichmentPanel_attachFileClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = {},
                onAttachFileClick = { clicked = true },
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("File").performClick()
        assert(clicked)
    }

    @Test
    fun enrichmentPanel_syncHealthClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onAttachPhotoClick = {},
                onAttachFileClick = {},
                onSyncHealthClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Health").performClick()
        assert(clicked)
    }
}
