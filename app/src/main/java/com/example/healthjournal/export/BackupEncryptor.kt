package com.example.healthjournal.export

import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.ZipParameters

/**
 * Wraps a plain full-backup ZIP into an AES-256 encrypted outer archive.
 *
 * The plain backup sits as a single entry named [INNER_ENTRY_NAME] inside the
 * encrypted archive, so restore must first decrypt the outer archive with the
 * passphrase and then process the inner backup. Keeping the inner archive
 * unmodified means encryption is a transparent outer layer that does not alter
 * the [BackupWriter] format.
 */
class BackupEncryptor {

    /**
     * Produces [targetZip] as an AES-256 encrypted archive containing the
     * contents of [sourceZip] under [INNER_ENTRY_NAME].
     *
     * @throws IllegalArgumentException if the passphrase is blank.
     */
    fun encrypt(sourceZip: File, targetZip: File, passphrase: String) {
        require(passphrase.isNotBlank()) { "Passphrase must not be blank" }

        val params = ZipParameters().apply {
            compressionMethod = CompressionMethod.DEFLATE
            compressionLevel = CompressionLevel.MAXIMUM
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            fileNameInZip = INNER_ENTRY_NAME
            isOverrideExistingFilesInZip = true
        }

        ZipFile(targetZip, passphrase.toCharArray()).use { zipFile ->
            zipFile.addFile(sourceZip, params)
        }
    }

    companion object {
        const val INNER_ENTRY_NAME = "backup.zip"
    }
}
