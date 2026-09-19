package com.example.healthjournal.domain

/**
 * Days a workout preset can be scheduled against; ANY means no fixed day.
 * A preset is a saved routine that drives the structured execution flow.
 */
enum class ScheduledDay {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
    ANY
}

/**
 * One exercise inside a preset: which catalog exercise to perform and the
 * progressive-overload defaults applied to each performed set (target count,
 * reps, weight) plus the rest length between sets. The executed session
 * records actuals; these are the planned targets.
 */
data class PresetExercise(
    val exerciseId: String,
    val targetSets: Int,
    val defaultReps: Int,
    val defaultWeightKg: Double,
    val restSeconds: Int
)