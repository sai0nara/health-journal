package com.example.healthjournal.data

import com.example.healthjournal.data.local.FakeWorkoutPresetDao
import com.example.healthjournal.data.local.WorkoutPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetRepositoryTest {

    private val activeProfile = FakeActiveProfileProvider()
    private val dao = FakeWorkoutPresetDao()
    private val repository = PresetRepository(dao, activeProfile)

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

    @Test
    fun presetsAreScopedToActiveProfile() = runBlocking {
        dao.upsertPreset(WorkoutPreset(id = "me", name = "Me"))
        dao.upsertPreset(
            WorkoutPreset(id = "sibling", name = "Sibling", profileId = "sibling")
        )

        assertEquals(listOf("me"), repository.presets.first().map { it.id })

        activeProfile.switchTo("sibling")

        assertEquals(listOf("sibling"), repository.presets.first().map { it.id })
    }

    @Test
    fun samePresetName_mayExistUnderTwoProfilesWithoutCollision() = runBlocking {
        activeProfile.switchTo("self")
        repository.savePreset(WorkoutPreset(id = "p-self", name = "Full Body", scheduledDay = "MONDAY"))

        activeProfile.switchTo("sibling")
        repository.savePreset(WorkoutPreset(id = "p-sibling", name = "Full Body", scheduledDay = "FRIDAY"))

        activeProfile.switchTo("self")
        val selfPresets = repository.presets.first()
        assertEquals(1, selfPresets.size)
        assertEquals("p-self", selfPresets.single().id)
        assertEquals("MONDAY", selfPresets.single().scheduledDay)

        activeProfile.switchTo("sibling")
        val siblingPresets = repository.presets.first()
        assertEquals(1, siblingPresets.size)
        assertEquals("p-sibling", siblingPresets.single().id)
        assertEquals("FRIDAY", siblingPresets.single().scheduledDay)
    }

    @Test
    fun savePreset_withExplicitProfile_persistsToCapturedProfileEvenAfterSwitch() = runBlocking {
        activeProfile.switchTo("self")
        val preset = WorkoutPreset(id = "p-captured", name = "Push Day")

        activeProfile.switchTo("sibling")
        repository.savePreset("self", preset)

        assertEquals(listOf("p-captured"), repository.getAllPresets("self").map { it.id })
        assertEquals(emptyList<String>(), repository.getAllPresets("sibling").map { it.id })
    }

    @Test
    fun deletePreset_removesOnlyActiveProfilePreset() = runBlocking {
        repository.savePreset("self", WorkoutPreset(id = "p-self", name = "Upper"))
        repository.savePreset("sibling", WorkoutPreset(id = "p-sibling", name = "Upper"))

        activeProfile.switchTo("self")
        repository.deletePreset("p-self")
        repository.deletePreset("p-sibling")

        assertEquals(emptyList<String>(), repository.getAllPresets("self").map { it.id })
        assertEquals(listOf("p-sibling"), repository.getAllPresets("sibling").map { it.id })
    }
}
