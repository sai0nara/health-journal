package com.example.healthjournal.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.ui.theme.HealthJournalTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose tests for the card quick-add vitals sheet: display-unit prefill,
 * inline validation, canonical conversion on save, and clearing behavior.
 */
class VitalsEntrySheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun storedEntry() = JournalEntry(
        entry_id = "vitals-entry",
        description = "Checkup",
        bp_systolic = 120.0,
        bp_diastolic = 80.0,
        heart_rate_avg = 72,
        blood_glucose_mmol = 5.4,
        body_temperature_c = 36.6,
        spo2_percent = 98
    )

    private fun showSheet(
        entry: JournalEntry = storedEntry(),
        unitSystem: UnitSystem = UnitSystem.METRIC,
        onDismiss: () -> Unit = {},
        onSave: (EntryVitals) -> Unit = {}
    ) {
        composeTestRule.setContent {
            HealthJournalTheme {
                VitalsEntrySheet(
                    entry = entry,
                    unitSystem = unitSystem,
                    onDismiss = onDismiss,
                    onSave = onSave
                )
            }
        }
    }

    private fun typeInto(tag: String, text: String) {
        composeTestRule.onNodeWithTag(tag).performTextInput(text)
    }

    @Test
    fun sheet_prefillsStoredVitalsInMetricUnits() {
        showSheet()

        composeTestRule.onNodeWithTag("vitals_sheet_title").assertTextContains("Vitals")
        composeTestRule.onNodeWithTag("vitals_field_systolic").assertTextContains("120")
        composeTestRule.onNodeWithTag("vitals_field_diastolic").assertTextContains("80")
        composeTestRule.onNodeWithTag("vitals_field_hr").assertTextContains("72")
        composeTestRule.onNodeWithTag("vitals_field_glucose").assertTextContains("5.4")
        composeTestRule.onNodeWithTag("vitals_field_temperature").assertTextContains("36.6")
        composeTestRule.onNodeWithTag("vitals_field_spo2").assertTextContains("98")
    }

    @Test
    fun sheet_imperial_prefillsConvertedGlucoseAndTemperature() {
        showSheet(unitSystem = UnitSystem.IMPERIAL)

        composeTestRule.onNodeWithTag("vitals_field_glucose").assertTextContains("97")
        composeTestRule.onNodeWithTag("vitals_field_temperature").assertTextContains("97.9")
        composeTestRule.onNodeWithTag("vitals_field_systolic").assertTextContains("120")
    }

    @Test
    fun sheet_save_returnsCanonicalVitals() {
        var saved: EntryVitals? = null
        showSheet(onSave = { saved = it })

        composeTestRule.onNodeWithTag("vitals_save").assertIsEnabled()
        composeTestRule.onNodeWithTag("vitals_save").performClick()

        assertEquals(
            EntryVitals(
                bpSystolic = 120.0,
                bpDiastolic = 80.0,
                heartRate = 72,
                bloodGlucoseMmol = 5.4,
                bodyTemperatureC = 36.6,
                spo2Percent = 98
            ),
            saved
        )
    }

    @Test
    fun sheet_cancel_dismissesWithoutSaving() {
        var dismissed = false
        var saved: EntryVitals? = null
        showSheet(onDismiss = { dismissed = true }, onSave = { saved = it })

        composeTestRule.onNodeWithTag("vitals_field_systolic").performTextClearance()
        typeInto("vitals_field_systolic", "130")
        composeTestRule.onNodeWithTag("vitals_cancel").performClick()

        assertTrue(dismissed)
        assertNull(saved)
    }

    @Test
    fun sheet_blankEntry_saveDisabled() {
        showSheet(entry = JournalEntry(entry_id = "blank", description = "Plain"))

        composeTestRule.onNodeWithTag("vitals_save").assertIsNotEnabled()
    }

    @Test
    fun sheet_loneSystolic_showsPairErrorAndDisablesSave() {
        showSheet(entry = JournalEntry(entry_id = "blank", description = "Plain"))

        typeInto("vitals_field_systolic", "120")

        composeTestRule.onNodeWithText("Enter both systolic and diastolic.").assertExists()
        composeTestRule.onNodeWithTag("vitals_save").assertIsNotEnabled()
    }

    @Test
    fun sheet_outOfRange_showsDisplayBounds() {
        showSheet(entry = JournalEntry(entry_id = "blank", description = "Plain"))

        typeInto("vitals_field_glucose", "0.5")

        composeTestRule.onNodeWithText("Must be between 1 and 50 mmol/L").assertExists()
        composeTestRule.onNodeWithTag("vitals_save").assertIsNotEnabled()
    }

    @Test
    fun sheet_clearingAllFields_savesEmptyVitals() {
        var saved: EntryVitals? = null
        showSheet(onSave = { saved = it })

        listOf(
            "vitals_field_systolic",
            "vitals_field_diastolic",
            "vitals_field_hr",
            "vitals_field_glucose",
            "vitals_field_temperature",
            "vitals_field_spo2"
        ).forEach { composeTestRule.onNodeWithTag(it).performTextClearance() }

        composeTestRule.onNodeWithTag("vitals_save").assertIsEnabled()
        composeTestRule.onNodeWithTag("vitals_save").performClick()

        assertEquals(EntryVitals(), saved)
    }

    @Test
    fun sheet_imperialInput_convertsOnSave() {
        var saved: EntryVitals? = null
        showSheet(unitSystem = UnitSystem.IMPERIAL, onSave = { saved = it })

        composeTestRule.onNodeWithTag("vitals_field_glucose").performTextClearance()
        typeInto("vitals_field_glucose", "97")
        composeTestRule.onNodeWithTag("vitals_field_temperature").performTextClearance()
        typeInto("vitals_field_temperature", "98.6")
        composeTestRule.onNodeWithTag("vitals_save").performClick()

        assertEquals(5.38, saved!!.bloodGlucoseMmol!!, 0.001)
        assertEquals(37.0, saved!!.bodyTemperatureC!!, 0.001)
    }
}
