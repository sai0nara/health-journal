package com.example.healthjournal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Light theme palette ("Medical Standard"): clinical off-white surfaces
 * with high-contrast trust-blue accents.
 */
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF0A66C2)
val LightSecondary = Color(0xFF20C997)
val LightTextPrimary = Color(0xFF212529)
val LightTextSecondary = Color(0xFF6C757D)
val LightError = Color(0xFFDC3545)

/**
 * Dark theme palette ("Eye-strain Reduction"): deep charcoal backgrounds
 * with elevated gray surfaces and slightly desaturated accents.
 */
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkPrimary = Color(0xFF4A90E2)
val DarkSecondary = Color(0xFF48D8A4)
val DarkTextPrimary = Color(0xFFE9ECEF)
val DarkTextSecondary = Color(0xFFA0AAB2)
val DarkError = Color(0xFFEF5350)

/**
 * Builds the Material 3 [ColorScheme] for the Light ("Medical Standard") theme.
 */
fun lightMedicalColorScheme(): ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = LightTextSecondary,
    error = LightError,
    onError = Color.White
)

/**
 * Builds the Material 3 [ColorScheme] for the Dark ("Eye-strain Reduction") theme.
 */
fun darkMedicalColorScheme(): ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF0B3B2E),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = DarkTextSecondary,
    error = DarkError,
    onError = Color(0xFF220507)
)

/**
 * Selects the medical color scheme based on the system dark-theme state.
 */
fun medicalColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) darkMedicalColorScheme() else lightMedicalColorScheme()

/**
 * Determines whether status bar icons should be rendered dark.
 * Dark icons are legible on the light theme's bright background;
 * light icons are legible on the dark theme's charcoal background.
 */
fun statusBarIconsDark(darkTheme: Boolean): Boolean = !darkTheme
