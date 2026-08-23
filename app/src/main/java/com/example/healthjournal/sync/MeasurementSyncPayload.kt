package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gson codec for the body_measurements.json Drive payload: a bare JSON
 * list, mirroring the health_journal_data.json journal format. A missing
 * or unparseable cloud file deserializes to an empty list so a fresh
 * device never crashes on legacy clouds without measurement data.
 */
object MeasurementSyncPayload {
    private val gson = Gson()
    private val type = object : TypeToken<List<BodyMeasurementEntry>>() {}.type

    fun toJson(measurements: List<BodyMeasurementEntry>): String =
        gson.toJson(measurements)

    fun fromJson(json: String?): List<BodyMeasurementEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<BodyMeasurementEntry>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
