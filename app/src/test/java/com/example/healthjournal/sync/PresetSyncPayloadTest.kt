package com.example.healthjournal.sync

import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract for the workout_presets.json cloud payload: a bare JSON list
 * serialized with Gson, mirroring the other snapshot sync payloads.
 */
class PresetSyncPayloadTest {

    private val full = WorkoutPreset(
        id = "p-1",
        name = "Push Day",
        scheduledDay = ScheduledDay.MONDAY.name,
        exercises = listOf(
            PresetExercise(
                exerciseId = "bench-press",
                targetSets = 3,
                defaultReps = 8,
                defaultWeightKg = 70.0,
                restSeconds = 90
            )
        ),
        warmupName = "Rowing",
        warmupDurationSeconds = 300,
        lastModified = 1_700_000_000_000L,
        profileId = "self"
    )

    @Test
    fun payload_roundTripsAllFields() {
        val json = PresetSyncPayload.toJson(listOf(full))

        val restored = PresetSyncPayload.fromJson(json)

        assertEquals(listOf(full), restored)
    }

    @Test
    fun payload_nullBlankOrMalformedJson_returnsEmptyList() {
        assertTrue(PresetSyncPayload.fromJson(null).isEmpty())
        assertTrue(PresetSyncPayload.fromJson("   ").isEmpty())
        assertTrue(PresetSyncPayload.fromJson("{not-valid-json").isEmpty())
    }

    @Test
    fun payload_missingProfileId_defaultsToSuppliedProfile() {
        val json = """[{"id":"p-sib","name":"Pull Day","lastModified":1000}]"""

        val restoredSelf = PresetSyncPayload.fromJson(json)
        val restoredSib = PresetSyncPayload.fromJson(json, defaultProfileId = "sib-1")

        assertEquals("self", restoredSelf.single().profileId)
        assertEquals("sib-1", restoredSib.single().profileId)
    }

    @Test
    fun payload_dropsMalformedEntries_andNormalizesNullLists() {
        val json = """
            [
              {"id":"","name":"No Id","lastModified":1},
              {"id":"p-blank-name","name":"   ","lastModified":1},
              {"id":"p-ok","name":"  Leg Day  ","scheduledDay":"","warmupName":"  ","warmupDurationSeconds":180,"lastModified":500}
            ]
        """.trimIndent()

        val restored = PresetSyncPayload.fromJson(json)

        assertEquals(1, restored.size)
        val preset = restored.single()
        assertEquals("p-ok", preset.id)
        assertEquals("Leg Day", preset.name)
        assertEquals(ScheduledDay.ANY.name, preset.scheduledDay)
        assertTrue(preset.exercises.isEmpty())
        assertNull(preset.warmupName)
        assertNull(preset.warmupDurationSeconds)
    }
}
