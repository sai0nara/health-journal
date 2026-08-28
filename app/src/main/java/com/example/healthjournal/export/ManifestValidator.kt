package com.example.healthjournal.export

/**
 * Outcome of [ManifestValidator.validate].
 */
sealed class ManifestValidation {
    object Valid : ManifestValidation()
    data class Invalid(val error: RestoreError) : ManifestValidation()
}

/**
 * Validates a parsed [BackupManifest] against the currently running app before
 * any destructive restore step.
 *
 * Rules:
 *  - Backup format version newer than [supportedFormatVersion] -> [RestoreError.UnsupportedFormat].
 *  - Schema version must equal [currentSchemaVersion] -> otherwise [RestoreError.VersionMismatch].
 */
class ManifestValidator(
    private val currentSchemaVersion: Int,
    private val supportedFormatVersion: Int = BackupWriter.BACKUP_FORMAT_VERSION
) {

    fun validate(manifest: BackupManifest): ManifestValidation {
        if (manifest.formatVersion > supportedFormatVersion) {
            return ManifestValidation.Invalid(
                RestoreError.UnsupportedFormat(manifest.formatVersion)
            )
        }
        if (manifest.schemaVersion != currentSchemaVersion) {
            return ManifestValidation.Invalid(
                RestoreError.VersionMismatch(
                    backupSchemaVersion = manifest.schemaVersion,
                    currentSchemaVersion = currentSchemaVersion
                )
            )
        }
        return ManifestValidation.Valid
    }
}
