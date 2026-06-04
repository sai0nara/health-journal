# Specification: Fix Sync Conflicts and Camera Attachment

## Goal
Resolve issues where local updates are lost during synchronization and restore camera photo attachment functionality.

## Problem Statement
1.  **Sync Conflicts**: When a user updates an existing entry (adding media or changing text), these changes are disappeared after the next sync. This is likely due to the "latest timestamp wins" logic in `SyncWorker` not being triggered by local edits, or the cloud version overwriting local changes because of timestamp parity.
2.  **Camera Failure**: Users are unable to attach photos using the camera. This likely stems from recent refactoring of the camera launcher and state management in `AddEntryScreen`.

## Objectives
- Ensure local edits always "win" over the cloud version by updating the `timestamp` on every save/update.
- Fix the camera implementation in `AddEntryScreen` to correctly capture and append photos to the `attachedPhotoUris` list.
- Verify that `ActivityResultContracts.TakePicture()` is used with a valid, accessible URI from the FileProvider.

## Success Criteria
- Editing an entry (text or media) and then syncing preserves the changes.
- Launching the camera captured a photo and adds it to the entry's photo list.
- Photos captured by camera are correctly uploaded to the cloud.
