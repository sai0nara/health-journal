package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val entry_id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val photo_url: String? = null,
    val steps: Int? = null,
    val heart_rate_avg: Int? = null,
    val sleep_hours: Float? = null,
    val ai_advice: String? = null
)
