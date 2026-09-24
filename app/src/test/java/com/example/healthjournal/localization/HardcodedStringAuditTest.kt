package com.example.healthjournal.localization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces the string-externalization rule: UI sources must never embed
 * user-facing text in `Text("...")` or `contentDescription = "..."` literals;
 * all such text must resolve via stringResource / pluralStringResource so the
 * Android resource system can substitute the Russian catalog on ru-RU
 * devices. Literals without letters (whitespace, pure symbols) are exempt.
 * RED until every screen is externalized.
 */
class HardcodedStringAuditTest {

    private val textLiteral = Regex("""Text\(\s*"((?:[^"\\]|\\.)*)"""")
    private val contentDescriptionLiteral =
        Regex("""contentDescription\s*=\s*"((?:[^"\\]|\\.)*)"""")
    private val interpolation = Regex("""\$\{[^}]*}|\$\w+""")
    private val hasLetter = Regex("""[A-Za-zА-Яа-яЁё]""")

    private fun resolveMainSourceDir(): File {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/java from ${System.getProperty("user.dir")}")
    }

    private fun isUserFacing(literal: String): Boolean {
        val static = interpolation.replace(literal, "")
        return hasLetter.containsMatchIn(static)
    }

    @Test
    fun mainSources_containNoHardcodedUserFacingLiterals() {
        val mainDir = resolveMainSourceDir()
        val violations = mutableListOf<String>()

        mainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val hits = textLiteral.findAll(line)
                        .map { it.groupValues[1] } +
                        contentDescriptionLiteral.findAll(line)
                            .map { it.groupValues[1] }
                    hits.filter(::isUserFacing).forEach { literal ->
                        violations.add(
                            "${file.relativeTo(mainDir)}:${index + 1}: \"$literal\""
                        )
                    }
                }
            }

        assertTrue(
            "Hardcoded user-facing strings found - externalize to values/strings.xml:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
