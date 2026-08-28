package com.example.healthjournal.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreIntegrationTest {

    private lateinit var db: JournalDatabase
    private lateinit var filesDir: File
    private lateinit var scratchDir: File
    private var syncTriggered = false

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JournalDatabase::class.java).build()
        filesDir = File(context.cacheDir, "restore_it_files_${System.nanoTime()}").apply { mkdirs() }
        scratchDir = File(context.cacheDir, "restore_it_scratch_${System.nanoTime()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        filesDir.deleteRecursively()
        scratchDir.deleteRecursively()
    }

    private fun buildBackup(data: BackupData, mediaFile: File?, target: File): File {
        FileOutputStream(target).use { fos ->
            ZipOutputStream(fos).use { zos ->
                val media = if (mediaFile != null) listOf("media/photo_b.jpg" to mediaFile) else emptyList()
                BackupWriter(Gson(), JournalDatabase.CURRENT_SCHEMA_VERSION).writeBackup(zos, data, media)
            }
        }
        return target
    }

    private fun coordinator(): RestoreCoordinator {
        val repo = RestoreRepository(db, filesDir)
        return RestoreCoordinator(
            scratchDir = scratchDir,
            backupReader = BackupReader(Gson()),
            manifestValidator = ManifestValidator(JournalDatabase.CURRENT_SCHEMA_VERSION),
            extractor = SafeBackupExtractor(),
            backupDataReader = BackupDataReader(Gson()),
            restoreRepository = repo,
            onRestoreFinished = { syncTriggered = true }
        )
    }

    private fun backupWith(entry: JournalEntry, mediaFile: File? = null): File {
        val data = BackupData(
            journalEntries = listOf(entry),
            bodyMeasurements = emptyList(),
            goals = emptyList(),
            personalCards = emptyList(),
            deletedEntries = emptyList(),
            entryTags = emptyList()
        )
        return buildBackup(data, mediaFile, File(filesDir, "backup_${System.nanoTime()}.zip"))
    }

    @Test
    fun restore_replacesDatabaseAtomically_andReImportsMedia() = runBlocking {
        val journalDao = db.journalDao()
        // Seed a pre-existing entry A that the restore must wipe out
        journalDao.insertEntry(JournalEntry(description = "ENTRY_A"))

        val mediaSource = File(filesDir, "src_photo_b.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val entryB = JournalEntry(
            description = "ENTRY_B",
            photo_urls = listOf("file:///original/device/photos/photo_b.jpg")
        )
        val archive = backupWith(entryB, mediaFile = mediaSource)

        val outcome = coordinator().run(archive, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Success)
        assertEquals(1, (outcome as RestoreCoordinator.Outcome.Success).result.journalEntryCount)

        val after = journalDao.getAllEntries().first()
        assertEquals(1, after.size)
        assertEquals("ENTRY_B", after.single().description)
        assertFalse("Pre-existing data must be wiped by atomic replace", after.any { it.description == "ENTRY_A" })

        // Media re-imported into filesDir/photos with URI remapped
        val copiedPhoto = File(filesDir, "photos/photo_b.jpg")
        assertTrue("Media file should be re-imported", copiedPhoto.exists())
        val remappedUri = "file://${copiedPhoto.absolutePath}"
        assertTrue(after.single().photo_urls!!.single().startsWith("file://"))
        assertEquals(remappedUri, after.single().photo_urls!!.single())

        // Post-restore sync callback fired
        assertTrue("Post-restore sync should be triggered", syncTriggered)
        // Staging/scratch cleaned up
        assertFalse(scratchDir.exists())
    }

    @Test
    fun restore_encryptedBackup_withCorrectPassphrase_succeeds() = runBlocking {
        val journalDao = db.journalDao()
        val entryB = JournalEntry(description = "ENCRYPTED_ENTRY")
        val plain = backupWith(entryB)
        val encrypted = File(filesDir, "enc_${System.nanoTime()}.zip")
        BackupEncryptor().encrypt(plain, encrypted, "super-secret")

        val outcome = coordinator().run(encrypted, passphrase = "super-secret")

        assertTrue(outcome is RestoreCoordinator.Outcome.Success)
        assertEquals("ENCRYPTED_ENTRY", journalDao.getAllEntries().first().single().description)
    }

    @Test
    fun restore_wrongPassphrase_failsAndLeavesOriginalDataIntact() = runBlocking {
        val journalDao = db.journalDao()
        journalDao.insertEntry(JournalEntry(description = "ORIGINAL"))

        val entryB = JournalEntry(description = "NEVER_APPLIED")
        val plain = backupWith(entryB)
        val encrypted = File(filesDir, "enc_${System.nanoTime()}.zip")
        BackupEncryptor().encrypt(plain, encrypted, "super-secret")

        val outcome = coordinator().run(encrypted, passphrase = "wrong-passphrase")

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.WrongPassphrase)

        val after = journalDao.getAllEntries().first()
        assertEquals("Original data must remain intact", 1, after.size)
        assertEquals("ORIGINAL", after.single().description)
        // Media import must not have happened
        assertFalse(File(filesDir, "photos").exists())
    }

    @Test
    fun restore_versionMismatch_failsWithoutRestoring() = runBlocking {
        val journalDao = db.journalDao()
        journalDao.insertEntry(JournalEntry(description = "KEEP_ME"))

        val data = BackupData(journalEntries = listOf(JournalEntry(description = "FUTURE")))
        val target = File(filesDir, "legacy_${System.nanoTime()}.zip")
        FileOutputStream(target).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // mismatched (future) schema version
                BackupWriter(Gson(), JournalDatabase.CURRENT_SCHEMA_VERSION + 1).writeBackup(zos, data, emptyList())
            }
        }

        val outcome = coordinator().run(target, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.VersionMismatch)
        assertEquals("KEEP_ME", journalDao.getAllEntries().first().single().description)
    }
}
