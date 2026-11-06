package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sync-visible marker for a permanently deleted profile. */
@Entity(tableName = "profile_deletions")
data class ProfileDeletionEntity(
    @PrimaryKey val profileId: String,
    val deletedAt: Long = System.currentTimeMillis()
)
