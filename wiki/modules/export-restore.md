# Export & Restore

> The backup pipeline: exporting and restoring journal data (encrypted ZIP archives and PDF), plus the restore UI.

Last updated: 2026-11-06

## What lives here

The `export` package holds the data-export and restore pipeline, and the export and
restore screens in [[ui-layer]] expose it to the user. Backups are written as
encrypted ZIP archives (AES-256 via the zip4j dependency) that bundle the exported
journal data; a PDF export path uses the PDF toolkit dependency.

## Key components

- **Export.** The export use cases produce either a full ZIP backup or a PDF. The ZIP
  path encrypts and archives the exported manifest, and separate reader/writer types
  handle both ZIP and non-ZIP forms. A full backup (`FullBackupUseCase`) writes
  `profiles.json` alongside every restorable entity across all active and archived
  profiles, including workout presets (`presets.json`) and workout sessions
  (`workouts.json`), and scopes sibling media entries under `media/<profileId>/`.
  Date-range PDF export (`PdfExportUseCase`) defaults to the active profile and lets
  the user select one or more active profiles, rendering a labeled section per
  selected profile while excluding archived profiles.
- **Backup encryption & integrity.** A backup encryptor protects archives, and a
  manifest validator checks archive structure on restore.
- **Safe extraction.** The extractor guards against zip-slip path traversal and
  zip-bomb expansion limits before files are written.
- **Restore.** The restore repository applies a chosen backup transactionally:
  unknown/untrusted archives and media are rejected or cleaned up, and a restore
  coordinator drives the flow. For multi-profile v2 backups, restore replaces all
  profiles and rows atomically and re-imports media into profile-scoped directories;
  for legacy v1 backups without profile metadata, `BackupDataReader` defaults missing
  `profileId` fields to `Self` and `RestoreRepository` wipes and restores only `Self`
  while preserving sibling profiles. See the restore worker in [[sync-engine]] for
  the background side.
- **Screens.** `ExportScreen` (format, active-profile chips, and date-range selection,
  where profile and date-range selection apply only to PDF) and `RestoreScreen`
  (archive pick/verify/restore) live in `ui/screens`.

## Caveats

The backup strategy is transactional replacement of the Room database at restore
time, not an in-place file swap of the database file — a plain "swap the database
file" approach is intentionally not used. See `app/build.gradle.kts` for the
encryption and PDF dependencies.

## Cross-references

- [[data-layer]] — the Room database that a restore replaces.
- [[sync-engine]] — the restore worker that connects this feature to background sync.
- [[ui-layer]] — the export and restore screens.
- [[instrumented]] — covered by Compose UI tests and the restore integration test.

## Sources

- `app/src/main/java/com/example/healthjournal/export/SafeBackupExtractor.kt` — zip-slip and zip-bomb guards.
- `app/src/main/java/com/example/healthjournal/export/BackupDataManifest.kt` — the full-backup snapshot shape.
- `app/src/main/java/com/example/healthjournal/export/BackupWriter.kt` — entity/manifest ZIP writer.
- `app/src/main/java/com/example/healthjournal/export/BackupDataReader.kt` — ZIP archive reader and legacy v1 `Self` normalization.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — all-profile v2 backup orchestrator.
- `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` — the transactional v2/v1 restore logic.
- `app/src/main/java/com/example/healthjournal/export/RestoreCoordinator.kt` — backup validation and restore orchestration.
- `app/src/main/java/com/example/healthjournal/export/RestoreViewModel.kt` — restore UI state and profile-count summary.
- `app/src/main/java/com/example/healthjournal/export/RestoreUiState.kt` — restore screen state models.
- `app/src/main/java/com/example/healthjournal/export/ExportViewModel.kt` — PDF active-profile selection and export orchestration.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — multi-profile PDF report generation.
- `app/src/main/java/com/example/healthjournal/export/BackupEncryptor.kt` — encryption for archives.

Back to [[overview]]
