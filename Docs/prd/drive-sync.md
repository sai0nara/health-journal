# Drive Sync — Product Requirements

> Synchronize the journal, body measurements, goals, personal card, and deleted-
> entry tombstones to a private Google Drive app-data folder, keeping multiple
> devices consistent in the background.

Last updated: 2026-09-02

## Overview

The app is the client that talks to Google Drive, both writing and reading. A
set of JSON files in Drive's private per-app folder hold full snapshots of the
journal, body measurements, measurements goals, the personal card, and a shared
deletion tombstone ledger. A WorkManager job merges local and remote each run
(last-write-wins), persists the merged result, and uploads the whole snapshot
back — full-snapshot bidirectional sync, not delta. Drive authorization uses
only the app-data scope; a background worker refreshes an access token silently.

## Goals / Non-goals

**Goals**

- Keep a user's data consistent across devices and re-installs via Drive.
- Sync journal entries (incl. archived), body measurements, goals, personal
  card, and cross-device deletions.
- Resolve conflicts deterministically by last-modified, cloud wins ties.
- Run in the background (periodic + after mutations) and on manual demand.
- Keep credentials out of persistent storage: only the account email is saved.

**Non-goals**

- Delta/incremental protocol (full snapshots are uploaded each run).
- A user-facing cloud file browser; app-data files are hidden from the user.
- Offline-first conflict UI (resolution is automatic).

## User stories

- As a user, I want my journal and health data on the cloud so a second device
  shows the same history.
- As a user, I want sync to happen in the background so I do not manage files.
- As a user, I want a deleted entry to stay deleted even if another device had a
  stale copy.

## Functional requirements

- FR-1: Journal entries (including archived) sync as a full snapshot.
- FR-2: Body measurements, measurement goals, and the personal card each sync
  as their own snapshot file.
- FR-3: Deletions are tracked in a shared tombstone ledger; the newest deletion
  wins and a cloud copy cannot resurrect a deleted item.
- FR-4: Conflicts resolve by latest `lastModified`, cloud wins ties.
- FR-5: Photos and attachments sync as separate binary files and URIs remap to
  local app storage after download.
- FR-6: Authorization requests only the Drive app-data scope.
- FR-7: Sync runs periodically (about every 15 minutes, unmetered network),
  after data mutations, and on an explicit Sync Now / pull-to-refresh.
- FR-8: A transient failure (auth or network) retries rather than failing
  permanently.

## Non-functional requirements

- Privacy: only the app-data folder is touched; the access token is refreshed
  per run and not stored.
- Robustness: missing/payload files degrade to empty lists so a fresh device
  never crashes.
- Freshness: `lastModified` gates all conflict decisions.

## Acceptance criteria

- AC-1: A mutation is uploaded and another device's sync converges to the same
  data.
- AC-2: A newer local edit wins over a stale cloud copy; a tie goes to cloud.
- AC-3: Deleting on one device removes the item from another (no resurrection).
- AC-4: Signed-in users get periodic and manual sync; the UI shows a status.
- AC-5: Re-authorization is surfaced when the silent token cannot be obtained.

## Out of scope

- Merge conflict UI; merge is automatic and last-write-wins.
- Offline unsynced queueing beyond the pending-sync status flag.

## Cross-references

- `Docs/prd/restore-from-backup.md` — restore triggers a post-restore re-sync.
- `Docs/prd/history-feed.md` — the History screen exposes sign-in/sync.
- [[sync-engine]] — the worker, merge, and scheduling.
- [[google-drive]] — authentication and the Drive service.

## Sources

- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — the sync job, scheduling, photo sync.
- `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` — pure conflict resolution.
- `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` — Drive REST client + file names.
- `app/src/main/java/com/example/healthjournal/sync/GoalSyncPayload.kt` — goal payload.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementSyncPayload.kt` — measurement payload.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementTombstonePayload.kt` — tombstone payload.
- `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` — personal card payload.
- `app/src/main/java/com/example/healthjournal/auth/GoogleAuthManager.kt` — sign-in and Drive authorization.
- `app/src/main/java/com/example/healthjournal/auth/SessionManager.kt` — persisted account identity.
- `app/src/main/java/com/example/healthjournal/data/local/DeletedEntry.kt` — the tombstone entity.
- `Docs/psd/drive-sync.md` — specification.
- `Docs/tests/drive-sync.md` — test cases.