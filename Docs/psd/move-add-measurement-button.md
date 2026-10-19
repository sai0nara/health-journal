# Move Add Measurement Button from History Screen — Product Specification

> Host the unchanged measurement capture sheet behind a new FAB on the Measurements screen and strip all capture wiring from History, keeping one creation action per screen.

Last updated: 2026-10-19

## Overview

The move is a host relocation, not a behavior change: the sheet component,
its ViewModel, validation, units, and sync path are untouched. History drops
its secondary FAB, sheet state, sheet host, measurement ViewModel instance,
factory parameter, and unit-sync block, leaving a single add-entry FAB.
Measurements gains a regular `FloatingActionButton` reusing the existing
content description, opening the same sheet through a local boolean behind
the screen's existing `BodyMeasurementViewModel`. The design reference is
`Docs/prd/move-add-measurement-button.md`.

## Architecture

- MVVM without DI framework, following the repo's manual-factory pattern:
  the Measurements route in
  `app/src/main/java/com/example/healthjournal/MainActivity.kt` already
  resolves `BodyMeasurementViewModel` via the shared factory, so no new
  threading is needed — the screen passes its own ViewModel into the sheet.
- Sheet visibility is local `remember` state (`showMeasurementSheet`),
  mirroring the pattern History used; form state stays in the ViewModel so
  rotation and draft retention behave exactly as before.
- History keeps its top-bar measurements navigation action, preserving the
  History → Measurements → FAB → save path.
- Seams for testing: FAB discoverable by its content description; sheet
  fields and save button keep their existing test tags.

## Data flow

1. User taps the Measurements FAB; `showMeasurementSheet` flips true and the
   existing sheet composes over the screen in
   `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt`.
2. User enters values in display units; validation, `canSave`, and the
   future-date guard run in
   `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt`
   exactly as before.
3. Save maps display-unit text to canonical metric columns, stamps
   pending-sync, persists the row, and resets the form; dismiss closes the
   sheet via `onDismiss`.
4. The measurements list and trend chart update reactively from the same
   repository flows; Drive sync converges as defined in
   `Docs/prd/body-measurements.md`.
5. History no longer participates: no measurement action, sheet, or ViewModel
   exists on that screen.

## Components

| Component | File | Responsibility |
|---|---|---|
| Measurements screen | `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` | FAB host, sheet host, tabs/chart/list unchanged |
| History screen | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | single add-entry FAB, feed/search/archive unchanged |
| Navigation | `app/src/main/java/com/example/healthjournal/MainActivity.kt` | measurements route wiring, History caller without factory arg |
| Capture sheet | `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` | field input + save, reused unchanged |
| Capture state | `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` | form/validation/save/undo, reused unchanged |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Empty measurements list | FAB visible above the empty state; sheet opens identically |
| Sheet dismissed mid-draft | draft retained in ViewModel; reopening restores typed values |
| Invalid input (future date, negative, over-bound, no values) | existing inline guidance; save blocked |
| Unit preference toggle | sheet re-renders through the shared converter; storage stays canonical metric |
| History overflow/top-bar | measurements chart-icon navigation unchanged |

## Dependencies

- Jetpack Compose Material 3 (`FloatingActionButton`, `ModalBottomSheet` host already in the sheet).
- No new libraries, permissions, background work, or storage.

## Cross-references

- `Docs/prd/move-add-measurement-button.md` — the requirements this specification implements.
- `Docs/tests/move-add-measurement-button.md` — the test cases that verify this design.
- `Docs/prd/body-measurements.md` — capture/validation/sync requirements preserved by this move.
- [[ui-layer]] — the History and Measurements screens.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — FAB and sheet host.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — single-FAB feed.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — route wiring.
- `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` — reused capture sheet.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — reused capture state.
- `Docs/prd/move-add-measurement-button.md` — requirements this specification implements.
- `Docs/tests/move-add-measurement-button.md` — test cases.
