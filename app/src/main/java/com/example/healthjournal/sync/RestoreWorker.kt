package com.example.healthjournal.sync

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.example.healthjournal.data.local.JournalDatabase
import com.example.healthjournal.export.BackupDataReader
import com.example.healthjournal.export.BackupReader
import com.example.healthjournal.export.ManifestValidator
import com.example.healthjournal.export.RestoreCoordinator
import com.example.healthjournal.export.RestoreError
import com.example.healthjournal.export.RestoreRepository
import com.example.healthjournal.export.SafeBackupExtractor
import com.google.gson.Gson
import java.io.File

/**
 * WorkManager entry point for restoring a full backup. Thin wrapper around the
 * JVM-testable [RestoreCoordinator]; keeps no business logic of its own.
 *
 * Inputs (via `inputData`):
 *  - [KEY_BACKUP_URI]: Uri (content:// or file://) of the backup archive.
 *  - [KEY_PASSPHRASE]: optional passphrase for AES-256 encrypted backups.
 *
 * On success it returns [Result.success] and enqueues a WorkManager re-sync (via
 * [SyncManager.triggerManualSync]) so the restored data is re-uploaded as the source
 * of truth. On failure it returns [Result.failure] with [KEY_ERROR_TYPE] /
 * [KEY_ERROR_MESSAGE] in the output data for the UI to surface.
 */
class RestoreWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uri = inputData.getString(KEY_BACKUP_URI)
            ?: return workerFailure(RestoreError.CorruptedFile("No backup URI provided."))
        val passphrase = inputData.getString(KEY_PASSPHRASE)
        val backupFile = resolveBackupFile(uri)

        val coordinator = coordinatorProvider(applicationContext)
        return when (val outcome = coordinator.run(backupFile, passphrase)) {
            is RestoreCoordinator.Outcome.Success -> Result.success()
            is RestoreCoordinator.Outcome.Failure -> workerFailure(outcome.error)
        }
    }

    /** Resolves a content:// or file:// URI into a local [File] (content URIs are copied to cache). */
    private fun resolveBackupFile(uri: String): File {
        val parsed = Uri.parse(uri)
        if (parsed.scheme == "file") {
            parsed.path?.let { return File(it) }
        }
        val tmp = File(applicationContext.cacheDir, "restore_input_${System.currentTimeMillis()}.zip")
        applicationContext.contentResolver.openInputStream(parsed)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        return tmp
    }

    private fun workerFailure(error: RestoreError): Result {
        val output = Data.Builder()
            .putString(KEY_ERROR_TYPE, errorType(error))
            .putString(KEY_ERROR_MESSAGE, error.message)
            .build()
        return Result.failure(output)
    }

    private fun errorType(error: RestoreError): String = when (error) {
        is RestoreError.WrongPassphrase -> ERROR_TYPE_WRONG_PASSPHRASE
        is RestoreError.VersionMismatch -> ERROR_TYPE_VERSION_MISMATCH
        is RestoreError.UnsupportedFormat -> ERROR_TYPE_UNSUPPORTED_FORMAT
        is RestoreError.InsufficientStorage -> ERROR_TYPE_INSUFFICIENT_STORAGE
        else -> ERROR_TYPE_CORRUPTED
    }

    companion object {
        const val KEY_BACKUP_URI = "key_backup_uri"
        const val KEY_PASSPHRASE = "key_passphrase"
        const val KEY_ERROR_TYPE = "key_error_type"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val ERROR_TYPE_WRONG_PASSPHRASE = "wrong_passphrase"
        const val ERROR_TYPE_VERSION_MISMATCH = "version_mismatch"
        const val ERROR_TYPE_UNSUPPORTED_FORMAT = "unsupported_format"
        const val ERROR_TYPE_INSUFFICIENT_STORAGE = "insufficient_storage"
        const val ERROR_TYPE_CORRUPTED = "corrupted"

        /**
         * Provider hook for tests/DI (mirrors [SyncWorker]'s companion providers).
         * `onRestoreFinished` enqueues a manual re-sync so restored data is treated as
         * the source of truth and re-uploaded to Google Drive.
         */
        var coordinatorProvider: (Context) -> RestoreCoordinator = { context ->
            val db = JournalDatabase.getDatabase(context)
            RestoreCoordinator(
                scratchDir = File(context.cacheDir, "restore_scratch_${System.currentTimeMillis()}"),
                backupReader = BackupReader(Gson()),
                manifestValidator = ManifestValidator(JournalDatabase.CURRENT_SCHEMA_VERSION),
                extractor = SafeBackupExtractor(),
                backupDataReader = BackupDataReader(Gson()),
                restoreRepository = RestoreRepository(db, context.filesDir),
                onRestoreFinished = { SyncManager.triggerManualSync(context) }
            )
        }
    }
}
