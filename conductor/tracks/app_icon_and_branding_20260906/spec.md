# Specification: Replace App Icon & Add App Bar Branding

## Overview
Replace the application launcher icon (standard + round) with the new source image (`Docs/Images/Gemini_Generated_Image_qirwoaqirwoaqirw.jpeg`, 2048x2048) and add in-app branding by displaying the same asset as a logo in the History screen's top app bar.

## Functional Requirements
- **Adaptive Launcher Icon (API 26+):** Add adaptive icon XML (`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`) with a foreground layer derived from the new source image and a background.
- **Legacy Fallback:** Generate legacy PNG launcher mipmaps (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) for both `ic_launcher` and `ic_launcher_round` for pre-API-26 devices.
- **Manifest:** `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` already reference these resources; the resource files are replaced with the new asset.
- **Round Icon:** The same source asset is used for both the standard and round launcher icons.
- **Top App Bar Branding:** Display the new logo (small, non-intrusive) in the History screen's top app bar, using Material 3 conventions.

## Non-Functional Requirements
- All mipmap densities generated from the single 2048x2048 source (square).
- Adaptive icon safe-zone respected so the foreground is not clipped on masked launchers.
- Logo rendering uses the existing local image asset (offline-first; no network fetch).
- Design system: any new UI uses `MaterialTheme.colorScheme` semantic tokens; the logo image is a fixed bundled asset.

## Acceptance Criteria
- Installing the app shows the new icon on the launcher (standard + round) on both modern (adaptive) and legacy devices.
- The logo appears in the History screen top app bar.
- All tests pass; UI tests cover the branded top app bar.
- Wiki lint passes (exit 0) with any affected wiki pages updated and committed together.

## Out of Scope
- Changing store-listing icon assets beyond the in-repo mipmap set.
- Applying the logo to screens beyond the History top app bar.
- Modifying app behavior or navigation.
