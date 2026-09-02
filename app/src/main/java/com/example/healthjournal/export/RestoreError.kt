package com.example.healthjournal.export

/**
 * Sealed hierarchy describing every typed failure a restore can encounter.
 * Mapped directly to the MVI [RestoreUiState.Error] causes in the UI layer.
 */
sealed class RestoreError(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The archive is not a valid/complete backup ZIP (missing manifest, malformed JSON, etc.). */
    class CorruptedFile(val detail: String = "The backup file is corrupted or incomplete.", cause: Throwable? = null) :
        RestoreError(detail, cause)

    /** The backup was produced by an incompatible backup format or database schema version. */
    class VersionMismatch(val backupSchemaVersion: Int, val currentSchemaVersion: Int) :
        RestoreError(
            "The backup was made with database schema v$backupSchemaVersion, but this app uses schema " +
                "v$currentSchemaVersion. Please open the backup with a compatible app version."
        )

    /** The passphrase is wrong or the archive is not the expected encrypted format. */
    class WrongPassphrase(cause: Throwable? = null) :
        RestoreError("The passphrase is incorrect or the file is not an encrypted backup.", cause)

    /** Not enough free storage to extract/restore the backup. */
    class InsufficientStorage(cause: Throwable? = null) :
        RestoreError("Not enough storage available to restore this backup.", cause)

    /** Unsupported backup format version. */
    class UnsupportedFormat(val backupFormatVersion: Int) :
        RestoreError("Backup format v$backupFormatVersion is not supported by this app version.")

    /** Any other I/O or unexpected failure. */
    class IOFailure(val detail: String, cause: Throwable? = null) :
        RestoreError(detail, cause)
}
