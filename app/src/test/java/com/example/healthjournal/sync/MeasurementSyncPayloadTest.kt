package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the body_measurements.json cloud payload: a bare JSON list
 * serialized with Gson, mirroring how health_journal_data.json stores
 * journal entries.
 */
class MeasurementSyncPayloadTest {

    private val full = BodyMeasurementEntry(
        entry_id = "m-1",
        timestamp = 1735689600000L,
        lastModified = 1700000000000L,
        weight_kg = 78.5,
        chest_cm = 100.0,
        waist_cm = 85.0,
        glute_cm = 95.0,
        thigh_cm = 55.0,
        calf_cm = 36.0,
        bicep_cm = 32.5
    )

    @Test
    fun payload_roundTripsAllFields() {
        val json = MeasurementSyncPayload.toJson(listOf(full))

        val restored = MeasurementSyncPayload.fromJson(json)

        assertEquals(listOf(full), restored)
    }

    @Test
    fun payload_emptyListRoundTripsAsEmptyArray() {
        val restored = MeasurementSyncPayload.fromJson(MeasurementSyncPayload.toJson(emptyList()))

        assertTrue(restored.isEmpty())
    }

    @Test
    fun payload_nullsOmittedFromJson() {
        val minimal = BodyMeasurementEntry(entry_id = "m-2", timestamp = 1L)

        val json = MeasurementSyncPayload.toJson(listOf(minimal))

        assertTrue(!json.contains("weight_kg"))
        assertTrue(json.contains("\"entry_id\":\"m-2\""))
    }

    @Test
    fun payload_legacyCloudFileWithoutMeasurements_yieldsEmptyList() {
        // A missing/absent measurements file downloads as null content.
        assertEquals(emptyList<BodyMeasurementEntry>(), MeasurementSyncPayload.fromJson(null))
    }
}
