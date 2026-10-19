# Track move_add_measurement_button_20261016 — Specification

## Overview

Relocate the body-measurement entry point off the History feed onto the Measurements screen, per `Docs/prd/move-add-measurement-button.md`. History keeps a single add-entry FAB; Measurements gains its own add-measurement FAB opening the unchanged `MeasurementEntrySheet`. Capture, validation, units, charts, goals, and sync behavior do not change — only the sheet's launch host moves.

Agreed decisions: (1A) regular `FloatingActionButton` with the Straighten icon reusing the existing `history_cd_add_measurements` content description; (2A) full removal of measurement wiring from History; (3A) local `showSheet` boolean in `MeasurementsScreen`; (4A) full test + docs scope.

## Functional Requirements

- FR-1: History shows exactly one FAB (add journal entry). The measurement `SmallFloatingActionButton`, `showMeasurementSheet` state, `MeasurementEntrySheet` host, measurement ViewModel instance, `measurementViewModelFactory` parameter, and the `onUnitSystemChanged` sync block are removed from `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`. Callers of `HistoryScreen` are updated for the removed parameter.
- FR-2: `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` provides a regular `FloatingActionButton` (Straighten icon, existing measurement content description) visible in both empty and populated states. Tapping it opens the existing `MeasurementEntrySheet`.
- FR-3: The sheet is hosted with a local `showSheet` boolean in `MeasurementsScreen`, reusing the screen's existing `BodyMeasurementViewModel` — no factory threading, no ViewModel or sheet changes.
- FR-4: The History top-bar measurements navigation action (`onMeasurementsClick`) is preserved, keeping the History → Measurements → FAB → save path.
- FR-5: Sheet behavior is unchanged: timestamp plus up to seven optional fields (at least one required), decimal keyboard, inline validation, preferred display units with canonical metric storage, save/dismiss semantics per `Docs/prd/body-measurements.md`.
- FR-6: Docs ship with the feature: add `Docs/psd/move-add-measurement-button.md` and `Docs/tests/move-add-measurement-button.md`, and drop the `— planned` mark on the PRD line in `Docs/index.md`.

## Non-Functional Requirements

- Coverage: >80% on new/changed code; UI tests on both touched surfaces (per `conductor/workflow.md`).
- Accessibility: relocated FAB keeps the same content description and remains reachable by screen reader and keyboard.
- No new permissions, background work, storage, or network paths.

## Acceptance Criteria

- AC-1: On History, exactly one FAB is visible; no measurement action or sheet can be triggered there.
- AC-2: On Measurements, the FAB is visible empty and populated and opens the entry sheet.
- AC-3: Saving a valid set from the relocated sheet persists locally (pending-sync) and updates the list/chart identically to today's flow.
- AC-4: Invalid input (future date, negative, over-bound, no values) stays blocked with existing inline guidance.
- AC-5: `HistoryScreenTest` updated (single FAB, no sheet trigger); `MeasurementScreenTest` / `MeasurementEntrySheetTest` cover FAB-open plus save; `./gradlew :app:testDebugUnitTest` green and `python3 wiki/lint.py` exits 0.

## Out of Scope

- Sheet/field redesign, in-place editing, widgets, Health Connect write-back.
- Validation, conversion, chart, goal, or sync logic changes.
- New destinations, bottom-bar, or settings changes.
