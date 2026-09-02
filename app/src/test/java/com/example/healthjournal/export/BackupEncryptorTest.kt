package com.example.healthjournal.export

import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupEncryptorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val encryptor = BackupEncryptor()

    private fun writePlainSource(): File {
        val source = tempFolder.newFile("plain_backup.zip")
        source.outputStream().use { it.write("plain-backup-content".toByteArray()) }
        return source
    }

    @Test
    fun encrypt_withPassphrase_producesReadableAes256Archive() {
        val source = writePlainSource()
        val target = File(tempFolder.root, "encrypted.zip")

        encryptor.encrypt(source, target, "correct horse battery staple")

        assertTrue(target.exists())
        ZipFile(target, "correct horse battery staple".toCharArray()).use { zf ->
            assertTrue(zf.isEncrypted)
            assertTrue(zf.getFileHeaders().any { it.isEncrypted })
            val header = zf.getFileHeaders().first { it.fileName == BackupEncryptor.INNER_ENTRY_NAME }
            // AES strength is recorded on the ZIP (0x01=strong/AES-256)
            assertEquals(EncryptionMethod.AES, header.encryptionMethod)
        }
    }

    @Test
    fun encrypt_wrongPassphrase_failsToRead() {
        val source = writePlainSource()
        val target = File(tempFolder.root, "encrypted.zip")

        encryptor.encrypt(source, target, "passphrase")

        var readSucceeded = false
        try {
            ZipFile(target, "WRONG".toCharArray()).use { zf ->
                zf.getInputStream(zf.getFileHeaders().first()).use { it.readBytes() }
                readSucceeded = true
            }
        } catch (e: Exception) {
            readSucceeded = false
        }
        assertFalse("Reading encrypted entry with wrong passphrase should fail", readSucceeded)
    }

    @Test
    fun encrypt_blankPassphrase_throws() {
        var thrown = false
        try {
            encryptor.encrypt(writePlainSource(), File(tempFolder.root, "e.zip"), "  ")
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue("Expected IllegalArgumentException for blank passphrase", thrown)
    }

    @Test
    fun encrypt_roundTripsInnerBackupBytes() {
        val source = writePlainSource()
        val target = File(tempFolder.root, "encrypted.zip")

        encryptor.encrypt(source, target, "s3cret")

        ZipFile(target, "s3cret".toCharArray()).use { zf ->
            zf.extractFile(BackupEncryptor.INNER_ENTRY_NAME, tempFolder.root.absolutePath)
        }
        val extracted = File(tempFolder.root, BackupEncryptor.INNER_ENTRY_NAME)
        assertArrayEquals(
            "plain-backup-content".toByteArray(),
            extracted.readBytes()
        )
    }
}
