# Implementation Plan: Fix Sync - Download Cloud Entries

## Phase 1: Diagnostics & Testing
- [~] Task: Create Instrumented Sync Regression Test
    - [ ] Write a test in `CloudSyncTest.kt` that specifically verifies downloading data from a mocked Drive service.
    - [ ] Verify if the test fails in the current state.
- [ ] Task: Add Trace Logging to `SyncWorker`
    - [ ] Add detailed logs for: Cloud file detection, Download result, JSON parsing success, Merge count, and DB insertion.

## Phase 2: Fix & Verification
- [ ] Task: Fix Download Logic
    - [ ] Address any issues found in `DriveServiceHelper` or `SyncWorker`.
    - [ ] Ensure `Gson` parsing handles all `JournalEntry` fields correctly.
- [ ] Task: Verify Fix with Regression Test
    - [ ] Run the instrumented tests on an emulator to confirm the fix.
- [ ] Task: Conductor - User Manual Verification 'Fix Sync' (Protocol in workflow.md)
