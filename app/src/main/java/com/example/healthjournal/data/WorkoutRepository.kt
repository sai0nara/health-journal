package com.example.healthjournal.data

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutSessionDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Thin persistence facade for workout sessions, mirroring
 * [BodyMeasurementRepository] conventions. Writes stamp `lastModified`
 * so crash-recovery freshness and sync merges resolve newest-wins.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutRepository(
    private val dao: WorkoutSessionDao,
    private val activeProfile: ActiveProfileProvider
) {

    /** Reactive workout-history feed, newest first. */
    val sessions: Flow<List<WorkoutSession>> =
        activeProfile.activeProfileId.flatMapLatest { dao.getAllSessions(it) }

    /** Finished-only workout history for discovery, newest first. */
    val completedSessions: Flow<List<WorkoutSession>> =
        activeProfile.activeProfileId.flatMapLatest { dao.getCompletedSessions(it) }

    fun currentProfileId(): String = activeProfile.currentProfileId()

    fun getCompletedWithSetMatrix(): Flow<List<WorkoutSession>> =
        activeProfile.activeProfileId.flatMapLatest { dao.getCompletedWithSetMatrix(it) }

    suspend fun getSessionById(sessionId: String): WorkoutSession? =
        getSessionById(currentProfileId(), sessionId)

    suspend fun getSessionById(profileId: String, sessionId: String): WorkoutSession? =
        dao.getSessionById(profileId, sessionId)

    /** The single unfinished session driving the resume prompt, if any. */
    suspend fun getUnfinishedSession(): WorkoutSession? =
        getUnfinishedSession(currentProfileId())

    suspend fun getUnfinishedSession(profileId: String): WorkoutSession? =
        dao.getUnfinishedSession(profileId)

    suspend fun saveSession(session: WorkoutSession) {
        saveSession(currentProfileId(), session)
    }

    suspend fun saveSession(profileId: String, session: WorkoutSession) {
        dao.upsertSession(
            session.copy(
                profileId = profileId,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun insertSessionIfNoUnfinished(session: WorkoutSession): Boolean =
        insertSessionIfNoUnfinished(currentProfileId(), session)

    suspend fun insertSessionIfNoUnfinished(profileId: String, session: WorkoutSession): Boolean =
        dao.insertSessionIfNoUnfinished(
            profileId,
            session.copy(
                profileId = profileId,
                lastModified = System.currentTimeMillis()
            )
        )

    suspend fun deleteSession(sessionId: String) {
        dao.deleteSessionById(currentProfileId(), sessionId)
    }

    suspend fun clearAll(profileId: String) {
        dao.clearAll(profileId)
    }

    suspend fun getAllSessions(profileId: String): List<WorkoutSession> =
        dao.getAllSessions(profileId).first()
}
