package com.example.healthjournal.sync

import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
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

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(
        json: String?,
        defaultProfileId: String = SELF_PROFILE_ID
    ): List<DeletedEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<DeletedEntry>::class.java)?.toList()
                ?.mapNotNull { entry ->
                    if (entry == null) return@mapNotNull null
                    val profileId = (entry.profileId as String?)?.takeIf { it.isNotBlank() }
                        ?: defaultProfileId
                    entry.copy(profileId = profileId)
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
