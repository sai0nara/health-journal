package com.example.healthjournal.util

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testParseBold() {
        val input = "**bold**"
        val parsed = MarkdownParser.parse(input)
        
        assertEquals("bold", parsed.text)
        val span = parsed.spanStyles.first()
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }

    @Test
    fun testParseItalic() {
        val input = "*italic*"
        val parsed = MarkdownParser.parse(input)
        
        assertEquals("italic", parsed.text)
        val span = parsed.spanStyles.first()
        assertEquals(FontStyle.Italic, span.item.fontStyle)
    }

    @Test
    fun testParseHeader() {
        val input = "# Header"
        val parsed = MarkdownParser.parse(input)
        
        assertEquals("Header", parsed.text)
        val span = parsed.spanStyles.first()
        assertEquals(24.sp, span.item.fontSize) // Assuming h1 is 24.sp
    }
}
