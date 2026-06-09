package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class AttachmentData(
    val name: String,
    val uri: String,
    val mimeType: String,
    val isLocalOnly: Boolean? = true,
    val remoteUrl: String? = null,
    val syncStatus: String? = "PENDING"
)

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val entry_id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = timestamp,
    val description: String,
    val photo_urls: List<String>? = emptyList(),
    val attachments: List<AttachmentData>? = emptyList(),
    val bp_systolic: Double? = null,
    val bp_diastolic: Double? = null,
    val heart_rate_avg: Int? = null,
    val sleep_hours: Float? = null,
    val ai_advice: String? = null,
    val isArchived: Boolean? = false,
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC"
)
