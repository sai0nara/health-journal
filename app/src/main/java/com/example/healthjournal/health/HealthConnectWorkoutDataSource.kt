package com.example.healthjournal.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneOffset

/**
 * [WorkoutHealthDataSource] backed by Health Connect exercise sessions.
 * Kept thin like [HealthConnectManager] (no unit tests; exercised on-device):
 * all mapping logic lives in the JVM-tested [toHealthRecord].
 */
class HealthConnectWorkoutDataSource(context: Context) : WorkoutHealthDataSource {

    private val appContext = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(appContext) }

    private val writePermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    )
    private val readPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    )

    override suspend fun hasPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(writePermissions + readPermissions)
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun writeRecord(record: WorkoutHealthRecord): Boolean {
        return try {
            val start = Instant.ofEpochMilli(record.startTimeMillis)
            val end = Instant.ofEpochMilli(record.endTimeMillis)
            client.insertRecords(
                listOf(
                    ExerciseSessionRecord(
                        startTime = start,
                        startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
                        endTime = end,
                        endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(end),
                        exerciseType = record.exerciseType.toSessionType(),
                        title = "Workout",
                        notes = record.notes,
                        metadata = Metadata()
                    )
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun readRecent(daysBack: Int): List<WorkoutHealthRecord> {
        return try {
            val now = Instant.now()
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        now.minusSeconds(daysBack * 86_400L),
                        now
                    ),
                    ascendingOrder = true
                )
            )
            response.records.map { session ->
                WorkoutHealthRecord(
                    exerciseType = session.exerciseType.toHealthType(),
                    startTimeMillis = session.startTime.toEpochMilli(),
                    endTimeMillis = session.endTime.toEpochMilli(),
                    caloriesKcal = null,
                    distanceMeters = null
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun HealthExerciseType.toSessionType(): Int = when (this) {
        HealthExerciseType.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        HealthExerciseType.STRENGTH_TRAINING -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
        HealthExerciseType.YOGA -> ExerciseSessionRecord.EXERCISE_TYPE_YOGA
        HealthExerciseType.UNKNOWN -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
    }

    private fun Int.toHealthType(): HealthExerciseType = when (this) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> HealthExerciseType.RUNNING
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> HealthExerciseType.STRENGTH_TRAINING
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> HealthExerciseType.YOGA
        else -> HealthExerciseType.UNKNOWN
    }
}
