package com.example.healthjournal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Rule
import org.junit.Test

class ProfileAvatarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun avatar_usesFirstLettersOfFirstTwoWords() {
        composeTestRule.setContent {
            HealthJournalTheme {
                ProfileAvatar(name = "Alex Rivera", accentColor = 0xFF00FF00.toInt())
            }
        }

        composeTestRule.onNodeWithTag("profile_avatar").assertIsDisplayed()
        composeTestRule.onNodeWithText("AR").assertIsDisplayed()
    }

    @Test
    fun avatar_usesSingleInitialForOneWord() {
        composeTestRule.setContent {
            HealthJournalTheme {
                ProfileAvatar(name = "Me", accentColor = null)
            }
        }

        composeTestRule.onNodeWithText("M").assertIsDisplayed()
    }

    @Test
    fun avatar_usesQuestionMarkForBlankName() {
        composeTestRule.setContent {
            HealthJournalTheme {
                ProfileAvatar(name = "   ", accentColor = null)
            }
        }

        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }
}
