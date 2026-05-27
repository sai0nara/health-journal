# Implementation Plan: Build-Time Versioning and APK Naming

## Phase 1: Gradle Configuration
- [x] Task: Generate Build Timestamp in Gradle
    - Add logic to `app/build.gradle.kts` to calculate current date/time.
- [x] Task: Inject Timestamp into BuildConfig
    - Add `buildConfigField` for `BUILD_TIMESTAMP`.
- [x] Task: Configure APK Naming Logic
    - Use `applicationVariants.all` to rename the output APK file.

## Phase 2: UI Updates
- [x] Task: Update AboutAppDialog
    - Modify `AboutAppDialog.kt` to read and display `BuildConfig.BUILD_TIMESTAMP`.

## Phase 3: Verification
- [x] Task: Verify APK Filename
    - Run `assembleDebug` and check the `app/build/outputs/apk/debug/` directory.
- [x] Task: Manual Screen Verification
    - Launch the app and verify the "About App" dialog shows the timestamp.
- [x] Task: Conductor - User Manual Verification 'Build Versioning' (Protocol in workflow.md)
