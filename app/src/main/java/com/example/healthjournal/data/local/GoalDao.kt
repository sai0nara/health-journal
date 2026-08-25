package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Persistence for per-parameter Body Analytics goal targets. */
@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals")
    suspend fun getAll(): List<GoalEntity>

    /** Reactive feed backing the chart's goal-line rendering. */
    @Query("SELECT * FROM goals")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("DELETE FROM goals WHERE parameterId = :parameterId")
    suspend fun deleteById(parameterId: String)

    /** Full-table wipe used by the sync snapshot prune step. */
    @Query("DELETE FROM goals")
    suspend fun clear()

    /** Replace the entire goals table with a merged snapshot. */
    @androidx.room.Transaction
    suspend fun importAll(goals: List<GoalEntity>) {
        clear()
        goals.forEach { upsertGoal(it) }
    }
}
