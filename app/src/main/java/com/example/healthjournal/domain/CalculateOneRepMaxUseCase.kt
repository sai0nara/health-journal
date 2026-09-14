package com.example.healthjournal.domain

/**
 * One-rep-max estimation via the Epley formula: 1RM = weight × (1 + reps/30).
 * Used by the weight analytics and alternative-movement conversion flows.
 */
class CalculateOneRepMaxUseCase {

    /** Epley estimate for a single logged set; rejects non-positive weight or reps. */
    fun estimateOneRepMax(weightKg: Double, reps: Int): Double {
        require(weightKg > 0.0) { "Weight must be greater than zero" }
        require(reps >= 1) { "Reps must be at least 1" }
        // A single completed rep is by definition the one-rep max.
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    /**
     * Best 1RM estimate across a set of logged (weight, reps) entries, or null
     * when there is no history. The maximum estimate represents the user's
     * practical peak for the exercise.
     */
    fun bestEstimateFromHistory(logs: List<Pair<Double, Int>>): Double? =
        logs.maxOfOrNull { estimateOneRepMax(it.first, it.second) }
}