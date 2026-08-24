package com.example.healthjournal.data.local

import java.util.UUID
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntry(
    @PrimaryKey val entry_id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val lastModified: Long = timestamp,
    val weight_kg: Double? = null,
    val chest_cm: Double? = null,
    val waist_cm: Double? = null,
    val glute_cm: Double? = null,
    val thigh_cm: Double? = null,
    val calf_cm: Double? = null,
    val bicep_cm: Double? = null,
    val isSynced: Boolean? = false,
    val syncStatus: String? = "PENDING_SYNC"
)
