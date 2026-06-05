# Specification: Fix Health Connect Permission Flow (Deep Fix)

## Goal
Force the Health Connect permission dialog to appear by correcting manifest configuration and launcher parameters.

## Problem Statement
Despite adding the required metadata and permission array, the system permission dialog for Health Connect is still not appearing. The app immediately reports that permissions are required.

## Potential Root Causes (Updated)
1.  **Activity Alias Restriction**: The `activity-alias` might be blocked by the `android:permission` attribute or the lack of a proper category.
2.  **Launcher Scope**: The `PermissionController.createRequestPermissionResultContract()` might be failing if the permission strings aren't exactly what it expects for the targeted SDK.
3.  **Rationale Activity Missing**: The system might require a dedicated `Activity` (not just an alias) to handle the rationale intent if it's strictly checking for it.
4.  **Package Visibility**: Although `com.google.android.apps.healthdata` is in `<queries>`, there might be other visibility issues.

## Objectives
- Remove restrictive permissions from the `activity-alias` in `AndroidManifest.xml`.
- Ensure the `Rationale` intent filter is correctly placed.
- Add an `intent-filter` for `ACTION_MANAGE_HEALTH_PERMISSIONS` if required by the SDK version.
- Implement a dedicated (minimal) Rationale Activity if the alias continues to fail.
- Audit and potentially hardcode the permission strings in the launcher to verify if dynamic retrieval is the issue.

## Success Criteria
- The "Sync Health" button click results in the system Health Connect permission request dialog appearing.
