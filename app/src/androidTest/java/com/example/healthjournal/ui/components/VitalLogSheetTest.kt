package com.example.healthjournal.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.domain.EntryVitals
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.VitalMetricTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VitalLogSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sheetRenders_saveInitiallyDisabledWhenFieldsBlank() {
        composeTestRule.setContent {
            HealthJournalTheme {
                VitalLogSheet(
                    initialMetric = VitalMetricTab.BLOOD_PRESSURE,
                    unitSystem = UnitSystem.METRIC,
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("vital_log_sheet_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("vital_log_save").assertIsNotEnabled()
    }

    @Test
    fun validVitalsEntered_enablesSaveAndInvokesCallback() {
        var savedVitals: EntryVitals? = null
        var savedTimestamp: Long? = null

        composeTestRule.setContent {
            HealthJournalTheme {
                VitalLogSheet(
                    initialMetric = VitalMetricTab.BLOOD_PRESSURE,
                    unitSystem = UnitSystem.METRIC,
                    onDismiss = {},
                    onSave = { vitals, ts ->
                        savedVitals = vitals
                        savedTimestamp = ts
                    }
                )
            }
        }

        // Type systolic and diastolic
        composeTestRule.onNodeWithTag("vitals_field_systolic").performTextInput("122")
        composeTestRule.onNodeWithTag("vitals_field_diastolic").performTextInput("82")

        composeTestRule.onNodeWithTag("vital_log_save").performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithTag("vital_log_save").performClick()

        assertNotNull(savedVitals)
        assertEquals(122.0, savedVitals?.bpSystolic)
        assertEquals(82.0, savedVitals?.bpDiastolic)
        assertTrue(savedTimestamp != null && savedTimestamp!! > 0L)
    }

    @Test
    fun cancelClick_invokesDismissCallback() {
        var dismissed = false

        composeTestRule.setContent {
            HealthJournalTheme {
                VitalLogSheet(
                    initialMetric = VitalMetricTab.HEART_RATE,
                    unitSystem = UnitSystem.METRIC,
                    onDismiss = { dismissed = true },
                    onSave = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("vital_log_cancel").performClick()
        assertTrue(dismissed)
    }
}
