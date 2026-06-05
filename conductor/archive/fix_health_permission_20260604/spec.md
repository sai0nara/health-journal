# Specification: Fix Health Connect Permission Flow

## Goal
Restore the ability for users to grant Health Connect permissions via the system dialog.

## Problem Statement
Clicking the "Health" button in the `AddEntryScreen` currently triggers a "Health permissions required for sync" Toast without ever showing the system permission request window. This prevents users from authorizing the app to read their health data.

## Potential Root Causes
1.  **Manifest Configuration**: The `ACTION_SHOW_PERMISSIONS_RATIONALE` intent filter or activity alias might be incomplete or misconfigured.
2.  **SDK Version Mismatch**: Recent downgrades to `1.1.0-alpha11` might require specific manifest metadata (e.g., `<meta-data android:name="health_permissions" ... />`).
3.  **Permission Set Mismatch**: The set of permissions passed to the launcher might be empty or contain strings not recognized by the system.
4.  **Activity Lifecycle**: The launcher might be initialized or called in a way that the system ignores on certain devices (like Samsung Fold).

## Objectives
- Harden the `AndroidManifest.xml` with all required Health Connect metadata and intent filters.
- Ensure `HealthConnectManager` provides a valid, non-empty set of permission strings.
- Verify and potentially simplify the `ActivityResultLauncher` implementation in `AddEntryScreen`.
- Add a "Check Availability" step to ensure Health Connect is installed and supported before attempting sync.

## Success Criteria
- Clicking "Sync Health" triggers the system Health Connect permission dialog (if not already granted).
- After granting permissions, the "Sync Health" action successfully imports data.
