package com.example.healthjournal.domain

/**
 * Validation for workout-preset editing, mirroring [ValidateStrengthExercise]
 * conventions: a preset needs a name and at least one exercise, and every
 * per-exercise default (target sets, reps, weight, rest) must be usable.
 */
object ValidatePreset {

    const val ERROR_NAME_REQUIRED = "Preset name is required"
    const val ERROR_EXERCISES_REQUIRED = "Add at least one exercise"
    const val ERROR_SETS_POSITIVE = "Target sets must be at least 1"
    const val ERROR_REPS_POSITIVE = "Reps must be at least 1"
    const val ERROR_WEIGHT_POSITIVE = "Weight must be greater than zero"
    const val ERROR_REST_NON_NEGATIVE = "Rest cannot be negative"

    /** Null when [name] is a usable preset name, else the inline error. */
    fun validateName(name: String): String? =
        if (name.isBlank()) ERROR_NAME_REQUIRED else null

    /** Null when the preset carries at least one exercise, else the inline error. */
    fun validateExercises(exercises: List<PresetExercise>): String? =
        if (exercises.isEmpty()) ERROR_EXERCISES_REQUIRED else null

    /** Null when every per-exercise default is usable, else the first offending rule. */
    fun validateExercise(exercise: PresetExercise): String? =
        when {
            exercise.targetSets < 1 -> ERROR_SETS_POSITIVE
            exercise.defaultReps < 1 -> ERROR_REPS_POSITIVE
            exercise.defaultWeightKg <= 0.0 -> ERROR_WEIGHT_POSITIVE
            exercise.restSeconds < 0 -> ERROR_REST_NON_NEGATIVE
            else -> null
        }
}