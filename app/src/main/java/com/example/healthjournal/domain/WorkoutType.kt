package com.example.healthjournal.domain

/**
 * Workout categories offered by the Workout hub, each carrying its
 * metabolic-equivalent (MET) factor used for calorie estimation.
 */
enum class WorkoutType(val label: String, val met: Double) {
    RUN("Run", 9.8),
    FITNESS("Fitness", 6.0),
    YOGA("Yoga", 3.0)
}
