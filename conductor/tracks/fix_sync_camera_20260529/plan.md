# Implementation Plan: Fix Sync Conflicts and Camera Attachment

## Phase 1: Sync Conflict Resolution
- [x] Task: Update Timestamp on Edit
    - Modify `JournalViewModel.updateEntry` to update the `timestamp` to the current time when an entry is edited. This ensures local changes have a newer timestamp than the cloud version.
- [x] Task: Verify Sync Logic
    - Review `SyncWorker.kt` to ensure it correctly prioritizes the local version when timestamps are newer.

## Phase 2: Camera Functionality Fix
- [x] Task: Debug Camera Launcher
    - Audit `AddEntryScreen.kt` camera logic.
    - Ensure the temporary file URI is correctly passed to `TakePicture()` and only added to the state if the capture is successful.
- [x] Task: Permissions & Provider Check
    - Verify `AndroidManifest.xml` and `file_paths.xml` for correct camera and file sharing configurations.

## Phase 3: Verification
- [x] Task: Conflict Regression Test
    - Add a test case to `SyncDownloadTest.kt` that simulates updating a local entry and ensuring it survives a sync against an older cloud version.
- [x] Task: Manual Camera Test
    - Manually verify camera capture on a physical device.
- [x] Task: Conductor - User Manual Verification 'Sync & Camera' (Protocol in workflow.md)
