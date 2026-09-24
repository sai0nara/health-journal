package com.example.healthjournal.domain

import androidx.annotation.StringRes
import com.example.healthjournal.R

/**
 * What a workout's target expresses, driving the configuration UX:
 * some activities are measured by distance, others by time, and a few
 * (cycling, hiking, HIIT) accept more than one kind of target.
 */
enum class WorkoutTargetKind(
    val supportsDistance: Boolean,
    val supportsDuration: Boolean
) {
    DISTANCE_KM(supportsDistance = true, supportsDuration = false),
    DURATION_MIN(supportsDistance = false, supportsDuration = true),
    DURATION_OR_DISTANCE(supportsDistance = true, supportsDuration = true),
    DURATION_OR_SET_MATRIX(supportsDistance = false, supportsDuration = true),
    MANUAL_INTERVALS(supportsDistance = false, supportsDuration = true),
    DURATION_WITH_LAPS(supportsDistance = false, supportsDuration = true),
    DURATION_WITH_MOVEMENTS(supportsDistance = false, supportsDuration = true)
}

/**
 * Workout categories offered by the Workout hub, each carrying its
 * metabolic-equivalent (MET) factor used for calorie estimation and the
 * kind of target the configuration screen asks the user for.
 */
enum class WorkoutType(
    val label: String,
    val met: Double,
    val targetKind: WorkoutTargetKind,
    @StringRes val labelRes: Int
) {
    RUN("Run", 9.8, WorkoutTargetKind.DISTANCE_KM, R.string.workout_type_run),
    FITNESS("Fitness", 6.0, WorkoutTargetKind.DURATION_OR_SET_MATRIX, R.string.workout_type_fitness),
    YOGA("Yoga", 3.0, WorkoutTargetKind.DURATION_MIN, R.string.workout_type_yoga),
    HIIT("HIIT", 8.0, WorkoutTargetKind.MANUAL_INTERVALS, R.string.workout_type_hiit),
    WALKING_HIKING("Walking/Hiking", 5.0, WorkoutTargetKind.DURATION_OR_DISTANCE, R.string.workout_type_walking_hiking),
    CYCLING("Cycling", 7.5, WorkoutTargetKind.DURATION_OR_DISTANCE, R.string.workout_type_cycling),
    STRETCHING_MOBILITY("Stretching", 2.5, WorkoutTargetKind.DURATION_MIN, R.string.workout_type_stretching),
    PILATES("Pilates", 3.0, WorkoutTargetKind.DURATION_MIN, R.string.workout_type_pilates),
    SWIMMING("Swimming", 8.0, WorkoutTargetKind.DURATION_WITH_LAPS, R.string.workout_type_swimming),
    CALISTHENICS("Calisthenics", 8.0, WorkoutTargetKind.DURATION_WITH_MOVEMENTS, R.string.workout_type_calisthenics);

    companion object {
        /** Safe lookup for stored names; null for unknown/legacy values. */
        fun fromName(name: String): WorkoutType? = entries.firstOrNull { it.name == name }
    }
}
