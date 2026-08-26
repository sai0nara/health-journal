package com.example.healthjournal.domain.validation

import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.UnitSystem

data class DemographicsValidationResult(
    val dateOfBirth: ValidationResult = ValidationResult.Valid,
    val height: ValidationResult = ValidationResult.Valid,
    val weight: ValidationResult = ValidationResult.Valid
) {
    val isValid: Boolean
        get() = dateOfBirth is ValidationResult.Valid &&
                height is ValidationResult.Valid &&
                weight is ValidationResult.Valid
}

class DemographicsValidator(
    private val validateDateOfBirth: ValidateDateOfBirthUseCase = ValidateDateOfBirthUseCase(),
    private val validateHeight: ValidateHeightUseCase = ValidateHeightUseCase(),
    private val validateWeight: ValidateWeightUseCase = ValidateWeightUseCase()
) {
    operator fun invoke(demographics: Demographics, unitSystem: UnitSystem): DemographicsValidationResult {
        return DemographicsValidationResult(
            dateOfBirth = validateDateOfBirth(demographics.dateOfBirth),
            height = validateHeight(demographics.heightCm, unitSystem),
            weight = validateWeight(demographics.weightKg, unitSystem)
        )
    }
}
