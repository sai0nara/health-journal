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

    private data class PendingSelection(val filePath: String, val passphrase: String?)

    private var pending: PendingSelection? = null

    /** User picked a backup file; validate its manifest before confirming. */
    fun selectBackup(uri: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Validating
            try {
                val file = resolveUriToFile(uri)
                val manifest = backupReader.readManifest(file, null)
                pending = PendingSelection(file.absolutePath, null)
                _uiState.value = RestoreUiState.ConfirmationRequired(
                    fileUri = file.absolutePath,
                    isEncrypted = false,
                    schemaVersion = manifest.schemaVersion,
                    backupTimestamp = manifest.backupTimestamp
                )
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
                val file = resolveUriToFile(current.fileUri)
                val manifest = backupReader.readManifest(file, passphrase)
                pending = PendingSelection(file.absolutePath, passphrase)
                _uiState.value = RestoreUiState.ConfirmationRequired(
                    fileUri = file.absolutePath,
                    isEncrypted = true,
                    schemaVersion = manifest.schemaVersion,
                    backupTimestamp = manifest.backupTimestamp
                )
            } catch (e: RestoreError.WrongPassphrase) {
                _uiState.value = RestoreUiState.Error(e, requestPassphrase = true)
            } catch (e: RestoreError) {
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                _uiState.value = RestoreUiState.Error(
                    RestoreError.CorruptedFile("Unable to open the selected backup.", e)
                )
            }
        }
    }

    /** User confirmed the restore; enqueue/run it and surface the outcome. */
    fun confirmRestore() {
        val selection = pending ?: return
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Processing
            try {
                val result = restoreToCompletion(getApplication(), selection.filePath, selection.passphrase)
                _uiState.value = RestoreUiState.Success(result)
            } catch (e: RestoreError.WrongPassphrase) {
                _uiState.value = RestoreUiState.Error(e, requestPassphrase = true)
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

    /** Resolves a content:// or file:// URI to a local [File] (content URIs are copied to cache). */
    private fun resolveUriToFile(uriString: String): File {
        if (uriString.startsWith("file://")) {
            return File(uriString.removePrefix("file://"))
        }
        val uri = Uri.parse(uriString)
        val tmp = File(getApplication<Application>().cacheDir, "restore_selected_${System.currentTimeMillis()}.zip")
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        }
        return tmp
    }

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
