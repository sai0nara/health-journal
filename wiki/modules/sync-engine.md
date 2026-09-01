# Sync Engine

> The Google Drive synchronization engine: a WorkManager worker plus the Drive REST client wrapper, built on the same Room database.

Last updated: 2026-09-01

## What lives here

The `sync` package is the production Google Drive sync engine. Despite the roadmap
heading in `AGENTS.md` listing Drive integration as open, **this code is implemented
and wired into production** — the classes below hold real Drive REST clients and a
scheduled worker, and they are covered by the JVM unit suite in `app/src/test`.

This is the single most common point of confusion about the repo: the roadmap line is
stale relative to the code.

## Key components

- **Worker.** `SyncWorker` is the WorkManager entry point that runs the sync. It
  delegates merging to `SyncMerge`.
- **Drive client.** `DriveServiceHelper` wraps the Google Drive REST service.
- **Payloads.** Sync payload models (`GoalSyncPayload`, `MeasurementSyncPayload`,
  `MeasurementTombstonePayload`, `PersonalCardSyncPayload`) shape what each domain
  pushes and pulls.
- **Restore.** `RestoreWorker` is the worker behind the restore side of
  [[export-restore]].

## Direction of the dependency

The app holds an outbound client to Google Drive: it calls the Drive API (see the
auth and API client dependencies and the [[google-drive]] integration page) and
schedules sync with WorkManager. Google Drive itself is a third-party service; this
repo does not hold or claim to own any of Drive's internals.

## Cross-references

- [[google-drive]] — the neighbour system this engine talks to, and its auth flow.
- [[auth]] — where the token used by the Drive client is obtained.
- [[data-layer]] — the Room database the engine reads and writes.
- [[unit-tests]] — sync merge and payload behavior is covered by JVM tests.

## Sources

- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — the sync worker.
- `app/src/main/java/com/example/healthjournal/sync/DriveServiceHelper.kt` — the Drive REST client wrapper.
- `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt` — merge and conflict rules.

Back to [[overview]]
