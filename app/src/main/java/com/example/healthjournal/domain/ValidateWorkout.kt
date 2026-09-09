package com.example.healthjournal.domain

/**
 * Validation for workout capture, mirroring [ValidateMeasurements]
 * conventions: validators return a field -> inline-error map, and an empty
 * map means the input is valid. Blank calories/notes are allowed.
 */
object ValidateWorkout {

    const val ERROR_TYPE_REQUIRED = "Select a workout type"
    const val ERROR_INVALID_FORMAT = "Invalid decimal format"
    const val ERROR_NON_POSITIVE = "Must be greater than zero"
    const val ERROR_NEGATIVE = "Cannot be negative"
    const val ERROR_FUTURE_DATE = "Date cannot be in the future"
    const val ERROR_DURATION_TOO_LONG = "Too long (max 1440 min)"
    const val ERROR_CALORIES_TOO_LARGE = "Too large (max 50000 kcal)"

    /** Sanity cap for a single session: longer than a day is a typo. */
    const val MAX_DURATION_MINUTES = 1440.0

    /** Sanity cap for a single session: beyond ultra-endurance totals. */
    const val MAX_CALORIES = 50000.0

    /**
     * Validates a manual past-workout log. Returns field -> error; empty
     * means valid.
     */
    fun validateManualLog(
        type: WorkoutType?,
        durationMinutes: String,
        calories: String,
        timestamp: Long,
        now: Long = System.currentTimeMillis()
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (type == null) {
            errors["type"] = ERROR_TYPE_REQUIRED
        }
        parseDuration(durationMinutes)?.let { errors["duration"] = it }
        val caloriesText = calories.trim()
        if (caloriesText.isNotEmpty()) {
            val value = caloriesText.replace(',', '.').toDoubleOrNull()
            when {
                value == null -> errors["calories"] = ERROR_INVALID_FORMAT
                value < 0.0 -> errors["calories"] = ERROR_NEGATIVE
                value > MAX_CALORIES -> errors["calories"] = ERROR_CALORIES_TOO_LARGE
            }
        }
        if (timestamp > now) {
            errors["timestamp"] = ERROR_FUTURE_DATE
        }
        return errors
    }

    /**
     * Validates a session configuration target (distance or duration
     * depending on workout type). Returns field -> error; empty means valid.
     */
    fun validateTarget(type: WorkoutType?, target: String): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (type == null) {
            errors["type"] = ERROR_TYPE_REQUIRED
        }
        parsePositiveDecimal(target)?.let { errors["target"] = it }
        return errors
    }

    /** Null when [raw] is a valid strictly-positive decimal, else the error. */
    private fun parsePositiveDecimal(raw: String): String? {
        val value = raw.trim().replace(',', '.').toDoubleOrNull()
        return when {
            value == null -> ERROR_INVALID_FORMAT
            value <= 0.0 -> ERROR_NON_POSITIVE
            else -> null
        }
    }

    /** Null when [raw] is a valid workout duration within the sanity cap. */
    private fun parseDuration(raw: String): String? {
        val value = raw.trim().replace(',', '.').toDoubleOrNull()
        return when {
            value == null -> ERROR_INVALID_FORMAT
            value <= 0.0 -> ERROR_NON_POSITIVE
            value > MAX_DURATION_MINUTES -> ERROR_DURATION_TOO_LONG
            else -> null
        }
    }
}
