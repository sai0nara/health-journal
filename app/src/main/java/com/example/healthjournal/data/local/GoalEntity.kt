package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One per-parameter Body Analytics goal target. `parameterId` stores the
 * [com.example.healthjournal.domain.MeasurementField] name; `target` is in
 * metric units (kg for weight, cm for girths). `lastModified` drives LWW
 * resolution during cloud sync of the goals snapshot.
 */
@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val parameterId: String,
    val target: Double,
    val lastModified: Long
)
