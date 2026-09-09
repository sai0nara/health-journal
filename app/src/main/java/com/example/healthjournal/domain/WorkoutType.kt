package com.example.healthjournal.domain

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
enum class WorkoutType(val label: String, val met: Double, val targetKind: WorkoutTargetKind) {
    RUN("Run", 9.8, WorkoutTargetKind.DISTANCE_KM),
    FITNESS("Fitness", 6.0, WorkoutTargetKind.DURATION_OR_SET_MATRIX),
    YOGA("Yoga", 3.0, WorkoutTargetKind.DURATION_MIN),
    HIIT("HIIT", 8.0, WorkoutTargetKind.MANUAL_INTERVALS),
    WALKING_HIKING("Walking/Hiking", 5.0, WorkoutTargetKind.DURATION_OR_DISTANCE),
    CYCLING("Cycling", 7.5, WorkoutTargetKind.DURATION_OR_DISTANCE),
    STRETCHING_MOBILITY("Stretching", 2.5, WorkoutTargetKind.DURATION_MIN),
    PILATES("Pilates", 3.0, WorkoutTargetKind.DURATION_MIN),
    SWIMMING("Swimming", 8.0, WorkoutTargetKind.DURATION_WITH_LAPS),
    CALISTHENICS("Calisthenics", 8.0, WorkoutTargetKind.DURATION_WITH_MOVEMENTS)
}