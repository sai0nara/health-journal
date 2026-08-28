# Specification: Restore from Backup

## Overview
Implement a robust **Restore from Backup** feature enabling users to recover a previous application state by selecting a locally stored `.zip` backup (via Android Storage Access Framework) and restoring all journal/health data with an atomic, fully-reversible process. This complements the existing **Export** capability (ZIP archive with `data.json` + `media/`), which is extended to produce a complete, round-trippable backup that includes all Room entities, the database schema version, and optional passphrase encryption.

## User Value Proposition
Users can recover previous application states, restore backups transferred across devices, and import offline datasets — providing full control over their personal data without relying on continuous cloud connectivity.

## User Flows
1. **Entry Point:** Inside the existing `ExportScreen` (reached from the HistoryScreen top-bar Export icon), a companion **Restore** action/tab prompts the user to select a `.zip` file.
2. **File Selection:** `ActivityResultContracts.OpenDocument()` launches `ACTION_OPEN_DOCUMENT` (MIME `application/zip`, `application/x-zip-compressed`); the user selects a local `.zip`.
3. **Validation & Confirmation:** The app validates the archive header in memory (verifies the backup manifest, schema version compatibility, passphrase/checksum if encrypted, and non-destructively inspects contents). A confirmation dialog shows backup metadata (e.g., backup timestamp, contents).
4. **Execution:** On confirmation, an indeterminate/determinate progress indicator is shown; extraction and database replacement run via a `CoroutineWorker`.
5. **Success/Error State:** On success, the app restarts softly / invalidates global state and shows a snackbar. On failure (corrupt ZIP, version mismatch, insufficient storage, wrong passphrase, low disk), it shows a clear, actionable error and **rolls back** to pre-restore state (atomic swap + cleanup of the staging area).

## Functional Requirements
- **Backup Format (extended Export):** Enhance `ZipExportUseCase` so its output ZIP contains all Room entities (journal entries, body measurements, goals, personal card, deleted-entry tombstones) serialized as JSON, the Room **database schema version**, a **backup manifest** (`backup.json`) with timestamp and contents, plus the attachment `media/` folder. This makes the backup fully round-trippable.
- **Restore:**
  - Read the backup manifest to validate version compatibility before extraction.
  - **Replace all local data** (journals, measurements, goals, personal card, tombstones) via an atomic swap: unzip into a staging folder under `context.cacheDir`, verify, then replace live data; on any failure delete staging and roll back.
  - Re-import attachment files into the app's media storage and remap references.
  - Safely close active Room connections before replacing `*.db`, `-shm`, `-wal`, then reopen (version-aware).
- **Encryption (AES-256):** Backup ZIP entries may be passphrase-protected via **Zip4j** (AES-256). Restore prompts for the passphrase; a wrong passphrase yields an actionable error and roll back. Export exposes an optional passphrase prompt.
- **Integrity & Versioning:** Validate a checksum/manifest and database schema version; reject mismatched or unsupported versions with a clear error before any destructive step.
- **Post-Restore Sync:** On successful restore, enqueue a WorkManager sync so the restored data is treated as the source of truth and re-uploaded to Google Drive.
- **Background Processing:** Long-running restoration executes in a `WorkManager CoroutineWorker` (survives process death); falls back to foreground-style progress reporting via the ViewModel `StateFlow`.
- **Storage Guard (ZIP bomb):** Before writing, verify available device storage and enforce a maximum expansion ratio / total uncompressed size budget.

## MVI State Contract (`RestoreUiState`, sealed interface via StateFlow)
- `Idle` — ready for file selection.
- `Validating` — inspecting structural integrity and version.
- `ConfirmationRequired(metadata)` — valid backup found; awaiting explicit confirmation.
- `Processing(progressPercentage)` — extracting and replacing database entities.
- `Success` — restoration finalized, caches re-initialized, sync enqueued.
- `Error(cause)` — categorized failures: `CorruptedFile`, `VersionMismatch`, `WrongPassphrase`, `InsufficientStorage`, `IOFailure`.

## Non-Functional Requirements
- **Offline-first:** Core restore works fully offline; no cloud dependency for the restore itself.
- **OOM prevention:** Buffered streaming (8KB–16KB buffers) throughout extraction to avoid `OutOfMemoryError` on large archives.
- **Atomicity & safety:** Zero data corruption via staged extraction + atomic swap; every failure path fully rolls back.
- **Security:** Strict Zip-Slip path traversal prevention (`canonicalPath.startsWith(targetDirCanonicalPath)`); scoped-storage compliance via `ContentResolver.openInputStream` only; no `READ_EXTERNAL_STORAGE`.
- **Design system:** All UI uses Material 3 via `MaterialTheme.colorScheme` semantic tokens (no absolute colors).

## Acceptance Criteria
- Selecting a valid, correct-version backup and confirming restores all data types (journals, measurements, goals, personal card, attachments) with progress UI.
- Restore is atomic: a deliberately corrupted/failed restore leaves the previous data fully intact.
- Wrong passphrase and version-mismatch fail gracefully with actionable, user-friendly errors and no data loss.
- A ZIP-bomb / excessive-expansion archive is rejected before disk writes.
- Zip-Slip / path-traversal entries cannot write outside the intended base directory.
- Successful restore triggers a re-sync (restored data uploaded to Drive) and a clear success snackbar; failure shows an error with recovery steps.
- Covered by unit tests (use cases, worker, repository, validation), >80% coverage on new code, and emulator UI tests.

## Out of Scope
- Direct restore from cloud (existing Google Drive sync covers cloud multi-device restore).
- Merge/selective import of individual records (restore is full replace).
- Backup directly to external cloud backup services other than the existing Drive sync.
- Auto-scheduled/periodic local backup generation.
