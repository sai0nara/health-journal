package com.example.healthjournal.domain.validation

import org.junit.Assert.*
import org.junit.Test

class ValidationResultTest {
    @Test
    fun `Valid result is correct type`() {
        val result = ValidationResult.Valid
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `Invalid result contains error resource id`() {
        val result = ValidationResult.Invalid(errorResId = 123)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(123, (result as ValidationResult.Invalid).errorResId)
    }
}