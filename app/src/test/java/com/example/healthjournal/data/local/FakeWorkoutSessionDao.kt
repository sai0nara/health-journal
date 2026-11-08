package com.example.healthjournal.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [WorkoutSessionDao] for ViewModel unit tests: behaves like Room
 * for upserts, identity lookup, profile scoping, and unfinished-session recovery.
 */
class FakeWorkoutSessionDao : WorkoutSessionDao {

    private val store = mutableMapOf<String, WorkoutSession>()
    private val feed = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override fun getAllSessions(profileId: String): Flow<List<WorkoutSession>> =
        feed.map { all -> all.filter { it.profileId == profileId } }

    override fun getCompletedSessions(profileId: String): Flow<List<WorkoutSession>> =
        feed.map { all ->
            all.filter { it.profileId == profileId && it.status == WorkoutStatus.COMPLETED.name }
        }

    override fun getCompletedWithSetMatrix(profileId: String): Flow<List<WorkoutSession>> =
        feed.map { all ->
            all.filter {
                it.profileId == profileId &&
                    it.status == WorkoutStatus.COMPLETED.name &&
                    it.setMatrix != null
            }.sortedBy { it.startTimestamp }
        }

    override suspend fun getSessionById(profileId: String, sessionId: String): WorkoutSession? =
        store[sessionId]?.takeIf { it.profileId == profileId }

    override suspend fun getUnfinishedSession(profileId: String): WorkoutSession? =
        store.values
            .filter {
                it.profileId == profileId && (
                    it.status == WorkoutStatus.ACTIVE.name ||
                        it.status == WorkoutStatus.PAUSED.name
                    )
            }
            .maxByOrNull { it.startTimestamp }

    override suspend fun upsertSession(session: WorkoutSession) {
        store[session.session_id] = session
        emit()
    }

    override suspend fun insertAll(sessions: List<WorkoutSession>) {
        sessions.forEach { store[it.session_id] = it }
        emit()
    }

    override suspend fun deleteSessionById(profileId: String, sessionId: String) {
        val existing = store[sessionId]
        if (existing?.profileId == profileId) store.remove(sessionId)
        emit()
    }

    override suspend fun clearAll(profileId: String) {
        store.entries.removeAll { it.value.profileId == profileId }
        emit()
    }

    private fun emit() {
        feed.value = store.values.sortedByDescending { it.startTimestamp }
    }
}
