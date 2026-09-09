package com.example.healthjournal.domain.validation

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Date-entry masking copied from the Personal Card Date of Birth rules
 * ([com.example.healthjournal.viewmodel.PersonalCardViewModel.onDateOfBirthChanged]):
 * keeps digits only (max 8), auto-inserts dashes as yyyy-MM-dd, and tracks
 * the cursor over the typed digits. Pure and JVM-testable.
 */
object DateInputMask {

    fun mask(value: TextFieldValue): TextFieldValue {
        val digits = value.text.filter { it.isDigit() }.take(8)
        val typedDigits = value.text
            .take(value.selection.end.coerceIn(0, value.text.length))
            .count { it.isDigit() }
            .coerceIn(0, digits.length)
        val formatted = when {
            digits.length <= 4 -> digits
            digits.length <= 6 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
            else -> "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6)}"
        }
        val dashesBeforeCursor =
            (if (digits.length >= 5 && typedDigits > 4) 1 else 0) +
                (if (digits.length >= 7 && typedDigits > 6) 1 else 0)
        val cursorPosition = (typedDigits + dashesBeforeCursor).coerceIn(0, formatted.length)
        return TextFieldValue(formatted, TextRange(cursorPosition))
    }
}
