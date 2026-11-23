package com.example.healthjournal.sync

import com.example.healthjournal.data.local.WorkoutPreset
import org.junit.Assert.assertEquals
import org.junit.Test

class PresetSyncMergeTest {

    @Test
    fun mergePresets_disjointIds_unionsCloudAndLocal() {
        val cloud = WorkoutPreset(id = "c1", name = "Cloud Routine", lastModified = 100L)
        val local = WorkoutPreset(id = "l1", name = "Local Routine", lastModified = 200L)

        val merged = SyncMerge.mergePresets(listOf(cloud), listOf(local))

        assertEquals(setOf("c1", "l1"), merged.map { it.id }.toSet())
    }

    @Test
    fun mergePresets_cloudNewer_cloudWins() {
        val cloud = WorkoutPreset(id = "p1", name = "Cloud Name", lastModified = 2_000L)
        val local = WorkoutPreset(id = "p1", name = "Local Name", lastModified = 1_000L)

        val merged = SyncMerge.mergePresets(listOf(cloud), listOf(local))

        assertEquals("Cloud Name", merged.single().name)
    }

    @Test
    fun mergePresets_localNewer_localWins() {
        val cloud = WorkoutPreset(id = "p1", name = "Cloud Name", lastModified = 1_000L)
        val local = WorkoutPreset(id = "p1", name = "Local Name", lastModified = 2_000L)

        val merged = SyncMerge.mergePresets(listOf(cloud), listOf(local))

        assertEquals("Local Name", merged.single().name)
    }

    @Test
    fun mergePresets_tie_cloudWins() {
        val cloud = WorkoutPreset(id = "p1", name = "Cloud Name", lastModified = 1_500L)
        val local = WorkoutPreset(id = "p1", name = "Local Name", lastModified = 1_500L)

        val merged = SyncMerge.mergePresets(listOf(cloud), listOf(local))

        assertEquals("Cloud Name", merged.single().name)
    }

    @Test
    fun mergePresets_compositeKeyByProfileAndId_isolatesProfiles() {
        val selfP = WorkoutPreset(id = "shared", name = "Self Push", profileId = "self", lastModified = 1_000L)
        val sibP = WorkoutPreset(id = "shared", name = "Sib Push", profileId = "sib-1", lastModified = 2_000L)

        val merged = SyncMerge.mergePresets(listOf(sibP), listOf(selfP))
        val byProfile = merged.associateBy { it.profileId }

        assertEquals(2, merged.size)
        assertEquals("Self Push", byProfile.getValue("self").name)
        assertEquals("Sib Push", byProfile.getValue("sib-1").name)
    }
}
