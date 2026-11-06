package com.example.healthjournal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileSwitcherTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sibling = ProfileEntity(
        id = "sibling",
        name = "Alex",
        relationship = ProfileRelationship.SIBLING.name
    )
    private val self = ProfileEntity(
        id = "self",
        name = "Me",
        relationship = ProfileRelationship.SELF.name
    )

    private fun setSwitcher(
        activeProfileId: String = "self",
        enabled: Boolean = true,
        onProfileSelected: (String) -> Unit = {},
        onManageProfiles: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            HealthJournalTheme {
                ProfileSwitcher(
                    profiles = listOf(self, sibling),
                    activeProfileId = activeProfileId,
                    enabled = enabled,
                    onProfileSelected = onProfileSelected,
                    onManageProfiles = onManageProfiles
                )
            }
        }
    }

    @Test
    fun switcher_showsActiveProfileName() {
        setSwitcher(activeProfileId = "sibling")

        composeTestRule.onNodeWithTag("profile_switcher").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alex").assertIsDisplayed()
    }

    @Test
    fun switcher_listsAllProfilesAndMarksActive() {
        setSwitcher()

        composeTestRule.onNodeWithTag("profile_switcher").performClick()
        composeTestRule.onNodeWithTag("profile_switcher_item_self").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_switcher_item_sibling").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_switcher_check_self", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("profile_switcher_check_sibling", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun selectingProfileInvokesCallbackAndClosesMenu() {
        val selected = mutableListOf<String>()
        setSwitcher(onProfileSelected = { selected += it })

        composeTestRule.onNodeWithTag("profile_switcher").performClick()
        composeTestRule.onNodeWithTag("profile_switcher_item_sibling").performClick()

        assertEquals(listOf("sibling"), selected)
        composeTestRule.onNodeWithTag("profile_switcher_item_sibling").assertDoesNotExist()
    }

    @Test
    fun manageProfilesInvokesCallback() {
        var managed = false
        setSwitcher(onManageProfiles = { managed = true })

        composeTestRule.onNodeWithTag("profile_switcher").performClick()
        composeTestRule.onNodeWithTag("profile_switcher_manage").performClick()

        assertEquals(true, managed)
    }

    @Test
    fun disabledSwitcher_doesNotOpenMenu() {
        setSwitcher(enabled = false)

        composeTestRule.onNodeWithTag("profile_switcher").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("profile_switcher").performClick()
        composeTestRule.onNodeWithTag("profile_switcher_manage").assertDoesNotExist()
    }

    @Test
    fun enabledSwitcher_isEnabled() {
        setSwitcher()

        composeTestRule.onNodeWithTag("profile_switcher").assertIsEnabled()
    }
}
