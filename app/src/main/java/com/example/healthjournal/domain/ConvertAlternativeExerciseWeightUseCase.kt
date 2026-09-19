package com.example.healthjournal.domain

/**
 * Converts a working weight between alternative movements (e.g. barbell squat
 * → leg press) using the user's historical 1RM ratio when both exercises have
 * logged history, and a default coefficient when history is insufficient.
 *
 * The default coefficient represents the typical weight relationship between
 * exercises of comparable intent; it is used only as a fallback so a fresh
 * user can still get a sensible equivalent starting weight.
 */
class ConvertAlternativeExerciseWeightUseCase {

    companion object {
        /** Fallback ratio applied when either side lacks logged 1RM history. */
        const val DEFAULT_COEFFICIENT: Double = 0.6
    }

    fun convert(
        sourceWeightKg: Double,
        sourceOneRepMaxKg: Double?,
        targetOneRepMaxKg: Double?
    ): Double {
        require(sourceWeightKg > 0.0) { "Weight must be greater than zero" }

        val scale = sourceOneRepMaxKg?.let { source ->
            targetOneRepMaxKg?.let { target -> target / source }
        }
        return sourceWeightKg * (scale ?: DEFAULT_COEFFICIENT)
    }
}