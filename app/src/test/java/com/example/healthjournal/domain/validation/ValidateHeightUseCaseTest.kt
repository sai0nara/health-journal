package com.example.healthjournal.domain.validation

import com.example.healthjournal.data.local.UnitSystem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidateHeightUseCaseTest {
    private lateinit var validateHeight: ValidateHeightUseCase

    @Before
    fun setup() {
        validateHeight = ValidateHeightUseCase()
    }

    @Test
    fun `null height is valid`() {
        val result = validateHeight(null, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `height in valid metric range is valid`() {
        val result = validateHeight(175.0, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `height below minimum metric is invalid`() {
        val result = validateHeight(15.0, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `height above maximum metric is invalid`() {
        val result = validateHeight(300.0, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `height in valid imperial range is valid`() {
        val result = validateHeight(177.8, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `height below minimum imperial is invalid`() {
        val result = validateHeight(15.0, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `height above maximum imperial is invalid`() {
        val result = validateHeight(280.0, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Invalid)
    }
}
