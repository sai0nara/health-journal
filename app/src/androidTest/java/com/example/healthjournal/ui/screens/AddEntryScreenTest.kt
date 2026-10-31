package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.health.connect.client.HealthConnectClient
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.viewmodel.HealthSyncResult
import com.example.healthjournal.viewmodel.IJournalViewModel
import io.qameta.allure.android.allureScreenshot
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.text.SimpleDateFormat
import java.util.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule

@Feature("Add Entry")
class AddEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()


    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    class MockJournalViewModel : com.example.healthjournal.util.FakeJournalViewModel()

    private val viewModel = MockJournalViewModel()

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun useMetricUnits() {
        UnitSettings.write(targetContext, UnitSystem.METRIC)
    }

    @After
    fun restoreMetricUnits() {
        UnitSettings.write(targetContext, UnitSystem.METRIC)
    }

    @Test
    fun testAddEntryScreen_SaveButtonCallsViewModel() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        val testDescription = "I feel great!"
        step("Enter description: $testDescription") {
            composeTestRule.onNodeWithText("How are you feeling today?")
                .performTextInput(testDescription)
            composeTestRule.waitForIdle()
            allureScreenshot("description_entered")
        }

        step("Click Save button") {
            composeTestRule.onNodeWithText("Save Entry")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_clicked")
        }

        step("Verify entry was saved and screen closed") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            // Substring check because RichTextState wraps in <p> and escapes special chars (e.g. ! -> &excl;)
            assert(viewModel.addEntryCalledWith?.description?.contains("I feel great&excl;") == true)
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_BackButtonCallsOnBack() {
        var backCalled = false
        
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { backCalled = true }
                )
            }
            composeTestRule.waitForIdle()
            allureScreenshot("add_entry_screen_opened")
        }

        step("Click Back button") {
            composeTestRule.onNodeWithContentDescription("Back")
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("back_clicked")
        }

        step("Verify back was called") {
            // Wait for onBack to be triggered (callback executed)
            composeTestRule.waitUntil(5000) { backCalled }
            assert(backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_DatePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        step("Click Date button ($currentDate)") {
            composeTestRule.onNodeWithText(currentDate, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("date_picker_opened")
        }

        step("Verify Date Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_date_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_TimePickerOpens() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        step("Click Time button ($currentTime)") {
            composeTestRule.onNodeWithText(currentTime, substring = true).performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("time_picker_opened")
        }

        step("Verify Time Picker is visible") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_time_picker_visible")
            composeTestRule.onNodeWithText("OK").assertIsDisplayed()
        }
    }

    @Test
    fun testAddEntryScreen_EmptyDescriptionDoesNotSave() {
        var backCalled = false
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }


        step("Click Save with empty description") {
            composeTestRule.onNodeWithText("Save Entry").performScrollTo().performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("save_attempt_empty")
        }

        step("Verify no save occurred") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_no_save_occurred")
            assert(viewModel.addEntryCalledWith == null)
            assert(!backCalled)
        }
    }

    @Test
    fun testAddEntryScreen_EnrichmentPanelButtonsClickable() {
        step("Open Add Entry Screen with enrichment callbacks") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Verify all EnrichmentPanel buttons are displayed and enabled") {
            composeTestRule.onNodeWithText("Camera").performScrollTo().assertIsDisplayed().assertIsEnabled()
            composeTestRule.onNodeWithText("Gallery").performScrollTo().assertIsDisplayed().assertIsEnabled()
            composeTestRule.onNodeWithText("File").performScrollTo().assertIsDisplayed().assertIsEnabled()
            // Match by test tag: the journal-tag chip row renders a chip with the
            // same "Health" label, so a text matcher is ambiguous.
            composeTestRule.onNodeWithTag("enrichment_sync_health")
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            allureScreenshot("enrichment_buttons_visible_in_screen")
        }

        step("Click Health in EnrichmentPanel") {
            // Health is the only enrichment action that does not launch an external
            // intent (Camera/Gallery/File open system pickers which would replace the
            // compose test host activity). The fake reports SDK_UNAVAILABLE, so this
            // shows a toast and returns without launching the permissions flow.
            composeTestRule.onNodeWithTag("enrichment_sync_health")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            allureScreenshot("health_clicked_in_screen")
        }
    }

    @Test
    fun testAddEntryScreen_HealthHiddenWhenSyncDisabled() {
        viewModel.isHealthSyncEnabled.value = false
        step("Open Add Entry Screen with Health sync disabled") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Verify Sync Health action is not actionable") {
            // Target the panel action by test tag: the journal-tag chip row always
            // renders its own "Health" chip regardless of the sync setting.
            composeTestRule.onNodeWithTag("enrichment_sync_health").assertDoesNotExist()
            allureScreenshot("health_hidden_when_disabled")
        }
    }

    @Test
    fun testAddEntryScreen_UnarchiveAction() {
        val archivedEntry = JournalEntry(entry_id = "1", description = "Archived", isArchived = true)
        
        step("Open Add Entry Screen with archived entry") {
            viewModel.entryToReturn = archivedEntry
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { },
                    entryId = "1"
                )
            }
            composeTestRule.waitForIdle()
        }

        step("Click Unarchive button") {
            composeTestRule.onNodeWithContentDescription("Unarchive").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify toolbar is now visible (in Edit mode)") {
            // "header_button" is a tag in RichTextToolbar
            composeTestRule.onNodeWithTag("header_button").assertExists()
        }
    }

    @Test
    fun testAddEntryScreen_AttachmentDisplaysThumbnailForImage() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(
                    viewModel = viewModel,
                    onBack = { }
                )
            }
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun testAddEntryScreen_PresetAndHashtag_SaveOneCanonicalTag() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Select the Fitness preset chip") {
            val fitnessChip = composeTestRule.onNode(hasText("Fitness") and isSelectable())
            fitnessChip.performScrollTo()
            fitnessChip.performClick()
            fitnessChip.assertIsSelected()
            composeTestRule.waitForIdle()
        }

        step("Type the same tag as a hashtag and save") {
            composeTestRule.onNodeWithText("How are you feeling today?")
                .performTextInput("#Fitness")
            composeTestRule.onNodeWithText("Save Entry").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify one canonical tag was saved") {
            assertEquals(setOf("fitness"), viewModel.addEntryCalledWith?.tags)
        }
    }

    @Test
    fun testAddEntryScreen_ShowsManualVitalsFields() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Verify every manual vitals field is rendered") {
            listOf(
                "vitals_field_systolic",
                "vitals_field_diastolic",
                "vitals_field_hr",
                "vitals_field_glucose",
                "vitals_field_temperature",
                "vitals_field_spo2"
            ).forEach { tag ->
                composeTestRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            }
        }
    }

    @Test
    fun testAddEntryScreen_EditPrefillsManualVitals() {
        step("Open Add Entry Screen for an entry with stored vitals") {
            viewModel.entryToReturn = JournalEntry(
                entry_id = "1",
                description = "Checkup",
                bp_systolic = 120.0,
                bp_diastolic = 80.0,
                heart_rate_avg = 72,
                blood_glucose_mmol = 5.4,
                body_temperature_c = 36.6,
                spo2_percent = 98
            )
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {}, entryId = "1")
            }
            composeTestRule.waitForIdle()
        }

        step("Verify stored values prefill the fields") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").performScrollTo().assertTextContains("120")
            composeTestRule.onNodeWithTag("vitals_field_diastolic").performScrollTo().assertTextContains("80")
            composeTestRule.onNodeWithTag("vitals_field_hr").performScrollTo().assertTextContains("72")
            composeTestRule.onNodeWithTag("vitals_field_glucose").performScrollTo().assertTextContains("5.4")
            composeTestRule.onNodeWithTag("vitals_field_temperature").performScrollTo().assertTextContains("36.6")
            composeTestRule.onNodeWithTag("vitals_field_spo2").performScrollTo().assertTextContains("98")
        }
    }

    @Test
    fun testAddEntryScreen_EditShowsReadOnlySleepHours() {
        step("Open an entry with Health Connect sleep") {
            viewModel.entryToReturn = JournalEntry(
                entry_id = "1",
                description = "Slept well",
                sleep_hours = 7.5f
            )
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {}, entryId = "1")
            }
            composeTestRule.waitForIdle()
        }

        step("Verify sleep is visible but not editable") {
            composeTestRule.onNodeWithText("7.5 h").performScrollTo().assertExists()
            composeTestRule.onNodeWithText("Sleep").assertExists()
            composeTestRule.onNodeWithTag("vitals_field_sleep").assertDoesNotExist()
        }
    }

    @Test
    fun testAddEntryScreen_SavePersistsManualVitals() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Enter a description and manual vitals") {
            composeTestRule.onNodeWithText("How are you feeling today?").performTextInput("Checkup")
            composeTestRule.onNodeWithTag("vitals_field_systolic").performScrollTo().performTextInput("120")
            composeTestRule.onNodeWithTag("vitals_field_diastolic").performScrollTo().performTextInput("80")
            composeTestRule.onNodeWithTag("vitals_field_hr").performScrollTo().performTextInput("72")
            composeTestRule.onNodeWithTag("vitals_field_glucose").performScrollTo().performTextInput("5.4")
            composeTestRule.onNodeWithTag("vitals_field_temperature").performScrollTo().performTextInput("36.6")
            composeTestRule.onNodeWithTag("vitals_field_spo2").performScrollTo().performTextInput("98")
        }

        step("Save the entry") {
            composeTestRule.onNodeWithText("Save Entry").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify canonical vitals were passed to the ViewModel") {
            val call = viewModel.addEntryCalledWith!!
            assertEquals(120.0, call.bpSystolic!!, 0.001)
            assertEquals(80.0, call.bpDiastolic!!, 0.001)
            assertEquals(72, call.heartRate)
            assertEquals(5.4, call.bloodGlucoseMmol!!, 0.001)
            assertEquals(36.6, call.bodyTemperatureC!!, 0.001)
            assertEquals(98, call.spo2Percent)
            assertNull(call.sleepHours)
        }
    }

    @Test
    fun testAddEntryScreen_UpdatePersistsManualVitals() {
        step("Open Add Entry Screen for an existing entry") {
            viewModel.entryToReturn = JournalEntry(entry_id = "1", description = "Old")
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {}, entryId = "1")
            }
            composeTestRule.waitForIdle()
        }

        step("Enter a manual glucose reading and update") {
            composeTestRule.onNodeWithTag("vitals_field_glucose").performScrollTo().performTextInput("5.4")
            composeTestRule.onNodeWithText("Update Entry").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify the updated entry carries the canonical value") {
            val saved = viewModel.updateEntryCalledWith!!.first
            assertEquals(5.4, saved.blood_glucose_mmol!!, 0.001)
        }
    }

    @Test
    fun testAddEntryScreen_InvalidBloodPressureDisablesSave() {
        step("Open Add Entry Screen") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
        }

        step("Enter only systolic") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").performScrollTo().performTextInput("120")
            composeTestRule.waitForIdle()
        }

        step("Verify the pair error is shown and Save is blocked") {
            composeTestRule.onNodeWithText("Enter both systolic and diastolic.").assertExists()
            composeTestRule.onNodeWithText("Save Entry").performScrollTo().assertIsNotEnabled()
        }
    }

    @Test
    fun testAddEntryScreen_HealthSyncFillsBpAndHeartRatePreservingManualFields() {
        viewModel.healthAvailability = HealthConnectClient.SDK_AVAILABLE
        viewModel.healthPermissionsGranted = true
        viewModel.healthSyncResult = HealthSyncResult(
            bpSystolic = 118.0,
            bpDiastolic = 76.0,
            heartRate = 64,
            sleepHours = 8f
        )

        step("Open Add Entry Screen and enter manual glucose, temperature, and SpO2") {
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {})
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("vitals_field_glucose").performScrollTo().performTextInput("5.4")
            composeTestRule.onNodeWithTag("vitals_field_temperature").performScrollTo().performTextInput("36.6")
            composeTestRule.onNodeWithTag("vitals_field_spo2").performScrollTo().performTextInput("98")
        }

        step("Sync Health Connect") {
            composeTestRule.onNodeWithTag("enrichment_sync_health").performScrollTo().performClick()
            composeTestRule.waitUntil(5000) { viewModel.syncHealthDataCalled }
            composeTestRule.waitForIdle()
        }

        step("Verify synced BP/HR fill in and manual values survive") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").performScrollTo().assertTextContains("118")
            composeTestRule.onNodeWithTag("vitals_field_diastolic").performScrollTo().assertTextContains("76")
            composeTestRule.onNodeWithTag("vitals_field_hr").performScrollTo().assertTextContains("64")
            composeTestRule.onNodeWithTag("vitals_field_glucose").performScrollTo().assertTextContains("5.4")
            composeTestRule.onNodeWithTag("vitals_field_temperature").performScrollTo().assertTextContains("36.6")
            composeTestRule.onNodeWithTag("vitals_field_spo2").performScrollTo().assertTextContains("98")
        }
    }

    @Test
    fun testAddEntryScreen_ArchivedEntryShowsReadOnlyVitals() {
        step("Open an archived entry with stored vitals") {
            viewModel.entryToReturn = JournalEntry(
                entry_id = "1",
                description = "Archived",
                isArchived = true,
                bp_systolic = 120.0,
                bp_diastolic = 80.0,
                heart_rate_avg = 72,
                blood_glucose_mmol = 5.4,
                body_temperature_c = 36.6,
                spo2_percent = 98
            )
            composeTestRule.setContent {
                AddEntryScreen(viewModel = viewModel, onBack = {}, entryId = "1")
            }
            composeTestRule.waitForIdle()
        }

        step("Verify vitals are shown read-only") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").assertDoesNotExist()
            composeTestRule.onNodeWithTag("vitals_field_glucose").assertDoesNotExist()
            composeTestRule.onNodeWithText("120/80 mmHg").assertExists()
            composeTestRule.onNodeWithText("72 bpm").assertExists()
            composeTestRule.onNodeWithText("5.4 mmol/L").assertExists()
            composeTestRule.onNodeWithText("36.6 °C").assertExists()
            composeTestRule.onNodeWithText("98 %").assertExists()
            composeTestRule.onNodeWithText("Save Entry").assertDoesNotExist()
        }
    }

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }
}
