package com.example.healthjournal.domain.validation

import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem

class ValidateHeightUseCase {
    companion object {
        const val MIN_HEIGHT_CM = 20.0
        const val MAX_HEIGHT_CM = 275.0
        const val MIN_HEIGHT_INCHES = 8.0
        const val MAX_HEIGHT_INCHES = 108.0
    }

    operator fun invoke(heightCm: Double?, unitSystem: UnitSystem): ValidationResult {
        if (heightCm == null) {
            return ValidationResult.Valid
        }

        val (minValue, maxValue) = when (unitSystem) {
            UnitSystem.METRIC -> MIN_HEIGHT_CM to MAX_HEIGHT_CM
            UnitSystem.IMPERIAL -> MIN_HEIGHT_INCHES to MAX_HEIGHT_INCHES
        }

        val displayValue = when (unitSystem) {
            UnitSystem.METRIC -> heightCm
            UnitSystem.IMPERIAL -> UnitConverter.cmToInches(heightCm)
        }

        return if (displayValue in minValue..maxValue) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                R.string.error_height_out_of_range,
                listOf(UnitConverter.formatDouble(minValue), UnitConverter.formatDouble(maxValue))
            )
        }
    }
}
