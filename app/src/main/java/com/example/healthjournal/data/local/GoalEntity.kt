package com.example.healthjournal.data.local

import androidx.room.Entity

/**
 * One per-parameter Body Analytics goal target. `parameterId` stores the
 * [com.example.healthjournal.domain.MeasurementField] name; `target` is in
 * metric units (kg for weight, cm for girths). `lastModified` drives LWW
 * resolution during cloud sync of the goals snapshot.
 */
@Entity(tableName = "goals", primaryKeys = ["profileId", "parameterId"])
data class GoalEntity(
    val parameterId: String,
    val target: Double,
    val lastModified: Long,
    val profileId: String = SELF_PROFILE_ID
)
