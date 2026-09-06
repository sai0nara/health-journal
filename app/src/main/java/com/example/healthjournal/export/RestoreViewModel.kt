package com.example.healthjournal.export

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.healthjournal.sync.RestoreWorker
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the restore-from-backup flow. Pure state transitions are
 * JVM-testable via injected [backupReader] and [restoreToCompletion]; the only
 * Android-specific steps (content-URI copying, WorkManager) live behind those
 * seams and are exercised by the on-device integration tests.
 */
class RestoreViewModel(
    application: Application,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val backupReader: BackupReader = BackupReader(Gson()),
    private val restoreToCompletion: suspend (Context, String, String?) -> RestoreResult = { context, uri, pass ->
        runRestoreThroughWorker(context, uri, pass)
    }
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val uiState: StateFlow<RestoreUiState> = _uiState

    private data class PendingSelection(val workerUri: String, val passphrase: String?)

    private var pending: PendingSelection? = null

    /** User picked a backup file; validate its manifest before confirming. */
    fun selectBackup(uri: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Validating
            try {
                confirmAfterValidation(uri, passphrase = null, isEncrypted = false)
            } catch (e: RestoreError.WrongPassphrase) {
                _uiState.value = RestoreUiState.PassphraseRequired(uri)
            } catch (e: RestoreError) {
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                _uiState.value = RestoreUiState.Error(
                    RestoreError.CorruptedFile("Unable to open the selected backup.", e)
                )
            }
        }
    }

    /** User entered a passphrase for an encrypted backup; re-validate the manifest. */
    fun submitPassphrase(passphrase: String) {
        val current = _uiState.value as? RestoreUiState.PassphraseRequired ?: return
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Validating
            try {
                confirmAfterValidation(current.fileUri, passphrase, isEncrypted = true)
            } catch (e: RestoreError.WrongPassphrase) {
                // Return to PassphraseRequired so the user can retry with a fresh passphrase,
                // rather than an Error that would otherwise no-op on retry.
                _uiState.value = RestoreUiState.PassphraseRequired(current.fileUri)
            } catch (e: RestoreError) {
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                _uiState.value = RestoreUiState.Error(
                    RestoreError.CorruptedFile("Unable to open the selected backup.", e)
                )
            }
        }
    }

    /**
     * Copies the selection to a temporary file (content URIs), reads + validates its
     * manifest, and records the ORIGINAL [workerUri] in [pending]. The worker receives the
     * original selection URI (it copies the file itself and owns its cleanup) rather than a
     * cache temp path that this ViewModel might delete before the async worker runs. A
     * temporary cache copy created only for validation is deleted immediately after reading;
     * a real `file://` selection is never deleted here.
     */
    private suspend fun confirmAfterValidation(workerUri: String, passphrase: String?, isEncrypted: Boolean) {
        val resolved = resolveUriToFile(workerUri)
        try {
            val manifest = backupReader.readManifest(resolved.file, passphrase)
            pending = PendingSelection(workerUri, passphrase)
            _uiState.value = RestoreUiState.ConfirmationRequired(
                fileUri = workerUri,
                isEncrypted = isEncrypted,
                schemaVersion = manifest.schemaVersion,
                backupTimestamp = manifest.backupTimestamp
            )
        } finally {
            if (resolved.isTemp) resolved.file.delete()
        }
    }

    /** User confirmed the restore; enqueue/run it and surface the outcome. */
    fun confirmRestore() {
        val selection = pending ?: return
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Processing
            try {
                val result = restoreToCompletion(getApplication(), selection.workerUri, selection.passphrase)
                _uiState.value = RestoreUiState.Success(result)
            } catch (e: RestoreError.WrongPassphrase) {
                _uiState.value = RestoreUiState.PassphraseRequired(selection.workerUri)
            } catch (e: RestoreError) {
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                _uiState.value = RestoreUiState.Error(
                    RestoreError.IOFailure("Restore could not be completed.", e)
                )
            }
        }
    }

    fun reset() {
        pending = null
        _uiState.value = RestoreUiState.Idle
    }

    fun backFromError() {
        reset()
    }

    /**
     * Resolves a content:// or file:// URI to a local [File] for manifest validation. For a
     * content:// URI the file is copied into the app cache and [ResolvedFile.isTemp] is true
     * (so the copy can be deleted after validation); a `file://` URI maps directly to the
     * user's file and [ResolvedFile.isTemp] is false (never deleted here).
     */
    private fun resolveUriToFile(uriString: String): ResolvedFile {
        if (uriString.startsWith("file://")) {
            return ResolvedFile(File(uriString.removePrefix("file://")), isTemp = false)
        }
        val uri = Uri.parse(uriString)
        val tmp = File(getApplication<Application>().cacheDir, "restore_selected_${System.currentTimeMillis()}.zip")
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        return ResolvedFile(tmp, isTemp = true)
    }

    private data class ResolvedFile(val file: File, val isTemp: Boolean)

}

