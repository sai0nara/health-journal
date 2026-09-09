package com.example.healthjournal.health

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.domain.WorkoutType

/**
 * Pure mapping between the local session model and the platform-agnostic
 * health record; JVM-testable because it never touches Health Connect types.
 */
fun WorkoutSession.toHealthRecord(now: Long = System.currentTimeMillis()): WorkoutHealthRecord {
    val exerciseType = when (WorkoutType.entries.firstOrNull { it.name == type }) {
        WorkoutType.RUN -> HealthExerciseType.RUNNING
        WorkoutType.FITNESS -> HealthExerciseType.STRENGTH_TRAINING
        WorkoutType.YOGA -> HealthExerciseType.YOGA
        WorkoutType.HIIT -> HealthExerciseType.HIIT
        WorkoutType.WALKING_HIKING -> HealthExerciseType.HIKING
        WorkoutType.CYCLING -> HealthExerciseType.CYCLING
        WorkoutType.STRETCHING_MOBILITY -> HealthExerciseType.STRETCHING
        WorkoutType.PILATES -> HealthExerciseType.PILATES
        WorkoutType.SWIMMING -> HealthExerciseType.SWIMMING
        WorkoutType.CALISTHENICS -> HealthExerciseType.CALISTHENICS
        null -> HealthExerciseType.UNKNOWN
    }
    return WorkoutHealthRecord(
        exerciseType = exerciseType,
        startTimeMillis = startTimestamp,
        endTimeMillis = endTimestamp ?: now,
        caloriesKcal = calories,
        distanceMeters = targetDistanceM,
        notes = notes.takeIf { it.isNotBlank() }
    )
}
