# Implementation Plan: Fix Health Connect Permission Flow

## Phase 1: Manifest & Configuration
- [x] Task: Add Health Connect Metadata
    - Add `<meta-data android:name="health_permissions" android:resource="@array/health_permissions" />` to `AndroidManifest.xml`.
- [x] Task: Create health_permissions.xml
    - Define the resource array in `res/values/health_permissions.xml`.
- [x] Task: Standardize Rationale Intent
    - Ensure `MainActivity` correctly handles the rationale action.

## Phase 2: Logic Hardening
- [x] Task: Verify Permission Strings
    - Log the permission strings in `HealthConnectManager` to ensure they are valid.
- [x] Task: Check SDK Availability
    - Implement a check in `HealthConnectManager` to verify if Health Connect is installed and show a prompt if missing.

## Phase 3: UI Refactoring
- [x] Task: Update ActivityResultLauncher
    - Refactor `healthPermissionsLauncher` in `AddEntryScreen.kt` to use a more defensive implementation.
    - Ensure `viewModel.healthPermissions` is not empty during the launch.

## Phase 4: Verification
- [x] Task: Manual Verification
    - Install the update and verify that the permission dialog appears.
- [x] Task: Conductor - User Manual Verification 'Permission Flow' (Protocol in workflow.md)
