package com.example.healthjournal.health

/** Platform-agnostic exercise counterpart of [com.example.healthjournal.domain.WorkoutType]. */
enum class HealthExerciseType {
    RUNNING,
    STRENGTH_TRAINING,
    YOGA,
    HIIT,
    HIKING,
    CYCLING,
    STRETCHING,
    PILATES,
    SWIMMING,
    CALISTHENICS,
    UNKNOWN
}

/**
 * Platform-agnostic workout record exchanged with health platforms; keeps
 * Health Connect client types out of JVM-testable signatures.
 */
data class WorkoutHealthRecord(
    val exerciseType: HealthExerciseType,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val caloriesKcal: Double?,
    val distanceMeters: Double?,
    val notes: String? = null
)

/**
 * Health-platform workout sync behind a seam: production uses
 * [HealthConnectWorkoutDataSource], unit tests use the in-memory fake.
 */
interface WorkoutHealthDataSource {

    suspend fun hasPermissions(): Boolean

    /** Persists a completed workout; false when permission is missing or the write fails. */
    suspend fun writeRecord(record: WorkoutHealthRecord): Boolean

    /** Recent workout records for discovery de-duplication, newest last. */
    suspend fun readRecent(daysBack: Int): List<WorkoutHealthRecord>
}
