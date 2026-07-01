# Implementation Plan: Categorization and Tagging (Illness, Checkup, Doctor, Exercises)

## Phase 1: Data Layer & Schema
- [x] Task: Define `JournalTag` enum and `EntryTagCrossRef` entity. [517c451]
- [ ] Task: Update `JournalDao` with `searchEntriesWithTags` query.
- [ ] Task: Implement Room migration for the new cross-reference table.
- [ ] Task: Update `JournalRepository` to handle adding/removing tags for an entry.
- [ ] Task: Write unit tests for the DAO and Repository tag operations.

## Phase 2: ViewModel & State Management
- [ ] Task: Update `JournalViewModel` to maintain `selectedTags` state.
- [ ] Task: Implement the combined search and filter logic using `combine` operator.
- [ ] Task: Add ViewModel methods for toggling tags on an entry.

## Phase 3: UI - Entry Tagging
- [ ] Task: Implement `TagSelectionRow` component using `FilterChip`.
- [ ] Task: Integrate `TagSelectionRow` into `JournalDetailScreen`.
- [ ] Task: Verify tag toggling updates the UI instantly.

## Phase 4: UI - List Filtering
- [ ] Task: Implement `FilterChipRow` in `HistoryScreen` and `ArchiveScreen`.
- [ ] Task: Connect `FilterChipRow` to the ViewModel's `selectedTags` state.
- [ ] Task: Verify that combining text search and tag filters works correctly.

## Phase 5: Synchronization Integration
- [ ] Task: Update Cloud Sync payload to include tags as a string array.
- [ ] Task: Update `PeriodicSyncWorker` to handle tag synchronization.
- [ ] Task: Verify that tag updates trigger `PENDING_SYNC` status.

## Phase 6: Verification
- [ ] Task: Perform full end-to-end manual verification of the tagging and filtering flow.
- [ ] Task: Conductor - User Manual Verification 'Categorization & Tagging' (Protocol in workflow.md)
