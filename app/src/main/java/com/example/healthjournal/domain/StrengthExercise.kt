package com.example.healthjournal.domain

import java.util.UUID

/**
 * One logged working set: the weight lifted, repetitions completed, optional
 * RPE, and completion state. In a preset routine [completed] marks the set as
 * performed; legacy v2 manual rows always start false and are joined to the
 * session as-is.
 */
data class StrengthSet(
    val id: String = UUID.randomUUID().toString(),
    val kg: Double,
    val reps: Int,
    val rpe: Int? = null,
    val completed: Boolean = false
)

/**
 * A named exercise holding its logged sets within an active session. When the
 * session runs a preset routine, the planned progressive-overload targets
 * ([targetSets], [targetReps], [targetWeightKg], [restSeconds]) and the linked
 * catalog [exerciseId] are carried here so each row is self-contained for
 * crash recovery; [isPlanned] distinguishes these from legacy v2 manual rows.
 */
data class StrengthExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sets: List<StrengthSet> = emptyList(),
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val targetWeightKg: Double? = null,
    val restSeconds: Int? = null,
    val exerciseId: String? = null
) {
    /** True when this exercise was built from a preset routine (planned targets present). */
    val isPlanned: Boolean get() = targetSets != null
}

/**
 * Validation for strength sets and exercise names, mirroring
 * [ValidateWorkout] conventions: validators return a field -> inline-error
 * map (empty means valid) and a single error string for the name field.
 */
object ValidateStrengthExercise {

    const val ERROR_WEIGHT_POSITIVE = "Weight must be greater than zero"
    const val ERROR_REPS_POSITIVE = "Reps must be at least 1"
    const val ERROR_RPE_RANGE = "RPE must be between 1 and 10"
    const val ERROR_NAME_REQUIRED = "Exercise name is required"

    /** Field -> error for a set entry; empty means the set is valid. */
    fun validateSet(kg: Double, reps: Int, rpe: Int? = null): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (kg <= 0.0) {
            errors["kg"] = ERROR_WEIGHT_POSITIVE
        }
        if (reps < 1) {
            errors["reps"] = ERROR_REPS_POSITIVE
        }
        if (rpe != null && rpe !in 1..10) {
            errors["rpe"] = ERROR_RPE_RANGE
        }
        return errors
    }

    /** Null when [name] is a usable exercise name, else the inline error. */
    fun validateName(name: String): String? =
        if (name.isBlank()) ERROR_NAME_REQUIRED else null
}

/**
 * Tonnage use-case for the strength set matrix:
 * tonnage = sum of kg x reps across every set of every exercise. For preset
 * routine rows (which persist pending rows prefilled with planned defaults)
 * only completed sets are counted, so unperformed targets never inflate the sum.
 */
object TonnageCalculator {

    fun tonnageKg(exercises: List<StrengthExercise>): Double =
        exercises.sumOf { exercise ->
            exercise.sets
                .filter { !exercise.isPlanned || it.completed }
                .sumOf { it.kg * it.reps }
        }
}