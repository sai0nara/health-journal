# Restore from Backup — Product Requirements

> Restore a full app backup so the journal, measurements, cards, and media all
> come back - completely replacing local data - either from a plain archive or
> from an encrypted one unlocked by a user passphrase.

Last updated: 2026-09-02

## Overview

The restore feature is the closing half of the backup story: what the export
feature writes, restore reads back. A user enters it from the Export screen's
Restore tab, picks a backup `.zip` through the system file picker, confirms what
it contains, and the replace then runs to completion even if the app dies
mid-way (WorkManager). If the archive is encrypted with a passphrase, the user
supplies it here. Every destructive decision happens after validation, so a bad
file never destroys current data.

## Goals / Non-goals

**Goals**

- Recover a prior dataset after a device loss, data wipe, or app reinstall.
- Transfer the full local dataset to a different device.
- Open encrypted archives while keeping the passphrase out of persistent storage.
- Succeed only atomically: on failure, current data stays intact.

**Non-goals**

- Cloud restore (Drive sync covers transport of a synced dataset).
- Selective or merge import.
- Scheduling or automating local backups.
- Publishing photos to the cloud as part of restore.

## User stories

- As a user who replaced or lost a device, I want to restore my backup so my
  health data comes back.
- As a user with an encrypted backup, I want to enter my passphrase to open it,
  and never re-enter it for the same archive within a session.
- As a user who picks an unrelated or stale file, I want a clear error and my
  current data left untouched.

## Functional requirements

- FR-1: Restore is reached from the Export screen's Restore tab.
- FR-2: The system file picker accepts `application/zip` and falls back to a
  broad MIME filter on pickers that reject it.
- FR-3: The archive is inspected before anything destructive happens: its
  manifest (backup timestamp, schema version, encryption flag) is read and
  validated against the app's current schema.
- FR-4: A confirmation step shows what will be restored; encrypted archives
  additionally require a passphrase before confirmation is offered.
- FR-5: The replace runs through a WorkManager job so it survives process death
  and completes offline.
- FR-6: The swap is one Room transaction: all tables are wiped and reinserted as
  a unit (this is a full replace, not a merge).
- FR-7: Photos and attachments are re-imported from the archive into app storage
  and their URIs remapped after the transaction.
- FR-8: Defensive unzipping rejects path traversal (zip-slip) and over-limit
  archive expansion (zip-bomb).
- FR-9: A successful restore re-syncs to Drive so the cloud stays authoritative.
- FR-10: Failures surface as typed errors (corrupt file, wrong passphrase,
  unsupported version, insufficient storage, mixed archive) with guidance and a
  retry path.

## Non-functional requirements

- Atomicity: the destructive swap is the last step and all-or-nothing.
- Privacy: passphrases are held in memory per attempt and never persisted; the
  encrypted container uses AES-256.
- Offline: the whole restore runs without a network connection.
- Robustness: unzipping is bounded (per-entry and cumulative expansion caps).
- Versioning: a backup from a newer schema version is refused, not guessed at.

## Acceptance criteria

- AC-1: A valid plain archive replaces all local journal data, measurements,
  personal cards, tombstones, and media.
- AC-2: A valid encrypted archive is restored after the correct passphrase is
  entered.
- AC-3: An encrypted archive with an incorrect passphrase fails with a
  "wrong passphrase" error and current data is unchanged.
- AC-4: An archive from a newer or unknown schema version fails with a clear
  version error and current data is unchanged.
- AC-5: A corrupt, misleading-MIME, or unsupported archive fails with a clear
  error and current data is unchanged.
- AC-6: Archive entries attempting path traversal, or absurd expansion, are
  rejected without partial writes.
- AC-7: A successful restore triggers a Drive re-sync of the restored dataset.
- AC-8: After restore, media files are accessible through the app's remapped
  URIs (not the archive's paths).

## Out of scope

- Restore from a cloud backup (Drive sync is the supported path).
- Merging a backup into the existing dataset.
- Restoring while the sync worker is mid-flight (restore wins and re-syncs).

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/RestoreScreen.kt` — the Restore tab UI (FR-1, FR-4, FR-10).
- `app/src/main/java/com/example/healthjournal/export/RestoreViewModel.kt` — the state machine driving the flow (FR-2..FR-4, FR-10).
- `app/src/main/java/com/example/healthjournal/export/RestoreCoordinator.kt` — the restore pipeline (FR-3..FR-8).
- `app/src/main/java/com/example/healthjournal/sync/RestoreWorker.kt` — WorkManager execution and post-restore sync (FR-5, FR-9).
- `app/src/main/java/com/example/healthjournal/export/RestoreError.kt` — the typed failure model (FR-10).
- `Docs/psd/restore-from-backup.md` — product specification for this feature.
- `Docs/tests/restore-from-backup.md` — test cases for this feature.