private const val WORK_NAME = "restore_backup"

/**
 * Production restore path: enqueues [RestoreWorker] as a unique one-time work and
 * suspends until it finishes, mapping success/failure back to a [RestoreResult] or
 * a typed [RestoreError]. Kept top-level so the constructor's default
 * [RestoreViewModel.restoreToCompletion] can reference it (default-arg expressions
 * cannot access instance members).
 */
private suspend fun runRestoreThroughWorker(context: Context, fileUri: String, passphrase: String?): RestoreResult {
    val request = OneTimeWorkRequestBuilder<RestoreWorker>()
        .setInputData(
            workDataOf(
                RestoreWorker.KEY_BACKUP_URI to fileUri,
                RestoreWorker.KEY_PASSPHRASE to passphrase
            )
        )
        .build()
    val wm = WorkManager.getInstance(context)
    wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)

    val flow = wm.getWorkInfosForUniqueWorkFlow(WORK_NAME)
    val finished = flow.first { infos ->
        infos.any { it.state == WorkInfo.State.SUCCEEDED || it.state == WorkInfo.State.FAILED }
    }.first { it.state == WorkInfo.State.SUCCEEDED || it.state == WorkInfo.State.FAILED }

    if (finished.state == WorkInfo.State.SUCCEEDED) {
        val data = finished.outputData
        return RestoreResult(
            journalEntryCount = data.getInt(RestoreWorker.KEY_RESULT_JOURNAL, 0),
            bodyMeasurementCount = data.getInt(RestoreWorker.KEY_RESULT_BODY, 0),
            goalCount = data.getInt(RestoreWorker.KEY_RESULT_GOALS, 0),
            deletedEntryCount = data.getInt(RestoreWorker.KEY_RESULT_DELETED, 0),
            tagCount = data.getInt(RestoreWorker.KEY_RESULT_TAGS, 0),
            mediaFileCount = data.getInt(RestoreWorker.KEY_RESULT_MEDIA, 0)
        )
    }

    throw when (finished.outputData.getString(RestoreWorker.KEY_ERROR_TYPE)) {
        RestoreWorker.ERROR_TYPE_WRONG_PASSPHRASE -> RestoreError.WrongPassphrase()
        RestoreWorker.ERROR_TYPE_VERSION_MISMATCH -> RestoreError.VersionMismatch(
            backupSchemaVersion = -1,
            currentSchemaVersion = -1
        )
        RestoreWorker.ERROR_TYPE_UNSUPPORTED_FORMAT -> RestoreError.UnsupportedFormat(-1)
        RestoreWorker.ERROR_TYPE_INSUFFICIENT_STORAGE -> RestoreError.InsufficientStorage()
        else -> RestoreError.CorruptedFile(
            finished.outputData.getString(RestoreWorker.KEY_ERROR_MESSAGE) ?: "Restore failed."
        )
    }
}
