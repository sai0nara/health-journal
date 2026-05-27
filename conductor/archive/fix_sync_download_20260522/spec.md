# Specification: Fix Sync - Download Cloud Entries

## Goal
Fix the issue where journal entries stored in Google Drive are not being downloaded to the application during synchronization.

## Problem Statement
Users report that even when synchronization is triggered and successfully "completes", existing entries from the cloud are not appearing in the local history. This indicates a failure in the cloud-to-local data flow.

## Objectives
- Investigate `DriveServiceHelper.downloadJournalData()` for potential failures or incorrect file resolution.
- Verify `SyncWorker` merge logic to ensure cloud entries are correctly prioritized and inserted into the local database.
- Ensure that the `appDataFolder` is being correctly queried.
- Add robust logging to identify the exact point of failure during a sync operation.

## Success Criteria
- Existing entries in the `appDataFolder` are correctly downloaded and merged with local data.
- The local history view displays all synced entries after a sync operation.
- Instrumented tests verify bidirectional sync (Upload and Download).
