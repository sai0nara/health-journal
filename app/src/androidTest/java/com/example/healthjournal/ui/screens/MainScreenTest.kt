package com.example.healthjournal.ui.screens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.ui.components.ProfileSwitcher
import com.example.healthjournal.ui.components.TodayBottomBar
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.viewmodel.TodayActivityCategory
import com.example.healthjournal.viewmodel.TodayActivityItem
import com.example.healthjournal.viewmodel.TodayDashboardUiState
import com.example.healthjournal.viewmodel.TodayMeasurementsCardState
import com.example.healthjournal.viewmodel.TodayVitalSlide
import com.example.healthjournal.viewmodel.TodayWorkoutsCardState
import com.example.healthjournal.viewmodel.VitalMetricTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_showsLogoTitleNoGearIconAndNoStaticTiles() {
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(
                    bottomBar = { TodayBottomBar(currentRoute = "main", onNavigate = {}) }
                )
            }
        }

        composeTestRule.onNodeWithTag("app_logo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Health Journal", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("top_bar_settings_gear").assertDoesNotExist()
        listOf("history", "vitals", "workout", "measurements", "presets", "archive", "export", "personal_card", "settings")
            .forEach { route ->
                composeTestRule.onNodeWithTag("main_tile_$route").assertDoesNotExist()
            }
        composeTestRule.onNodeWithTag("bottom_nav_today").assertIsSelected()
    }

    @Test
    fun mainScreen_emptyStateRendersSingleVitalsSlideAndEmptyActivity() {
        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen()
            }
        }

        composeTestRule.onAllNodesWithTag("vitals_pager_dot").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("No data yet • Tap to log").assertCountEquals(3)
        composeTestRule.onNodeWithText("No recent activity").assertIsDisplayed()
    }

    @Test
    fun mainScreen_populatedCardsAndRecentActivityRenderAndDrillDown() {
        val openedTabs = mutableListOf<VitalMetricTab>()
        var openedWorkouts = false
        var openedMeasurements = false
        var openedHistory = false

        val state = TodayDashboardUiState(
            vitalsSlides = listOf(
                TodayVitalSlide(
                    tab = VitalMetricTab.BLOOD_PRESSURE,
                    latestValueDisplay = "118/76 mmHg",
                    secondaryValueDisplay = "64 bpm",
                    timestampMillis = System.currentTimeMillis(),
                    sparklinePoints = listOf(115f, 118f)
                ),
                TodayVitalSlide(
                    tab = VitalMetricTab.HEART_RATE,
                    latestValueDisplay = "64 bpm",
                    timestampMillis = System.currentTimeMillis(),
                    sparklinePoints = listOf(62f, 64f)
                )
            ),
            workoutsCard = TodayWorkoutsCardState(
                latestWorkoutName = "Strength Training",
                latestTimestampMillis = System.currentTimeMillis(),
                completedThisWeek = 3,
                weeklyTarget = 4,
                progressFraction = 0.75f,
                isEmptyState = false
            ),
            measurementsCard = TodayMeasurementsCardState(
                latestWeightDisplay = "82.4 kg",
                latestTimestampMillis = System.currentTimeMillis(),
                weeklyDeltaDisplay = "-0.6 kg",
                sparklinePoints = listOf(83.0f, 82.4f),
                isEmptyState = false
            ),
            recentActivity = listOf(
                TodayActivityItem(
                    id = "j_1",
                    category = TodayActivityCategory.VITALS,
                    summary = "Vitals - 118/76 mmHg",
                    timestampMillis = System.currentTimeMillis(),
                    isToday = true,
                    isYesterday = false,
                    formattedTime = "08:30 AM",
                    vitalTab = VitalMetricTab.BLOOD_PRESSURE
                )
            )
        )

        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(
                    state = state,
                    onOpenVitals = { openedTabs += it },
                    onOpenWorkouts = { openedWorkouts = true },
                    onOpenMeasurements = { openedMeasurements = true },
                    onOpenHistory = { openedHistory = true }
                )
            }
        }

        composeTestRule.onAllNodesWithTag("vitals_pager_dot").assertCountEquals(2)
        composeTestRule.onNodeWithText("118/76 mmHg • 64 bpm").assertIsDisplayed()
        composeTestRule.onNodeWithText("3/4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Measurements").assertIsDisplayed()
        composeTestRule.onNodeWithText("08:30 AM").assertIsDisplayed()

        composeTestRule.onNodeWithTag("overview_vitals_card").performClick()
        composeTestRule.onNodeWithTag("overview_workouts_card").performClick()
        composeTestRule.onNodeWithTag("overview_measurements_card").performClick()
        composeTestRule.onNodeWithTag("recent_activity_view_all").performClick()

        assertEquals(listOf(VitalMetricTab.BLOOD_PRESSURE), openedTabs)
        assertTrue(openedWorkouts)
        assertTrue(openedMeasurements)
        assertTrue(openedHistory)
    }

    @Test
    fun mainScreen_quickLogFabOpensBottomSheetWithPresetsAndShortcuts() {
        val state = TodayDashboardUiState(
            quickLogPresets = listOf(WorkoutPreset(id = "p1", name = "Push Day"))
        )
        var startedPreset: WorkoutPreset? = null

        composeTestRule.setContent {
            HealthJournalTheme {
                MainScreen(
                    state = state,
                    onStartPresetWorkout = { startedPreset = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("today_quick_log_fab").performClick()
        composeTestRule.onNodeWithTag("quick_log_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Push Day").performClick()

        assertEquals("p1", startedPreset?.id)
    }

    @Test
    fun moreScreen_exposesSettingsExportAndPresets() {
        var settingsOpened = false
        var exportOpened = false
        var presetsOpened = false

        composeTestRule.setContent {
            HealthJournalTheme {
                MoreScreen(
                    onOpenSettings = { settingsOpened = true },
                    onOpenExport = { exportOpened = true },
                    onOpenPresets = { presetsOpened = true },
                    bottomBar = { TodayBottomBar(currentRoute = "more", onNavigate = {}) }
                )
            }
        }

        composeTestRule.onNodeWithTag("bottom_nav_more").assertIsSelected()
        composeTestRule.onNodeWithTag("more_item_settings").performClick()
        composeTestRule.onNodeWithTag("more_item_export").performClick()
        composeTestRule.onNodeWithTag("more_item_presets").performClick()

        assertTrue(settingsOpened)
        assertTrue(exportOpened)
        assertTrue(presetsOpened)
    }

    @Test
    fun profileSwitcher_includesPersonalCardAboveManageProfiles() {
        var personalCardClicked = false
        composeTestRule.setContent {
            HealthJournalTheme {
                ProfileSwitcher(
                    profiles = listOf(
                        ProfileEntity(id = SELF_PROFILE_ID, name = "Me", relationship = "Self")
                    ),
                    activeProfileId = SELF_PROFILE_ID,
                    onProfileSelected = {},
                    onManageProfiles = {},
                    onPersonalCard = { personalCardClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("profile_switcher").performClick()
        composeTestRule.onNodeWithTag("profile_switcher_personal_card").performClick()

        assertTrue(personalCardClicked)
    }
}
