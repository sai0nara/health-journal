package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.PermissionsRationaleActivity
import com.example.healthjournal.R
import com.example.healthjournal.util.targetString
import org.junit.Rule
import org.junit.Test

class PermissionsRationaleActivityTest {

    @get:Rule
    val composeTestRule =
        createAndroidComposeRule<PermissionsRationaleActivity>()

    @Test
    fun rationale_rendersExternalizedStrings() {
        composeTestRule.onNodeWithText(targetString(R.string.permissions_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            targetString(R.string.permissions_body),
            substring = true
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(targetString(R.string.permissions_close)).assertIsDisplayed()
    }
}
