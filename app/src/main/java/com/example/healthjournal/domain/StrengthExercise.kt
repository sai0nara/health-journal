package com.example.healthjournal.domain

import java.util.UUID

/** One logged working set: the weight lifted and the repetitions completed. */
data class StrengthSet(
    val id: String = UUID.randomUUID().toString(),
    val kg: Double,
    val reps: Int
)

/** A named exercise holding its logged sets within an active session. */
data class StrengthExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<StrengthSet> = emptyList()
)

/**
 * Validation for strength sets and exercise names, mirroring
 * [ValidateWorkout] conventions: validators return a field -> inline-error
 * map (empty means valid) and a single error string for the name field.
 */
object ValidateStrengthExercise {

    const val ERROR_WEIGHT_POSITIVE = "Weight must be greater than zero"
    const val ERROR_REPS_POSITIVE = "Reps must be at least 1"
    const val ERROR_NAME_REQUIRED = "Exercise name is required"

    /** Field -> error for a set entry; empty means the set is valid. */
    fun validateSet(kg: Double, reps: Int): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (kg <= 0.0) {
            errors["kg"] = ERROR_WEIGHT_POSITIVE
        }
        if (reps < 1) {
            errors["reps"] = ERROR_REPS_POSITIVE
        }
        return errors
    }

    /** Null when [name] is a usable exercise name, else the inline error. */
    fun validateName(name: String): String? =
        if (name.isBlank()) ERROR_NAME_REQUIRED else null
}

/**
 * Tonnage use-case for the strength set matrix:
 * tonnage = sum of kg x reps across every set of every exercise.
 */
object TonnageCalculator {

    fun tonnageKg(exercises: List<StrengthExercise>): Double =
        exercises.sumOf { exercise ->
            exercise.sets.sumOf { it.kg * it.reps }
        }
}