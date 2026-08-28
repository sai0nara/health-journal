package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()
    private val writer = BackupWriter(gson = gson, schemaVersion = 12)

    private fun createBackup(
        data: BackupData,
        mediaFiles: List<Pair<String, File>> = emptyList()
    ): File {
        val zipFile = tempFolder.newFile("backup.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            writer.writeBackup(zos, data, mediaFiles)
        }
        return zipFile
    }

    private fun readEntryText(zip: File, name: String): String? {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry(name) ?: return null
            return zf.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }

    private fun readEntryBytes(zip: File, name: String): ByteArray? {
        ZipFile(zip).use { zf ->
            val entry = zf.getEntry(name) ?: return null
            return zf.getInputStream(entry).use { it.readBytes() }
        }
    }

    private val sampleData = BackupData(
        journalEntries = listOf(
            JournalEntry(entry_id = "e1", description = "Day one")
        ),
        bodyMeasurements = listOf(
            BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)
        ),
        goals = listOf(
            GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)
        ),
        personalCards = listOf(
            PersonalCard(id = "personal_card")
        ),
        deletedEntries = listOf(
            DeletedEntry(entry_id = "eDeleted", deletedAt = 2L)
        ),
        entryTags = listOf(
            EntryTagCrossRef(entryId = "e1", tag = "health")
        )
    )

    @Test
    fun writeBackup_writesManifestWithVersionTimestampAndContents() {
        val zip = createBackup(sampleData)

        val manifestJson = readEntryText(zip, BackupWriter.MANIFEST_NAME)
        assertNotNull(manifestJson)
        val manifest = gson.fromJson(manifestJson, BackupManifest::class.java)

        assertEquals(BackupWriter.BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(12, manifest.schemaVersion)
        assertTrue(manifest.backupTimestamp > 0)
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.JOURNAL))
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.BODY_MEASUREMENTS))
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.GOALS))
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.PERSONAL_CARD))
    }

    @Test
    fun writeBackup_writesAllEntityJsonFiles() {
        val zip = createBackup(sampleData)

        val entries = gson.fromJson(
            readEntryText(zip, "data.json")!!,
            Array<JournalEntry>::class.java
        )
        assertEquals(1, entries.size)
        assertEquals("e1", entries[0].entry_id)

        val measurements = gson.fromJson(
            readEntryText(zip, BackupWriter.EntityFile.BODY_MEASUREMENTS)!!,
            Array<BodyMeasurementEntry>::class.java
        )
        assertEquals(1, measurements.size)

        val goals = gson.fromJson(
            readEntryText(zip, BackupWriter.EntityFile.GOALS)!!,
            Array<GoalEntity>::class.java
        )
        assertEquals(1, goals.size)

        val cards = gson.fromJson(
            readEntryText(zip, BackupWriter.EntityFile.PERSONAL_CARD)!!,
            Array<PersonalCard>::class.java
        )
        assertEquals(1, cards.size)

        val deleted = gson.fromJson(
            readEntryText(zip, BackupWriter.EntityFile.DELETED_ENTRIES)!!,
            Array<DeletedEntry>::class.java
        )
        assertEquals(1, deleted.size)

        val tags = gson.fromJson(
            readEntryText(zip, BackupWriter.EntityFile.ENTRY_TAGS)!!,
            Array<EntryTagCrossRef>::class.java
        )
        assertEquals(1, tags.size)
    }

    @Test
    fun writeBackup_writesMediaFilesUnderMediaFolder() {
        val mediaFile = tempFolder.newFile("photo_test.jpg")
        mediaFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        val zip = createBackup(
            sampleData,
            mediaFiles = listOf("media/photo_test.jpg" to mediaFile)
        )

        val bytes = readEntryBytes(zip, "media/photo_test.jpg")
        assertNotNull(bytes)
        assertEquals(5, bytes!!.size)
        assertEquals(byteArrayOf(1, 2, 3, 4, 5).contentToString(), bytes.contentToString())
    }

    @Test
    fun writeBackup_emptyCollectionsProduceEmptyArrays() {
        val empty = BackupData(
            journalEntries = emptyList(),
            bodyMeasurements = emptyList(),
            goals = emptyList(),
            personalCards = emptyList(),
            deletedEntries = emptyList(),
            entryTags = emptyList()
        )
        val zip = createBackup(empty)

        val entries = gson.fromJson(readEntryText(zip, "data.json")!!, Array<JournalEntry>::class.java)
        assertEquals(0, entries.size)
        assertNotNull(readEntryText(zip, "backup.json"))
    }

    @Test
    fun writeBackup_skipsMediaFilesThatDoNotExist() {
        val zip = createBackup(
            sampleData,
            mediaFiles = listOf("media/missing.jpg" to File("/nonexistent/missing.jpg"))
        )

        assertTrue(zip.length() > 0)
        assertEquals(null, readEntryBytes(zip, "media/missing.jpg"))
    }
}
