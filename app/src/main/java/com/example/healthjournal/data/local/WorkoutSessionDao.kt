package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    /** Reactive feed of workout history, newest first. */
    @Query("SELECT * FROM workout_sessions ORDER BY startTimestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    /** Finished workout history for discovery, newest first. */
    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY startTimestamp DESC")
    fun getCompletedSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE session_id = :sessionId")
    suspend fun getSessionById(sessionId: String): WorkoutSession?

    /** The single unfinished session used for crash recovery, if any. */
    @Query("SELECT * FROM workout_sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getUnfinishedSession(): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSession)

    /** Bulk insert for full-backup restore (identity-preserving, replaces overlaps). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<WorkoutSession>)

    @Query("DELETE FROM workout_sessions WHERE session_id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAll()
}
