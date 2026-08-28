package com.example.healthjournal.domain.validation

import com.example.healthjournal.data.local.UnitSystem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ValidateWeightUseCaseTest {
    private lateinit var validateWeight: ValidateWeightUseCase

    @Before
    fun setup() {
        validateWeight = ValidateWeightUseCase()
    }

    @Test
    fun `null weight is valid`() {
        val result = validateWeight(null, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `weight in valid metric range is valid`() {
        val result = validateWeight(70.0, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `weight below minimum metric is invalid`() {
        val result = validateWeight(0.1, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `weight above maximum metric is invalid`() {
        val result = validateWeight(700.0, UnitSystem.METRIC)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `weight in valid imperial range is valid`() {
        val result = validateWeight(70.0, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `weight below minimum imperial is invalid`() {
        val result = validateWeight(0.1, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `weight above maximum imperial is invalid`() {
        val result = validateWeight(660.0, UnitSystem.IMPERIAL)
        assertTrue(result is ValidationResult.Invalid)
    }
}
