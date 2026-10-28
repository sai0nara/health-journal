package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.data.local.WorkoutSession
import com.google.gson.Gson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupDataReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()

    private fun writeJson(staging: File, name: String, value: Any) {
        File(staging, name).writeText(gson.toJson(value))
    }

    private val entities = BackupData(
        journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x")),
        bodyMeasurements = listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)),
        goals = listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)),
        personalCards = listOf(PersonalCard(id = "pc")),
        deletedEntries = listOf(DeletedEntry(entry_id = "d1")),
        entryTags = listOf(EntryTagCrossRef("e1", "health")),
        workoutSessions = listOf(
            WorkoutSession(
                session_id = "w1",
                type = "RUN",
                startTimestamp = 10L,
                warmupName = "Rowing",
                warmupDurationSeconds = 300,
                warmupElapsedSeconds = 300,
                warmupOutcome = "COMPLETED"
            )
        ),
        presets = listOf(
            WorkoutPreset(id = "p1", name = "Leg day", warmupName = "Rowing", warmupDurationSeconds = 300)
        )
    )

    @Test
    fun read_populatesAllEntityListsFromStagingFiles() {
        val staging = tempFolder.newFolder("stage")
        writeJson(staging, BackupWriter.EntityFile.JOURNAL, entities.journalEntries)
        writeJson(staging, BackupWriter.EntityFile.BODY_MEASUREMENTS, entities.bodyMeasurements)
        writeJson(staging, BackupWriter.EntityFile.GOALS, entities.goals)
        writeJson(staging, BackupWriter.EntityFile.PERSONAL_CARD, entities.personalCards)
        writeJson(staging, BackupWriter.EntityFile.DELETED_ENTRIES, entities.deletedEntries)
        writeJson(staging, BackupWriter.EntityFile.ENTRY_TAGS, entities.entryTags)
        writeJson(staging, BackupWriter.EntityFile.WORKOUTS, entities.workoutSessions)
        writeJson(staging, BackupWriter.EntityFile.PRESETS, entities.presets)

        val result = BackupDataReader(gson).read(staging)

        assertEquals(entities, result)
    }

    @Test
    fun read_missingEntityFiles_defaultToEmptyLists() {
        val staging = tempFolder.newFolder("empty_stage")

        val result = BackupDataReader(gson).read(staging)

        assertEquals(BackupData(), result)
    }

    @Test
    fun read_legacyPresetAndSessionWithoutWarmupFields_restoresNulls() {
        val staging = tempFolder.newFolder("legacy_stage")
        File(staging, BackupWriter.EntityFile.PRESETS).writeText(
            """[{"id":"p1","name":"Legacy","lastModified":1}]"""
        )
        File(staging, BackupWriter.EntityFile.WORKOUTS).writeText(
            """[{"session_id":"w1","type":"RUN","startTimestamp":1}]"""
        )

        val result = BackupDataReader(gson).read(staging)

        assertNull(result.presets.single().warmupName)
        assertNull(result.presets.single().warmupDurationSeconds)
        assertNull(result.workoutSessions.single().warmupName)
        assertNull(result.workoutSessions.single().warmupDurationSeconds)
        assertNull(result.workoutSessions.single().warmupElapsedSeconds)
        assertNull(result.workoutSessions.single().warmupOutcome)
    }

    @Test
    fun read_throwsOnMalformedEntityFile() {
        val staging = tempFolder.newFolder("bad_entity")
        writeJson(staging, BackupWriter.EntityFile.JOURNAL, entities.journalEntries)
        File(staging, BackupWriter.EntityFile.GOALS).writeText("not json at all")

        val error = assertThrows(RestoreError.CorruptedFile::class.java) {
            BackupDataReader(gson).read(staging)
        }
        assertEquals("Malformed ${BackupWriter.EntityFile.GOALS} in backup.", error.message)
    }
}
