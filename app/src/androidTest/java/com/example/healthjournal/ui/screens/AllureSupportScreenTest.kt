package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Description
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Story
import org.junit.Rule
import org.junit.Test

@Feature("Allure Integration")
class AllureSupportScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.END)

    @Test
    @Story("Test Reporting UI")
    @Description("Verify that the Allure Support screen displays correctly")
    fun allureSupportScreen_displaysInformation() {
        composeTestRule.setContent {
            AllureSupportScreen(onBack = {})
        }

        composeTestRule.onNodeWithText("Allure Test Support").assertExists()
        composeTestRule.onNodeWithText("Status: Configured").assertExists()
    }
}
