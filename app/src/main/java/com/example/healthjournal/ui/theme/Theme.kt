package com.example.healthjournal.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Root theme for Health Journal.
 *
 * Follows the OS dark-mode preference by default (via [isSystemInDarkTheme])
 * and applies the medical semantic color palette. Also keeps the status bar
 * icon appearance in sync with the active theme so icons remain visible.
 */
@Composable
fun HealthJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = medicalColorScheme(darkTheme),
        content = content
    )
    StatusBarAppearanceEffect(darkTheme)
}

/**
 * Keeps the system status bar icon appearance aligned with the active theme:
 * dark icons on the light theme, light icons on the dark theme.
 */
@Composable
private fun StatusBarAppearanceEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                statusBarIconsDark(darkTheme)
        }
    }
}
