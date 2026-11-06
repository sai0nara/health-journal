package com.example.healthjournal.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [WorkoutPresetDao] for unit tests: behaves like Room for
 * upserts, profile-scoped by-id reads, deletes, and bulk restore.
 */
class FakeWorkoutPresetDao : WorkoutPresetDao {

    private val store = linkedMapOf<String, WorkoutPreset>()
    private val feed = MutableStateFlow<List<WorkoutPreset>>(emptyList())

    override fun getAllPresets(profileId: String): Flow<List<WorkoutPreset>> =
        feed.map { all -> all.filter { it.profileId == profileId } }

    override suspend fun getPresetById(profileId: String, presetId: String): WorkoutPreset? =
        store[presetId]?.takeIf { it.profileId == profileId }

    override suspend fun upsertPreset(preset: WorkoutPreset) {
        store[preset.id] = preset
        emit()
    }

    override suspend fun deletePresetById(profileId: String, presetId: String) {
        val existing = store[presetId]
        if (existing?.profileId == profileId) store.remove(presetId)
        emit()
    }

    override suspend fun insertAll(presets: List<WorkoutPreset>) {
        presets.forEach { store[it.id] = it }
        emit()
    }

    override suspend fun clearAll(profileId: String) {
        store.entries.removeAll { it.value.profileId == profileId }
        emit()
    }

    fun visiblePresets(): List<WorkoutPreset> = store.values.toList()

    private fun emit() {
        feed.value = store.values.sortedByDescending { it.lastModified }.toList()
    }
}
