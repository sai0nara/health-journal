package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutPreset
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
    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun read(stagingDir: File): BackupData {
        val profiles = readList(stagingDir, BackupWriter.EntityFile.PROFILES, ProfileEntity::class.java)
            .mapNotNull { p ->
                val id = (p.id as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val name = (p.name as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val rel = (p.relationship as String?)?.takeIf { it.isNotBlank() } ?: "OTHER"
                if (p.id == id && p.name == name && p.relationship == rel) p else p.copy(id = id, name = name, relationship = rel)
            }
        val journalEntries = readList(stagingDir, BackupWriter.EntityFile.JOURNAL, JournalEntry::class.java)
            .map { e ->
                val pid = (e.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (e.profileId == pid) {
                    e
                } else {
                    e.copy(
                        entry_id = (e.entry_id as String?) ?: "",
                        description = (e.description as String?) ?: "",
                        profileId = pid
                    ).also { it.tags = e.tags }
                }
            }
        val bodyMeasurements = readList(stagingDir, BackupWriter.EntityFile.BODY_MEASUREMENTS, BodyMeasurementEntry::class.java)
            .map { m ->
                val pid = (m.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (m.profileId == pid) {
                    m
                } else {
                    m.copy(
                        entry_id = (m.entry_id as String?) ?: "",
                        syncStatus = (m.syncStatus as String?) ?: "PENDING_SYNC",
                        profileId = pid
                    )
                }
            }
        val goals = readList(stagingDir, BackupWriter.EntityFile.GOALS, GoalEntity::class.java)
            .map { g ->
                val pid = (g.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (g.profileId == pid) {
                    g
                } else {
                    g.copy(
                        parameterId = (g.parameterId as String?) ?: "",
                        profileId = pid
                    )
                }
            }
        val personalCards = readList(stagingDir, BackupWriter.EntityFile.PERSONAL_CARD, PersonalCard::class.java)
            .map { c ->
                val pid = (c.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (c.profileId == pid) {
                    c
                } else {
                    c.copy(
                        id = (c.id as String?) ?: "personal_card",
                        profileId = pid
                    )
                }
            }
        val deletedEntries = readList(stagingDir, BackupWriter.EntityFile.DELETED_ENTRIES, DeletedEntry::class.java)
            .map { d ->
                val pid = (d.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (d.profileId == pid) {
                    d
                } else {
                    d.copy(
                        entry_id = (d.entry_id as String?) ?: "",
                        profileId = pid
                    )
                }
            }
        val entryTags = readList(stagingDir, BackupWriter.EntityFile.ENTRY_TAGS, EntryTagCrossRef::class.java)
        val workoutSessions = readList(stagingDir, BackupWriter.EntityFile.WORKOUTS, WorkoutSession::class.java)
            .map { w ->
                val pid = (w.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (w.profileId == pid) {
                    w
                } else {
                    w.copy(
                        session_id = (w.session_id as String?) ?: "",
                        type = (w.type as String?) ?: "RUN",
                        status = (w.status as String?) ?: "ACTIVE",
                        notes = (w.notes as String?) ?: "",
                        profileId = pid
                    )
                }
            }
        val presets = readList(stagingDir, BackupWriter.EntityFile.PRESETS, WorkoutPreset::class.java)
            .map { p ->
                val pid = (p.profileId as String?)?.takeIf { it.isNotBlank() } ?: SELF_PROFILE_ID
                if (p.profileId == pid) {
                    p
                } else {
                    p.copy(
                        id = (p.id as String?) ?: "",
                        name = (p.name as String?) ?: "",
                        scheduledDay = (p.scheduledDay as String?) ?: "ANY",
                        exercises = p.exercises ?: emptyList(),
                        profileId = pid
                    )
                }
            }
        return BackupData(
            journalEntries = journalEntries,
            bodyMeasurements = bodyMeasurements,
            goals = goals,
            personalCards = personalCards,
            deletedEntries = deletedEntries,
            entryTags = entryTags,
            workoutSessions = workoutSessions,
            presets = presets,
            profiles = profiles
        )
    }

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
