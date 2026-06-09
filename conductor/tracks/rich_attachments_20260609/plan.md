# Implementation Plan: Rich Attachments & Archive Integration

## Phase 1: Local Storage & Compression Service
- [ ] Task: Create `MediaCompressionService` interface and implementation using `Bitmap.compress`.
    - [ ] Write unit tests for successful and failed compression scenarios.
    - [ ] Implement local saving logic directly to `context.filesDir`.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Local Storage & Compression Service' (Protocol in workflow.md)

## Phase 2: Database Schema & Repository Updates
- [ ] Task: Update `JournalEntry` and local database schemas to support a list of attachment paths with `syncStatus`.
    - [ ] Write Room migration and DAO unit tests.
    - [ ] Update `JournalEntryRepository` to handle local URI saving and metadata splitting.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Database Schema & Repository Updates' (Protocol in workflow.md)

## Phase 3: Sync Worker (WorkManager)
- [ ] Task: Implement `PeriodicSyncWorker` for two-stage synchronization.
    - [ ] Write WorkManager unit tests (mocking cloud upload).
    - [ ] Configure network constraints (UNMETERED).
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Sync Worker (WorkManager)' (Protocol in workflow.md)

## Phase 4: UI Updates - AddAttachment Flow
- [ ] Task: Update `AddEntryScreen` with "Add Attachment" button and visual picker contracts (`PickVisualMedia`, `OpenDocument`).
    - [ ] Write UI tests for selecting and displaying local thumbnails (loading/processing state).
    - [ ] Implement Coil for asynchronous image loading.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: UI Updates - AddAttachment Flow' (Protocol in workflow.md)

## Phase 5: UI Updates - Archive/Read-Only Flow
- [ ] Task: Update `JournalDetailScreen` for "Read-Only/Archived" mode.
    - [ ] Implement grid of static thumbnails that expand when tapped.
    - [ ] Write UI tests for viewing archived entries with media thumbnails.
    - [ ] Ensure "Unarchive" transitions to active editable mode smoothly.
- [ ] Task: Conductor - User Manual Verification 'Phase 5: UI Updates - Archive/Read-Only Flow' (Protocol in workflow.md)
