package com.example.healthjournal.sync

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.google.gson.Gson

/**
 * Gson codec for the body_measurements.json Drive payload: a bare JSON
 * list, mirroring the health_journal_data.json journal format. A missing
 * or unparseable cloud file deserializes to an empty list so a fresh
 * device never crashes on legacy clouds without measurement data.
 */
object MeasurementSyncPayload {
    private val gson = Gson()

    fun toJson(measurements: List<BodyMeasurementEntry>): String =
        gson.toJson(measurements)

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(
        json: String?,
        defaultProfileId: String = SELF_PROFILE_ID
    ): List<BodyMeasurementEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson(json, Array<BodyMeasurementEntry>::class.java)?.toList()
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
