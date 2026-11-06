package com.example.healthjournal.sync

import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.google.gson.Gson

object JournalSyncPayload {
    private val gson = Gson()

    fun toJson(entries: List<JournalEntry>): String = gson.toJson(entries)

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(
        json: String?,
        defaultProfileId: String = SELF_PROFILE_ID
    ): List<JournalEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val rawEntries = gson.fromJson(json, Array<JournalEntry>::class.java)?.toList()
                ?: return emptyList()
            rawEntries.mapNotNull { entry ->
                if (entry == null) return@mapNotNull null
                val profileId = (entry.profileId as String?)?.takeIf { it.isNotBlank() }
                    ?: defaultProfileId
                entry.copy(
                    profileId = profileId,
                    isSynced = entry.isSynced ?: true,
                    syncStatus = entry.syncStatus ?: "SYNCED",
                    attachments = entry.attachments?.mapNotNull { att ->
                        att?.copy(
                            syncStatus = att.syncStatus ?: "SYNCED",
                            isLocalOnly = att.isLocalOnly ?: false
                        )
                    } ?: emptyList(),
                    photo_urls = entry.photo_urls?.filterNotNull() ?: emptyList()
                ).also { it.tags = entry.tags }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
