package com.example.healthjournal.domain

/**
 * MET-based calorie pre-fill for workout summaries:
 * calories = MET x body weight (kg) x duration (hours).
 */
object CalorieEstimator {

    /** Fallback body weight when the user has none recorded. */
    const val DEFAULT_WEIGHT_KG = 70.0

    fun estimate(
        type: WorkoutType,
        durationMinutes: Double,
        weightKg: Double = DEFAULT_WEIGHT_KG
    ): Double = type.met * weightKg * (durationMinutes / 60.0)
}
