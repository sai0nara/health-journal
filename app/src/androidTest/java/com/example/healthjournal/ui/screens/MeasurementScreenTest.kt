package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.viewmodel.BodyMeasurementViewModel
import io.mockk.*
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.kotlin.Feature
import io.qameta.allure.kotlin.Step
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

@Feature("Body Measurements Screen")
class MeasurementScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val screenshotRule = ScreenshotRule(mode = ScreenshotRule.Mode.FAILURE)

    private val repository: BodyMeasurementRepository = mockk(relaxed = true)

    // Eager dispatcher keeps repository calls on the test thread so MockK
    // verification never races with worker-thread coroutines.
    private val testDispatcher = kotlinx.coroutines.test.UnconfinedTestDispatcher()

    private fun setScreen(entries: List<BodyMeasurementEntry>, darkTheme: Boolean = false) {
        every { repository.allEntries } returns MutableStateFlow(entries)
        val viewModel = BodyMeasurementViewModel(repository, testDispatcher)
        composeTestRule.setContent {
            com.example.healthjournal.ui.theme.HealthJournalTheme(darkTheme = darkTheme) {
                MeasurementsScreen(viewModel = viewModel, onBack = {})
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyList_showsFriendlyEmptyState() {
        step("Open Measurements screen with no records") {
            setScreen(emptyList())
        }

        step("Verify empty state message") {
            composeTestRule.onNodeWithTag("bm_empty_state").assertExists()
            composeTestRule.onNodeWithText("No body measurements yet").assertExists()
        }
    }

    @Test
    fun entries_listedNewestFirst() {
        val older = BodyMeasurementEntry(entry_id = "old", timestamp = 1_000L, weight_kg = 80.0)
        val newer = BodyMeasurementEntry(entry_id = "new", timestamp = 2_000L, weight_kg = 78.5)

        step("Open Measurements screen with two records") {
            setScreen(listOf(newer, older))
        }

        step("Verify newest card renders above the older one") {
            val newerY = composeTestRule
                .onAllNodesWithText("78.5 kg")
                .fetchSemanticsNodes()[0]
                .positionInRoot.y
            val olderY = composeTestRule
                .onAllNodesWithText("80 kg")
                .fetchSemanticsNodes()[0]
                .positionInRoot.y
            check(newerY < olderY) { "Expected newest entry ($newerY) above older ($olderY)" }
        }
    }

    @Test
    fun trendChart_renderedWithWeightData() {
        step("Open screen with two weighted entries") {
            setScreen(
                listOf(
                    BodyMeasurementEntry(timestamp = 1_000L, weight_kg = 80.0),
                    BodyMeasurementEntry(timestamp = 2_000L, weight_kg = 79.0)
                )
            )
        }

        step("Verify chart visible") {
            composeTestRule.onNodeWithTag("bm_trend_chart").assertExists()
        }
    }

    @Test
    fun trendChart_hiddenWithoutWeightData() {
        step("Open screen with girth-only entry") {
            setScreen(listOf(BodyMeasurementEntry(waist_cm = 85.0)))
        }

        step("Verify chart absent") {
            composeTestRule.onNodeWithTag("bm_trend_chart").assertDoesNotExist()
        }
    }

    @Test
    fun delete_showsUndoSnackbar_andUndoRestoresRecord() {
        val victim = BodyMeasurementEntry(entry_id = "victim-1", weight_kg = 82.0)

        step("Open Measurements screen with one record and delete it") {
            setScreen(listOf(victim))
            composeTestRule
                .onNodeWithContentDescription("Delete measurement")
                .performClick()
            composeTestRule.waitForIdle()
        }

        step("Verify snackbar with Undo action appears") {
            composeTestRule.onNodeWithText("Measurement deleted").assertExists()
            composeTestRule.onNodeWithText("Undo").assertExists()
        }

        step("Tap Undo and verify restore re-inserted the snapshot") {
            composeTestRule.onNodeWithText("Undo").performClick()
            composeTestRule.waitForIdle()

            val restored = slot<BodyMeasurementEntry>()
            coVerifyOrder {
                repository.deleteEntry("victim-1")
                repository.insert(capture(restored))
            }
            check(restored.captured.entry_id == "victim-1")
            check(restored.captured.weight_kg == 82.0)
        }
    }

    private fun step(description: String, block: () -> Unit) {
        io.qameta.allure.kotlin.Allure.step(description) { block() }
    }

    @Test
    fun measurementsScreen_rendersUnderLightPalette() {
        step("Open with one entry under light palette") {
            setScreen(listOf(BodyMeasurementEntry(weight_kg = 78.5)), darkTheme = false)
        }
        step("Verify content") {
            composeTestRule.onNodeWithText("78.5 kg").assertExists()
        }
    }

    @Test
    fun measurementsScreen_rendersUnderDarkPalette() {
        step("Open with one entry under dark palette") {
            setScreen(listOf(BodyMeasurementEntry(weight_kg = 78.5)), darkTheme = true)
        }
        step("Verify content") {
            composeTestRule.onNodeWithText("78.5 kg").assertExists()
        }
    }

    @Test
    fun entries_showCloudSyncIconMatchingSyncState() {
        step("Open with one local-only and one cloud-synced record") {
            setScreen(
                listOf(
                    BodyMeasurementEntry(entry_id = "local", timestamp = 1_000L, weight_kg = 80.0),
                    BodyMeasurementEntry(
                        entry_id = "cloud",
                        timestamp = 2_000L,
                        weight_kg = 79.0,
                        isSynced = true
                    )
                )
            )
        }

        step("Verify per-card sync icons mirror the journal pattern") {
            composeTestRule.onNodeWithContentDescription("Local Only").assertExists()
            composeTestRule.onNodeWithContentDescription("Cloud Synced").assertExists()
        }
    }
}
