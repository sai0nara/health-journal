package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class MeasurementSyncMergeTest {

    @Test
    fun merge_disjointIds_unionsCloudAndLocal() {
        val cloud = BodyMeasurementEntry(entry_id = "c1", timestamp = 1L, weight_kg = 70.0, lastModified = 100)
        val local = BodyMeasurementEntry(entry_id = "l1", timestamp = 2L, weight_kg = 80.0, lastModified = 200)

        val merged = SyncMerge.mergeMeasurements(listOf(cloud), listOf(local))

        assertEquals(setOf("c1", "l1"), merged.map { it.entry_id }.toSet())
    }

    @Test
    fun merge_cloudNewer_cloudWins() {
        val cloud = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 75.0, lastModified = 2000)
        val local = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 70.0, lastModified = 1000)

        val merged = SyncMerge.mergeMeasurements(listOf(cloud), listOf(local))

        assertEquals(75.0, merged.single().weight_kg!!, 0.001)
    }

    @Test
    fun merge_localNewer_localWins() {
        val cloud = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 75.0, lastModified = 1000)
        val local = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 80.0, lastModified = 2000)

        val merged = SyncMerge.mergeMeasurements(listOf(cloud), listOf(local))

        assertEquals(80.0, merged.single().weight_kg!!, 0.001)
    }

    @Test
    fun merge_tie_cloudWins() {
        val cloud = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 75.0, lastModified = 1500)
        val local = BodyMeasurementEntry(entry_id = "m", timestamp = 1L, weight_kg = 80.0, lastModified = 1500)

        val merged = SyncMerge.mergeMeasurements(listOf(cloud), listOf(local))

        assertEquals(75.0, merged.single().weight_kg!!, 0.001)
    }

    @Test
    fun merge_emptyCloud_keepsLocal() {
        val local = BodyMeasurementEntry(entry_id = "l1", timestamp = 1L, waist_cm = 85.0, lastModified = 100)

        val merged = SyncMerge.mergeMeasurements(emptyList(), listOf(local))

        assertEquals("l1", merged.single().entry_id)
    }
}
