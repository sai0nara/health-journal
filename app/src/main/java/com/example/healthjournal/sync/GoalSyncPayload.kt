package com.example.healthjournal.sync

import com.example.healthjournal.data.local.GoalEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gson codec for the body_measurements_goals.json Drive payload: a bare
 * JSON list, mirroring the measurements payload contract. A missing or
 * unparseable cloud file deserializes to an empty list so the analytics
 * section gracefully falls back to trend-only charts.
 */
object GoalSyncPayload {
    private val gson = Gson()
    private val type = object : TypeToken<List<GoalEntity>>() {}.type

    fun toJson(goals: List<GoalEntity>): String =
        gson.toJson(goals)

    fun fromJson(json: String?): List<GoalEntity> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<GoalEntity>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
