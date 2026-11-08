package com.example.healthjournal.data.local

import androidx.room.Entity

@Entity(tableName = "deleted_entries", primaryKeys = ["profileId", "entry_id"])
data class DeletedEntry(
    val entry_id: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val profileId: String = SELF_PROFILE_ID
)
