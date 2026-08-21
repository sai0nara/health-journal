package com.example.healthjournal.util

import android.text.Html

/**
 * Normalizes HTML produced by the rich text editor before persistence.
 *
 * richeditor-compose encodes non-ASCII characters as HTML5 named
 * character references (e.g., Cyrillic "п" becomes "&pcy;"). Its own
 * HTML decoder mishandles whitespace adjacent to those references,
 * which made saved entries render without spaces between words.
 * Storing raw UTF-8 instead avoids the defective decode path entirely.
 *
 * Only entities whose decoded value contains non-ASCII characters are
 * converted; ASCII-significant escapes (&amp;, &lt;, &gt;, &quot;,
 * &apos;, editor escapes like &excl;) are preserved so HTML structure
 * and editor round-trips remain intact.
 */
object HtmlEntities {

    private val NAMED_ENTITY = Regex("&([a-zA-Z][a-zA-Z0-9]{1,31});")

    /**
     * Decodes named character references that resolve to non-ASCII
     * characters, leaving all other content untouched.
     *
     * @param html the HTML to normalize
     * @param decodeSingle decodes a single entity NAME to its value;
     *   defaults to the Android framework entity table. Injectable for tests.
     */
    fun decodeNonAsciiNamedEntities(
        html: String,
        decodeSingle: (String) -> String = ::decodeWithFramework
    ): String = NAMED_ENTITY.replace(html) { match ->
        val decoded = decodeSingle(match.groupValues[1])
        if (decoded.any { it.code > 0x7E }) decoded else match.value
    }

    private fun decodeWithFramework(name: String): String =
        Html.fromHtml("&$name;", Html.FROM_HTML_MODE_COMPACT).toString()
}
