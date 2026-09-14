package com.example.healthjournal.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import java.util.UUID

/**
 * A saved workout routine: a named plan (optionally attached to a scheduled
 * day) whose exercises carry progressive-overload defaults (target sets,
 * reps, weight, rest). Exercises are stored as Gson JSON, keeping a preset
 * atomic in a single row.
 */
@Entity(
    tableName = "workout_presets",
    indices = [Index(value = ["name"])]
)
data class WorkoutPreset(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val scheduledDay: String = ScheduledDay.ANY.name,
    val exercises: List<PresetExercise> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)