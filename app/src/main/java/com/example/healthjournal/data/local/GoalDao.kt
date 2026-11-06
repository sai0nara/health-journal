package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Persistence for per-parameter, per-profile Body Analytics goal targets. */
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE profileId = :profileId")
    suspend fun getAll(profileId: String): List<GoalEntity>

    /** Reactive feed backing the chart's goal-line rendering. */
    @Query("SELECT * FROM goals WHERE profileId = :profileId")
    fun observeAll(profileId: String): Flow<List<GoalEntity>>

    @Query("DELETE FROM goals WHERE profileId = :profileId AND parameterId = :parameterId")
    suspend fun deleteById(profileId: String, parameterId: String)

    /** Per-profile wipe used by the sync snapshot prune step. */
    @Query("DELETE FROM goals WHERE profileId = :profileId")
    suspend fun clear(profileId: String)

    /** Replace one profile's goals with a merged snapshot. */
    @androidx.room.Transaction
    suspend fun importAll(profileId: String, goals: List<GoalEntity>) {
        clear(profileId)
        goals.forEach { upsertGoal(it.copy(profileId = profileId)) }
    }
}
