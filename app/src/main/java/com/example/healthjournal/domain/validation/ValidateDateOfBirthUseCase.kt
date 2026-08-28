package com.example.healthjournal.domain.validation

import com.example.healthjournal.R
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class ValidateDateOfBirthUseCase {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    operator fun invoke(dateString: String): ValidationResult {
        if (dateString.isBlank()) {
            return ValidationResult.Valid
        }

        val date = try {
            LocalDate.parse(dateString, formatter)
        } catch (e: DateTimeParseException) {
            return ValidationResult.Invalid(R.string.error_invalid_date_format)
        }

        val today = LocalDate.now()

        if (date.isAfter(today)) {
            return ValidationResult.Invalid(R.string.error_date_in_future)
        }

        val age = Period.between(date, today).years
        if (age > 130) {
            return ValidationResult.Invalid(R.string.error_age_too_high)
        }

        return ValidationResult.Valid
    }
}
