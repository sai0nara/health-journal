# Specification: Add App Versioning and About Screen

## Goal
Include version information in the application's public identity (App Name) and provide a dedicated "About" section within the app to display technical version details.

## Objectives
- Update the visible application name to include the version (e.g., "Health Journal v1.0").
- Implement an "About App" dialog or screen accessible from the main history view.
- Programmatically retrieve the version name from the package manager to ensure the UI always matches the build configuration.

## Proposed Changes
### 1. Build Configuration
- Update `app/src/main/res/values/strings.xml` to include the version in `app_name`.

### 2. UI Implementation
- Add an "Info" icon to the `TopAppBar` in `HistoryScreen.kt`.
- Create a `AboutAppDialog` Composable that displays:
    - App Name
    - Version Name (e.g., "1.0")
    - Developer/Project Info
    - License information (optional)

## Success Criteria
- The app name on the device launcher reflects the version.
- Tapping the "Info" icon opens a dialog showing the correct version information.
- The version information is retrieved dynamically from `BuildConfig` or `PackageManager`.
