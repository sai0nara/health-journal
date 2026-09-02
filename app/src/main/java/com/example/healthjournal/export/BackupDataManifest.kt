package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard

/**
 * Immutable snapshot of every Room entity that participates in a full backup
 * and restore. Restore is a full-replace operation (no merge).
 */
data class BackupData(
    val journalEntries: List<JournalEntry> = emptyList(),
    val bodyMeasurements: List<BodyMeasurementEntry> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val personalCards: List<PersonalCard> = emptyList(),
    val deletedEntries: List<DeletedEntry> = emptyList(),
    val entryTags: List<EntryTagCrossRef> = emptyList()
)

/**
 * Header written as the first entry of a full backup ZIP. Drives restore
 * version/compatibility validation before any destructive step.
 */
data class BackupManifest(
    val formatVersion: Int,
    val schemaVersion: Int,
    val backupTimestamp: Long,
    val contents: List<String>
)
