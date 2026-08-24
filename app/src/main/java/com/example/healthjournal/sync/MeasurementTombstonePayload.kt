package com.example.healthjournal.sync

import com.example.healthjournal.data.local.DeletedEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Gson codec for the body_measurements_tombstones.json Drive payload: a bare
 * JSON list of [DeletedEntry] rows forming the cross-device deletion ledger.
 * A missing or unparseable cloud file deserializes to an empty list so a
 * fresh device never crashes on legacy clouds without the ledger file.
 */
object MeasurementTombstonePayload {
    private val gson = Gson()
    private val type = object : TypeToken<List<DeletedEntry>>() {}.type

    fun toJson(tombstones: List<DeletedEntry>): String =
        gson.toJson(tombstones)

    fun fromJson(json: String?): List<DeletedEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<DeletedEntry>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
