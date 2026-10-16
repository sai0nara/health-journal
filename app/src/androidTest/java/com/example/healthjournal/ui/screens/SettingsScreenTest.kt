package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.data.local.HealthSyncSettings
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.FakeJournalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_rendersExternalizedEnglishStrings() {
        composeTestRule.setContent {
            HealthJournalTheme {
                SettingsScreen(viewModel = FakeJournalViewModel(), onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithTag("settings_unit_system").performClick()
        composeTestRule.onNodeWithTag("settings_unit_system_metric")
            .assertIsDisplayed()
            .assertTextEquals("Metric (kg/cm)")
        composeTestRule.onNodeWithTag("settings_unit_system_imperial")
            .assertIsDisplayed()
            .assertTextEquals("Imperial (lbs/in)")
    }

    @Test
    fun settingsScreen_healthSyncToggle_defaultsOffAndPersists() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        HealthSyncSettings.write(targetContext, false)
        try {
            composeTestRule.setContent {
                HealthJournalTheme {
                    SettingsScreen(viewModel = FakeJournalViewModel(), onBack = {})
                }
            }

            composeTestRule.onNodeWithTag("settings_health_sync_toggle")
                .assertIsDisplayed()
                .assertIsOff()
            assertFalse(HealthSyncSettings.read(targetContext))

            composeTestRule.onNodeWithTag("settings_health_sync_toggle").performClick()

            composeTestRule.onNodeWithTag("settings_health_sync_toggle").assertIsOn()
            assertTrue(HealthSyncSettings.read(targetContext))
        } finally {
            HealthSyncSettings.write(targetContext, false)
        }
    }

    @Test
    fun settingsScreen_signedOut_showsSignInAndCallsViewModel() {
        val viewModel = FakeJournalViewModel()
        viewModel.isUserSignedIn.value = false
        composeTestRule.setContent {
            HealthJournalTheme {
                SettingsScreen(viewModel = viewModel, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Account").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sign_in").assertIsDisplayed().performClick()
        assertTrue(viewModel.signInCalled)
    }

    @Test
    fun settingsScreen_signedIn_showsEmailAndSignOut() {
        val viewModel = object : FakeJournalViewModel() {
            override val syncStatus = MutableStateFlow<String?>("Synced")
        }
        viewModel.isUserSignedIn.value = true
        viewModel.userEmail.value = "test@example.com"
        composeTestRule.setContent {
            HealthJournalTheme {
                SettingsScreen(viewModel = viewModel, onBack = {})
            }
        }

        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Synced").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_sign_out").assertIsDisplayed().performClick()
        assertTrue(viewModel.signOutCalled)
    }
}
