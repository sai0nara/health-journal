package com.example.healthjournal.domain.validation

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errorResId: Int, val formatArgs: List<Any> = emptyList()) : ValidationResult
}
