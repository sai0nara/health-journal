package com.example.healthjournal.export

import com.example.healthjournal.data.local.JournalEntry
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupReaderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val gson = Gson()
    private lateinit var reader: BackupReader

    @Before
    fun setup() {
        reader = BackupReader(gson = gson)
    }

    private fun writePlainBackup(): File {
        val zip = tempFolder.newFile("plain_backup.zip")
        val data = BackupData(
            journalEntries = listOf(JournalEntry(entry_id = "e1", description = "x"))
        )
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            BackupWriter(gson, schemaVersion = 12).writeBackup(zos, data, emptyList())
        }
        return zip
    }

    private fun writeBackupWithoutManifest(): File {
        val zip = tempFolder.newFile("no_manifest.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry("other.json"))
            zos.write("[]".toByteArray())
            zos.closeEntry()
        }
        return zip
    }

    private fun writeInvalidManifestJson(): File {
        val zip = tempFolder.newFile("bad_manifest.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry(BackupWriter.MANIFEST_NAME))
            zos.write("not-json{{{".toByteArray())
            zos.closeEntry()
        }
        return zip
    }

    private fun writeRandomBytes(): File {
        val f = tempFolder.newFile("random.bin")
        f.writeBytes(ByteArray(64) { it.toByte() })
        return f
    }

    private fun encryptedVariantOf(source: File, passphrase: String): File {
        val target = tempFolder.newFile("encrypted.zip")
        BackupEncryptor().encrypt(source, target, passphrase)
        return target
    }

    @Test
    fun readManifest_plainBackup_returnsManifest() {
        val manifest = reader.readManifest(writePlainBackup(), passphrase = null)

        assertEquals(BackupWriter.BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(12, manifest.schemaVersion)
        assertTrue(manifest.backupTimestamp > 0)
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.JOURNAL))
    }

    @Test
    fun readManifest_missingManifest_throwsCorruptedFile() {
        try {
            reader.readManifest(writeBackupWithoutManifest(), passphrase = null)
            fail("Expected CorruptedFile")
        } catch (e: RestoreError.CorruptedFile) {
            assertTrue(true)
        }
    }

    @Test
    fun readManifest_invalidManifestJson_throwsCorruptedFile() {
        try {
            reader.readManifest(writeInvalidManifestJson(), passphrase = null)
            fail("Expected CorruptedFile")
        } catch (e: RestoreError.CorruptedFile) {
            assertTrue(true)
        }
    }

    @Test
    fun readManifest_randomBinary_throwsCorruptedFile() {
        try {
            reader.readManifest(writeRandomBytes(), passphrase = null)
            fail("Expected CorruptedFile")
        } catch (e: RestoreError.CorruptedFile) {
            assertTrue(true)
        }
    }

    @Test
    fun readManifest_encryptedBackup_withCorrectPassphrase_returnsManifest() {
        val plain = writePlainBackup()
        val encrypted = encryptedVariantOf(plain, "s3cret passphrase")

        val manifest = reader.readManifest(encrypted, passphrase = "s3cret passphrase")

        assertEquals(12, manifest.schemaVersion)
        assertTrue(manifest.contents.contains(BackupWriter.EntityFile.JOURNAL))
    }

    @Test
    fun readManifest_encryptedBackup_withWrongPassphrase_throwsWrongPassphrase() {
        val plain = writePlainBackup()
        val encrypted = encryptedVariantOf(plain, "right pass")

        try {
            reader.readManifest(encrypted, passphrase = "wrong pass")
            fail("Expected WrongPassphrase")
        } catch (e: RestoreError.WrongPassphrase) {
            assertTrue(true)
        }
    }
}
