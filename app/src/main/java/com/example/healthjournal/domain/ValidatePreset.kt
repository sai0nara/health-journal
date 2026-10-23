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
    const val ERROR_WARMUP_NAME_REQUIRED = "Warm-up name is required"
    const val ERROR_WARMUP_NAME_TOO_LONG = "Warm-up name must be 60 characters or fewer"
    const val ERROR_WARMUP_DURATION_RANGE = "Warm-up duration must be between 1 and 60 minutes"

    const val WARMUP_NAME_MAX_LENGTH = 60
    const val WARMUP_MIN_MINUTES = 1
    const val WARMUP_MAX_MINUTES = 60
    const val WARMUP_DEFAULT_MINUTES = 5

    /** Null when [name] is a usable preset name, else the inline error. */
    fun validateName(name: String): String? =
        if (name.isBlank()) ERROR_NAME_REQUIRED else null

    /** Null when [name] is a usable warm-up name, else the inline error. */
    fun validateWarmupName(name: String): String? =
        when {
            name.isBlank() -> ERROR_WARMUP_NAME_REQUIRED
            name.trim().length > WARMUP_NAME_MAX_LENGTH -> ERROR_WARMUP_NAME_TOO_LONG
            else -> null
        }

    /** Null when [minutes] is within the supported warm-up range. */
    fun validateWarmupDuration(minutes: Int): String? =
        if (minutes !in WARMUP_MIN_MINUTES..WARMUP_MAX_MINUTES) {
            ERROR_WARMUP_DURATION_RANGE
        } else {
            null
        }

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