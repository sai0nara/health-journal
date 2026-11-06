package com.example.healthjournal.data

import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutPresetDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Thin persistence facade for workout presets, mirroring
 * [WorkoutRepository] conventions. Writes stamp `lastModified` so
 * sync merges and Drive restore resolve newest-wins.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresetRepository(
    private val dao: WorkoutPresetDao,
    private val activeProfile: ActiveProfileProvider
) {

    fun currentProfileId(): String = activeProfile.currentProfileId()

    /** Reactive preset-library feed, newest-edited first. */
    val presets: Flow<List<WorkoutPreset>> =
        activeProfile.activeProfileId.flatMapLatest { dao.getAllPresets(it) }

    suspend fun getPreset(presetId: String): WorkoutPreset? =
        getPreset(currentProfileId(), presetId)

    suspend fun getPreset(profileId: String, presetId: String): WorkoutPreset? =
        dao.getPresetById(profileId, presetId)

    suspend fun savePreset(preset: WorkoutPreset) {
        savePreset(currentProfileId(), preset)
    }

    suspend fun savePreset(profileId: String, preset: WorkoutPreset) {
        dao.upsertPreset(
            preset.copy(
                profileId = profileId,
                lastModified = System.currentTimeMillis()
            )
        )
    }

    suspend fun deletePreset(presetId: String) {
        dao.deletePresetById(currentProfileId(), presetId)
    }

    suspend fun getAllPresets(profileId: String): List<WorkoutPreset> =
        dao.getAllPresets(profileId).first()
}
