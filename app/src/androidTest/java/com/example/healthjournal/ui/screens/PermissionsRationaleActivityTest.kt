package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.PermissionsRationaleActivity
import org.junit.Rule
import org.junit.Test

class PermissionsRationaleActivityTest {

    @get:Rule
    val composeTestRule =
        createAndroidComposeRule<PermissionsRationaleActivity>()

    @Test
    fun rationale_rendersExternalizedEnglishStrings() {
        composeTestRule.onNodeWithText("Health Data Access").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "This app requires access to your Health data",
            substring = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").assertIsDisplayed()
    }
}
