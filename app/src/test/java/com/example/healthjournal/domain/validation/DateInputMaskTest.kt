package com.example.healthjournal.domain.validation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for DateInputMask, which copies the Personal Card Date of
 * Birth entry rules: digits are kept (max 8), non-digits stripped, dashes
 * auto-inserted as yyyy-MM-dd, and the cursor tracks the typed digits.
 */
class DateInputMaskTest {

    private fun masked(text: String, cursor: Int = text.length): TextFieldValue =
        DateInputMask.mask(TextFieldValue(text, TextRange(cursor)))

    @Test
    fun fullDate_insertsDashes() {
        val result = masked("20240115", 8)

        assertEquals("2024-01-15", result.text)
        assertEquals(10, result.selection.end)
    }

    @Test
    fun partialInput_formatsProgressively() {
        assertEquals("2024", masked("2024", 4).text)
        assertEquals("2024-01", masked("202401", 6).text)
    }

    @Test
    fun nonDigits_areStripped() {
        val result = masked("20a24b01", 8)

        assertEquals("2024-01", result.text)
    }

    @Test
    fun input_cappedAtEightDigits() {
        val result = masked("202401151", 9)

        assertEquals("2024-01-15", result.text)
        assertEquals(10, result.selection.end)
    }

    @Test
    fun midTextEdit_keepsCursorOnTypedDigits() {
        val result = masked("2024-0115", 7)

        assertEquals("2024-01-15", result.text)
        assertEquals(7, result.selection.end)
    }

    @Test
    fun blankInput_staysBlank() {
        val result = masked("", 0)

        assertEquals("", result.text)
        assertEquals(0, result.selection.end)
    }
}
