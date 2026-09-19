package com.example.healthjournal.data

import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutPresetDao
import kotlinx.coroutines.flow.Flow

/**
 * Thin persistence facade for workout presets, mirroring
 * [WorkoutRepository] conventions. Writes stamp `lastModified` so
 * sync merges and Drive restore resolve newest-wins.
 */
class PresetRepository(private val dao: WorkoutPresetDao) {

    /** Reactive preset-library feed, newest-edited first. */
    val presets: Flow<List<WorkoutPreset>> = dao.getAllPresets()

    suspend fun getPreset(presetId: String): WorkoutPreset? =
        dao.getPresetById(presetId)

    suspend fun savePreset(preset: WorkoutPreset) {
        dao.upsertPreset(preset.copy(lastModified = System.currentTimeMillis()))
    }

    suspend fun deletePreset(presetId: String) {
        dao.deletePresetById(presetId)
    }
}