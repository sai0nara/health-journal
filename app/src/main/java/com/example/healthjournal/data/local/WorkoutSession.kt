package com.example.healthjournal.data.local

import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Lifecycle states of a workout session; only one unfinished session exists at a time. */
enum class WorkoutStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    DISCARDED
}

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey val session_id: String = UUID.randomUUID().toString(),
    val type: String = "RUN",
    val status: String = WorkoutStatus.ACTIVE.name,
    val startTimestamp: Long = System.currentTimeMillis(),
    val endTimestamp: Long? = null,
    val elapsedSeconds: Long = 0,
    val targetDistanceM: Double? = null,
    val targetDurationMin: Double? = null,
    val calories: Double? = null,
    val notes: String = "",
    val lastModified: Long = startTimestamp,
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC"
)
