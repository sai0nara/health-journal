package com.example.healthjournal.data

import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.WorkoutPreset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetRepositoryTest {

    private val repository = PresetRepository(FakeWorkoutPresetDao())

    @Test
    fun savePreset_roundTripsWarmupFields() = runBlocking {
        val preset = WorkoutPreset(
            name = "Leg Day",
            warmupName = "Rowing",
            warmupDurationSeconds = 300
        )

        repository.savePreset(preset)
        val loaded = repository.getPreset(preset.id)

        assertEquals("Rowing", loaded?.warmupName)
        assertEquals(300, loaded?.warmupDurationSeconds)
    }

    @Test
    fun savePreset_withoutWarmup_keepsNullFields() = runBlocking {
        val preset = WorkoutPreset(name = "Leg Day")

        repository.savePreset(preset)
        val loaded = repository.getPreset(preset.id)

        assertNull(loaded?.warmupName)
        assertNull(loaded?.warmupDurationSeconds)
    }
}
