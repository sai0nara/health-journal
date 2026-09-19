package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for ConvertAlternativeExerciseWeightUseCase: converts a working
 * weight between alternative movements using the user's historical 1RM ratio
 * when both sides have history, and a default coefficient otherwise.
 */
class ConvertAlternativeExerciseWeightUseCaseTest {

    private val useCase = ConvertAlternativeExerciseWeightUseCase()

    @Test
    fun bothOneRepMaxKnown_usesHistoricalRatio() {
        // Squat 1RM 100 kg, leg-press 1RM 200 kg: a 80 kg squat works to 160 kg leg press.
        val result = useCase.convert(
            sourceWeightKg = 80.0,
            sourceOneRepMaxKg = 100.0,
            targetOneRepMaxKg = 200.0
        )
        assertEquals(160.0, result, 0.01)
    }

    @Test
    fun ratioBelowOne_scaleDown() {
        // Leg-press 1RM 300 kg, squat 1RM 150 kg: 200 kg leg press works to 100 kg squat.
        val result = useCase.convert(
            sourceWeightKg = 200.0,
            sourceOneRepMaxKg = 300.0,
            targetOneRepMaxKg = 150.0
        )
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun equalOneRepMax_preservesWeight() {
        val result = useCase.convert(
            sourceWeightKg = 75.0,
            sourceOneRepMaxKg = 120.0,
            targetOneRepMaxKg = 120.0
        )
        assertEquals(75.0, result, 0.01)
    }

    @Test
    fun missingSourceHistory_usesDefaultCoefficient() {
        val result = useCase.convert(
            sourceWeightKg = 80.0,
            sourceOneRepMaxKg = null,
            targetOneRepMaxKg = 200.0
        )
        assertEquals(80.0 * ConvertAlternativeExerciseWeightUseCase.DEFAULT_COEFFICIENT, result, 0.01)
    }

    @Test
    fun missingTargetHistory_usesDefaultCoefficient() {
        val result = useCase.convert(
            sourceWeightKg = 80.0,
            sourceOneRepMaxKg = 100.0,
            targetOneRepMaxKg = null
        )
        assertEquals(80.0 * ConvertAlternativeExerciseWeightUseCase.DEFAULT_COEFFICIENT, result, 0.01)
    }

    @Test
    fun missingBothHistories_usesDefaultCoefficient() {
        val result = useCase.convert(
            sourceWeightKg = 80.0,
            sourceOneRepMaxKg = null,
            targetOneRepMaxKg = null
        )
        assertEquals(80.0 * ConvertAlternativeExerciseWeightUseCase.DEFAULT_COEFFICIENT, result, 0.01)
    }

    @Test
    fun zeroSourceWeight_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.convert(
                sourceWeightKg = 0.0,
                sourceOneRepMaxKg = 100.0,
                targetOneRepMaxKg = 200.0
            )
        }
    }

    @Test
    fun negativeSourceWeight_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.convert(
                sourceWeightKg = -20.0,
                sourceOneRepMaxKg = 100.0,
                targetOneRepMaxKg = 200.0
            )
        }
    }
}