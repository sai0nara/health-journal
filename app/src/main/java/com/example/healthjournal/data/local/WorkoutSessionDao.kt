package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    /** Reactive feed of workout history, newest first. */
    @Query("SELECT * FROM workout_sessions WHERE profileId = :profileId ORDER BY startTimestamp DESC")
    fun getAllSessions(profileId: String): Flow<List<WorkoutSession>>

    /** Finished workout history for discovery, newest first. */
    @Query(
        "SELECT * FROM workout_sessions WHERE profileId = :profileId AND status = 'COMPLETED' " +
            "ORDER BY startTimestamp DESC"
    )
    fun getCompletedSessions(profileId: String): Flow<List<WorkoutSession>>

    /** Completed strength sessions carrying a set matrix, oldest first, for per-exercise analytics. */
    @Query(
        "SELECT * FROM workout_sessions WHERE profileId = :profileId AND status = 'COMPLETED' " +
            "AND setMatrix IS NOT NULL ORDER BY startTimestamp ASC"
    )
    fun getCompletedWithSetMatrix(profileId: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE profileId = :profileId AND session_id = :sessionId")
    suspend fun getSessionById(profileId: String, sessionId: String): WorkoutSession?

    /** The single unfinished session for one profile, used for crash recovery. */
    @Query(
        "SELECT * FROM workout_sessions WHERE profileId = :profileId AND status IN ('ACTIVE', 'PAUSED') " +
            "ORDER BY startTimestamp DESC LIMIT 1"
    )
    suspend fun getUnfinishedSession(profileId: String): WorkoutSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: WorkoutSession)

    /** Bulk insert for full-backup restore (identity-preserving, replaces overlaps). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<WorkoutSession>)

    @Query("DELETE FROM workout_sessions WHERE profileId = :profileId AND session_id = :sessionId")
    suspend fun deleteSessionById(profileId: String, sessionId: String)

    @Query("DELETE FROM workout_sessions WHERE profileId = :profileId")
    suspend fun clearAll(profileId: String)

    /** Inserts only when this profile has no ACTIVE/PAUSED session. */
    @Transaction
    suspend fun insertSessionIfNoUnfinished(profileId: String, session: WorkoutSession): Boolean {
        if (getUnfinishedSession(profileId) != null) return false
        upsertSession(session.copy(profileId = profileId))
        return true
    }
}
