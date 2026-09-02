package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import java.io.File
import org.junit.Assert.assertEquals
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
        entryTags = listOf(EntryTagCrossRef("e1", "health"))
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
    fun read_survivesIndividuallyMalformedEntityFiles() {
        val staging = tempFolder.newFolder("partially_bad")
        writeJson(staging, BackupWriter.EntityFile.JOURNAL, entities.journalEntries)
        // Malformed goals file should not abort the whole read
        File(staging, BackupWriter.EntityFile.GOALS).writeText("not json at all")

        val result = BackupDataReader(gson).read(staging)

        assertEquals(entities.journalEntries, result.journalEntries)
        assertEquals(emptyList<GoalEntity>(), result.goals)
    }
}
