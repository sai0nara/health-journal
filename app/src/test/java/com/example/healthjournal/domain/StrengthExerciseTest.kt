package com.example.healthjournal.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the strength set/reps matrix domain: exercises hold named
 * sets of (kg, reps), a set is valid when weight is positive and at least one
 * rep is logged, and tonnage is the sum of kg x reps across every set.
 */
class StrengthExerciseTest {

    @Test
    fun set_withPositiveWeightAndReps_isValid() {
        val errors = ValidateStrengthExercise.validateSet(kg = 20.0, reps = 8)

        assertEquals(emptyMap<String, String>(), errors)
    }

    @Test
    fun set_withZeroKg_isInvalid() {
        val errors = ValidateStrengthExercise.validateSet(kg = 0.0, reps = 8)

        assertEquals(ValidateStrengthExercise.ERROR_WEIGHT_POSITIVE, errors["kg"])
    }

    @Test
    fun set_withNegativeKg_isInvalid() {
        val errors = ValidateStrengthExercise.validateSet(kg = -2.5, reps = 8)

        assertEquals(ValidateStrengthExercise.ERROR_WEIGHT_POSITIVE, errors["kg"])
    }

    @Test
    fun set_withZeroReps_isInvalid() {
        val errors = ValidateStrengthExercise.validateSet(kg = 20.0, reps = 0)

        assertEquals(ValidateStrengthExercise.ERROR_REPS_POSITIVE, errors["reps"])
    }

    @Test
    fun exercise_withBlankName_isInvalid() {
        assertEquals(
            ValidateStrengthExercise.ERROR_NAME_REQUIRED,
            ValidateStrengthExercise.validateName("  ")
        )
        assertNull(ValidateStrengthExercise.validateName("Bench Press"))
    }

    @Test
    fun eachSet_andEachExercise_carriesAnId() {
        val exercise = StrengthExercise(
            name = "Squat",
            sets = listOf(StrengthSet(kg = 60.0, reps = 10), StrengthSet(kg = 70.0, reps = 8))
        )

        assertNotNull(exercise.id)
        assertNotNull(exercise.sets[0].id)
        assertNotNull(exercise.sets[1].id)
    }

    @Test
    fun tonnage_sumsKgTimesRepsAcrossAllSetsAndExercises() {
        val exercises = listOf(
            StrengthExercise(
                name = "Squat",
                sets = listOf(
                    StrengthSet(kg = 60.0, reps = 10), // 600
                    StrengthSet(kg = 70.0, reps = 8)   // 560
                )
            ),
            StrengthExercise(
                name = "Bench",
                sets = listOf(StrengthSet(kg = 50.0, reps = 5)) // 250
            )
        )

        assertEquals(600.0 + 560.0 + 250.0, TonnageCalculator.tonnageKg(exercises), 0.0)
    }

    @Test
    fun tonnage_emptyMatrix_isZero() {
        assertEquals(0.0, TonnageCalculator.tonnageKg(emptyList()), 0.0)
    }

    @Test
    fun tonnage_exerciseWithoutSets_contributesNothing() {
        val exercises = listOf(
            StrengthExercise(name = "Plank", sets = emptyList()),
            StrengthExercise(
                name = "Row",
                sets = listOf(StrengthSet(kg = 40.0, reps = 12)) // 480
            )
        )

        assertEquals(480.0, TonnageCalculator.tonnageKg(exercises), 0.0)
    }
}