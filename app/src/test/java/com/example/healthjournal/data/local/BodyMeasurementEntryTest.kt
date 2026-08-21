package com.example.healthjournal.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the BodyMeasurementEntry Room entity, verifying
 * sync defaults and nullable measurement columns per the track spec.
 */
class BodyMeasurementEntryTest {

    @Test
    fun defaults_useUuidPrimaryKeyAndCurrentTimestamp() {
        val entry = BodyMeasurementEntry()

        assertTrue(entry.entry_id.isNotEmpty())
        assertNotEquals(entry.entry_id, BodyMeasurementEntry().entry_id)
        assertTrue(entry.timestamp > 0)
        assertEquals(entry.timestamp, entry.lastModified)
    }

    @Test
    fun defaults_markEntryAsPendingSyncAndNotSynced() {
        val entry = BodyMeasurementEntry()

        assertEquals("PENDING_SYNC", entry.syncStatus)
        assertEquals(false, entry.isSynced)
    }

    @Test
    fun allSevenMeasurementFields_defaultToNull() {
        val entry = BodyMeasurementEntry()

        assertNull(entry.weight_kg)
        assertNull(entry.chest_cm)
        assertNull(entry.waist_cm)
        assertNull(entry.glute_cm)
        assertNull(entry.thigh_cm)
        assertNull(entry.calf_cm)
        assertNull(entry.bicep_cm)
    }

    @Test
    fun partialEntry_allowsSingleFieldValue() {
        val entry = BodyMeasurementEntry(waist_cm = 85.0)

        assertNull(entry.weight_kg)
        assertEquals(85.0, entry.waist_cm!!, 0.0)
    }

    @Test
    fun copy_updatesLastModifiedIndependently() {
        val created = 1_000L
        val entry = BodyMeasurementEntry(timestamp = created, lastModified = created)

        val updated = entry.copy(weight_kg = 78.5, lastModified = 2_000L)

        assertEquals(created, updated.timestamp)
        assertEquals(2_000L, updated.lastModified)
        assertEquals(78.5, updated.weight_kg!!, 0.0)
    }
}
