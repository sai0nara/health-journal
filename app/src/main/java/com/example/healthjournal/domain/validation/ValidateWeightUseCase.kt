package com.example.healthjournal.domain.validation

import com.example.healthjournal.R
import com.example.healthjournal.data.local.UnitConverter
import com.example.healthjournal.data.local.UnitSystem

class ValidateWeightUseCase {
    companion object {
        const val MIN_WEIGHT_KG = 0.5
        const val MAX_WEIGHT_KG = 650.0

        val MIN_WEIGHT_LBS: Double = UnitConverter.kgToLbs(MIN_WEIGHT_KG)
        val MAX_WEIGHT_LBS: Double = UnitConverter.kgToLbs(MAX_WEIGHT_KG)
    }

    operator fun invoke(weightKg: Double?, unitSystem: UnitSystem): ValidationResult {
        if (weightKg == null) {
            return ValidationResult.Valid
        }

        val (minValue, maxValue) = when (unitSystem) {
            UnitSystem.METRIC -> MIN_WEIGHT_KG to MAX_WEIGHT_KG
            UnitSystem.IMPERIAL -> MIN_WEIGHT_LBS to MAX_WEIGHT_LBS
        }

        val displayValue = when (unitSystem) {
            UnitSystem.METRIC -> weightKg
            UnitSystem.IMPERIAL -> UnitConverter.kgToLbs(weightKg)
        }

        return if (displayValue in minValue..maxValue) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                R.string.error_weight_out_of_range,
                listOf(UnitConverter.formatDouble(minValue), UnitConverter.formatDouble(maxValue))
            )
        }
    }
}
