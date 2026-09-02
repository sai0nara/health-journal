# Restore from Backup — Product Specification

> A user drives restore through a single-purpose screen + ViewModel state
> machine; validation and the destructive swap run in a pure-JVM coordinator
> behind a WorkManager job so every step (passphrase, swap, media re-import) is
> individually testable and the replace survives process death.

Last updated: 2026-09-02

## Overview

Restore reuses the export/backup contract: the archive is a `.zip` the export
feature wrote, optionally wrapped in an outer AES-256 container. The pipeline is
"read first, swap last":

1. Read and validate the manifest before touching anything.
2. Confirm (with passphrase if encrypted).
3. Run the WorkManager job: decrypt, extract into a staging dir, re-import,
   swap the whole database in one transaction.
4. Surface a typed success/error and, on success, re-sync to Drive.

## Architecture

- MVVM with a single `RestoreViewModel` exposing `RestoreUiState` as `StateFlow`;
  the screen is a pure function of that state, which keeps the UI trivial to
  snapshot-test.
- The heavy lifting is a **pure JVM coordinator** (`RestoreCoordinator`) with
  injected seams (reader, extractor, repository, `onRestoreFinished`): no
  Android classes on the hot path, so the whole restore logic runs in local
  unit tests at JVM speed.
- Execution is delegated to a WorkManager `CoroutineWorker` so a killed process
  does not abort a partially applied restore mid-write.
- The database swap uses Room's `@Transaction`: wipe + reinsert in one atomic
  unit in the repository.

## Data flow

1. User taps Restore in the Export screen; `RestoreScreen` opens the system file
   picker (`application/zip`, broad fallback).
2. `selectBackup(uri)` reads the picker result; the ViewModel moves to
   `Validating` and asks the reader for the manifest.
3. A plain archive goes straight to `ConfirmationRequired`; an encrypted one to
   `PassphraseRequired` first. The confirmation card shows backup timestamp,
   schema version, and the encryption flag.
4. `confirmRestore` (after an optional passphrase via token) moves to
   `Processing` and hands `(uri, passphrase)` to the worker.
5. The worker calls the coordinator. The coordinator, in order:
   a. decrypts the outer AES-256 container if a passphrase was given, else
      rejects it as encrypted-without-passphrase;
   b. validates the inner manifest against the current schema version;
   c. extracts entries through `SafeBackupExtractor` into a staging directory
      (zip-slip + expansion-cap guards here);
   d. maps the staged backup data to local DTOs via `BackupDataReader`;
   e. calls `RestoreRepository.restore(...)`: one transaction wipes all tables
      and reinserts the imported data, then media is copied into app storage and
      every media URI is remapped from archive path to local URI;
   f. calls `onRestoreFinished`, then deletes the staging directory (finally).
6. On success the worker returns entity/reference counts and enqueues a manual
   Drive sync so the cloud reflects the restored state. On failure it returns
   the typed error for the screen to show with a retry path.

## Components

| Component | File | Responsibility |
|---|---|---|
| Restore screen | `app/src/main/java/com/example/healthjournal/ui/screens/RestoreScreen.kt` | render state, dispatch picker/passphrase/confirm, show errors |
| Restore ViewModel | `app/src/main/java/com/example/healthjournal/export/RestoreViewModel.kt` | MVI state machine; injectable reader/runner seams for tests |
| Restore UI state | `app/src/main/java/com/example/healthjournal/export/RestoreUiState.kt` | sealed state: idle/validating/confirm/passphrase/processing/success/error |
| Restore worker | `app/src/main/java/com/example/healthjournal/sync/RestoreWorker.kt` | WorkManager job; feeds uri+passphrase to the coordinator; post-success sync |
| Restore coordinator | `app/src/main/java/com/example/healthjournal/export/RestoreCoordinator.kt` | the end-to-end pipeline (decrypt→validate→extract→import→swap→cleanup), pure JVM |
| Backup reader | `app/src/main/java/com/example/healthjournal/export/BackupReader.kt` | detect plain vs encrypted outer container; read the manifest |
| Backup data reader | `app/src/main/java/com/example/healthjournal/export/BackupDataReader.kt` | map staged archive data to local DTOs |
| Manifest validator | `app/src/main/java/com/example/healthjournal/export/ManifestValidator.kt` | schema-version and manifest-shape checks |
| Safe extractor | `app/src/main/java/com/example/healthjournal/export/SafeBackupExtractor.kt` | canonical-path zip-slip guard; per-entry + cumulative expansion caps |
| Encryptor/decryptor | `app/src/main/java/com/example/healthjournal/export/BackupEncryptor.kt` | outer AES-256 container (zip4j) with inner `backup.zip` entry |
| Restore repository | `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` | atomic wipe+reinsert transaction; media re-import + URI remap |
| Error model | `app/src/main/java/com/example/healthjournal/export/RestoreError.kt` | typed failures mapped to screen guidance + retry |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Archive is encrypted but no passphrase | treated as needing a passphrase; never read past the container |
| Wrong passphrase | `WrongPassphrase`; data intact; screen offers retry with the passphrase field |
| Schema version newer than app or unknown | `VersionMismatch`; data intact |
| Corrupt container / unreadable manifest | `CorruptedFile`; data intact |
| Archive is not a backup at all | `UnsupportedFormat`; data intact |
| Storage insufficient for staging or re-import | `InsufficientStorage`; staging cleaned up, data intact |
| Entry path escapes staging dir or expands beyond caps | extraction aborts; `IOFailure`/`CorruptedFile`; data intact |
| Swap transaction fails mid-way | Room rolls the whole swap back; data intact |
| Process dies during the worker | WorkManager resumes; replace stays atomic |
| Restore succeeds | manual Drive sync enqueued so the cloud is re-authoritative |

## Dependencies

- Room (`@Transaction`) for the atomic swap; `val` MediaStore scope for photos/.
- WorkManager (unique work key identifying a single active restore).
- zip4j for the encrypted outer container (AES-256, inner `backup.zip` entry).
- STL-free pure-JVM coordinator kept off the Android classpath for JVM tests.

## Cross-references

- `Docs/prd/restore-from-backup.md` — the requirements this specification implements.
- `Docs/tests/restore-from-backup.md` — the test cases that verify this design.
- [[export-restore]] — the export/restore pipeline page explaining the cited code.
- [[sync-engine]] — the restore worker + re-sync seam.
- [[data-layer]] — the Room database the atomic swap targets.
- [[ui-layer]] — the RestoreScreen rendering these states.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/RestoreScreen.kt` — screen/state rendering.
- `app/src/main/java/com/example/healthjournal/export/RestoreViewModel.kt` — state machine.
- `app/src/main/java/com/example/healthjournal/export/RestoreUiState.kt` — state contract.
- `app/src/main/java/com/example/healthjournal/export/RestoreCoordinator.kt` — pipeline.
- `app/src/main/java/com/example/healthjournal/export/SafeBackupExtractor.kt` — unzip guards.
- `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` — transaction + media re-import.
- `app/src/main/java/com/example/healthjournal/export/RestoreError.kt` — failure model.
- `app/src/main/java/com/example/healthjournal/export/BackupReader.kt` — container/manifest detection.
- `app/src/main/java/com/example/healthjournal/sync/RestoreWorker.kt` — WorkManager execution.
- `Docs/prd/restore-from-backup.md` — requirements this implements.
- `Docs/tests/restore-from-backup.md` — test cases.