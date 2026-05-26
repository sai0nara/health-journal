# Implementation Plan: Add App Versioning and About Screen

## Phase 1: Resource & Config Updates
- [x] Task: Update App Name String
    - Change `app_name` in `strings.xml` to "Health Journal v1.0".
- [x] Task: Ensure BuildConfig access
    - Verify that `buildConfig = true` is enabled in `build.gradle.kts` (or use `PackageManager`).

## Phase 2: UI Implementation
- [x] Task: Create AboutAppDialog Composable
    - Implement a clean Material 3 dialog displaying version info.
- [x] Task: Integrate Menu Entry
    - Add an `IconButton` with `Icons.Default.Info` to the `HistoryScreen` TopAppBar.
    - Wire the icon to show the `AboutAppDialog`.

## Phase 3: Verification
- [x] Task: Automated UI Test
    - Add a test case to `HistoryScreenTest.kt` that verifies the "About" dialog opens and displays version info.
- [x] Task: Conductor - User Manual Verification 'App Versioning' (Protocol in workflow.md)
