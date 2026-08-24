package com.example.healthjournal.sync

import com.example.healthjournal.data.local.DeletedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementTombstonePayloadTest {

    @Test
    fun fromJson_nullOrBlank_returnsEmptyList() {
        assertTrue(MeasurementTombstonePayload.fromJson(null).isEmpty())
        assertTrue(MeasurementTombstonePayload.fromJson("").isEmpty())
    }

    @Test
    fun fromJson_garbage_returnsEmptyList() {
        assertTrue(MeasurementTombstonePayload.fromJson("not-json{").isEmpty())
    }

    @Test
    fun toJsonFromJson_roundTripsTombstones() {
        val tombstones = listOf(
            DeletedEntry("a", 1_000L),
            DeletedEntry("b", 2_000L)
        )

        val parsed = MeasurementTombstonePayload.fromJson(
            MeasurementTombstonePayload.toJson(tombstones)
        )

        assertEquals(tombstones, parsed)
    }
}
