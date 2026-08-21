package com.example.healthjournal.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for HTML named-entity normalization used when persisting
 * rich text. Only entities that decode to non-ASCII characters are
 * converted to raw UTF-8; ASCII-significant escapes (structural or
 * produced by the editor, e.g. &amp;, &lt;, &excl;) must be preserved
 * so HTML structure and editor round-trips remain intact.
 */
class HtmlEntitiesTest {

    /** Fake framework decoder mapping a few known names, echoing unknowns. */
    private val fakeDecoder: (String) -> String = { name ->
        when (name) {
            "pcy" -> "п"
            "rcy" -> "р"
            "icy" -> "и"
            "vcy" -> "в"
            "iecy" -> "е"
            "tcy" -> "т"
            "mcy" -> "м"
            "nbsp" -> "\u00A0"
            else -> "&$name;"
        }
    }

    @Test
    fun decodesNonAsciiNamedEntitiesToRawUtf8() {
        val input = "<p>&pcy;&rcy;&icy;&vcy;&iecy;&tcy; &mcy;&icy;&rcy;</p>"
        assertEquals("<p>привет мир</p>", HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder))
    }

    @Test
    fun preservesAsciiStructuralEscapes() {
        val input = "<p>a &amp; b &lt;tag&gt; &quot;q&quot; &apos;a&apos;</p>"
        assertEquals(input, HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder))
    }

    @Test
    fun preservesEditorEscapedAsciiPunctuation() {
        val input = "<p>I feel great&excl;</p>"
        assertEquals(input, HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder))
    }

    @Test
    fun preservesUnknownOrMalformedSequences() {
        val input = "<p>100 &thisisnotanentity & broken</p>"
        assertEquals(input, HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder))
    }

    @Test
    fun leavesPlainTextUntouched() {
        val input = "<p>plain ascii text</p>"
        assertEquals(input, HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder))
    }

    @Test
    fun handlesMixedContentAndAttributes() {
        val input = "<p style=\"color:red\">&pcy;&rcy; &amp; hello</p>"
        assertEquals(
            "<p style=\"color:red\">пр &amp; hello</p>",
            HtmlEntities.decodeNonAsciiNamedEntities(input, fakeDecoder)
        )
    }
}
