package com.example.healthjournal.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.qameta.allure.kotlin.Feature
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests verifying that the system status bar remains legible in both
 * themes: dark icons over the light theme's bright background and light
 * icons over the dark theme's charcoal background, with a transparent
 * status bar aligned to the app background (edge-to-edge).
 */
@Feature("Medical App Color System")
@RunWith(AndroidJUnit4::class)
class StatusBarAppearanceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun insetsController(): WindowInsetsControllerCompat =
        WindowCompat.getInsetsController(
            composeTestRule.activity.window,
            composeTestRule.activity.window.decorView
        )

    @Test
    fun statusBar_showsDarkIconsAndTransparentBarInLightMode() {
        composeTestRule.activity.enableEdgeToEdge()
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = false) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(true, insetsController().isAppearanceLightStatusBars)
        assertEquals(Color.TRANSPARENT, composeTestRule.activity.window.statusBarColor)
    }

    @Test
    fun statusBar_showsLightIconsAndTransparentBarInDarkMode() {
        composeTestRule.activity.enableEdgeToEdge()
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = true) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(false, insetsController().isAppearanceLightStatusBars)
        assertEquals(Color.TRANSPARENT, composeTestRule.activity.window.statusBarColor)
    }

    @Test
    fun statusBar_iconAppearance_updatesInstantlyWhenThemeToggles() {
        val darkTheme = mutableStateOf(false)
        composeTestRule.activity.enableEdgeToEdge()
        composeTestRule.setContent {
            HealthJournalTheme(darkTheme = darkTheme.value) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(true, insetsController().isAppearanceLightStatusBars)

        composeTestRule.runOnIdle { darkTheme.value = true }
        composeTestRule.waitForIdle()
        assertEquals(false, insetsController().isAppearanceLightStatusBars)

        composeTestRule.runOnIdle { darkTheme.value = false }
        composeTestRule.waitForIdle()
        assertEquals(true, insetsController().isAppearanceLightStatusBars)
    }
}
