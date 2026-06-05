# Implementation Plan: Archive and Multi-Delete Functionality

## Phase 1: Data Layer Updates
- [x] Task: Refactor JournalEntry Entity
    - Add `isArchived: Boolean = false`.
- [x] Task: Update Room Database
    - Increment DB version in `JournalDatabase.kt`.
- [x] Task: Update Repository & ViewModel
    - Add `archiveEntry(entryId: String)` and `restoreEntry(entryId: String)` methods.
    - Add `deleteEntries(entryIds: List<String>)` and `deleteAllArchivedEntries()` methods.
    - Update ViewModel to expose separate flows for active and archived entries.

## Phase 2: UI Implementation (History & Menu)
- [x] Task: Update HistoryScreen
    - Filter entries to show only non-archived items.
    - Add "Move to Archive" action to entry items (e.g., via Swipe-to-Action or Context Menu).
- [x] Task: Add Navigation Entry
    - Add an "Archive" item to the `HistoryScreen` TopAppBar or a Navigation Drawer.

## Phase 3: Archive Screen implementation
- [x] Task: Create ArchiveScreen
    - Display a list of archived entries.
    - Implement multi-selection mode.
    - Add "Delete Selected", "Restore Selected", and "Delete All" actions in the TopAppBar when items are selected.

## Phase 4: Cloud Synchronization
- [x] Task: Update Sync Strategy
    - Ensure `isArchived` is synced.
    - Implement a way for `SyncWorker` to recognize permanent deletions (e.g., local deletion results in cloud deletion on next sync).

## Phase 5: Verification
- [x] Task: Unit & Instrumented Tests 5881d34
    - Add tests for archiving, restoring, and bulk deletion logic.
- [x] Task: Conductor - User Manual Verification 'Archive & Delete' (Protocol in workflow.md) 5881d34

## Phase: Review Fixes
- [x] Task: Apply review suggestions cf0f663
