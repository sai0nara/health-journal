package com.example.healthjournal.localization

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces the string-externalization rule: UI sources must never embed
 * user-facing text in `Text("...")` (including multi-line), `placeholder`,
 * `contentDescription`, `showSnackbar("...")`, or `Toast.makeText(..., "...")`
 * literals; all such text must resolve via stringResource /
 * pluralStringResource so the Android resource system can substitute the
 * Russian catalog on ru-RU devices. Literals without letters (whitespace,
 * pure symbols) and machine constants (status codes, routes, keys) are
 * exempt. Display strings built inside ViewModels (sync status, Toast
 * messages) are converted to resource IDs by hand following the existing
 * validation `errorResId` pattern and verified by review + locale UI tests.
 * RED until every screen is externalized.
 */
class HardcodedStringAuditTest {

    private val carriers = listOf(
        Regex("""Text\(\s*"((?:[^"\\]|\\.)*)""", RegexOption.DOT_MATCHES_ALL),
        Regex("""placeholder\s*=\s*"((?:[^"\\]|\\.)*)"""),
        Regex("""contentDescription\s*=\s*"((?:[^"\\]|\\.)*)"""),
        Regex("""showSnackbar\(\s*"((?:[^"\\]|\\.)*)""", RegexOption.DOT_MATCHES_ALL),
        Regex("""Toast\.makeText\(\s*[^,]+,\s*"((?:[^"\\]|\\.)*)""", RegexOption.DOT_MATCHES_ALL)
    )
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
                val content = file.readText()
                carriers.forEach { carrier ->
                    carrier.findAll(content)
                        .map { it.groupValues[1] to it.range.first }
                        .filter { isUserFacing(it.first) }
                        .forEach { (literal, offset) ->
                            val line = content.substring(0, offset)
                                .count { it == '\n' } + 1
                            violations.add(
                                "${file.relativeTo(mainDir)}:$line: \"$literal\""
                            )
                        }
                }
            }

        assertTrue(
            "Hardcoded user-facing strings found - externalize to values/strings.xml:\n" +
                violations.sorted().joinToString("\n"),
            violations.isEmpty()
        )
    }
}
