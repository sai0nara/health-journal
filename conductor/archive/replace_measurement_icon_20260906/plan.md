# Implementation Plan: Replace Body Measurement Action Icon

## Phase 1: Chart Asset Preparation
- [x] Task: Process the source image (trim empty dark margins, make background transparent, center artwork) into `drawable-nodpi/measurements_chart.png`, legible at 24dp
- [x] Task: Conductor - User Manual Verification 'Chart Asset' (Protocol in workflow.md)

## Phase 2: Action Icon Replacement
- [x] Task: Write failing Compose UI test asserting the new chart action icon exists and still triggers measurements navigation
- [x] Task: Replace `MonitorWeight` with the tinted (`onSurface`) drawable in the History top-bar 'View Measurements' action; keep content description, behavior, and placement
- [x] Task: Run instrumented UI tests until green on device
- [x] Task: Verify light/dark theme legibility and design-system compliance (semantic color tokens)
- [x] Task: Update any affected wiki pages (agent owns the vault) and run wiki lint (exit 0)
- [x] Task: Conductor - User Manual Verification 'Measurement Icon' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 17d0eff
