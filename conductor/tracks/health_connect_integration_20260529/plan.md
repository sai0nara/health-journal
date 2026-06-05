# Implementation Plan: Health Connect Integration

## Phase 1: SDK Setup & Permissions
- [x] Task: Add Health Connect Dependencies
    - Update `build.gradle.kts` with Health Connect libraries.
    - Add required permissions to `AndroidManifest.xml`.
- [x] Task: Permission Management Flow
    - Implement a `HealthConnectManager` to handle permission checks and requests.
    - Add a permission rationale UI or handle the intent from the settings.

## Phase 2: Data Retrieval Logic
- [x] Task: Implement Data Fetching
    - Add functions to `HealthConnectManager` to query Steps, Heart Rate, and Sleep.
    - Handle scenarios where data is missing or Health Connect is not installed.
- [x] Task: ViewModel Integration
    - Update `JournalViewModel` to expose health sync functionality.

## Phase 3: UI Integration
- [x] Task: Wiring "Sync Health" Button
    - Update `AddEntryScreen` to call the health sync logic.
    - Display a loading state or confirmation when data is imported.
- [x] Task: Visualizing Metrics
    - Ensure the imported metrics are visible in the `HistoryScreen` and `AddEntryScreen`.

## Phase 4: Verification
- [x] Task: Manual Verification on Device
    - Verify data import from a device with active health data (e.g., Google Fit or Samsung Health).
- [x] Task: Conductor - User Manual Verification 'Health Sync' (Protocol in workflow.md)

