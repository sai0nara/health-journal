# Specification: Archive and Multi-Delete Functionality

## Goal
Implement a two-stage deletion process: moving entries to an "Archive" state, from which they can be permanently deleted individually or in bulk.

## Objectives
- Update the `JournalEntry` entity to support an "archived" state.
- Implement UI for moving entries to the archive (e.g., swipe action or long-press menu).
- Create a dedicated "Archive" screen to view and manage archived items.
- Implement multi-selection logic in the Archive screen for "Delete Selected" and "Delete All" actions.
- Ensure the synchronization logic handles the archived state and permanent deletions correctly in the cloud.

## User Stories
- **Move to Archive**: As a user, I want to move entries I no longer want in my main history to an archive so my main view stays clean.
- **View Archive**: As a user, I want a dedicated area to see my archived entries.
- **Restore Entry**: As a user, I want to be able to move an entry back from the archive to my history (optional but recommended).
- **Permanent Delete**: As a user, I want to permanently delete specific entries, multiple entries, or all entries from the archive.

## Technical Requirements
- Add `isArchived: Boolean` to `JournalEntry`.
- Filter `HistoryScreen` to show only `isArchived == false`.
- New `ArchiveScreen` showing `isArchived == true`.
- Update `SyncWorker` to handle deletions (likely using a "tombstone" pattern or simple cloud-match-delete logic).

## Success Criteria
- Entries can be moved to the archive and disappear from the main history.
- Archived entries can be viewed in a separate list.
- Selecting multiple entries in the archive and clicking delete permanently removes them from the local DB and the cloud.
- "Delete All" in the archive clears all archived records.
