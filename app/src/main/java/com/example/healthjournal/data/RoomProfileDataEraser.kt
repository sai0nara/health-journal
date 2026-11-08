package com.example.healthjournal.data

import androidx.room.withTransaction
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.ProfileDeletionEntity
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import java.io.File
import java.net.URLDecoder

/** Deletes every profile-owned row, cleans sandbox media, and leaves a sync-visible tombstone. */
class RoomProfileDataEraser(
    private val database: JournalDatabase,
    private val filesDir: File? = null
) : ProfileDataEraser {

    override suspend fun erase(profileId: String) {
        val doomedEntries = runCatching {
            database.journalDao().getAllEntriesInDateRange(profileId, 0L, Long.MAX_VALUE)
        }.getOrDefault(emptyList())
        database.withTransaction {
            database.journalDao().deleteAllForProfile(profileId)
            database.bodyMeasurementDao().clearAll(profileId)
            database.goalDao().clear(profileId)
            database.personalCardDao().clearAll(profileId)
            database.workoutSessionDao().clearAll(profileId)
            database.workoutPresetDao().clearAll(profileId)
            database.profileDeletionDao().insert(ProfileDeletionEntity(profileId))
            database.profileDao().delete(profileId)
        }
        deleteProfileSandboxMedia(profileId, doomedEntries)
    }

    private fun deleteProfileSandboxMedia(profileId: String, entries: List<JournalEntry>) {
        val sandboxRoot = filesDir ?: return
        val sandboxPath = sandboxRoot.absolutePath
        entries.forEach { entry ->
            entry.photo_urls?.forEach { url -> deleteSandboxedFile(url, sandboxPath) }
            entry.attachments?.forEach { att -> deleteSandboxedFile(att.uri, sandboxPath) }
        }
        if (profileId.isNotBlank() && profileId != SELF_PROFILE_ID) {
            File(sandboxRoot, "photos/$profileId").takeIf { it.exists() }?.deleteRecursively()
            File(sandboxRoot, "attachments/$profileId").takeIf { it.exists() }?.deleteRecursively()
        }
    }

    private fun deleteSandboxedFile(uriString: String, sandboxPath: String) {
        runCatching {
            val rawPath = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString
            val decoded = URLDecoder.decode(rawPath, "UTF-8")
            if (decoded.startsWith(sandboxPath)) {
                File(decoded).delete()
            }
        }
    }
}
