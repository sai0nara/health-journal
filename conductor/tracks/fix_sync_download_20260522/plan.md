# Implementation Plan: Fix Sync - Download Cloud Entries

## Phase 1: Diagnostics & Testing
- [x] Task: Create Instrumented Sync Regression Test 9c5d09e
    - [x] Write a test in `CloudSyncTest.kt` that specifically verifies downloading data from a mocked Drive service.
    - [x] Verify if the test fails in the current state.
- [x] Task: Add Trace Logging to `SyncWorker` 9c5d09e
    - [x] Add detailed logs for: Cloud file detection, Download result, JSON parsing success, Merge count, and DB insertion.

## Phase 2: Fix & Verification
- [x] Task: Fix Download Logic 9c5d09e
    - [x] Address any issues found in `DriveServiceHelper` or `SyncWorker`.
    - [x] Ensure `Gson` parsing handles all `JournalEntry` fields correctly.
- [x] Task: Verify Fix with Regression Test 9c5d09e
    - [x] Run the instrumented tests on an emulator to confirm the fix.
- [x] Task: Conductor - User Manual Verification 'Fix Sync' (Protocol in workflow.md) 9c5d09e
