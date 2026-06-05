package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_entries")
data class DeletedEntry(
    @PrimaryKey val entry_id: String,
    val deletedAt: Long = System.currentTimeMillis()
)
