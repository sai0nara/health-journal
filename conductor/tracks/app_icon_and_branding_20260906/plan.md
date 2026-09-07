# Implementation Plan: Replace App Icon & Add App Bar Branding

## Phase 1: Icon Resource Generation & Manifest Verification
- [x] Task: Add adaptive icon XML resources (mipmap-anydpi-v26/ic_launcher.xml, ic_launcher_round.xml) with foreground from the new source image + background layer
- [x] Task: Generate legacy PNG launcher mipmaps (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi) for ic_launcher and ic_launcher_round from the 2048x2048 source
- [x] Task: Verify AndroidManifest references (@mipmap/ic_launcher, @mipmap/ic_launcher_round) resolve to the new resources
- [x] Task: Build the app (assembleDebug) to confirm icon resources compile
- [x] Task: Conductor - User Manual Verification 'Icon Resources' (Protocol in workflow.md)

## Phase 2: Top App Bar Branding
- [x] Task: Write failing Compose UI test asserting the logo appears in the History top app bar
- [x] Task: Add the logo image as a bundled drawable resource and render it in the History screen top app bar
- [x] Task: Run instrumented UI test until green on device
- [x] Task: Verify design-system compliance (Material 3 colorScheme tokens; logo is a fixed bundled asset)
- [x] Task: Update any affected wiki pages (agent owns the vault) and run wiki lint (exit 0); commit wiki + code together
- [x] Task: Conductor - User Manual Verification 'Top App Bar Branding' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 6958bd8
