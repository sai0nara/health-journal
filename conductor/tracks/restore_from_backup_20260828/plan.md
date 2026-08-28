# Restore from Backup - Implementation Plan

## Phase 1: Backup Format Extension (Export) [checkpoint: e4358d9]
- [x] Task: Write failing unit tests for full-backup ZIP writer (manifest, schema version, all entities, media)
- [x] Task: Extend ZipExportUseCase to serialize all Room entities + schema version + backup.json manifest
- [x] Task: Add optional AES-256 passphrase encryption (Zip4j) to backup writer
- [x] Task: Write tests for encrypted & unencrypted backup creation
- [x] Task: Verify Export still produces valid shareable archives
- [x] Task: Conductor - User Manual Verification 'Backup Format' (Protocol in workflow.md)

## Phase 2: Restore Domain & Data Layer [checkpoint: b03a725]
- [x] Task: Write failing tests for backup manifest reader/parser
- [x] Task: Create backup reader + manifest validation (schema version, checksum)
- [x] Task: Write failing tests for validation use cases (version mismatch, corrupt, checksum)
- [x] Task: Implement validation use cases (VersionMismatch, CorruptedFile, WrongPassphrase)
- [x] Task: Write failing tests for Zip-Slip / path-traversal prevention
- [x] Task: Implement safe-extraction with canonical path guard
- [x] Task: Write failing tests for ZIP-bomb / expansion-ratio guard
- [x] Task: Implement storage/expansion-limit guard
- [x] Task: Create RestoreRepository (staging, atomic swap, rollback, media re-import)
- [x] Task: Write unit tests for RestoreRepository incl. rollback path
- [x] Task: Conductor - User Manual Verification 'Restore Domain' (Protocol in workflow.md)

## Phase 3: Worker & Atomic Database Swap
- [ ] Task: Write failing tests for database swap / connection-close-reopen logic
- [ ] Task: Implement safe Room DB replacement (close, swap .db/-shm/-wal, reopen, version-aware)
- [ ] Task: Write failing tests for RestoreWorker
- [ ] Task: Implement RestoreWorker (CoroutineWorker) orchestrating extract -> swap -> media import -> success/failure
- [ ] Task: Implement failure rollback + cleanup of staging directory
- [ ] Task: Implement post-restore WorkManager sync enqueue
- [ ] Task: Write unit tests covering worker success, corruption, and rollback
- [ ] Task: Conductor - User Manual Verification 'Restore Worker' (Protocol in workflow.md)

## Phase 4: MVI UI (ExportScreen Restore tab)
- [ ] Task: Write failing UI/viewmodel tests for RestoreUiState transitions
- [ ] Task: Create RestoreViewModel with sealed RestoreUiState (Idle, Validating, ConfirmationRequired, Processing, Success, Error)
- [ ] Task: Add file selection (OpenDocument) + validation flow
- [ ] Task: Add confirmation dialog showing backup metadata
- [ ] Task: Add passphrase entry dialog (encrypted backups)
- [ ] Task: Add progress indicator + success snackbar + error states with recovery steps
- [ ] Task: Integrate Restore tab into ExportScreen
- [ ] Task: Write Compose UI tests for restore flow
- [ ] Task: Conductor - User Manual Verification 'Restore UI' (Protocol in workflow.md)

## Phase 5: Integration & Polish
- [ ] Task: Wire/verify navigation & DI wiring in MainActivity/factories
- [ ] Task: End-to-end test: export -> encrypt -> full wipe app -> restore -> verify
- [ ] Task: Write integration tests (export/restore round-trip, sync enqueue)
- [ ] Task: Test light/dark theme rendering of restore UI
- [ ] Task: Manual risk pass: ZIP-bomb, Zip-Slip, wrong passphrase, version mismatch, low disk
- [ ] Task: Conductor - User Manual Verification 'Integration' (Protocol in workflow.md)
