package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.text.Charsets

/**
 * Loads a [BackupData] snapshot from the entity JSON files that [BackupWriter]
 * serialized, after they have been safely extracted to a staging directory.
 *
 * Pure JVM (no Android dependencies); tolerant of a missing or individually
 * malformed entity file, which defaults that collection to empty rather than
 * aborting the whole restore.
 */
class BackupDataReader(private val gson: Gson) {

    /** Reads every entity collection present in [stagingDir] into a [BackupData]. */
    fun read(stagingDir: File): BackupData = BackupData(
        journalEntries = readList<JournalEntry>(stagingDir, BackupWriter.EntityFile.JOURNAL),
        bodyMeasurements = readList<BodyMeasurementEntry>(stagingDir, BackupWriter.EntityFile.BODY_MEASUREMENTS),
        goals = readList<GoalEntity>(stagingDir, BackupWriter.EntityFile.GOALS),
        personalCards = readList<PersonalCard>(stagingDir, BackupWriter.EntityFile.PERSONAL_CARD),
        deletedEntries = readList<DeletedEntry>(stagingDir, BackupWriter.EntityFile.DELETED_ENTRIES),
        entryTags = readList<EntryTagCrossRef>(stagingDir, BackupWriter.EntityFile.ENTRY_TAGS)
    )

    private inline fun <reified T> readList(stagingDir: File, fileName: String): List<T> {
        val file = File(stagingDir, fileName)
        if (!file.isFile) return emptyList()
        val json = file.readText(Charsets.UTF_8)
        if (json.isBlank()) return emptyList()
        return try {
            val token = object : TypeToken<List<T>>() {}.type
            gson.fromJson<List<T>>(json, token) ?: emptyList()
        } catch (e: Exception) {
            throw RestoreError.CorruptedFile("Malformed $fileName in backup.", e)
        }
    }
}
