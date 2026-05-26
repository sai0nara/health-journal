# Implementation Plan: Add App Versioning and About Screen

## Phase 1: Resource & Config Updates
- [x] Task: Update App Name String 9c5d09e
    - Change `app_name` in `strings.xml` to "Health Journal v1.0".
- [x] Task: Ensure BuildConfig access 9c5d09e
    - Verify that `buildConfig = true` is enabled in `build.gradle.kts` (or use `PackageManager`).

## Phase 2: UI Implementation
- [x] Task: Create AboutAppDialog Composable 9c5d09e
    - Implement a clean Material 3 dialog displaying version info.
- [x] Task: Integrate Menu Entry 9c5d09e
    - Add an `IconButton` with `Icons.Default.Info` to the `HistoryScreen` TopAppBar.
    - Wire the icon to show the `AboutAppDialog`.

## Phase 3: Verification
- [x] Task: Automated UI Test 9c5d09e
    - Add a test case to `HistoryScreenTest.kt` that verifies the "About" dialog opens and displays version info.
- [x] Task: Conductor - User Manual Verification 'App Versioning' (Protocol in workflow.md) 9c5d09e
