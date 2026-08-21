package com.example.healthjournal.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the Medical App semantic color system.
 *
 * Verifies that both the Light ("Medical Standard") and Dark
 * ("Eye-strain Reduction") palettes expose the exact brand colors
 * defined in the track specification, and that the correct scheme
 * is selected based on the system dark-theme flag.
 */
class MedicalColorSystemTest {

    // ---------------------------------------------------------------
    // Light Palette ("Medical Standard")
    // ---------------------------------------------------------------

    @Test
    fun lightPalette_exposesExactMedicalStandardValues() {
        assertEquals(Color(0xFFF8F9FA), LightBackground)
        assertEquals(Color(0xFFFFFFFF), LightSurface)
        assertEquals(Color(0xFF0A66C2), LightPrimary)
        assertEquals(Color(0xFF20C997), LightSecondary)
        assertEquals(Color(0xFF212529), LightTextPrimary)
        assertEquals(Color(0xFF6C757D), LightTextSecondary)
        assertEquals(Color(0xFFDC3545), LightError)
    }

    @Test
    fun lightColorScheme_mapsSemanticTokensToMaterialRoles() {
        val scheme = lightMedicalColorScheme()

        assertEquals(Color(0xFFF8F9FA), scheme.background)
        assertEquals(Color(0xFFFFFFFF), scheme.surface)
        assertEquals(Color(0xFF0A66C2), scheme.primary)
        assertEquals(Color(0xFF20C997), scheme.secondary)
        assertEquals(Color(0xFF212529), scheme.onBackground)
        assertEquals(Color(0xFF212529), scheme.onSurface)
        assertEquals(Color(0xFF6C757D), scheme.onSurfaceVariant)
        assertEquals(Color(0xFFDC3545), scheme.error)
    }

    // ---------------------------------------------------------------
    // Dark Palette ("Eye-strain Reduction")
    // ---------------------------------------------------------------

    @Test
    fun darkPalette_exposesExactEyeStrainReductionValues() {
        assertEquals(Color(0xFF121212), DarkBackground)
        assertEquals(Color(0xFF1E1E1E), DarkSurface)
        assertEquals(Color(0xFF4A90E2), DarkPrimary)
        assertEquals(Color(0xFF48D8A4), DarkSecondary)
        assertEquals(Color(0xFFE9ECEF), DarkTextPrimary)
        assertEquals(Color(0xFFA0AAB2), DarkTextSecondary)
        assertEquals(Color(0xFFEF5350), DarkError)
    }

    @Test
    fun darkColorScheme_mapsSemanticTokensToMaterialRoles() {
        val scheme = darkMedicalColorScheme()

        assertEquals(Color(0xFF121212), scheme.background)
        assertEquals(Color(0xFF1E1E1E), scheme.surface)
        assertEquals(Color(0xFF4A90E2), scheme.primary)
        assertEquals(Color(0xFF48D8A4), scheme.secondary)
        assertEquals(Color(0xFFE9ECEF), scheme.onBackground)
        assertEquals(Color(0xFFE9ECEF), scheme.onSurface)
        assertEquals(Color(0xFFA0AAB2), scheme.onSurfaceVariant)
        assertEquals(Color(0xFFEF5350), scheme.error)
    }

    // ---------------------------------------------------------------
    // System Theme Selection
    // ---------------------------------------------------------------

    @Test
    fun medicalColorScheme_returnsDarkSchemeWhenDarkThemeRequested() {
        val scheme = medicalColorScheme(darkTheme = true)

        assertEquals(Color(0xFF121212), scheme.background)
        assertEquals(Color(0xFF4A90E2), scheme.primary)
    }

    @Test
    fun medicalColorScheme_returnsLightSchemeWhenDarkThemeNotRequested() {
        val scheme = medicalColorScheme(darkTheme = false)

        assertEquals(Color(0xFFF8F9FA), scheme.background)
        assertEquals(Color(0xFF0A66C2), scheme.primary)
    }

    @Test
    fun statusBarIcons_shouldBeDarkContentInLightMode() {
        assertEquals(true, statusBarIconsDark(darkTheme = false))
    }

    @Test
    fun statusBarIcons_shouldBeLightContentInDarkMode() {
        assertEquals(false, statusBarIconsDark(darkTheme = true))
    }
}
