package com.example.healthjournal.export

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.healthjournal.sync.RestoreWorker
import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the restore-from-backup flow. Pure state transitions are
 * JVM-testable via injected [backupReader], [restoreToCompletion], and
 * [openUriInputStream]; the Android-specific steps (content-URI copying,
 * WorkManager) live behind those seams and are exercised by the on-device
 * integration tests.
 */
class RestoreViewModel(
    application: Application,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val backupReader: BackupReader = BackupReader(Gson(), scratchDir = resolveCacheDir(application)),
    private val restoreToCompletion: suspend (Context, String, String?) -> RestoreResult = { context, uri, pass ->
        runRestoreThroughWorker(context, uri, pass)
    },
    private val openUriInputStream: (Context, String) -> InputStream? = { context, uriString ->
        context.contentResolver.openInputStream(Uri.parse(uriString))
    }
) : AndroidViewModel(application) {

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RestoreViewModel::class.java)) {
                return RestoreViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private val _uiState = MutableStateFlow<RestoreUiState>(RestoreUiState.Idle)
    val uiState: StateFlow<RestoreUiState> = _uiState

    private data class PendingSelection(val workerUri: String, val passphrase: String?)
    private data class ResolvedSelection(val file: File, val workerUri: String, val isStaged: Boolean)

    @Volatile
    private var pending: PendingSelection? = null

    @Volatile
    private var stagedCacheFile: File? = null

    private fun clearStagedFile() {
        stagedCacheFile?.delete()
        stagedCacheFile = null
    }

    /** User picked a backup file; stage content URIs once and validate the manifest. */
    fun selectBackup(uri: String) {
        viewModelScope.launch(dispatcher) {
            clearStagedFile()
            pending = null
            _uiState.value = RestoreUiState.Validating
            try {
                val resolved = resolveInitialSelection(uri)
                confirmAfterValidation(resolved, passphrase = null, isEncrypted = false)
            } catch (e: RestoreError.WrongPassphrase) {
                val workerUri = stagedCacheFile?.let { "file://${it.absolutePath}" } ?: uri
                _uiState.value = RestoreUiState.PassphraseRequired(workerUri)
            } catch (e: RestoreError) {
                clearStagedFile()
                logRestoreError("selectBackup failed: ${e.message}", e)
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                clearStagedFile()
                logRestoreError("selectBackup unexpected failure", e)
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
                val resolved = resolveLocalFile(current.fileUri)
                confirmAfterValidation(resolved, passphrase, isEncrypted = true)
            } catch (e: RestoreError.WrongPassphrase) {
                // Keep stagedCacheFile alive and return to PassphraseRequired so the user
                // can retry with a fresh passphrase rather than re-opening content:// URIs.
                _uiState.value = RestoreUiState.PassphraseRequired(current.fileUri)
            } catch (e: RestoreError) {
                clearStagedFile()
                logRestoreError("submitPassphrase failed: ${e.message}", e)
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                clearStagedFile()
                logRestoreError("submitPassphrase unexpected failure", e)
                _uiState.value = RestoreUiState.Error(
                    RestoreError.CorruptedFile("Unable to open the selected backup.", e)
                )
            }
        }
    }

    /**
     * Reads + validates the manifest and records [PendingSelection] pointing either to the
     * user's original `file://` URI or to our retained staged cache copy for `content://`
     * selections, avoiding ephemeral SAF URI grant expiration.
     */
    private fun confirmAfterValidation(
        resolved: ResolvedSelection,
        passphrase: String?,
        isEncrypted: Boolean
    ) {
        val manifest = backupReader.readManifest(resolved.file, passphrase)
        pending = PendingSelection(resolved.workerUri, passphrase)
        _uiState.value = RestoreUiState.ConfirmationRequired(
            fileUri = resolved.workerUri,
            isEncrypted = isEncrypted,
            schemaVersion = manifest.schemaVersion,
            backupTimestamp = manifest.backupTimestamp,
            formatVersion = manifest.formatVersion
        )
    }

    /** User confirmed the restore; enqueue/run it and surface the outcome. */
    fun confirmRestore() {
        val selection = pending ?: return
        viewModelScope.launch(dispatcher) {
            _uiState.value = RestoreUiState.Processing
            try {
                val result = restoreToCompletion(getApplication(), selection.workerUri, selection.passphrase)
                clearStagedFile()
                pending = null
                _uiState.value = RestoreUiState.Success(result)
            } catch (e: RestoreError.WrongPassphrase) {
                // Keep staged file alive so the user can retry passphrase entry.
                _uiState.value = RestoreUiState.PassphraseRequired(selection.workerUri)
            } catch (e: RestoreError) {
                clearStagedFile()
                pending = null
                logRestoreError("confirmRestore failed: ${e.message}", e)
                _uiState.value = RestoreUiState.Error(e)
            } catch (e: Exception) {
                clearStagedFile()
                pending = null
                logRestoreError("confirmRestore unexpected failure", e)
                _uiState.value = RestoreUiState.Error(
                    RestoreError.IOFailure("Restore could not be completed.", e)
                )
            }
        }
    }

    fun reset() {
        clearStagedFile()
        pending = null
        _uiState.value = RestoreUiState.Idle
    }

    fun backFromError() {
        reset()
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value !is RestoreUiState.Processing) {
            clearStagedFile()
        }
    }

    private fun resolveInitialSelection(uriString: String): ResolvedSelection {
        if (uriString.startsWith("file://")) {
            return resolveLocalFile(uriString)
        }
        val app = getApplication<Application>()
        val stagingDir = File(resolveCacheDir(app), STAGED_DIR_NAME).apply { mkdirs() }
        val tmp = File.createTempFile("restore_staged_", ".zip", stagingDir)
        try {
            val stream = openUriInputStream(app, uriString)
                ?: throw RestoreError.IOFailure("Unable to open the selected backup file.")
            stream.use { input ->
                tmp.outputStream().use { out -> input.copyTo(out) }
            }
        } catch (e: RestoreError) {
            tmp.delete()
            throw e
        } catch (e: Exception) {
            tmp.delete()
            throw RestoreError.IOFailure("Unable to read the selected backup file.", e)
        }
        if (!tmp.isFile || tmp.length() == 0L) {
            tmp.delete()
            throw RestoreError.CorruptedFile("The selected backup file is empty or unreadable.")
        }
        stagedCacheFile = tmp
        return ResolvedSelection(
            file = tmp,
            workerUri = "file://${tmp.absolutePath}",
            isStaged = true
        )
    }

    private fun resolveLocalFile(uriString: String): ResolvedSelection {
        if (uriString.startsWith("file://")) {
            val stripped = File(uriString.removePrefix("file://"))
            val file = if (stripped.exists()) {
                stripped
            } else {
                runCatching { Uri.parse(uriString)?.path?.let(::File) }.getOrNull() ?: stripped
            }
            return ResolvedSelection(
                file = file,
                workerUri = uriString,
                isStaged = file.parentFile == File(resolveCacheDir(getApplication()), STAGED_DIR_NAME)
            )
        }
        return resolveInitialSelection(uriString)
    }

    private fun logRestoreError(message: String, error: Throwable) {
        runCatching { Log.e(TAG, message, error) }
    }

    companion object {
        internal const val STAGED_DIR_NAME = "restore_staged"
        private const val TAG = "RestoreViewModel"
    }
}

