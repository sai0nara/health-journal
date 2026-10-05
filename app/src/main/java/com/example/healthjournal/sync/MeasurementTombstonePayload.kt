package com.example.healthjournal.sync

import com.example.healthjournal.data.local.DeletedEntry
import com.google.gson.Gson

/**
 * Gson codec for the body_measurements_tombstones.json Drive payload: a bare
 * JSON list of [DeletedEntry] rows forming the cross-device deletion ledger.
 * A missing or unparseable cloud file deserializes to an empty list so a
 * fresh device never crashes on legacy clouds without the ledger file.
 */
object MeasurementTombstonePayload {
    private val gson = Gson()


    fun toJson(tombstones: List<DeletedEntry>): String =
        gson.toJson(tombstones)

    fun fromJson(json: String?): List<DeletedEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<DeletedEntry>::class.java)?.toList() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
