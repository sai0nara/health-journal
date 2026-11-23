package com.example.healthjournal.util

import java.util.Locale

/**
 * Pure Kotlin HTML-to-plain-text converter for single-line UI summaries
 * (e.g., Recent Activity on the Today Dashboard). Keeps JVM unit tests
 * independent of Android's [android.text.Html] framework stubs.
 */
object HtmlPlainText {

    private val BLOCK_OR_BREAK_TAG = Regex(
        pattern = "</?(?:br|p|div|li|tr|h[1-6])(?:\\s[^>]*)?/?>|</>",
        option = RegexOption.IGNORE_CASE
    )
    private val ANY_TAG = Regex("<[^>]*>")
    private val NAMED_ENTITY = Regex("&([a-zA-Z][a-zA-Z0-9]{1,31});")
    private val AMP_ENTITY = Regex("&amp;", RegexOption.IGNORE_CASE)
    private val NUMERIC_DEC_ENTITY = Regex("&#(\\d+);")
    private val NUMERIC_HEX_ENTITY = Regex("&#[xX]([0-9a-fA-F]+);")
    private val WHITESPACE = Regex("\\s+")

    // HTML5 named references for printable ASCII punctuation emitted by richeditor-compose
    // (e.g., '#' -> &num;, '!' -> &excl;) alongside standard XML/HTML entities.
    private val NAMED_ASCII = mapOf(
        "num" to "#",
        "excl" to "!",
        "quest" to "?",
        "period" to ".",
        "comma" to ",",
        "colon" to ":",
        "semi" to ";",
        "lpar" to "(",
        "rpar" to ")",
        "lsqb" to "[",
        "rsqb" to "]",
        "lcub" to "{",
        "rcub" to "}",
        "percnt" to "%",
        "dollar" to "$",
        "ast" to "*",
        "plus" to "+",
        "equals" to "=",
        "sol" to "/",
        "bsol" to "\\",
        "commat" to "@",
        "Hat" to "^",
        "grave" to "`",
        "verbar" to "|",
        "vert" to "|",
        "lowbar" to "_",
        "hyphen" to "-",
        "dash" to "-",
        "tilde" to "~",
        "nbsp" to " ",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'"
    )

    private val LEGACY_NAMED = mapOf(
        "nbsp" to " ",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'"
    )

    /**
     * Strips HTML tags, decodes common HTML character entities, and collapses
     * all whitespace/line breaks into single spaces.
     */
    fun toPlainText(html: String): String {
        if (html.isBlank()) return ""
        val withoutBlocks = BLOCK_OR_BREAK_TAG.replace(html, " ")
        val withoutTags = ANY_TAG.replace(withoutBlocks, "")
        val decoded = decodeEntities(withoutTags)
        return WHITESPACE.replace(decoded, " ").trim()
    }

    private fun decodeEntities(input: String): String {
        if (!input.contains('&')) return input
        var text = NAMED_ENTITY.replace(input) { match ->
            val name = match.groupValues[1]
            if (name.equals("amp", ignoreCase = true)) {
                match.value
            } else {
                NAMED_ASCII[name] ?: LEGACY_NAMED[name.lowercase(Locale.ROOT)] ?: match.value
            }
        }
        text = NUMERIC_DEC_ENTITY.replace(text) { match ->
            match.groupValues[1].toIntOrNull()
                ?.takeIf { Character.isValidCodePoint(it) }
                ?.let { String(Character.toChars(it)) }
                ?: match.value
        }
        text = NUMERIC_HEX_ENTITY.replace(text) { match ->
            match.groupValues[1].toIntOrNull(16)
                ?.takeIf { Character.isValidCodePoint(it) }
                ?.let { String(Character.toChars(it)) }
                ?: match.value
        }
        // Decode &amp; last so double-encoded entities like &amp;lt; stay "&lt;"
        return AMP_ENTITY.replace(text, "&")
    }
}
