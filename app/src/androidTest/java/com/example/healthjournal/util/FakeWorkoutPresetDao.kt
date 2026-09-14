package com.example.healthjournal.util

import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutPresetDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [WorkoutPresetDao] for instrumented UI tests.
 */
class FakeWorkoutPresetDao : WorkoutPresetDao {

    private val store = linkedMapOf<String, WorkoutPreset>()
    private val feed = MutableStateFlow<List<WorkoutPreset>>(emptyList())

    override fun getAllPresets(): Flow<List<WorkoutPreset>> = feed

    override suspend fun getPresetById(presetId: String): WorkoutPreset? = store[presetId]

    override suspend fun upsertPreset(preset: WorkoutPreset) {
        store[preset.id] = preset
        emit()
    }

    override suspend fun deletePresetById(presetId: String) {
        store.remove(presetId)
        emit()
    }

    override suspend fun insertAll(presets: List<WorkoutPreset>) {
        presets.forEach { store[it.id] = it }
        emit()
    }

    override suspend fun clearAll() {
        store.clear()
        emit()
    }

    private fun emit() {
        feed.value = store.values.sortedByDescending { it.lastModified }.toList()
    }
}