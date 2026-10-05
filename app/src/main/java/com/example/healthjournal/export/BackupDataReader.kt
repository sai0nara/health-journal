package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.WorkoutSession
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
        journalEntries = readList(stagingDir, BackupWriter.EntityFile.JOURNAL, JournalEntry::class.java),
        bodyMeasurements = readList(stagingDir, BackupWriter.EntityFile.BODY_MEASUREMENTS, BodyMeasurementEntry::class.java),
        goals = readList(stagingDir, BackupWriter.EntityFile.GOALS, GoalEntity::class.java),
        personalCards = readList(stagingDir, BackupWriter.EntityFile.PERSONAL_CARD, PersonalCard::class.java),
        deletedEntries = readList(stagingDir, BackupWriter.EntityFile.DELETED_ENTRIES, DeletedEntry::class.java),
        entryTags = readList(stagingDir, BackupWriter.EntityFile.ENTRY_TAGS, EntryTagCrossRef::class.java),
        workoutSessions = readList(stagingDir, BackupWriter.EntityFile.WORKOUTS, WorkoutSession::class.java)
    )

    private fun <T> readList(stagingDir: File, fileName: String, clazz: Class<T>): List<T> {
        val file = File(stagingDir, fileName)
        if (!file.isFile) return emptyList()
        val json = file.readText(Charsets.UTF_8)
        if (json.isBlank()) return emptyList()
        return try {
            val token = TypeToken.getParameterized(List::class.java, clazz).type
            gson.fromJson<List<T>>(json, token) ?: emptyList()
        } catch (e: Exception) {
            throw RestoreError.CorruptedFile("Malformed $fileName in backup.", e)
        }
    }
}
