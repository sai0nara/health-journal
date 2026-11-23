package com.example.healthjournal.data

import com.example.healthjournal.data.local.GoalDao
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.domain.MeasurementField
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

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

    /** Reactive weekly workout target per profile, defaulting to [DEFAULT_WORKOUT_WEEKLY_TARGET]. */
    val workoutWeeklyTarget: Flow<Int> = goals.map { list ->
        list.firstOrNull { it.parameterId == WORKOUT_WEEKLY_TARGET }
            ?.target?.toInt()
            ?.coerceIn(MIN_WORKOUT_WEEKLY_TARGET, MAX_WORKOUT_WEEKLY_TARGET)
            ?: DEFAULT_WORKOUT_WEEKLY_TARGET
    }

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

    suspend fun setWorkoutWeeklyTarget(target: Int) {
        require(target in MIN_WORKOUT_WEEKLY_TARGET..MAX_WORKOUT_WEEKLY_TARGET) {
            "Weekly workout target must be between $MIN_WORKOUT_WEEKLY_TARGET and $MAX_WORKOUT_WEEKLY_TARGET"
        }
        val profileId = activeProfile.currentProfileId()
        dao.upsertGoal(
            GoalEntity(
                parameterId = WORKOUT_WEEKLY_TARGET,
                target = target.toDouble(),
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

    companion object {
        const val WORKOUT_WEEKLY_TARGET = "WORKOUT_WEEKLY_TARGET"
        const val DEFAULT_WORKOUT_WEEKLY_TARGET = 4
        const val MIN_WORKOUT_WEEKLY_TARGET = 1
        const val MAX_WORKOUT_WEEKLY_TARGET = 14
    }
}
