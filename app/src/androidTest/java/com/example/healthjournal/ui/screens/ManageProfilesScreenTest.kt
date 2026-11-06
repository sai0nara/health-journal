package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.ProfileRepository
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeProfileDao
import com.example.healthjournal.util.FakeProfileDataEraser
import com.example.healthjournal.util.FakeProfileStore
import com.example.healthjournal.viewmodel.ProfileViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManageProfilesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dao = FakeProfileDao()
    private val repository = ProfileRepository(
        profileDao = dao,
        settings = FakeProfileStore(),
        eraser = FakeProfileDataEraser()
    )
    private val self = ProfileEntity(
        id = SELF_PROFILE_ID,
        name = "Me",
        relationship = ProfileRelationship.SELF.name
    )
    private val sibling = ProfileEntity(
        id = "sibling",
        name = "Alex",
        relationship = ProfileRelationship.SIBLING.name
    )

    @Before
    fun seedProfiles() = runBlocking {
        dao.upsert(self)
        dao.upsert(sibling)
    }

    private fun showScreen(): ProfileViewModel {
        val viewModel = ProfileViewModel(repository)
        composeTestRule.setContent {
            HealthJournalTheme {
                ManageProfilesScreen(viewModel = viewModel, onBack = {})
            }
        }
        waitForTag("manage_profile_row_self")
        return viewModel
    }

    private fun waitForTag(tag: String, exists: Boolean = true) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() == exists
        }
    }

    @Test
    fun oneProfile_showsActiveRowAndPrivacyNote() {
        showScreen()

        composeTestRule.onNodeWithText("Me").assertIsDisplayed()
        composeTestRule.onNodeWithTag("manage_profiles_add").assertIsEnabled()
        composeTestRule.onNodeWithText(
            "Profiles organize the records in this app. They are not a security boundary."
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("manage_profiles_archived_section").assertDoesNotExist()
    }

    @Test
    fun emptyProfiles_showsEmptyState() {
        runBlocking {
            dao.delete(SELF_PROFILE_ID)
            dao.delete("sibling")
        }
        val viewModel = ProfileViewModel(repository)
        composeTestRule.setContent {
            HealthJournalTheme {
                ManageProfilesScreen(viewModel = viewModel, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("No profiles yet").assertIsDisplayed()
    }

    @Test
    fun createProfile_addsActiveRowWithRelationship() {
        showScreen()

        composeTestRule.onNodeWithTag("manage_profiles_add").performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput("Sam")
        composeTestRule.onNodeWithTag("profile_relationship_child").performClick()
        composeTestRule.onNodeWithTag("profile_editor_save").performClick()

        waitForTag("profile_editor_dialog", exists = false)
        composeTestRule.onNodeWithText("Sam").assertIsDisplayed()
        composeTestRule.onNodeWithTag("manage_profiles_active_section").assertIsDisplayed()
    }

    @Test
    fun editProfile_updatesName() {
        showScreen()

        composeTestRule.onNodeWithTag("manage_profile_edit_sibling").performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput("Alexandra")
        composeTestRule.onNodeWithTag("profile_editor_save").performClick()

        waitForTag("profile_editor_dialog", exists = false)
        composeTestRule.onNodeWithText("Alexandra").assertIsDisplayed()
    }

    @Test
    fun archiveAndRestore_movesProfileBetweenSections() {
        showScreen()

        composeTestRule.onNodeWithTag("manage_profile_archive_sibling").performClick()
        waitForTag("manage_profile_restore_sibling")
        composeTestRule.onNodeWithTag("manage_profiles_archived_section").assertIsDisplayed()

        composeTestRule.onNodeWithTag("manage_profile_restore_sibling").performClick()
        waitForTag("manage_profile_archive_sibling")
        composeTestRule.onNodeWithTag("manage_profiles_archived_section").assertDoesNotExist()
    }

    @Test
    fun deleteProfile_requiresExactTypedName() {
        runBlocking { repository.archive("sibling") }
        showScreen()

        composeTestRule.onNodeWithTag("manage_profile_delete_sibling").performClick()
        composeTestRule.onNodeWithTag("profile_delete_name_input").performTextInput("alex")
        composeTestRule.onNodeWithTag("profile_delete_confirm").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("profile_delete_name_input").performTextClearance()
        composeTestRule.onNodeWithTag("profile_delete_name_input").performTextInput("Alex")
        composeTestRule.onNodeWithTag("profile_delete_confirm").assertIsEnabled()
        composeTestRule.onNodeWithTag("profile_delete_confirm").performClick()

        waitForTag("manage_profile_row_sibling", exists = false)
    }

    @Test
    fun tenActiveProfiles_disablesAddAndShowsLimit() {
        runBlocking {
            repeat(8) { index ->
                dao.upsert(
                    ProfileEntity(
                        id = "sibling-$index",
                        name = "Sibling $index",
                        relationship = ProfileRelationship.SIBLING.name
                    )
                )
            }
        }
        showScreen()

        composeTestRule.onNodeWithTag("manage_profiles_list")
            .performScrollToNode(hasTestTag("manage_profiles_add"))
        composeTestRule.onNodeWithTag("manage_profiles_add").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Maximum of 10 active profiles").assertIsDisplayed()
    }

    @Test
    fun blankName_showsInlineErrorAndKeepsEditorOpen() {
        showScreen()

        composeTestRule.onNodeWithTag("manage_profiles_add").performClick()
        composeTestRule.onNodeWithTag("profile_name_input").performTextInput("   ")
        composeTestRule.onNodeWithTag("profile_editor_save").performClick()

        composeTestRule.onNodeWithText("Enter a profile name.").assertIsDisplayed()
        composeTestRule.onNodeWithTag("profile_editor_dialog").assertExists()
    }
}
