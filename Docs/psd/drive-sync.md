# Drive Sync — Product Specification

> A WorkManager worker performs full-snapshot bidirectional sync over a set of
> Drive app-data JSON files; a pure, stateless merge decides conflicts by
> last-modified with cloud-winning ties, and a shared tombstone ledger carries
> cross-device deletions.

Last updated: 2026-09-02

## Overview

The sync engine lives in `sync/`. Each domain is a sibling JSON file in Drive's
private app-data folder, holding a full snapshot. On every run the worker
downloads the cloud snapshot(s), downloads missing media binaries, merges with
the local rows via a pure merge module, persists the result, then uploads the
entire merged set back. Scheduling uses WorkManager periodic (about 15 min,
unmetered) plus one-shot jobs after mutations and on manual demand.

## Architecture

- `SyncWorker` is a `CoroutineWorker` that: reads local rows, downloads/parses
  cloud JSON, applies the pure merge, persists via the repositories/DAOs, uploads
  the merged snapshot and any new media. It holds no merge logic itself.
- `SyncMerge` is pure and stateless (keyed by id, last-modified LWW, cloud wins
  ties), so it runs fully under JVM unit tests.
- Auth: `GoogleAuthManager` obtains an id token via Credential Manager and a
  Drive app-data access token; `SessionManager` persists only the account email.
- Scheduling lives in a `SyncManager` object with periodic and manual unique
  work names; the app enqueues periodic on startup and after mutations.

## Data flow (journal path shown; other domains parallel)

1. Worker starts with constraints met; tries a silent Drive access token.
2. Download `health_journal_data.json`, parse to journal entries.
3. Remove any cloud rows the local tombstone ledger says were deleted.
4. Download missing photo/attachment binaries; remap cloud URIs to local paths.
5. Read local entries (incl. archived); call `SyncMerge.merge` keyed by id.
6. Persist the merged set (and tags) via the repository.
7. Upload new local media, then upload the merged snapshot back.
8. Prune expired tombstones at the end of the run.

## Components

| Component | File | Responsibility |
|---|---|---|
| Sync worker | `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` | orchestration + scheduling; photo/media sync |
| Sync merge | `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` | pure LWW merge for entries/measurements/goals/card |
| Drive helper | `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` | Drive REST client; upload/download; file-name constants |
| Payloads | `app/src/main/java/com/example/healthjournal/sync/GoalSyncPayload.kt` etc. | Gson JSON serialization, null-defensive parse |
| Auth | `app/src/main/java/com/example/healthjournal/auth/GoogleAuthManager.kt` | sign-in, Drive scope, silent token |
| Session | `app/src/main/java/com/example/healthjournal/auth/SessionManager.kt` | persisted account email |
| Tombstones | `app/src/main/java/com/example/healthjournal/data/local/DeletedEntry.kt` | deletion ledger |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Silent token unavailable | `KEY_AUTH_REQUIRED` progress; transient retry so periodic sync survives |
| Missing/bad cloud file | parses to empty list; no crash |
| Both sides edited same id | newer `lastModified` wins; cloud wins ties |
| Item deleted on one device | tombstone removes cloud/stale copies; newest deletion wins |
| Cloud entry with null tags | local tags preserved rather than wiped |
| Upload/network failure | `Result.retry()` |
| Non-transient failure | `Result.failure` with an error message surfaced in UI |

## Dependencies

- WorkManager (periodic + one-shot); the Drive REST client (`google-api-services-drive`).
- Credential Manager + Identity (Play services) for authorization.
- Gson for payload serialization; Room for local reads/writes.

## Sources

- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — orchestration + scheduling.
- `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` — conflict resolution.
- `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` — Drive client + file names.
- `app/src/main/java/com/example/healthjournal/sync/GoalSyncPayload.kt` — goal payload.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementSyncPayload.kt` — measurement payload.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementTombstonePayload.kt` — tombstone payload.
- `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` — personal card payload.
- `app/src/main/java/com/example/healthjournal/auth/GoogleAuthManager.kt` — authorization.
- `app/src/main/java/com/example/healthjournal/auth/SessionManager.kt` — account persistence.
- `app/src/main/java/com/example/healthjournal/data/local/DeletedEntry.kt` — tombstone ledger.
- `Docs/prd/drive-sync.md` — requirements.
- `Docs/tests/drive-sync.md` — test cases.