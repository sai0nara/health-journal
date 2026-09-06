package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.viewmodel.BodyAnalyticsViewModel
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

    /** Test context exposing seams for goal-flow seeding and DAO verification. */
    private class ScreenContext(
        val goalsRepository: GoalsRepository,
        val goalsDao: com.example.healthjournal.data.local.GoalDao,
        val goalsFlow: MutableStateFlow<List<com.example.healthjournal.data.local.GoalEntity>>
    )

    private fun setScreen(
        entries: List<BodyMeasurementEntry>,
        darkTheme: Boolean = false
    ): ScreenContext {
        every { repository.allEntries } returns MutableStateFlow(entries)
        val goalsFlow =
            MutableStateFlow(emptyList<com.example.healthjournal.data.local.GoalEntity>())
        val goalsDao = mockk<com.example.healthjournal.data.local.GoalDao>(relaxed = true)
        every { goalsDao.observeAll() } returns goalsFlow
        val goalsRepository = GoalsRepository(goalsDao)
        val viewModel = BodyMeasurementViewModel(repository, testDispatcher)
        val analyticsViewModel = BodyAnalyticsViewModel(
            repository,
            goalsRepository,
            testDispatcher
        )
        composeTestRule.setContent {
            com.example.healthjournal.ui.theme.HealthJournalTheme(darkTheme = darkTheme) {
                MeasurementsScreen(
                    viewModel = viewModel,
                    analyticsViewModel = analyticsViewModel,
                    onBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        return ScreenContext(goalsRepository, goalsDao, goalsFlow)
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

        step("Verify default Weight chart visible") {
            composeTestRule.onNodeWithTag("bm_chart_WEIGHT").assertExists()
        }
    }

    @Test
    fun trendChart_hiddenWithoutWeightData() {
        step("Open screen with girth-only entry") {
            setScreen(listOf(BodyMeasurementEntry(waist_cm = 85.0)))
        }

        step("Verify Weight page shows empty state instead of a chart") {
            composeTestRule.onNodeWithTag("bm_chart_WEIGHT").assertDoesNotExist()
            composeTestRule.onNodeWithTag("bm_param_empty_WEIGHT").assertExists()
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

    @Test
    fun analytics_sevenParameterTabsRender() {
        step("Open with one weighted record") {
            setScreen(listOf(BodyMeasurementEntry(entry_id = "e1", timestamp = 1_000L, weight_kg = 80.0)))
        }

        step("Verify all seven parameter tabs exist") {
            // Scoped to the tab row: chart headers repeat the parameter name.
            listOf("Weight", "Chest", "Waist", "Glute", "Thighs", "Calves", "Biceps")
                .forEach { label ->
                    composeTestRule.onNode(
                        hasAnyAncestor(hasTestTag("bm_tabs")) and hasText(label)
                    ).assertExists()
                }
        }
    }

    @Test
    fun analytics_tappingTabSwapsPlottedSeries() {
        step("Open with weight and waist records") {
            setScreen(
                listOf(
                    BodyMeasurementEntry(entry_id = "e1", timestamp = 4_000L, weight_kg = 80.0),
                    BodyMeasurementEntry(entry_id = "w1", timestamp = 5_000L, waist_cm = 85.0)
                )
            )
        }

        step("Verify default page is Weight and Waist chart not composed") {
            composeTestRule.onNodeWithTag("bm_chart_WEIGHT").assertExists()
            composeTestRule.onNodeWithTag("bm_chart_WAIST").assertDoesNotExist()
        }

        step("Tap the Waist tab") {
            composeTestRule.onNodeWithText("Waist").performClick()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag("bm_chart_WAIST")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }

        step("Verify Waist chart composed and Weight page disposed") {
            composeTestRule.onNodeWithTag("bm_chart_WAIST").assertExists()
            composeTestRule.onNodeWithTag("bm_chart_WEIGHT").assertDoesNotExist()
        }
    }

    @Test
    fun analytics_emptyParameterPageShowsFriendlyMessage() {
        step("Open with a weight-only record and switch to Calves") {
            setScreen(listOf(BodyMeasurementEntry(entry_id = "e1", timestamp = 1_000L, weight_kg = 80.0)))
            // Seven tabs overflow narrow displays: scroll the row first.
            // The scroll action lives on the row's internal viewport container.
            composeTestRule.onNode(
                hasScrollAction() and hasAnyDescendant(hasText("Calves"))
            ).performScrollToNode(hasText("Calves"))
            composeTestRule.onNodeWithText("Calves").performClick()
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag("bm_param_empty_CALF")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }

        step("Verify empty message for Calves") {
            composeTestRule.onNodeWithTag("bm_param_empty_CALF").assertExists()
            composeTestRule.onNodeWithText("No Calves data yet").assertExists()
        }
    }

    @Test
    fun goalSheet_opensPrefilledAndSavesViaRepository() {
        val ctx = setScreen(
            listOf(BodyMeasurementEntry(entry_id = "e1", timestamp = 1_000L, weight_kg = 80.0))
        )
        ctx.goalsFlow.value = listOf(
            com.example.healthjournal.data.local.GoalEntity("WEIGHT", 75.0, 1L)
        )

        step("Open the Set Goal sheet for Weight") {
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag("bm_set_goal")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("bm_set_goal").performClick()
        }

        step("Sheet is pre-filled with the current goal") {
            composeTestRule.onNodeWithTag("bm_goal_input")
                .assertTextContains("75", substring = true)
        }

        step("Saving persists the new target via repository") {
            composeTestRule.onNodeWithTag("bm_goal_input").performTextClearance()
            composeTestRule.onNodeWithTag("bm_goal_input").performTextInput("70.5")
            composeTestRule.onNodeWithTag("bm_goal_save").performClick()
            io.mockk.coVerify {
                ctx.goalsDao.upsertGoal(
                    match {
                        it.parameterId == "WEIGHT" &&
                            kotlin.math.abs(it.target - 70.5) < 0.001
                    }
                )
            }
        }
    }

    @Test
    fun goalSheet_clearDeletesGoalViaRepository() {
        val ctx = setScreen(
            listOf(BodyMeasurementEntry(entry_id = "e1", timestamp = 1_000L, weight_kg = 80.0))
        )
        ctx.goalsFlow.value = listOf(
            com.example.healthjournal.data.local.GoalEntity("WEIGHT", 75.0, 1L)
        )

        step("Open the Set Goal sheet and clear") {
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag("bm_set_goal")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("bm_set_goal").performClick()
            composeTestRule.onNodeWithTag("bm_goal_clear").performClick()
        }

        step("Clear deletes via repository") {
            io.mockk.coVerify { ctx.goalsDao.deleteById("WEIGHT") }
        }
    }

    @Test
    fun card_allParamsVisibleNotTruncated() {
        val fullEntry = BodyMeasurementEntry(
            entry_id = "full",
            timestamp = 3_000L,
            weight_kg = 78.5,
            chest_cm = 98.0,
            waist_cm = 85.0,
            glute_cm = 102.0,
            thigh_cm = 55.0,
            calf_cm = 37.0,
            bicep_cm = 33.0
        )

        step("Open with an entry that has all seven parameters") {
            setScreen(listOf(fullEntry))
        }

        step("Weight is displayed") {
            composeTestRule.onNodeWithText("78.5 kg").assertExists()
        }

        step("All circumference params are individually visible") {
            composeTestRule.onNodeWithText("Chest 98 cm").assertExists()
            composeTestRule.onNodeWithText("Waist 85 cm").assertExists()
            composeTestRule.onNodeWithText("Glute 102 cm").assertExists()
            composeTestRule.onNodeWithText("Thighs 55 cm").assertExists()
            composeTestRule.onNodeWithText("Calves 37 cm").assertExists()
            composeTestRule.onNodeWithText("Biceps 33 cm").assertExists()
        }
    }

    @Test
    fun card_weightOnlyShowsCleanLayout() {
        step("Open with a weight-only entry") {
            setScreen(listOf(BodyMeasurementEntry(entry_id = "wt", timestamp = 1_000L, weight_kg = 80.0)))
        }

        step("Weight is displayed as individual text node") {
            composeTestRule.onNodeWithText("80 kg").assertExists()
        }
    }

    @Test
    fun card_circumferenceOnlyShowsGrid() {
        step("Open with circumference-only entry") {
            setScreen(
                listOf(
                    BodyMeasurementEntry(
                        entry_id = "circ",
                        timestamp = 2_000L,
                        waist_cm = 85.0,
                        chest_cm = 98.0
                    )
                )
            )
        }

        step("Both circumference params are visible as individual text nodes") {
            composeTestRule.onNodeWithText("Waist 85 cm").assertExists()
            composeTestRule.onNodeWithText("Chest 98 cm").assertExists()
        }
    }

    @Test
    fun card_partialParamsOnlyNonNullShown() {
        step("Open with weight + waist only") {
            setScreen(
                listOf(
                    BodyMeasurementEntry(
                        entry_id = "partial",
                        timestamp = 1_500L,
                        weight_kg = 75.0,
                        waist_cm = 80.0
                    )
                )
            )
        }

        step("Recorded params are visible as individual text nodes") {
            composeTestRule.onNodeWithText("75 kg").assertExists()
            composeTestRule.onNodeWithText("Waist 80 cm").assertExists()
        }
    }

    @Test
    fun goalSheet_invalidInputShowsErrorAndBlocksSave() {
        val ctx = setScreen(
            listOf(BodyMeasurementEntry(entry_id = "e1", timestamp = 1_000L, weight_kg = 80.0))
        )

        step("Open the Set Goal sheet and enter an out-of-bounds value") {
            composeTestRule.waitUntil(5_000) {
                composeTestRule.onAllNodesWithTag("bm_set_goal")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithTag("bm_set_goal").performClick()
            composeTestRule.onNodeWithTag("bm_goal_input").performTextInput("9999")
            composeTestRule.onNodeWithTag("bm_goal_save").performClick()
        }

        step("Inline error appears and save is blocked") {
            composeTestRule.onNodeWithTag("bm_goal_error", useUnmergedTree = true)
                .assertExists()
                .assertTextEquals("Too large (max 500 kg)")
            io.mockk.coVerify(exactly = 0) { ctx.goalsDao.upsertGoal(any()) }
        }
    }
}
