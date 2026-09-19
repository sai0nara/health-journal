package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for CalculateOneRepMaxUseCase using the Epley formula:
 * 1RM = weight × (1 + reps/30).
 */
class CalculateOneRepMaxUseCaseTest {

    private val useCase = CalculateOneRepMaxUseCase()

    @Test
    fun singleRep_oneRepEqualsWeight() {
        assertEquals(100.0, useCase.estimateOneRepMax(weightKg = 100.0, reps = 1), 0.01)
    }

    @Test
    fun epley_tenRepsAt100kg_is133Point33() {
        assertEquals(133.33, useCase.estimateOneRepMax(weightKg = 100.0, reps = 10), 0.01)
    }

    @Test
    fun epley_fiveRepsAt80kg_is93Point33() {
        assertEquals(93.33, useCase.estimateOneRepMax(weightKg = 80.0, reps = 5), 0.01)
    }

    @Test
    fun epley_twoRepsAt120kg_is128Point0() {
        // 120 × (1 + 2/30) = 120 × 1.0667 = 128.0
        assertEquals(128.0, useCase.estimateOneRepMax(weightKg = 120.0, reps = 2), 0.01)
    }

    @Test
    fun epley_lowerBoundaryOneKgTwoReps_isOnePoint0667() {
        // The lowest legal Epley input pair still evaluates the fraction:
        // 1 × (1 + 2/30) = 1.0667.
        assertEquals(1.0667, useCase.estimateOneRepMax(weightKg = 1.0, reps = 2), 0.01)
    }

    @Test
    fun zeroWeight_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.estimateOneRepMax(weightKg = 0.0, reps = 10)
        }
    }

    @Test
    fun negativeWeight_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.estimateOneRepMax(weightKg = -10.0, reps = 5)
        }
    }

    @Test
    fun zeroReps_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.estimateOneRepMax(weightKg = 80.0, reps = 0)
        }
    }

    @Test
    fun bestEstimateFromHistory_returnsHighest() {
        val logs = listOf(
            80.0 to 10, // 106.67
            100.0 to 5,  // 116.67
            60.0 to 8   // 76.0
        )
        assertEquals(116.67, useCase.bestEstimateFromHistory(logs)!!, 0.01)
    }

    @Test
    fun bestEstimateFromHistory_emptyList_returnsNull() {
        assertNull(useCase.bestEstimateFromHistory(emptyList()))
    }
}