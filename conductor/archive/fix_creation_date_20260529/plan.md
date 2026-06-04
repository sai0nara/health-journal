# Implementation Plan: Preserve Creation Date and Add lastModified Field

## Phase 1: Data Model Updates
- [x] Task: Refactor JournalEntry
    - Add `lastModified: Long = timestamp` to `JournalEntry.kt`.
- [x] Task: Room Migration
    - Update `JournalDatabase` version.
    - Implement migration (defaulting `lastModified` to existing `timestamp`).

## Phase 2: Logic Refactoring
- [x] Task: Update ViewModel Edits
    - Modify `JournalViewModel.updateEntry` to update `lastModified` but preserve original `timestamp`.
- [x] Task: Update Sync Resolution
    - Modify `SyncWorker.kt` to use `lastModified` for the "latest wins" comparison.

## Phase 3: UI Improvements
- [x] Task: Display Timestamps
    - Update `HistoryScreen` to clearly show creation time.
    - (Optional) Add "Edited: <time>" if `lastModified > timestamp`.

## Phase 4: Verification
- [x] Task: Synchronization Regression Test
    - Update `SyncDownloadTest.kt` to verify conflict resolution using `lastModified`.
- [x] Task: Conductor - User Manual Verification 'Creation Date' (Protocol in workflow.md)
