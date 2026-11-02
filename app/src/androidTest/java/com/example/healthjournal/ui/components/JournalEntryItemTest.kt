package com.example.healthjournal.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class JournalEntryItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun entryItem_rendersTagChipsAndReportsTap() {
        var tapped: String? = null
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Evening walk"),
                    onClick = {},
                    onPhotoClick = {},
                    tags = listOf("recovery", "Fitness"),
                    onTagClick = { tapped = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_tag_recovery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_tag_Fitness").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_tag_recovery").performClick()
        assertEquals("recovery", tapped)
    }

    @Test
    fun entryItem_noTags_rendersNoTagRow() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Plain entry"),
                    onClick = {},
                    onPhotoClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_tag_row").assertDoesNotExist()
    }

    private fun vitalsEntry(
        glucose: Double? = 5.4,
        temperature: Double? = 36.6,
        spo2: Int? = 98
    ) = JournalEntry(
        entry_id = "vitals-entry",
        description = "Checkup",
        bp_systolic = 120.0,
        bp_diastolic = 80.0,
        heart_rate_avg = 72,
        sleep_hours = 7.5f,
        blood_glucose_mmol = glucose,
        body_temperature_c = temperature,
        spo2_percent = spo2
    )

    @Test
    fun entryItem_rendersVitalsChipsWithUnits() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = vitalsEntry(),
                    onClick = {},
                    onPhotoClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_row", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("entry_vitals_bp").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_vitals_bp").assertTextContains("120/80 mmHg")
        composeTestRule.onNodeWithTag("entry_vitals_hr").assertTextContains("72 bpm")
        composeTestRule.onNodeWithTag("entry_vitals_glucose").assertTextContains("5.4 mmol/L")
        composeTestRule.onNodeWithTag("entry_vitals_temp").assertTextContains("36.6 °C")
        composeTestRule.onNodeWithTag("entry_vitals_spo2").assertTextContains("98 %")
        composeTestRule.onNodeWithTag("entry_vitals_sleep").assertTextContains("7.5 h")
    }

    @Test
    fun entryItem_imperial_rendersConvertedGlucoseAndTemperature() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = vitalsEntry(),
                    onClick = {},
                    onPhotoClick = {},
                    unitSystem = UnitSystem.IMPERIAL
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_glucose").assertTextContains("97 mg/dL")
        composeTestRule.onNodeWithTag("entry_vitals_temp").assertTextContains("97.9 °F")
        composeTestRule.onNodeWithTag("entry_vitals_bp").assertTextContains("120/80 mmHg")
    }

    @Test
    fun entryItem_noVitals_rendersNoVitalsRow() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Plain entry"),
                    onClick = {},
                    onPhotoClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_row").assertDoesNotExist()
        composeTestRule.onNodeWithTag("entry_vitals_sleep").assertDoesNotExist()
    }

    @Test
    fun entryItem_addVitalsAction_reportsEntryTap() {
        val entry = JournalEntry(entry_id = "no-vitals", description = "Plain entry")
        var tapped: JournalEntry? = null

        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = entry,
                    onClick = {},
                    onPhotoClick = {},
                    onVitalsClick = { tapped = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_action").assertTextContains("Add vitals")
        composeTestRule.onNodeWithTag("entry_vitals_action").performClick()
        assertEquals("no-vitals", tapped?.entry_id)
    }

    @Test
    fun entryItem_editVitalsLabel_whenVitalsExist() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = vitalsEntry(),
                    onClick = {},
                    onPhotoClick = {},
                    onVitalsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_action").assertTextContains("Edit vitals")
    }

    @Test
    fun entryItem_sleepOnly_stillOffersAddVitals() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = JournalEntry(description = "Slept", sleep_hours = 7.5f),
                    onClick = {},
                    onPhotoClick = {},
                    onVitalsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_action").assertTextContains("Add vitals")
    }

    @Test
    fun entryItem_archived_rendersChipsButNoAction() {
        composeTestRule.setContent {
            HealthJournalTheme {
                JournalEntryItem(
                    entry = vitalsEntry().copy(isArchived = true),
                    onClick = {},
                    onPhotoClick = {},
                    onVitalsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("entry_vitals_bp").assertIsDisplayed()
        composeTestRule.onNodeWithTag("entry_vitals_action").assertDoesNotExist()
    }
}
