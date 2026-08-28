package com.example.healthjournal.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestValidatorTest {

    private fun manifest(
        formatVersion: Int = BackupWriter.BACKUP_FORMAT_VERSION,
        schemaVersion: Int = 12,
        timestamp: Long = 1L
    ) = BackupManifest(
        formatVersion = formatVersion,
        schemaVersion = schemaVersion,
        backupTimestamp = timestamp,
        contents = listOf(BackupWriter.EntityFile.JOURNAL)
    )

    @Test
    fun validate_matchingFormatAndSchema_isValid() {
        val result = ManifestValidator(currentSchemaVersion = 12).validate(manifest())

        assertTrue(result is com.example.healthjournal.export.ManifestValidation.Valid)
    }

    @Test
    fun validate_futureFormatVersion_isUnsupportedFormat() {
        val result = ManifestValidator(
            currentSchemaVersion = 12,
            supportedFormatVersion = BackupWriter.BACKUP_FORMAT_VERSION
        ).validate(manifest(formatVersion = BackupWriter.BACKUP_FORMAT_VERSION + 1))

        val invalid = result as com.example.healthjournal.export.ManifestValidation.Invalid
        assertTrue(invalid.error is RestoreError.UnsupportedFormat)
        assertEquals(BackupWriter.BACKUP_FORMAT_VERSION + 1, (invalid.error as RestoreError.UnsupportedFormat).backupFormatVersion)
    }

    @Test
    fun validate_schemaMismatch_isVersionMismatch() {
        val result = ManifestValidator(currentSchemaVersion = 12).validate(manifest(schemaVersion = 11))

        val invalid = result as com.example.healthjournal.export.ManifestValidation.Invalid
        assertTrue(invalid.error is RestoreError.VersionMismatch)
        val vm = invalid.error as RestoreError.VersionMismatch
        assertEquals(11, vm.backupSchemaVersion)
        assertEquals(12, vm.currentSchemaVersion)
    }

    @Test
    fun validate_lowerThanSupportedFormat_butSchemaOk_isValid() {
        val result = ManifestValidator(currentSchemaVersion = 12).validate(manifest(formatVersion = 1))

        assertTrue(result is com.example.healthjournal.export.ManifestValidation.Valid)
    }
}
