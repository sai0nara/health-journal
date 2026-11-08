package com.example.healthjournal.sync

import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.google.gson.Gson

data class ProfileSyncSnapshot(
    val profiles: List<ProfileEntity> = emptyList(),
    val deletions: List<ProfileDeletionEntity> = emptyList()
)

object ProfileSyncPayload {
    private val gson = Gson()

    fun toJson(
        profiles: List<ProfileEntity>,
        deletions: List<ProfileDeletionEntity> = emptyList()
    ): String = gson.toJson(ProfileSyncSnapshot(profiles = profiles, deletions = deletions))

    @Suppress("UNNECESSARY_SAFE_CALL", "UNNECESSARY_ELVIS", "USELESS_ELVIS")
    fun fromJson(json: String?): ProfileSyncSnapshot {
        if (json.isNullOrBlank()) return ProfileSyncSnapshot()
        return try {
            val raw = gson.fromJson(json, ProfileSyncSnapshot::class.java)
                ?: return ProfileSyncSnapshot()
            val cleanProfiles = raw.profiles?.mapNotNull { p ->
                val id = (p?.id as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val name = (p.name as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val relationship = (p.relationship as String?)?.takeIf { it.isNotBlank() }
                    ?: ProfileRelationship.OTHER.name
                p.copy(
                    id = id,
                    name = name,
                    relationship = relationship,
                    isSynced = p.isSynced ?: true,
                    syncStatus = p.syncStatus ?: "SYNCED"
                )
            } ?: emptyList()
            val cleanDeletions = raw.deletions?.mapNotNull { d ->
                val profileId = (d?.profileId as String?)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                d.copy(profileId = profileId)
            } ?: emptyList()
            ProfileSyncSnapshot(profiles = cleanProfiles, deletions = cleanDeletions)
        } catch (_: Exception) {
            ProfileSyncSnapshot()
        }
    }
}
