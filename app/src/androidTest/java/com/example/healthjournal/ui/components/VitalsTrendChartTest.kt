package com.example.healthjournal.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import com.example.healthjournal.data.local.UnitSystem
import com.example.healthjournal.ui.theme.HealthJournalTheme
import com.example.healthjournal.util.RecordingHapticFeedback
import com.example.healthjournal.viewmodel.VitalMetricTab
import com.example.healthjournal.viewmodel.VitalPoint
import com.example.healthjournal.viewmodel.VitalsTimeWindow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VitalsTrendChartTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val now = System.currentTimeMillis()

    private val testBpPoints = listOf(
        VitalPoint(timestamp = now - 2 * 86400000L, value = 118.0, secondaryValue = 76.0),
        VitalPoint(timestamp = now - 86400000L, value = 124.0, secondaryValue = 82.0)
    )

    private val testGlucosePoints = listOf(
        VitalPoint(timestamp = now - 2 * 86400000L, value = 5.2),
        VitalPoint(timestamp = now - 86400000L, value = 5.8)
    )

    @Test
    fun chartRenders_withDataPoints_displaysTestTag() {
        composeTestRule.setContent {
            HealthJournalTheme {
                VitalsTrendChart(
                    metric = VitalMetricTab.BLOOD_PRESSURE,
                    window = VitalsTimeWindow.DAYS_30,
                    points = testBpPoints,
                    unitSystem = UnitSystem.METRIC
                )
            }
        }

        composeTestRule.onNodeWithTag("vitals_trend_chart").assertIsDisplayed()
    }

    @Test
    fun chartRenders_emptyState_whenNoDataPoints() {
        composeTestRule.setContent {
            HealthJournalTheme {
                VitalsTrendChart(
                    metric = VitalMetricTab.HEART_RATE,
                    window = VitalsTimeWindow.DAYS_7,
                    points = emptyList(),
                    unitSystem = UnitSystem.METRIC
                )
            }
        }

        composeTestRule.onNodeWithTag("vitals_trend_chart_empty").assertIsDisplayed()
    }

    @Test
    fun touchScrubber_triggersHapticFeedback_andDisplaysTooltip() {
        val recordingHaptic = RecordingHapticFeedback()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides recordingHaptic) {
                HealthJournalTheme {
                    VitalsTrendChart(
                        metric = VitalMetricTab.BLOOD_GLUCOSE,
                        window = VitalsTimeWindow.DAYS_30,
                        points = testGlucosePoints,
                        unitSystem = UnitSystem.METRIC
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("vitals_trend_chart").performTouchInput {
            swipeRight()
        }

        composeTestRule.waitForIdle()
        assertTrue("Scrubber drag must record haptic feedback", recordingHaptic.events.isNotEmpty())
        composeTestRule.onNodeWithTag("vitals_chart_tooltip").assertIsDisplayed()
    }
}
