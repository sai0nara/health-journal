# Implementation Plan: Complete Google Drive Cloud Synchronization

## Phase 1: Foundation & Refactoring [checkpoint: ffbe707]
- [x] Task: Audit and Refactor `DriveServiceHelper` [55dd689]
    - [x] Review current `DriveServiceHelper.kt` implementation.
    - [x] Write unit tests for Google Drive client initialization.
    - [x] Implement robust error handling for API calls.
- [x] Task: Finalize Authentication Flow [5efba6f]
    - [x] Write tests for Google Sign-In and scope authorization.
    - [x] Ensure `GoogleAuthManager.kt` correctly handles token refreshing and session persistence.
- [x] Task: Conductor - User Manual Verification 'Foundation & Refactoring' (Protocol in workflow.md) [ffbe707]

## Phase 2: Core Sync Logic
- [~] Task: Implement Database Backup Logic
    - [ ] Write tests for database export to JSON/Binary.
    - [ ] Implement secure file preparation for upload.
- [ ] Task: Implement File Upload and Download
    - [ ] Write tests for `DriveServiceHelper` upload/download methods.
    - [ ] Implement metadata-based file tracking (e.g., storing last sync timestamp).
- [ ] Task: Conductor - User Manual Verification 'Core Sync Logic' (Protocol in workflow.md)

## Phase 3: Conflict Resolution & Robustness
- [ ] Task: Implement Conflict Detection
    - [ ] Write tests for identifying local vs. remote differences.
    - [ ] Implement a basic "last-write-wins" resolution strategy.
- [ ] Task: Harden `SyncWorker`
    - [ ] Write tests for `SyncWorker.kt` behavior under various network conditions.
    - [ ] Implement exponential backoff and retry logic in `SyncWorker`.
- [ ] Task: Conductor - User Manual Verification 'Conflict Resolution & Robustness' (Protocol in workflow.md)

## Phase 4: UI & Integration
- [ ] Task: Enhance Cloud Sync UI
    - [ ] Write UI tests for the Sync settings/status screen.
    - [ ] Implement real-time sync status updates in the UI (Idle, Syncing, Success, Error).
- [ ] Task: Manual Sync Trigger
    - [ ] Add a "Sync Now" button and verify its functionality.
- [ ] Task: Conductor - User Manual Verification 'UI & Integration' (Protocol in workflow.md)
