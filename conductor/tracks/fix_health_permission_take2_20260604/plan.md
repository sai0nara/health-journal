# Implementation Plan: Fix Health Connect Permission Flow (Deep Fix)

## Phase 1: Manifest Refinement
- [x] Task: Relax Activity Alias
    - Remove `android:permission` from `.HealthConnectRationaleActivity` alias.
- [x] Task: Add Management Intent
    - Add `ACTION_MANAGE_HEALTH_PERMISSIONS` intent filter to the activity.

## Phase 2: Activity Implementation
- [x] Task: Create Rationale Activity
    - Create a minimal `PermissionsRationaleActivity.kt` to handle the rationale flow explicitly.
    - Update manifest to point to this activity.

## Phase 3: Launcher Hardening
- [x] Task: Audit Launcher Call
    - Update `AddEntryScreen.kt` to log the exactly what is being passed to the launcher.
    - Ensure the set is not empty and contains valid strings.

## Phase 4: Verification
- [x] Task: Manual Verification on Device
    - Verify dialog appears.
- [x] Task: Conductor - User Manual Verification 'Permission Flow Fix' (Protocol in workflow.md)
