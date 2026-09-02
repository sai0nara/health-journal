package com.example.healthjournal.export

/**
 * MVI UI state for the Restore-backup flow. Each state maps to a distinct
 * surface in the Restore UI: selection, validation, confirmation (with backup
 * metadata), passphrase entry (encrypted backups), progress, success, or error.
 */
sealed class RestoreUiState {
    /** No selection yet; user has not chosen a backup. */
    object Idle : RestoreUiState()

    /** A file was selected and its manifest is being read/validated. */
    object Validating : RestoreUiState()

    /** An unencrypted backup is validated; awaiting user confirmation to apply it. */
    data class ConfirmationRequired(
        val fileUri: String,
        val isEncrypted: Boolean,
        val schemaVersion: Int,
        val backupTimestamp: Long
    ) : RestoreUiState()

    /** The selected backup is AES-256 encrypted; a passphrase is required. */
    data class PassphraseRequired(
        val fileUri: String
    ) : RestoreUiState()

    /** Backup confirmed; a restore is in progress (worker enqueued/running). */
    object Processing : RestoreUiState()

    /** Restore completed successfully with per-entity counts. */
    data class Success(val result: RestoreResult) : RestoreUiState()

    /**
     * A typed failure occurred. [requestPassphrase] signals the UI to re-prompt for a
     * passphrase (wrong passphrase), so the user can retry without reselecting.
     */
    data class Error(
        val error: RestoreError,
        val requestPassphrase: Boolean = false
    ) : RestoreUiState()
}
