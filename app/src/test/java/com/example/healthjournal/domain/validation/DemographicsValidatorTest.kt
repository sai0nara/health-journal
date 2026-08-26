package com.example.healthjournal.domain.validation

import com.example.healthjournal.data.local.Demographics
import com.example.healthjournal.data.local.UnitSystem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DemographicsValidatorTest {
    private lateinit var validator: DemographicsValidator

    @Before
    fun setup() {
        validator = DemographicsValidator()
    }

    @Test
    fun `empty demographics is valid`() {
        val result = validator(Demographics(), UnitSystem.METRIC)
        assertTrue(result.isValid)
    }

    @Test
    fun `valid demographics is valid`() {
        val demographics = Demographics(
            dateOfBirth = "1990-01-15",
            heightCm = 175.0,
            weightKg = 70.0
        )
        val result = validator(demographics, UnitSystem.METRIC)
        assertTrue(result.isValid)
    }

    @Test
    fun `invalid date makes result invalid`() {
        val demographics = Demographics(
            dateOfBirth = "2030-01-01",
            heightCm = 175.0,
            weightKg = 70.0
        )
        val result = validator(demographics, UnitSystem.METRIC)
        assertFalse(result.isValid)
        assertTrue(result.dateOfBirth is ValidationResult.Invalid)
    }

    @Test
    fun `invalid height makes result invalid`() {
        val demographics = Demographics(
            dateOfBirth = "1990-01-15",
            heightCm = 300.0,
            weightKg = 70.0
        )
        val result = validator(demographics, UnitSystem.METRIC)
        assertFalse(result.isValid)
        assertTrue(result.height is ValidationResult.Invalid)
    }

    @Test
    fun `invalid weight makes result invalid`() {
        val demographics = Demographics(
            dateOfBirth = "1990-01-15",
            heightCm = 175.0,
            weightKg = 700.0
        )
        val result = validator(demographics, UnitSystem.METRIC)
        assertFalse(result.isValid)
        assertTrue(result.weight is ValidationResult.Invalid)
    }
}
