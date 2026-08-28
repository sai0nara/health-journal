package com.example.healthjournal.export

import android.net.Uri
import com.example.healthjournal.data.BodyMeasurementRepository
import com.example.healthjournal.data.GoalsRepository
import com.example.healthjournal.data.JournalRepository
import com.example.healthjournal.data.PersonalCardRepository
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first

/**
 * Produces a full, round-trippable database backup archive that extends the
 * legacy shareable export with every Room entity plus the schema version and a
 * [BackupManifest]. Media referenced by journal entries is written under
 * `media/<filename>`, keyed by the file's last path segment so restore can
 * re-import and remap URIs (mirroring SyncWorker's remap convention).
 */
class FullBackupUseCase(
    private val database: JournalDatabase,
    private val journalRepository: JournalRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val goalsRepository: GoalsRepository,
    private val personalCardRepository: PersonalCardRepository,
    private val filesDir: File,
    private val exportsDir: File,
    private val gson: Gson = Gson()
) {

    suspend fun execute(): File {
        exportsDir.mkdirs()
        val schemaVersion = database.openHelper.readableDatabase.version
        val archive = File(exportsDir, "health_journal_backup_${System.currentTimeMillis()}.zip")

        val journalDao = database.journalDao()
        val bodyMeasurementDao = database.bodyMeasurementDao()

        val journalEntries = journalRepository.getAllEntriesInDateRange(0L, Long.MAX_VALUE)
        val bodyMeasurements = bodyMeasurementRepository.allEntries.first()
        val goals = goalsRepository.getAll()

        val personalCards = personalCardRepository.getPersonalCardSnapshot()
            ?.let { listOf(it) }
            ?: emptyList()

        val deletedEntries = mergeTombstones(
            journalDao.getAllDeletedEntries(),
            bodyMeasurementDao.getAllDeletedEntries()
        )

        val data = BackupData(
            journalEntries = journalEntries,
            bodyMeasurements = bodyMeasurements,
            goals = goals,
            personalCards = personalCards,
            deletedEntries = deletedEntries,
            entryTags = journalDao.getAllTags()
        )

        val writer = BackupWriter(gson = gson, schemaVersion = schemaVersion)

        ZipOutputStream(FileOutputStream(archive)).use { zos ->
            writer.writeBackup(zos, data, collectMedia(journalEntries))
        }

        return archive
    }

    private fun mergeTombstones(
        journal: List<DeletedEntry>,
        measurements: List<DeletedEntry>
    ): List<DeletedEntry> {
        val byId = LinkedHashMap<String, DeletedEntry>()
        journal.forEach { byId[it.entry_id] = it }
        measurements.forEach { byId.putIfAbsent(it.entry_id, it) }
        return byId.values.toList()
    }

    /**
     * Resolves every unique attachment/photo file referenced by [entries] into
     * `media/<filename>` pairs, deduplicated by filename.
     */
    private fun collectMedia(entries: List<com.example.healthjournal.data.local.JournalEntry>): List<Pair<String, File>> {
        val seen = LinkedHashSet<String>()
        val result = mutableListOf<Pair<String, File>>()

        for (entry in entries) {
            entry.attachments?.forEach { attachment ->
                val file = resolveFile(attachment.uri)
                val name = MEDIA_PREFIX + (attachment.name ?: attachment.uri.substringAfterLast("/"))
                if (file != null && seen.add(name)) result.add(name to file)
            }
            entry.photo_urls?.forEach { photoUrl ->
                val file = resolveFile(photoUrl)
                val name = MEDIA_PREFIX + photoUrl.substringAfterLast("/")
                if (file != null && seen.add(name)) result.add(name to file)
            }
        }

        return result
    }

    private fun resolveFile(fileUri: String): File? {
        val uri = Uri.parse(fileUri)
        val path = if (uri.scheme == "file") uri.path else fileUri
        if (path.isNullOrBlank()) return null
        val file = File(path)
        return if (file.isFile) file else null
    }

    companion object {
        private const val MEDIA_PREFIX = "media/"
    }
}
