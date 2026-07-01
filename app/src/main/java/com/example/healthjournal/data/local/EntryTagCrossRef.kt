package com.example.healthjournal.data.local

import androidx.room.Entity

/**
 * Cross-reference table to facilitate a many-to-many relationship 
 * between [JournalEntry] and [JournalTag].
 */
@Entity(primaryKeys = ["entryId", "tag"])
data class EntryTagCrossRef(
    val entryId: String,
    val tag: String
)
