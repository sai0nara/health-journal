# Sync Engine

> The Google Drive synchronization engine: a WorkManager worker plus the Drive REST client wrapper, built on the same Room database.

Last updated: 2026-11-06

## What lives here

The `sync` package is the production Google Drive sync engine. Despite the roadmap
heading in `AGENTS.md` listing Drive integration as open, **this code is implemented
and wired into production** — the classes below hold real Drive REST clients and a
scheduled worker, and they are covered by the JVM unit suite in `app/src/test`.

This is the single most common point of confusion about the repo: the roadmap line is
stale relative to the code.

## Key components

- **Worker.** `SyncWorker` is the WorkManager entry point that runs the sync. It
  first syncs `profiles.json` metadata and deletion tombstones across devices,
  applies remote deletions via `ProfileDataEraser` (switching the active profile first
  if needed), deletes per-profile cloud files for deleted profiles, and then iterates
  every local profile to sync journal entries, body measurements, goals, and personal
  cards. It delegates merging to `SyncMerge`.
- **Drive client.** `DriveServiceHelper` wraps the Google Drive REST service, keeping
  the `Self` profile on legacy filenames while partitioning sibling profiles into
  UUID-suffixed files and providing `deleteDataFile` for profile cleanup.
- **Payloads.** Sync payload models (`ProfileSyncPayload`, `JournalSyncPayload`,
  `GoalSyncPayload`, `MeasurementSyncPayload`, `MeasurementTombstonePayload`,
  `PersonalCardSyncPayload`) shape what each domain pushes and pulls, normalizing
  missing legacy `profileId` fields to `Self`.
- **Restore.** `RestoreWorker` is the worker behind the restore side of
  [[export-restore]]; after restore completes it ensures the `Self` profile is
  initialized before triggering a manual sync.

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
- `app/src/main/java/com/example/healthjournal/sync/ProfileSyncPayload.kt` — profile metadata and deletion tombstone codec.
- `app/src/main/java/com/example/healthjournal/sync/JournalSyncPayload.kt` — journal entry and tombstone codec with legacy `Self` normalization.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementSyncPayload.kt` — body measurement sync codec.
- `app/src/main/java/com/example/healthjournal/sync/MeasurementTombstonePayload.kt` — measurement tombstone codec.
- `app/src/main/java/com/example/healthjournal/sync/GoalSyncPayload.kt` — goal sync codec.
- `app/src/main/java/com/example/healthjournal/sync/PersonalCardSyncPayload.kt` — personal card sync codec.
- `app/src/main/java/com/example/healthjournal/sync/RestoreWorker.kt` — post-restore worker ensuring `Self` profile and triggering Drive sync.

Back to [[overview]]
