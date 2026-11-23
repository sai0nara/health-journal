package com.example.healthjournal.sync

import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.data.local.WorkoutPreset
import com.example.healthjournal.domain.PresetExercise
import com.example.healthjournal.domain.ScheduledDay
import com.google.gson.Gson

/**
 * Gson codec for the workout_presets.json Drive payload: a bare JSON list
 * of [WorkoutPreset] rows, mirroring [MeasurementSyncPayload]. A missing or
 * unparseable cloud file deserializes to an empty list so legacy clouds
 * without routine presets degrade safely.
 */
object PresetSyncPayload {
    private val gson = Gson()

    fun toJson(presets: List<WorkoutPreset>): String =
        gson.toJson(presets)

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(
        json: String?,
        defaultProfileId: String = SELF_PROFILE_ID
    ): List<WorkoutPreset> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<WorkoutPreset>::class.java)?.toList()
                ?.mapNotNull { preset ->
                    if (preset == null) return@mapNotNull null
                    val id = (preset.id as String?)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    val name = (preset.name as String?)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    val profileId = (preset.profileId as String?)?.takeIf { it.isNotBlank() }
                        ?: defaultProfileId
                    val scheduledDay = (preset.scheduledDay as String?)?.takeIf { it.isNotBlank() }
                        ?: ScheduledDay.ANY.name
                    val exercises = (preset.exercises as List<PresetExercise>?)
                        ?.filterNotNull()
                        ?.mapNotNull { ex ->
                            val exId = (ex.exerciseId as String?)?.trim()?.takeIf { it.isNotEmpty() }
                                ?: return@mapNotNull null
                            ex.copy(exerciseId = exId)
                        } ?: emptyList()
                    val warmupName = preset.warmupName?.trim()?.takeIf { it.isNotEmpty() }
                    val warmupDurationSeconds = if (warmupName != null) {
                        preset.warmupDurationSeconds
                    } else {
                        null
                    }
                    preset.copy(
                        id = id,
                        name = name,
                        scheduledDay = scheduledDay,
                        exercises = exercises,
                        warmupName = warmupName,
                        warmupDurationSeconds = warmupDurationSeconds,
                        profileId = profileId
                    )
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
