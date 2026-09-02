package com.example.healthjournal.export

import com.example.healthjournal.data.local.BodyMeasurementEntry
import com.example.healthjournal.data.local.DeletedEntry
import com.example.healthjournal.data.local.EntryTagCrossRef
import com.example.healthjournal.data.local.GoalEntity
import com.example.healthjournal.data.local.JournalEntry
import com.example.healthjournal.data.local.PersonalCard
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RestoreCoordinatorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()
    private val schemaVersion = 12

    private val sampleData = BackupData(
        journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x")),
        bodyMeasurements = listOf(BodyMeasurementEntry(entry_id = "m1", weight_kg = 80.0)),
        goals = listOf(GoalEntity(parameterId = "weight", target = 75.0, lastModified = 1L)),
        personalCards = listOf(PersonalCard(id = "pc")),
        deletedEntries = listOf(DeletedEntry(entry_id = "d1")),
        entryTags = listOf(EntryTagCrossRef("e1", "health"))
    )

    private fun buildPlainBackup(schema: Int = schemaVersion, format: Int = BackupWriter.BACKUP_FORMAT_VERSION): File {
        val zip = File(tempFolder.root, "backup_${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { out ->
            BackupWriter(gson, schema, format).writeBackup(out, sampleData, emptyList())
        }
        return zip
    }

    private fun encryptBackup(plain: File, passphrase: String): File {
        val encrypted = File(tempFolder.root, "enc_${System.nanoTime()}.zip")
        BackupEncryptor().encrypt(plain, encrypted, passphrase)
        return encrypted
    }

    private val successResult = RestoreResult(
        journalEntryCount = 1,
        bodyMeasurementCount = 1,
        goalCount = 1,
        deletedEntryCount = 1,
        tagCount = 1,
        mediaFileCount = 0
    )

    private fun harness(scratch: File, repo: RestoreRepository? = null): Pair<RestoreCoordinator, RestoreRepository> {
        val restoreRepo = repo ?: mockk<RestoreRepository> {
            coEvery { restore(any(), any()) } returns successResult
        }
        val syncTriggered = mutableListOf<Unit>()
        val coordinator = RestoreCoordinator(
            scratchDir = scratch,
            backupReader = BackupReader(gson),
            manifestValidator = ManifestValidator(schemaVersion),
            extractor = SafeBackupExtractor(),
            backupDataReader = BackupDataReader(gson),
            restoreRepository = restoreRepo,
            onRestoreFinished = { syncTriggered.add(Unit) }
        )
        // expose syncTriggered for assertion
        return coordinator to restoreRepo
    }

    @Test
    fun run_success_restoresData_andTriggersPostRestoreSync() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, repo) = harness(scratch)
        val plain = buildPlainBackup()

        val outcome = coordinator.run(plain, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Success)
        val success = outcome as RestoreCoordinator.Outcome.Success
        assertEquals(successResult, success.result)
        coVerify { repo.restore(any(), any()) }
        // Staging cleaned up
        assertFalse(scratch.exists())
    }

    @Test
    fun run_encryptedWithCorrectPassphrase_restores() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, repo) = harness(scratch)
        val plain = buildPlainBackup()
        val encrypted = encryptBackup(plain, "correct-horse")

        val outcome = coordinator.run(encrypted, passphrase = "correct-horse")

        assertTrue(outcome is RestoreCoordinator.Outcome.Success)
        coVerify { repo.restore(any(), any()) }
        assertFalse(scratch.exists())
    }

    @Test
    fun run_wrongPassphrase_returnsWrongPassphrase_andDoesNotRestore() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, repo) = harness(scratch)
        val plain = buildPlainBackup()
        val encrypted = encryptBackup(plain, "correct-horse")

        val outcome = coordinator.run(encrypted, passphrase = "wrong")

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.WrongPassphrase)
        coVerify(inverse = true) { repo.restore(any(), any()) }
        assertFalse(scratch.exists())
    }

    @Test
    fun run_versionMismatch_returnsFailure_andDoesNotRestore() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, repo) = harness(scratch)
        val legacy = buildPlainBackup(schema = schemaVersion - 1)

        val outcome = coordinator.run(legacy, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.VersionMismatch)
        coVerify(inverse = true) { repo.restore(any(), any()) }
        assertFalse(scratch.exists())
    }

    @Test
    fun run_unsupportedFormat_returnsUnsupportedFormat() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, _) = harness(scratch)
        val future = buildPlainBackup(format = BackupWriter.BACKUP_FORMAT_VERSION + 1)

        val outcome = coordinator.run(future, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.UnsupportedFormat)
        assertFalse(scratch.exists())
    }

    @Test
    fun run_zipSlipArchive_returnsCorruptedFile() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val (coordinator, repo) = harness(scratch)
        val evil = File(tempFolder.root, "evil_${System.nanoTime()}.zip")
        ZipOutputStream(FileOutputStream(evil)).use { out ->
            // valid manifest first so validation passes, then a zip-slip entry
            val manifest = BackupManifest(BackupWriter.BACKUP_FORMAT_VERSION, schemaVersion, 1L, emptyList())
            out.putNextEntry(ZipEntry(BackupWriter.MANIFEST_NAME))
            out.write(gson.toJson(manifest).toByteArray())
            out.closeEntry()
            out.putNextEntry(ZipEntry("../escape.txt"))
            out.write("pwn".toByteArray())
            out.closeEntry()
        }

        val outcome = coordinator.run(evil, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.CorruptedFile)
        coVerify(inverse = true) { repo.restore(any(), any()) }
        assertFalse(scratch.exists())
    }

    @Test
    fun run_restoreFailure_returnsFailure_andCleansStaging() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val failingRepo = mockk<RestoreRepository> {
            coEvery { restore(any(), any()) } throws RestoreError.CorruptedFile("insert failed")
        }
        val (coordinator, _) = harness(scratch, failingRepo)
        val plain = buildPlainBackup()

        val outcome = coordinator.run(plain, passphrase = null)

        assertTrue(outcome is RestoreCoordinator.Outcome.Failure)
        assertTrue((outcome as RestoreCoordinator.Outcome.Failure).error is RestoreError.CorruptedFile)
        assertFalse(scratch.exists())
    }

    @Test
    fun run_plainBackup_passesExtractedMediaStagingDirToRestore() = runTest {
        val scratch = tempFolder.newFolder("scratch")
        val repo = mockk<RestoreRepository>()
        val capturedMediaDirs = mutableListOf<File?>()
        coEvery { repo.restore(any(), any()) } answers {
            capturedMediaDirs.add(secondArg())
            successResult
        }
        val (coordinator, _) = harness(scratch, repo)
        val plain = buildPlainBackup()

        coordinator.run(plain, passphrase = null)

        assertEquals(1, capturedMediaDirs.size)
        assertEquals("media", capturedMediaDirs.single()?.name)
    }
}
