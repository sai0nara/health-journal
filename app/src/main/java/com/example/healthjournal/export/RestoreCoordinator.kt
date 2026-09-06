package com.example.healthjournal.export

import java.io.File
import net.lingala.zip4j.exception.ZipException

/**
 * Orchestrates a full backup restore end to end:
 *
 *   1. Decrypt the outer AES-256 layer (if a passphrase is supplied) to reach the
 *      plain `backup.zip` inside.
 *   2. Read + validate the [BackupManifest] (format & schema) BEFORE any destructive
 *      step.
 *   3. Safely extract the plain backup into a staging directory (zip-slip / zip-bomb
 *      guarded) and load the entity JSON into a [BackupData].
 *   4. Apply it atomically via [RestoreRepository] (single-transaction wipe + re-insert
 *      with automatic rollback, plus media re-import).
 *   5. On success, invoke [onRestoreFinished] (the worker uses this to enqueue a
 *      WorkManager re-sync) and clean up the scratch/staging directory.
 *
 * This class is pure JVM (no Android dependencies) so the whole worker's orchestration
 * is unit-testable; [com.example.healthjournal.sync.RestoreWorker] is a thin wrapper.
 */
class RestoreCoordinator(
    private val scratchDir: File,
    private val backupReader: BackupReader,
    private val manifestValidator: ManifestValidator,
    private val extractor: SafeBackupExtractor,
    private val backupDataReader: BackupDataReader,
    private val restoreRepository: RestoreRepository,
    private val onRestoreFinished: suspend () -> Unit = {},
) {

    sealed class Outcome {
        data class Success(val result: RestoreResult) : Outcome()
        data class Failure(val error: RestoreError) : Outcome()
    }

    /** Runs a restore of [backupFile]; returns [Outcome.Success] or [Outcome.Failure]. */
    suspend fun run(backupFile: File, passphrase: String?): Outcome {
        scratchDir.mkdirs()
        try {
            val plainZip = decryptInnerIfEncrypted(backupFile, passphrase)

            val manifest = backupReader.readManifest(plainZip, null)
            when (val valid = manifestValidator.validate(manifest)) {
                is ManifestValidation.Invalid -> return Outcome.Failure(valid.error)
                ManifestValidation.Valid -> Unit
            }

            val staging = File(scratchDir, "staging")
            extractor.extract(plainZip, staging)
            val data = backupDataReader.read(staging)

            val result = restoreRepository.restore(data, mediaStagingDir = File(staging, "media"))

            onRestoreFinished()
            return Outcome.Success(result)
        } catch (e: RestoreError) {
            return Outcome.Failure(e)
        } catch (e: Exception) {
            return Outcome.Failure(RestoreError.CorruptedFile("Restore failed.", e))
        } finally {
            scratchDir.deleteRecursively()
        }
    }

    /**
     * If a passphrase is supplied, decrypts the single inner `backup.zip` entry of the
     * outer AES-256 archive into a scratch file and returns it; otherwise returns
     * [backupFile] unchanged (a plain backup).
     */
    private fun decryptInnerIfEncrypted(backupFile: File, passphrase: String?): File {
        if (passphrase == null || passphrase.isBlank()) return backupFile
        val inner = File(scratchDir, "inner_backup.zip")
        try {
            net.lingala.zip4j.ZipFile(backupFile, passphrase.toCharArray()).use { zf ->
                val header = zf.getFileHeader(BackupEncryptor.INNER_ENTRY_NAME)
                    ?: throw RestoreError.CorruptedFile("No encrypted backup entry found.")
                inner.outputStream().use { out ->
                    zf.getInputStream(header).use { it.copyTo(out) }
                }
            }
            return inner
        } catch (e: ZipException) {
            throw RestoreError.WrongPassphrase(e)
        } catch (e: RestoreError) {
            throw e
        } catch (e: Exception) {
            throw RestoreError.WrongPassphrase(e)
        }
    }
}
