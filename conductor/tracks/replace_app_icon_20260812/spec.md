# Specification: Replace App Icon

## Goal
Replace the default/missing launcher icon for the Health Journal Android app with the custom artwork provided in `Docs/Images/Icon.png`.

## Objectives
- Generate mipmap launcher icons across standard densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi).
- Update `AndroidManifest.xml` to set `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.
- Ensure the app compiles and launches successfully with the new launcher icon.

## Technical Requirements
- Image processing script to generate mipmap icons from `Docs/Images/Icon.png`.
- XML manifests updated with icon attributes.
- Build verification via Gradle (`./gradlew assembleDebug`).

## Success Criteria
- Android app builds without errors.
- App launcher icon is properly referenced and displayed.