private fun resolveCacheDir(application: Application): File {
    val dir = runCatching { application.cacheDir }.getOrNull()
    return if (dir != null && dir.path.isNotBlank()) {
        dir
    } else {
        File(System.getProperty("java.io.tmpdir") ?: ".")
    }
}

private const val WORK_NAME = "restore_backup"

/**
 * Production restore path: enqueues [RestoreWorker] as a unique one-time work and
 * suspends until that exact request ID finishes, mapping success/failure/cancellation
 * back to a [RestoreResult] or a typed [RestoreError].
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

    val finished = checkNotNull(
        wm.getWorkInfoByIdFlow(request.id)
            .first { info -> info != null && info.state.isFinished }
    )

    if (finished.state == WorkInfo.State.SUCCEEDED) {
        val data = finished.outputData
        return RestoreResult(
            journalEntryCount = data.getInt(RestoreWorker.KEY_RESULT_JOURNAL, 0),
            bodyMeasurementCount = data.getInt(RestoreWorker.KEY_RESULT_BODY, 0),
            goalCount = data.getInt(RestoreWorker.KEY_RESULT_GOALS, 0),
            deletedEntryCount = data.getInt(RestoreWorker.KEY_RESULT_DELETED, 0),
            tagCount = data.getInt(RestoreWorker.KEY_RESULT_TAGS, 0),
            mediaFileCount = data.getInt(RestoreWorker.KEY_RESULT_MEDIA, 0),
            workoutCount = data.getInt(RestoreWorker.KEY_RESULT_WORKOUTS, 0),
            presetCount = data.getInt(RestoreWorker.KEY_RESULT_PRESETS, 0),
            profileCount = data.getInt(RestoreWorker.KEY_RESULT_PROFILES, 1)
        )
    }

    if (finished.state == WorkInfo.State.CANCELLED) {
        throw RestoreError.IOFailure("Restore was cancelled.")
    }

    val data = finished.outputData
    throw when (data.getString(RestoreWorker.KEY_ERROR_TYPE)) {
        RestoreWorker.ERROR_TYPE_WRONG_PASSPHRASE -> RestoreError.WrongPassphrase()
        RestoreWorker.ERROR_TYPE_VERSION_MISMATCH -> RestoreError.VersionMismatch(
            backupSchemaVersion = data.getInt(RestoreWorker.KEY_BACKUP_SCHEMA_VERSION, -1),
            currentSchemaVersion = data.getInt(RestoreWorker.KEY_CURRENT_SCHEMA_VERSION, -1)
        )
        RestoreWorker.ERROR_TYPE_UNSUPPORTED_FORMAT -> RestoreError.UnsupportedFormat(
            data.getInt(RestoreWorker.KEY_BACKUP_FORMAT_VERSION, -1)
        )
        RestoreWorker.ERROR_TYPE_INSUFFICIENT_STORAGE -> RestoreError.InsufficientStorage()
        RestoreWorker.ERROR_TYPE_IO_FAILURE -> RestoreError.IOFailure(
            data.getString(RestoreWorker.KEY_ERROR_MESSAGE) ?: "Restore failed."
        )
        else -> RestoreError.CorruptedFile(
            data.getString(RestoreWorker.KEY_ERROR_MESSAGE) ?: "Restore failed."
        )
    }
}
