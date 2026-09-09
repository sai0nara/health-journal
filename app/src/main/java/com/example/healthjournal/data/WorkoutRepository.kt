package com.example.healthjournal.data

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutSessionDao
import kotlinx.coroutines.flow.Flow

/**
 * Thin persistence facade for workout sessions, mirroring
 * [BodyMeasurementRepository] conventions. Writes stamp `lastModified`
 * so crash-recovery freshness and sync merges resolve newest-wins.
 */
class WorkoutRepository(private val dao: WorkoutSessionDao) {

    /** Reactive workout-history feed, newest first. */
    val sessions: Flow<List<WorkoutSession>> = dao.getAllSessions()

    /** Finished-only workout history for discovery, newest first. */
    val completedSessions: Flow<List<WorkoutSession>> = dao.getCompletedSessions()

    suspend fun getSessionById(sessionId: String): WorkoutSession? =
        dao.getSessionById(sessionId)

    /** The single unfinished session driving the resume prompt, if any. */
    suspend fun getUnfinishedSession(): WorkoutSession? =
        dao.getUnfinishedSession()

    suspend fun saveSession(session: WorkoutSession) {
        dao.upsertSession(session.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun deleteSession(sessionId: String) {
        dao.deleteSessionById(sessionId)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
