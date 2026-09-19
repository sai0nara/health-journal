package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A movement in the searchable exercise catalog: its muscle category, and the
 * ids of alternative movements it can convert weight against. Built-in rows
 * are seeded from a versioned constants source; [isUserAdded] marks rows the
 * user created.
 */
@Entity(
    tableName = "exercise_catalog",
    indices = [Index(value = ["name"])]
)
data class ExerciseCatalogItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleCategory: String,
    val alternativeIds: List<String> = emptyList(),
    val isUserAdded: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)