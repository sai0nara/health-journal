package com.example.healthjournal.sync

import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.google.gson.Gson

/**
 * Gson codec for the body_measurements_goals.json Drive payload: a bare
 * JSON list, mirroring the measurements payload contract. A missing or
 * unparseable cloud file deserializes to an empty list so the analytics
 * section gracefully falls back to trend-only charts.
 */
object GoalSyncPayload {
    private val gson = Gson()

    fun toJson(goals: List<GoalEntity>): String =
        gson.toJson(goals)

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(
        json: String?,
        defaultProfileId: String = SELF_PROFILE_ID
    ): List<GoalEntity> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<GoalEntity>::class.java)?.toList()
                ?.mapNotNull { goal ->
                    if (goal == null) return@mapNotNull null
                    val profileId = (goal.profileId as String?)?.takeIf { it.isNotBlank() }
                        ?: defaultProfileId
                    goal.copy(profileId = profileId)
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
