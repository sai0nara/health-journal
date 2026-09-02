package com.example.healthjournal.export

import com.google.gson.Gson
import java.io.File
import java.util.zip.ZipFile
import kotlin.text.Charsets
import net.lingala.zip4j.exception.ZipException

/**
 * Reads and parses the [BackupManifest] of a full backup archive, handling both
 * plain backups (root `backup.json`) and AES-256 encrypted outer archives (a
 * single `backup.zip` entry created by [BackupEncryptor]).
 *
 * This layer never modifies the archive or the database; it only inspects the
 * header so the UI/worker can validate before any destructive restore.
 */
class BackupReader(
    private val gson: Gson,
    private val supportedFormatVersion: Int = BackupWriter.BACKUP_FORMAT_VERSION,
    private val scratchDir: File = File(System.getProperty("java.io.tmpdir") ?: ".")
) {

    /**
     * Returns the parsed [BackupManifest] of [backupFile].
     *
     * @param passphrase the passphrase for encrypted backups; a non-null value
     *   forces the encrypted (zip4j) code path.
     * @throws RestoreError.CorruptedFile if the archive/manifest is malformed.
     * @throws RestoreError.WrongPassphrase if the passphrase is incorrect.
     */
    fun readManifest(backupFile: File, passphrase: String?): BackupManifest {
        return if (passphrase != null || isEncryptedContainer(backupFile)) {
            readEncryptedManifest(backupFile, passphrase)
        } else {
            readPlainManifest(backupFile)
        }
    }

    private fun isEncryptedContainer(backupFile: File): Boolean {
        return try {
            net.lingala.zip4j.ZipFile(backupFile).use { zf ->
                zf.getFileHeader(BackupEncryptor.INNER_ENTRY_NAME) != null
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun readPlainManifest(backupFile: File): BackupManifest {
        try {
            val json = ZipFile(backupFile).use { zf ->
                val entry = zf.getEntry(BackupWriter.MANIFEST_NAME)
                    ?: throw RestoreError.CorruptedFile("Missing ${BackupWriter.MANIFEST_NAME} in backup.")
                zf.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            return parseManifest(json)
        } catch (e: RestoreError.CorruptedFile) {
            throw e
        } catch (e: Exception) {
            throw RestoreError.CorruptedFile("Unable to read backup archive.", e)
        }
    }

    private fun readEncryptedManifest(backupFile: File, passphrase: String?): BackupManifest {
        if (passphrase == null || passphrase.isBlank()) {
            throw RestoreError.WrongPassphrase()
        }
        val inner = File(scratchDir, "restore_${System.currentTimeMillis()}_${BackupEncryptor.INNER_ENTRY_NAME}")
        try {
            net.lingala.zip4j.ZipFile(backupFile, passphrase.toCharArray()).use { zf ->
                val header = zf.getFileHeader(BackupEncryptor.INNER_ENTRY_NAME)
                    ?: throw RestoreError.CorruptedFile("No encrypted backup entry found.")
                inner.outputStream().use { out ->
                    zf.getInputStream(header).use { it.copyTo(out) }
                }
            }
            return readPlainManifest(inner)
        } catch (e: RestoreError.CorruptedFile) {
            throw e
        } catch (e: ZipException) {
            throw RestoreError.WrongPassphrase(e)
        } catch (e: Exception) {
            throw RestoreError.WrongPassphrase(e)
        } finally {
            inner.delete()
        }
    }

    private fun parseManifest(json: String): BackupManifest {
        val manifest = try {
            gson.fromJson(json, BackupManifest::class.java)
        } catch (e: Exception) {
            throw RestoreError.CorruptedFile("Malformed ${BackupWriter.MANIFEST_NAME}.", e)
        }
        if (manifest == null || manifest.formatVersion <= 0) {
            throw RestoreError.CorruptedFile("Malformed ${BackupWriter.MANIFEST_NAME}.")
        }
        return manifest
    }
}
