package com.example.healthjournal.export

import androidx.room.withTransaction
import com.example.healthjournal.data.local.AttachmentData
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import java.io.File

/** Summary returned by [RestoreRepository.restore] for the confirmation UI. */
data class RestoreResult(
    val journalEntryCount: Int,
    val bodyMeasurementCount: Int,
    val goalCount: Int,
    val deletedEntryCount: Int,
    val tagCount: Int,
    val mediaFileCount: Int
) {
    val totalRecords: Int
        get() = journalEntryCount + bodyMeasurementCount + goalCount + deletedEntryCount + tagCount
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
    suspend fun restore(data: BackupData, mediaStagingDir: File?): RestoreResult {
        val (remappedEntries, mediaCount, copiedFiles) = reimportMedia(data.journalEntries, mediaStagingDir)
        val dataToWrite = data.copy(journalEntries = remappedEntries)

        try {
            runInTransaction {
                val journalDao = db.journalDao()
                val bodyDao = db.bodyMeasurementDao()
                val goalDao = db.goalDao()
                val personalCardDao = db.personalCardDao()

                journalDao.clearAllEntries()
                journalDao.clearAllDeletedEntries()
                journalDao.clearAllTags()
                bodyDao.clearAll()
                goalDao.clear()
                personalCardDao.clearAll()

                journalDao.insertAll(dataToWrite.journalEntries)
                journalDao.insertAllDeletedEntries(dataToWrite.deletedEntries)
                journalDao.insertAllTags(dataToWrite.entryTags)
                bodyDao.replaceAll(dataToWrite.bodyMeasurements)
                goalDao.importAll(dataToWrite.goals)
                dataToWrite.personalCards.forEach { personalCardDao.insertOrUpdate(it) }
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
            tagCount = data.entryTags.size,
            mediaFileCount = mediaCount
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
        val photosDir = File(filesDir, "photos").apply { mkdirs() }
        val attachmentsDir = File(filesDir, "attachments").apply { mkdirs() }
        var mediaCount = 0
        val copiedFiles = mutableListOf<File>()

        val remapped = entries.map { entry ->
            val newPhotos = entry.photo_urls?.map { url ->
                val filename = url.substringAfterLast('/')
                val dest = File(photosDir, filename)
                val copied = copyFromStaging(stagingDir, filename, dest)
                if (copied) {
                    mediaCount++
                    copiedFiles.add(dest)
                }
                "file://" + dest.absolutePath
            } ?: emptyList()

            val newAttachments = entry.attachments?.map { att ->
                val filename = att.uri.substringAfterLast('/')
                val dest = File(attachmentsDir, filename)
                val copied = copyFromStaging(stagingDir, filename, dest)
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

    private fun copyFromStaging(stagingDir: File, filename: String, dest: File): Boolean {
        val source = File(stagingDir, filename)
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
