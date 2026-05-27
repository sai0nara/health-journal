# Specification: Build-Time Versioning and APK Naming

## Goal
Integrate a dynamic build timestamp into the application's metadata and output artifacts.

## Problem Statement
Standard builds are difficult to distinguish when multiple iterations are generated on the same day. Including the date and time of the build in the APK filename and within the app's "About" section will improve traceability.

## Objectives
- Automatically generate a build timestamp during the Gradle build process.
- Inject this timestamp into `BuildConfig` for on-screen display.
- Configure the APK output filename to include the version name and build timestamp.
- Update the "About App" dialog to display this detailed build information.

## Proposed Pattern
- **APK Filename:** `app-debug-v[VERSION]-[YYYYMMDD]-[HHMM].apk`
- **On-Screen Display:** `Version: [VERSION] (Build: [YYYYMMDD]-[HHMM])`

## Success Criteria
- Running `./gradlew assembleDebug` produces an APK with the correct timestamped name.
- The "About App" dialog in the application displays the exact build timestamp.
