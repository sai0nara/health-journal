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
                onCameraClick = {},
                onGalleryClick = {},
                onAttachFileClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gallery").assertIsDisplayed()
        composeTestRule.onNodeWithText("File").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health").assertIsDisplayed()
    }

    @Test
    fun enrichmentPanel_cameraClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onCameraClick = { clicked = true },
                onGalleryClick = {},
                onAttachFileClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Camera").performClick()
        assert(clicked)
    }

    @Test
    fun enrichmentPanel_galleryClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onCameraClick = {},
                onGalleryClick = { clicked = true },
                onAttachFileClick = {},
                onSyncHealthClick = {}
            )
        }

        composeTestRule.onNodeWithText("Gallery").performClick()
        assert(clicked)
    }

    @Test
    fun enrichmentPanel_attachFileClick_triggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            EnrichmentPanel(
                onCameraClick = {},
                onGalleryClick = {},
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
                onCameraClick = {},
                onGalleryClick = {},
                onAttachFileClick = {},
                onSyncHealthClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Health").performClick()
        assert(clicked)
    }
}
