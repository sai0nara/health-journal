package com.example.healthjournal.util

import com.example.healthjournal.data.local.WorkoutSession
import com.example.healthjournal.data.local.WorkoutSessionDao
import com.example.healthjournal.data.local.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [WorkoutSessionDao] for instrumented UI tests: behaves like Room
 * for upserts, identity lookup, and the unfinished-session recovery query.
 */
class FakeWorkoutSessionDao : WorkoutSessionDao {

    private val store = mutableMapOf<String, WorkoutSession>()
    private val feed = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override fun getAllSessions(): Flow<List<WorkoutSession>> = feed

    override suspend fun getSessionById(sessionId: String): WorkoutSession? =
        store[sessionId]

    override suspend fun getUnfinishedSession(): WorkoutSession? =
        store.values
            .filter {
                it.status == WorkoutStatus.ACTIVE.name ||
                    it.status == WorkoutStatus.PAUSED.name
            }
            .maxByOrNull { it.startTimestamp }

    override suspend fun upsertSession(session: WorkoutSession) {
        store[session.session_id] = session
        emit()
    }

    override suspend fun deleteSessionById(sessionId: String) {
        store.remove(sessionId)
        emit()
    }

    override suspend fun clearAll() {
        store.clear()
        emit()
    }

    private fun emit() {
        feed.value = store.values.sortedByDescending { it.startTimestamp }
    }
}
