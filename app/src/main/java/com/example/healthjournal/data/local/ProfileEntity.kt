package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Stable id for the migrated/initial Self profile. */
const val SELF_PROFILE_ID = "self"

enum class ProfileRelationship {
    SELF,
    SIBLING,
    CHILD,
    PARENT,
    OTHER
}

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String,
    val accentColor: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = createdAt,
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC"
)
