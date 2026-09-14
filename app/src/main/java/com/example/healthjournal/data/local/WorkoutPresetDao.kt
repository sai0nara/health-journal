package com.example.healthjournal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPresetDao {

    /** Reactive feed of all presets, newest-edited first. */
    @Query("SELECT * FROM workout_presets ORDER BY lastModified DESC")
    fun getAllPresets(): Flow<List<WorkoutPreset>>

    @Query("SELECT * FROM workout_presets WHERE id = :presetId")
    suspend fun getPresetById(presetId: String): WorkoutPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreset(preset: WorkoutPreset)

    @Query("DELETE FROM workout_presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: String)

    /** Bulk insert for full-backup restore (identity-preserving, replaces overlaps). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<WorkoutPreset>)

    @Query("DELETE FROM workout_presets")
    suspend fun clearAll()
}