package com.example.healthjournal.domain.validation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ValidateDateOfBirthUseCaseTest {
    private lateinit var validateDateOfBirth: ValidateDateOfBirthUseCase
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Before
    fun setup() {
        validateDateOfBirth = ValidateDateOfBirthUseCase()
    }

    @Test
    fun `blank date is valid`() {
        val result = validateDateOfBirth("")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `valid past date is valid`() {
        val result = validateDateOfBirth("1990-01-15")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `future date is invalid`() {
        val futureDate = LocalDate.now().plusYears(1).format(formatter)
        val result = validateDateOfBirth(futureDate)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `date more than 130 years ago is invalid`() {
        val oldDate = LocalDate.now().minusYears(131).format(formatter)
        val result = validateDateOfBirth(oldDate)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `exactly 130 years ago is valid`() {
        val date = LocalDate.now().minusYears(130).format(formatter)
        val result = validateDateOfBirth(date)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `invalid format is invalid`() {
        val result = validateDateOfBirth("not-a-date")
        assertTrue(result is ValidationResult.Invalid)
    }
}
