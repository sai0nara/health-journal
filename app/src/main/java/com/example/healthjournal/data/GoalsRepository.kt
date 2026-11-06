package com.example.healthjournal.data

import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.domain.MeasurementField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Thin persistence facade for Body Analytics goal targets, mirroring
 * [BodyMeasurementRepository] conventions. Writes stamp `lastModified`
 * so the sync snapshot merge can resolve conflicts newest-wins.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsRepository(
    private val dao: GoalDao,
    private val activeProfile: ActiveProfileProvider
) {

    /** Reactive feed backing the chart's goal-line rendering. */
    val goals: Flow<List<GoalEntity>> =
        activeProfile.activeProfileId.flatMapLatest { dao.observeAll(it) }

    suspend fun getAll(): List<GoalEntity> = getAll(activeProfile.currentProfileId())

    suspend fun getAll(profileId: String): List<GoalEntity> = dao.getAll(profileId)

    suspend fun setGoal(field: MeasurementField, target: Double) {
        val profileId = activeProfile.currentProfileId()
        dao.upsertGoal(
            GoalEntity(
                parameterId = field.name,
                target = target,
                lastModified = System.currentTimeMillis(),
                profileId = profileId
            )
        )
    }

    suspend fun clearGoal(parameterId: String) {
        dao.deleteById(activeProfile.currentProfileId(), parameterId)
    }

    /** Per-profile wipe used by the sync snapshot prune step. */
    suspend fun clearAll(profileId: String) {
        dao.clear(profileId)
    }
}
