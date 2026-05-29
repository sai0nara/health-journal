# Specification: Preserve Creation Date and Add lastModified Field

## Goal
Decouple the entry creation timestamp from the sync conflict resolution timestamp.

## Problem Statement
The app currently updates the `timestamp` field on every edit. This fulfills the "latest timestamp wins" sync requirement but overwrites the original creation date of the entry. Users need to see when an entry was originally created, while the system still needs a "last modified" timestamp for synchronization logic.

## Objectives
- Add a `lastModified: Long` field to the `JournalEntry` entity.
- Ensure the `timestamp` field remains immutable after creation.
- Refactor the synchronization logic in `SyncWorker` to use `lastModified` instead of `timestamp` for conflict resolution.
- Update the UI to display the original creation date and optionally a "Last edited" indicator.

## Success Criteria
- Editing an entry does not change its displayed "Created at" date.
- Bidirectional sync still works correctly using the `lastModified` field.
- Existing data is migrated correctly.
