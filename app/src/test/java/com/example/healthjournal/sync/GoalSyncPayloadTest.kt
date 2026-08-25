package com.example.healthjournal.sync

import com.example.healthjournal.data.local.GoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the body_measurements_goals.json cloud payload: a bare
 * JSON list serialized with Gson, mirroring the measurements payload.
 */
class GoalSyncPayloadTest {

    private val full = GoalEntity(
        parameterId = "WEIGHT",
        target = 75.0,
        lastModified = 1700000000000L
    )

    @Test
    fun payload_roundTripsAllFields() {
        val json = GoalSyncPayload.toJson(listOf(full))

        val restored = GoalSyncPayload.fromJson(json)

        assertEquals(listOf(full), restored)
    }

    @Test
    fun payload_emptyListRoundTripsAsEmptyArray() {
        val restored = GoalSyncPayload.fromJson(GoalSyncPayload.toJson(emptyList()))

        assertTrue(restored.isEmpty())
    }

    @Test
    fun payload_garbageJson_returnsEmptyList() {
        assertEquals(emptyList<GoalEntity>(), GoalSyncPayload.fromJson("not json []{"))
    }

    @Test
    fun payload_nullContent_returnsEmptyList() {
        // A missing goals cloud file downloads as null content.
        assertEquals(emptyList<GoalEntity>(), GoalSyncPayload.fromJson(null))
    }
}
