# Specification: Body Analytics — Per-Parameter Trends & Goal Lines

## Overview
Replace the single weight-trend chart section inside the Measurements screen with a tabbed per-parameter analytics view. Each of the seven tracked parameters (Weight, Chest, Waist, Glute, Thighs, Calves, Biceps) gets its own trend graph rendered by the existing dependency-free Compose Canvas engine, extended with a dashed horizontal goal line and a shaded delta area between the series and the target. Users can set one numeric goal per parameter; goals persist in a new Room `goals` table and roam across devices via Google Drive using a new sibling payload file, leaving every existing cloud contract untouched.

## Functional Requirements
- **FR1 — Tabbed chart section:** The chart section of `MeasurementsScreen` renders a horizontally scrollable Material3 `ScrollableTabRow` listing all seven parameters plus a paged chart area (`HorizontalPager`) synchronized two-way with tab selection (tap switches page; swipe updates selected tab).
- **FR2 — Per-parameter trend:** Each page plots that parameter's chronological series (existing entries source, newest-first feed re-sorted ascending for plotting) using the extended custom Canvas chart with automatic min/max scaling consistent with today's weight chart.
- **FR3 — Goal line:** When the active parameter has a stored goal, the chart draws a dashed horizontal line at the target value with a small label (value + unit). No goal → no line.
- **FR4 — Delta area fill:** The region between the series polyline and the goal line is filled with a translucent tint derived from theme colors (semantic tokens only), visually showing distance from target.
- **FR5 — Goal setting flow:** A "Set Goal" affordance (icon beside the chart header) opens a bottom-sheet dialog pre-filled with the active parameter's current goal. Input is metric-only (kg for Weight, cm otherwise). Saving persists immediately; a Clear action deletes the goal.
- **FR6 — Goal validation:** Strict bounds enforced inline (positive value, parameter-appropriate upper sanity cap, e.g. Weight ≤ 400 kg, girths ≤ 300 cm). Invalid input blocks save with an error message; parity with existing measurement-field validation UX.
- **FR7 — Goal persistence:** New Room entity `GoalEntity(parameter_id PK, target REAL, lastModified INTEGER)` + DAO + `GoalsRepository`, following existing repository conventions.
- **FR8 — Goal sync:** New sibling Drive file `body_measurements_goals.json` (Gson codec mirroring `MeasurementSyncPayload` style; null/garbage-safe → empty list). SyncWorker imports/uploads goals after the measurements pipeline: full-snapshot merge — upsert remote goals with newer `lastModified`, prune local rows absent from a valid cloud snapshot so cleared goals propagate. Upload/persist failures → `Result.retry()`.
- **FR9 — View model:** Dedicated `BodyAnalyticsViewModel` (MVVM, consistent with stack) exposing selected tab, per-param point lists, goal values, and dialog state; consumes `BodyMeasurementRepository` + `GoalsRepository`.

## Non-Functional Requirements
- **NFR1 — Zero new dependencies:** Charting stays on the extended in-house Canvas; no Vico/DataStore/SQLCipher introduced.
- **NFR2 — Cloud contracts preserved:** Existing `health_journal_data.json` and `body_measurements.json` payload formats are untouched; goals use their own sibling file.
- **NFR3 — Style:** Material semantic color tokens exclusively; KDoc explains *why*; test tags for all new interactive elements.
- **NFR4 — Testing:** Unit tests for goal validation, snapshot merge logic, and codec; instrumented tests for tab switching, goal dialog save/clear, goal-line/delta-fill rendering, and a worker round-trip test. Full suites green.

## Acceptance Criteria
1. Tabs switch charts instantly; each parameter shows its own correctly scaled trend.
2. Goal line + label render exactly at the stored target; absent when no goal.
3. Delta area visibly shades between series and goal line.
4. Set/clear goal works, validates inline, survives process restart (Room persistence).
5. Cross-device: goal set on device A appears on B after sync; a cleared goal disappears on B; conflicting edits resolve newest-wins.
6. Offline goal edits succeed locally and reconcile on next successful sync.
7. Replaced chart section introduces no regression: full unit + instrumented suites green.

## Out of Scope
- Scrub crosshair interactions and haptic feedback (deferred).
- Landscape full-screen chart expansion (deferred).
- LTTB downsampling (deferred; journal-scale data doesn't warrant it yet).
- Imperial units / unit-system switching.
- Journal-entry analytics.
- Database encryption (SQLCipher): noted from the original brief — the app does not use SQLCipher today; adopting it would be its own track.
