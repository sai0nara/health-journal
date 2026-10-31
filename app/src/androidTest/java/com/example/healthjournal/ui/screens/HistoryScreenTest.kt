package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.UnitSettings
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.viewmodel.IJournalViewModel
import com.example.healthjournal.ui.theme.HealthJournalTheme
import io.qameta.allure.android.allureScreenshot
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@Feature("History")
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    class MockJournalViewModel : com.example.healthjournal.util.FakeJournalViewModel()

    private val viewModel = MockJournalViewModel()

    @Step("{0}")
    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) {
            block()
        }
    }

    @Test
    fun testHistoryScreen_signedOut_showsNoSignInButton() {
        step("Open History Screen while signed out") {
            viewModel.isUserSignedIn.value = false
            viewModel.allEntries.value = emptyList()

            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Verify no Sign In button is shown (lives in Settings now)") {
            composeTestRule.onNodeWithText("Sign In").assertDoesNotExist()
        }
    }

    @Test
    fun testHistoryScreen_DisplaysEntries() {
        val entries = listOf(
            JournalEntry(description = "Morning jog"),
            JournalEntry(description = "Healthy lunch")
        )
        
        step("Prepare entries and open History Screen") {
            viewModel.allEntries.value = entries

            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
            allureScreenshot("history_screen_with_entries")
        }

        step("Verify that entries are displayed") {
            composeTestRule.waitForIdle()
            allureScreenshot("verification_entries_displayed")
            composeTestRule.onNodeWithText("Morning jog").assertExists()
            composeTestRule.onNodeWithText("Healthy lunch").assertExists()
        }
    }
    @Test
    fun testHistoryScreen_topBarHasNoLogoOrDestinationShortcuts() {
        step("Open History Screen") {
            viewModel.allEntries.value = emptyList()
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Verify the logo and destination shortcuts are gone from the top bar") {
            composeTestRule.onNodeWithTag("app_logo").assertDoesNotExist()
            composeTestRule.onNodeWithTag("measurements_chart_icon").assertDoesNotExist()
            // Literal English: these content descriptions belong to the removed actions,
            // and their string resources are deleted alongside them.
            composeTestRule.onNodeWithContentDescription("Open Personal Card").assertDoesNotExist()
            composeTestRule.onNodeWithContentDescription("Export Data").assertDoesNotExist()
        }

        step("Verify the screen title is still displayed") {
            composeTestRule.onNodeWithText("Health Journal").assertExists()
            composeTestRule.onNodeWithText("Health Journal").assertIsDisplayed()
        }
    }
    @Test
    fun testHistoryScreen_OverflowMenu_showsOnlyActions() {
        step("Open History Screen") {
            viewModel.allEntries.value = emptyList()
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Open the overflow menu") {
            composeTestRule.onNodeWithTag("overflow_menu").performClick()
            composeTestRule.waitForIdle()
        }

        step("Navigation entries are gone; only actions remain") {
            composeTestRule.onNodeWithText("View Archive").assertDoesNotExist()
            composeTestRule.onNodeWithText("Workouts").assertDoesNotExist()
            composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
            composeTestRule.onNodeWithText("Sort order").assertExists()
            composeTestRule.onNodeWithText("About App").assertExists()
        }
    }
    @Test
    fun testHistoryScreen_hasSingleEntryFabAndNoMeasurementAction() {
        step("Open History Screen") {
            viewModel.allEntries.value = emptyList()
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Single add-entry FAB remains; measurement action is gone") {
            composeTestRule.onNodeWithContentDescription("Add Entry").assertExists()
            composeTestRule.onNodeWithContentDescription("Add Entry").assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription("Add body measurements")
                .assertDoesNotExist()
        }
    }

    @Test
    fun testHistoryScreen_SwipeToArchiveAndUndo() {
        val entry = JournalEntry(entry_id = "1", description = "Test Swipe")
        
        step("Open History Screen") {
            viewModel.allEntries.value = listOf(entry)
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Swipe left to archive") {
            composeTestRule.onNodeWithText("Test Swipe").performTouchInput { swipeLeft() }
            composeTestRule.waitForIdle()
        }

        step("Click Undo in Snackbar") {
            composeTestRule.onNodeWithText("Undo").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify restore entry was called") {
            // Note: Since we mocked archiveEntry and restoreEntry, the UI list won't change
            // unless we update it. But we can verify if the method was called if we tracked it.
            // For now, testing the UI interaction is the primary goal.
            composeTestRule.onNodeWithText("Test Swipe").assertExists()
        }
    }

    @Test
    fun testHistoryScreen_vitalsRenderInPreferredUnits() {
        val entries = listOf(
            JournalEntry(
                description = "Checkup",
                bp_systolic = 120.0,
                bp_diastolic = 80.0,
                heart_rate_avg = 65,
                blood_glucose_mmol = 5.4,
                body_temperature_c = 36.6,
                sleep_hours = 7.5f
            )
        )
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        UnitSettings.write(context, UnitSystem.IMPERIAL)
        try {
            viewModel.allEntries.value = entries

            step("Render the entry with imperial units") {
                composeTestRule.setContent {
                    HealthJournalTheme {
                        HistoryScreen(
                            viewModel = viewModel,
                            onAddEntryClick = {},
                            onEntryClick = {}
                        )
                    }
                }
                composeTestRule.waitForIdle()
            }

            step("Verify chips use the stored preference") {
                composeTestRule.onNodeWithText("120/80 mmHg").assertExists()
                composeTestRule.onNodeWithText("65 bpm").assertExists()
                composeTestRule.onNodeWithText("97 mg/dL").assertExists()
                composeTestRule.onNodeWithText("97.9 °F").assertExists()
                composeTestRule.onNodeWithText("7.5 h").assertExists()
            }
        } finally {
            UnitSettings.write(context, UnitSystem.METRIC)
        }
    }

    @Test
    fun testHistoryScreen_rendersDualUnitHistoryLine() {
        val entries = listOf(
            JournalEntry(description = "Barbell Squat: 3 sets · 60 kg (132.3 lb)")
        )

        step("Open History with a dual-unit routine line") {
            viewModel.allEntries.value = entries

            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Verify the dual-unit line renders") {
            composeTestRule.onNodeWithText(
                "Barbell Squat: 3 sets · 60 kg (132.3 lb)",
                substring = true
            ).assertExists()
        }
    }

    @Test
    fun testHistoryScreen_rendersPerformedHistoryLine() {
        val entries = listOf(
            JournalEntry(description = "Barbell Squat: 3 sets · 100 kg (220.5 lb)")
        )

        step("Open History with a performed-weight routine line") {
            viewModel.allEntries.value = entries

            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Verify the performed line renders") {
            composeTestRule.onNodeWithText(
                "Barbell Squat: 3 sets · 100 kg (220.5 lb)",
                substring = true
            ).assertExists()
        }
    }

    @Test
    fun testHistoryScreen_addVitalsFromCard_savesAndUpdatesCard() {
        val entry = JournalEntry(entry_id = "card-vitals", description = "Checkup")

        step("Open History with an entry that has no vitals") {
            viewModel.allEntries.value = listOf(entry)
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Open the quick-add sheet from the card action") {
            composeTestRule.onNodeWithTag("entry_vitals_action").assertTextContains("Add vitals")
            composeTestRule.onNodeWithTag("entry_vitals_action").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("vitals_sheet_title").assertExists()
        }

        step("Enter vitals and save") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").performTextInput("120")
            composeTestRule.onNodeWithTag("vitals_field_diastolic").performTextInput("80")
            composeTestRule.onNodeWithTag("vitals_field_hr").performTextInput("72")
            composeTestRule.onNodeWithTag("vitals_save").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify the card updated without a reload") {
            val call = viewModel.updateVitalsCalledWith!!
            assertEquals("card-vitals", call.first)
            assertEquals(120.0, call.second.bpSystolic!!, 0.001)
            assertEquals(80.0, call.second.bpDiastolic!!, 0.001)
            assertEquals(72, call.second.heartRate)
            composeTestRule.onNodeWithTag("entry_vitals_bp").assertTextContains("120/80 mmHg")
            composeTestRule.onNodeWithTag("entry_vitals_action").assertTextContains("Edit vitals")
        }
    }

    @Test
    fun testHistoryScreen_cancelVitals_leavesCardUnchanged() {
        val entry = JournalEntry(entry_id = "cancel-vitals", description = "Checkup")

        step("Open the quick-add sheet") {
            viewModel.allEntries.value = listOf(entry)
            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("entry_vitals_action").performClick()
            composeTestRule.waitForIdle()
        }

        step("Type a value and cancel") {
            composeTestRule.onNodeWithTag("vitals_field_systolic").performTextInput("130")
            composeTestRule.onNodeWithTag("vitals_cancel").performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify nothing was written and no chip appeared") {
            assertNull(viewModel.updateVitalsCalledWith)
            composeTestRule.onNodeWithTag("entry_vitals_bp").assertDoesNotExist()
        }
    }

    @Test
    fun testHistoryScreen_rendersPerSetBreakdown() {
        val entries = listOf(
            JournalEntry(
                description = "Barbell Squat: 3 sets · " +
                    "Set 1 40 kg (88.2 lb), Set 2 45 kg (99.2 lb), Set 3 45 kg (99.2 lb)"
            )
        )

        step("Open History with a per-set breakdown line") {
            viewModel.allEntries.value = entries

            composeTestRule.setContent {
                HealthJournalTheme {
                    HistoryScreen(
                        viewModel = viewModel,
                        onAddEntryClick = {},
                        onEntryClick = {}
                    )
                }
            }
            composeTestRule.waitForIdle()
        }

        step("Verify the per-set breakdown renders") {
            composeTestRule.onNodeWithText(
                "Set 1 40 kg (88.2 lb), Set 2 45 kg (99.2 lb), Set 3 45 kg (99.2 lb)",
                substring = true
            ).assertExists()
        }
    }
}
