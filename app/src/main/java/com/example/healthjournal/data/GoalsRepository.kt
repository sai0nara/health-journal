package com.example.healthjournal.data

import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.domain.MeasurementField
import kotlinx.coroutines.flow.Flow

/**
 * Thin persistence facade for Body Analytics goal targets, mirroring
 * [BodyMeasurementRepository] conventions. Writes stamp `lastModified`
 * so the sync snapshot merge can resolve conflicts newest-wins.
 */
class GoalsRepository(private val dao: GoalDao) {

    /** Reactive feed backing the chart's goal-line rendering. */
    val goals: Flow<List<GoalEntity>> = dao.observeAll()

    suspend fun getAll(): List<GoalEntity> = dao.getAll()

    suspend fun setGoal(field: MeasurementField, target: Double) {
        dao.upsertGoal(
            GoalEntity(
                parameterId = field.name,
                target = target,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearGoal(parameterId: String) {
        dao.deleteById(parameterId)
    }

    /** Full wipe used by the sync snapshot prune step. */
    suspend fun clearAll() {
        dao.clear()
    }
}
