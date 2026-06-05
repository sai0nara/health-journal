# Implementation Plan: Switch Steps Sync to Blood Pressure Sync

## Phase 1: Data Model & Config
- [x] Task: Update JournalEntry Entity
    - Remove `steps`.
    - Add `bp_systolic` and `bp_diastolic`.
- [x] Task: Update Permissions
    - Swap steps permission for blood pressure in `AndroidManifest.xml`.
    - Update `res/values/health_permissions.xml`.
- [x] Task: Room Migration
    - Increment DB version in `JournalDatabase.kt`.
    - (Destructive migration is acceptable as per previous instructions for dev phase).

## Phase 2: Logic Implementation
- [x] Task: Refactor HealthConnectManager
    - Remove `StepsRecord` dependencies and `getSteps` method.
    - Implement `getBloodPressure` method.
- [x] Task: Update ViewModel
    - Refactor `HealthSyncResult` and `syncHealthData` to return blood pressure instead of steps.

## Phase 3: UI Integration
- [x] Task: Update AddEntryScreen
    - Remove steps display/input.
    - Add systolic/diastolic fields.
- [x] Task: Update HistoryScreen
    - Replace steps icon/value with blood pressure display.

## Phase 4: Verification
- [x] Task: Unit & Instrumented Tests
    - Update `JournalViewModelTest` and `SyncDownloadTest` to reflect data changes.
- [x] Task: Conductor - User Manual Verification 'Blood Pressure Sync' (Protocol in workflow.md)
