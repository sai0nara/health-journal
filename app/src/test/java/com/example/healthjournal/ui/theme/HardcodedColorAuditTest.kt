package com.example.healthjournal.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces the semantic token migration rule: UI sources must never
 * reference absolute colors (e.g., Color(0xFF...), Color.Black); all
 * colors must come from MaterialTheme.colorScheme roles. Only the
 * theme definition package (ui/theme) may declare literal colors.
 */
class HardcodedColorAuditTest {

    private val forbiddenPattern = Regex(
        """Color\(0x[0-9A-Fa-f]{6,8}\)|Color\.(Blue|White|Black|Red|Gray|Green|Yellow|Cyan|Magenta|LightGray|DarkGray|Transparent)\b"""
    )

    private fun resolveMainSourceDir(): File {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/java from ${System.getProperty("user.dir")}")
    }

    @Test
    fun mainSources_containNoHardcodedColorsOutsideThemePackage() {
        val mainDir = resolveMainSourceDir()
        val violations = mutableListOf<String>()

        mainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.contains("${File.separator}ui${File.separator}theme${File.separator}") && !it.path.contains("/ui/theme/") }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    if (forbiddenPattern.containsMatchIn(line)) {
                        violations.add("${file.relativeTo(mainDir)}:${index + 1}: ${line.trim()}")
                    }
                }
            }

        assertTrue(
            "Hardcoded colors found outside ui/theme - migrate to MaterialTheme.colorScheme:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
