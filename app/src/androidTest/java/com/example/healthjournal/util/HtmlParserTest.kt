package com.example.healthjournal.util

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HtmlParserTest {

    @Test
    fun testParseBold() {
        val input = "<b>bold</b>"
        val parsed = HtmlParser.parse(input)
        
        assertEquals("bold", parsed.text)
        val span = parsed.spanStyles.first()
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }

    @Test
    fun testParseItalic() {
        val input = "<i>italic</i>"
        val parsed = HtmlParser.parse(input)
        
        assertEquals("italic", parsed.text)
        val span = parsed.spanStyles.first()
        assertEquals(FontStyle.Italic, span.item.fontStyle)
    }

    @Test
    fun testParseMixed() {
        val input = "<b>bold</b> and <i>italic</i>"
        val parsed = HtmlParser.parse(input)
        
        assertEquals("bold and italic", parsed.text)
        // Verify multiple spans
        assertEquals(2, parsed.spanStyles.size)
    }
}
