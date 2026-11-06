package com.example.healthjournal.export

import androidx.room.withTransaction
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.ProfileEntity
import com.example.healthjournal.data.local.ProfileRelationship
import com.example.healthjournal.data.local.SELF_PROFILE_ID
import com.example.healthjournal.domain.TagIdentity
import java.io.File

/** Summary returned by [RestoreRepository.restore] for the confirmation UI. */
data class RestoreResult(
    val journalEntryCount: Int,
    val bodyMeasurementCount: Int,
    val goalCount: Int,
    val deletedEntryCount: Int,
    val tagCount: Int,
    val mediaFileCount: Int,
    val workoutCount: Int = 0,
    val presetCount: Int = 0,
    val profileCount: Int = 1
) {
    val totalRecords: Int
        get() = journalEntryCount + bodyMeasurementCount + goalCount + deletedEntryCount + tagCount +
            workoutCount + presetCount
}

/**
 * Applies a validated [BackupData] to the live database as an atomic full
 * replacement (wipe + re-insert in a single transaction, so any failure rolls
 * back and leaves the previous data intact), and re-imports media files from a
 * staged extraction directory, remapping journal URIs to this device's files.
 *
 * The transaction runner is injectable so the repository is unit-testable
 * without a real Room database.
 */
class RestoreRepository(
    private val db: JournalDatabase,
    private val filesDir: File,
    private val runInTransaction: suspend (suspend () -> Unit) -> Unit = { block ->
        db.withTransaction { block() }
    }
) {

    /**
     * Restores [data] atomically.
     *
     * @param mediaStagingDir extracted `media/` folder (filename-keyed) from a
     *   [SafeBackupExtractor]; null to skip media re-import.
     * @throws RestoreError if validation/import fails (database is left untouched).
     */
    suspend fun restore(
        data: BackupData,
        mediaStagingDir: File?,
        formatVersion: Int
    ): RestoreResult = restore(data.copy(formatVersion = formatVersion), mediaStagingDir)

    suspend fun restore(data: BackupData, mediaStagingDir: File?): RestoreResult {
        val isMultiProfileV2 = data.formatVersion >= 2 || data.profiles.isNotEmpty()
        val normalizedEntries = if (isMultiProfileV2) {
            data.journalEntries.map { e ->
                val pid = e.profileId.ifBlank { SELF_PROFILE_ID }
                if (e.profileId == pid) e else e.copy(profileId = pid).also { it.tags = e.tags }
            }
        } else {
            data.journalEntries.map { it.copy(profileId = SELF_PROFILE_ID).also { copy -> copy.tags = it.tags } }
        }
        val (remappedEntries, mediaCount, copiedFiles) = reimportMedia(normalizedEntries, mediaStagingDir)
        val dataToWrite = data.copy(journalEntries = remappedEntries)
        val canonicalTags = dataToWrite.entryTags
            .groupBy { it.entryId }
            .flatMap { (entryId, refs) ->
                TagIdentity.canonicalDistinct(refs.map { it.tag })
                    .map { EntryTagCrossRef(entryId, it) }
            }

        val restoredProfileCount = if (isMultiProfileV2) {
            data.profiles.size.coerceAtLeast(1)
        } else {
            1
        }

        try {
            runInTransaction {
                val journalDao = db.journalDao()
                val bodyDao = db.bodyMeasurementDao()
                val goalDao = db.goalDao()
                val personalCardDao = db.personalCardDao()
                val workoutDao = db.workoutSessionDao()
                val presetDao = db.workoutPresetDao()

                if (!isMultiProfileV2) {
                    // A v1 backup has no profile ownership: restore it into Self
                    // and leave any sibling profiles untouched.
                    journalDao.clearAllEntries(SELF_PROFILE_ID)
                    journalDao.clearAllDeletedEntries(SELF_PROFILE_ID)
                    journalDao.clearAllTags(SELF_PROFILE_ID)
                    bodyDao.clearAll(SELF_PROFILE_ID)
                    goalDao.clear(SELF_PROFILE_ID)
                    personalCardDao.clearAll(SELF_PROFILE_ID)
                    workoutDao.clearAll(SELF_PROFILE_ID)
                    presetDao.clearAll(SELF_PROFILE_ID)

                    journalDao.insertAll(dataToWrite.journalEntries.map { it.copy(profileId = SELF_PROFILE_ID) })
                    journalDao.insertAllDeletedEntries(dataToWrite.deletedEntries.map { it.copy(profileId = SELF_PROFILE_ID) })
                    journalDao.insertAllTags(canonicalTags)
                    bodyDao.replaceAll(dataToWrite.bodyMeasurements.map { it.copy(profileId = SELF_PROFILE_ID) })
                    goalDao.importAll(SELF_PROFILE_ID, dataToWrite.goals)
                    dataToWrite.personalCards.forEach {
                        personalCardDao.insertOrUpdate(it.copy(profileId = SELF_PROFILE_ID))
                    }
                    workoutDao.insertAll(dataToWrite.workoutSessions.map { it.copy(profileId = SELF_PROFILE_ID) })
                    presetDao.insertAll(dataToWrite.presets.map { it.copy(profileId = SELF_PROFILE_ID) })
                } else {
                    val profileDao = runCatching { db.profileDao() }.getOrNull()
                    val existingIds = runCatching { profileDao?.getAll()?.map { it.id }.orEmpty() }
                        .getOrDefault(emptyList())
                    val restoredProfiles = data.profiles.ifEmpty {
                        listOf(
                            ProfileEntity(
                                id = SELF_PROFILE_ID,
                                name = "Me",
                                relationship = ProfileRelationship.SELF.name
                            )
                        )
                    }.let { list ->
                        if (list.any { it.id == SELF_PROFILE_ID || it.relationship == ProfileRelationship.SELF.name }) {
                            list
                        } else {
                            listOf(
                                ProfileEntity(
                                    id = SELF_PROFILE_ID,
                                    name = "Me",
                                    relationship = ProfileRelationship.SELF.name
                                )
                            ) + list
                        }
                    }

                    val allProfileIdsToWipe = (
                        existingIds +
                            restoredProfiles.map { it.id } +
                            dataToWrite.journalEntries.map { it.profileId } +
                            dataToWrite.bodyMeasurements.map { it.profileId } +
                            dataToWrite.goals.map { it.profileId } +
                            dataToWrite.personalCards.map { it.profileId } +
                            dataToWrite.workoutSessions.map { it.profileId } +
                            dataToWrite.presets.map { it.profileId } +
                            SELF_PROFILE_ID
                        ).filter { it.isNotBlank() }.toSet()

                    allProfileIdsToWipe.forEach { pid ->
                        journalDao.clearAllEntries(pid)
                        journalDao.clearAllDeletedEntries(pid)
                        journalDao.clearAllTags(pid)
                        bodyDao.clearAll(pid)
                        goalDao.clear(pid)
                        personalCardDao.clearAll(pid)
                        workoutDao.clearAll(pid)
                        presetDao.clearAll(pid)
                    }

                    if (profileDao != null) {
                        existingIds.forEach { profileDao.delete(it) }
                        restoredProfiles.forEach { profileDao.upsert(it) }
                    }

                    journalDao.insertAll(
                        dataToWrite.journalEntries.map {
                            val pid = it.profileId.ifBlank { SELF_PROFILE_ID }
                            if (it.profileId == pid) it else it.copy(profileId = pid)
                        }
                    )
                    journalDao.insertAllDeletedEntries(
                        dataToWrite.deletedEntries.map {
                            val pid = it.profileId.ifBlank { SELF_PROFILE_ID }
                            if (it.profileId == pid) it else it.copy(profileId = pid)
                        }
                    )
                    journalDao.insertAllTags(canonicalTags)
                    bodyDao.replaceAll(
                        dataToWrite.bodyMeasurements.map {
                            val pid = it.profileId.ifBlank { SELF_PROFILE_ID }
                            if (it.profileId == pid) it else it.copy(profileId = pid)
                        }
                    )
                    val goalsByProfile = dataToWrite.goals.groupBy { it.profileId.ifBlank { SELF_PROFILE_ID } }
                    if (goalsByProfile.isEmpty()) {
                        goalDao.importAll(SELF_PROFILE_ID, emptyList())
                    } else {
                        goalsByProfile.forEach { (pid, goalsForProfile) ->
                            goalDao.importAll(pid, goalsForProfile.map { it.copy(profileId = pid) })
                        }
                    }
                    dataToWrite.personalCards.forEach { card ->
                        val pid = card.profileId.ifBlank { SELF_PROFILE_ID }
                        personalCardDao.insertOrUpdate(card.copy(profileId = pid))
                    }
                    workoutDao.insertAll(
                        dataToWrite.workoutSessions.map {
                            val pid = it.profileId.ifBlank { SELF_PROFILE_ID }
                            if (it.profileId == pid) it else it.copy(profileId = pid)
                        }
                    )
                    presetDao.insertAll(
                        dataToWrite.presets.map {
                            val pid = it.profileId.ifBlank { SELF_PROFILE_ID }
                            if (it.profileId == pid) it else it.copy(profileId = pid)
                        }
                    )
                }
            }
        } catch (e: Exception) {
            copiedFiles.forEach { it.delete() }
            throw e
        }

        return RestoreResult(
            journalEntryCount = data.journalEntries.size,
            bodyMeasurementCount = data.bodyMeasurements.size,
            goalCount = data.goals.size,
            deletedEntryCount = data.deletedEntries.size,
            tagCount = canonicalTags.size,
            mediaFileCount = mediaCount,
            workoutCount = data.workoutSessions.size,
            presetCount = data.presets.size,
            profileCount = restoredProfileCount
        )
    }

    /**
     * Deleted entries are inserted by merge/replace within the transaction using
     * [com.example.healthjournal.data.local.JournalDao.insertAllDeletedEntries].
     */
    private fun reimportMedia(
        entries: List<JournalEntry>,
        stagingDir: File?
    ): Triple<List<JournalEntry>, Int, List<File>> {
        if (stagingDir == null) return Triple(entries, 0, emptyList())
        val basePhotosDir = File(filesDir, "photos").apply { mkdirs() }
        val baseAttachmentsDir = File(filesDir, "attachments").apply { mkdirs() }
        var mediaCount = 0
        val copiedFiles = mutableListOf<File>()

        val remapped = entries.map { entry ->
            val profileId = entry.profileId.ifBlank { SELF_PROFILE_ID }
            val photosDir = if (profileId == SELF_PROFILE_ID) {
                basePhotosDir
            } else {
                File(basePhotosDir, profileId).apply { mkdirs() }
            }
            val attachmentsDir = if (profileId == SELF_PROFILE_ID) {
                baseAttachmentsDir
            } else {
                File(baseAttachmentsDir, profileId).apply { mkdirs() }
            }

            val newPhotos = entry.photo_urls?.map { url ->
                val filename = url.substringAfterLast('/')
                val dest = File(photosDir, filename)
                val copied = copyFromStaging(stagingDir, profileId, filename, dest)
                if (copied) {
                    mediaCount++
                    copiedFiles.add(dest)
                }
                "file://" + dest.absolutePath
            } ?: emptyList()

            val newAttachments = entry.attachments?.map { att ->
                val filename = att.uri.substringAfterLast('/')
                val dest = File(attachmentsDir, filename)
                val copied = copyFromStaging(stagingDir, profileId, filename, dest)
                if (copied) {
                    mediaCount++
                    copiedFiles.add(dest)
                }
                att.copy(uri = "file://" + dest.absolutePath)
            } ?: emptyList()

            entry.copy(photo_urls = newPhotos, attachments = newAttachments)
        }
        return Triple(remapped, mediaCount, copiedFiles)
    }

    private fun copyFromStaging(
        stagingDir: File,
        profileId: String,
        filename: String,
        dest: File
    ): Boolean {
        val scopedSource = File(stagingDir, "$profileId/$filename")
        val source = if (scopedSource.isFile) scopedSource else File(stagingDir, filename)
        if (!source.isFile) return false
        return try {
            dest.parentFile?.mkdirs()
            source.copyTo(dest, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
