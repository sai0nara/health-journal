package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val expectedRoutes = listOf(
        "history",
        "workout",
        "measurements",
        "presets",
        "archive",
        "export",
        "personal_card",
        "settings"
    )

    @Test
    fun mainScreen_rendersEightTilesInOrder() {
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(onTileClick = {})
            }
        }

        expectedRoutes.forEach { route ->
            composeTestRule.onNodeWithTag("main_tile_$route").assertIsDisplayed()
        }
    }

    @Test
    fun mainScreen_showsTitleAndAboutAction() {
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(onTileClick = {})
            }
        }

        composeTestRule.onNodeWithText("Health Journal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("main_about").assertIsDisplayed()
    }

    @Test
    fun mainScreen_aboutActionOpensDialog() {
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(onTileClick = {})
            }
        }

        composeTestRule.onNodeWithTag("main_about").performClick()
        composeTestRule.onNodeWithText("About Health Journal").assertIsDisplayed()
    }

    @Test
    fun mainScreen_tileClickNavigatesToSection() {
        val clicked = mutableListOf<String>()
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(onTileClick = { clicked += it })
            }
        }

        composeTestRule.onNodeWithTag("main_tile_workout").performClick()
        assertEquals(listOf("workout"), clicked)
    }
}
