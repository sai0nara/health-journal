# Implementation Plan: Categorization and Tagging (Illness, Checkup, Doctor, Exercises)

## Phase 1: Data Layer & Schema
- [x] Task: Define `JournalTag` enum and `EntryTagCrossRef` entity. [517c451]
- [x] Task: Update `JournalDao` with `searchEntriesWithTags` query.
- [x] Task: Implement Room migration for the new cross-reference table.
- [x] Task: Update `JournalRepository` to handle adding/removing tags for an entry.
- [x] Task: Write unit tests for the DAO and Repository tag operations.

## Phase 2: ViewModel & State Management
- [x] Task: Update `JournalViewModel` to maintain `selectedTags` state.
- [x] Task: Implement the combined search and filter logic using `combine` operator.
- [x] Task: Add ViewModel methods for toggling tags on an entry.

## Phase 3: UI - Entry Tagging
- [x] Task: Implement `TagSelectionRow` component using `FilterChip`.
- [x] Task: Integrate `TagSelectionRow` into `JournalDetailScreen`.
- [x] Task: Verify tag toggling updates the UI instantly.

## Phase 4: UI - List Filtering
- [x] Task: Implement `FilterChipRow` in `HistoryScreen` and `ArchiveScreen`.
- [x] Task: Connect `FilterChipRow` to the ViewModel's `selectedTags` state.
- [x] Task: Verify that combining text search and tag filters works correctly.

## Phase 5: Synchronization Integration
- [x] Task: Update Cloud Sync payload to include tags as a string array.
- [x] Task: Update `PeriodicSyncWorker` to handle tag synchronization.
- [x] Task: Verify that tag updates trigger `PENDING_SYNC` status.

## Phase 6: Verification
- [x] Task: Perform full end-to-end manual verification of the tagging and filtering flow.
- [x] Task: Conductor - User Manual Verification 'Categorization & Tagging' (Protocol in workflow.md)
