package com.example.healthjournal.export

import com.google.gson.Gson
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets

/**
 * Serializes a full [BackupData] snapshot plus media files into a ZIP archive
 * that serves as a round-trippable, full-database backup.
 *
 * Layout:
 *  - [EntityFile.JOURNAL]  -> data.json                 (journal entries; kept as the
 *                                                       legacy name for backward compat)
 *  - [EntityFile.BODY_MEASUREMENTS] -> body_measurements.json
 *  - [EntityFile.GOALS]            -> goals.json
 *  - [EntityFile.PERSONAL_CARD]    -> personal_card.json
 *  - [EntityFile.DELETED_ENTRIES]  -> deleted_entries.json
 *  - [EntityFile.ENTRY_TAGS]       -> entry_tags.json
 *  - media/<filename>              -> referenced attachment/photo files
 *  - [MANIFEST_NAME] -> backup.json (written first)
 *
 * The writer is pure JVM (no Android dependencies) so it is unit-testable with
 * plain temp files and a passed-in [Gson] instance.
 */
class BackupWriter(
    private val gson: Gson,
    private val schemaVersion: Int,
    private val formatVersion: Int = BACKUP_FORMAT_VERSION
) {

    /**
     * Writes the full manifest, entity JSON files and media files into [zip].
     *
     * @param mediaFiles pairs of (entryNameInZip, sourceFile). Missing files are
     *   silently skipped so a broken attachment never aborts the whole backup.
     * @return the [BackupManifest] that was written, for verification/tests.
     */
    fun writeBackup(
        zip: ZipOutputStream,
        data: BackupData,
        mediaFiles: List<Pair<String, File>>
    ): BackupManifest {
        val contents = mutableListOf(
            EntityFile.JOURNAL,
            EntityFile.BODY_MEASUREMENTS,
            EntityFile.GOALS,
            EntityFile.PERSONAL_CARD,
            EntityFile.DELETED_ENTRIES,
            EntityFile.ENTRY_TAGS
        )

        val manifest = BackupManifest(
            formatVersion = formatVersion,
            schemaVersion = schemaVersion,
            backupTimestamp = System.currentTimeMillis(),
            contents = contents
        )

        writeJsonEntry(zip, MANIFEST_NAME, manifest)
        writeJsonEntry(zip, EntityFile.JOURNAL, data.journalEntries)
        writeJsonEntry(zip, EntityFile.BODY_MEASUREMENTS, data.bodyMeasurements)
        writeJsonEntry(zip, EntityFile.GOALS, data.goals)
        writeJsonEntry(zip, EntityFile.PERSONAL_CARD, data.personalCards)
        writeJsonEntry(zip, EntityFile.DELETED_ENTRIES, data.deletedEntries)
        writeJsonEntry(zip, EntityFile.ENTRY_TAGS, data.entryTags)

        for ((entryName, sourceFile) in mediaFiles) {
            writeMediaEntry(zip, entryName, sourceFile)
        }

        return manifest
    }

    private fun writeJsonEntry(zip: ZipOutputStream, name: String, value: Any) {
        val json = gson.toJson(value)
        zip.putNextEntry(ZipEntry(name))
        zip.write(json.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun writeMediaEntry(zip: ZipOutputStream, entryName: String, source: File) {
        if (!source.isFile || !source.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        source.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                zip.write(buffer, 0, read)
            }
        }
        zip.closeEntry()
    }

    companion object {
        const val BACKUP_FORMAT_VERSION = 1
        const val MANIFEST_NAME = "backup.json"
        private const val BUFFER_SIZE = 8192
    }

    object EntityFile {
        const val JOURNAL = "data.json"
        const val BODY_MEASUREMENTS = "body_measurements.json"
        const val GOALS = "goals.json"
        const val PERSONAL_CARD = "personal_card.json"
        const val DELETED_ENTRIES = "deleted_entries.json"
        const val ENTRY_TAGS = "entry_tags.json"
    }
}
