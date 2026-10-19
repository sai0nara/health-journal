# Move Add Measurement Button from History Screen — Product Requirements

> Relocate the body-measurement entry point off the History feed onto the Measurements screen so each FAB opens exactly the flow its screen owns.

Last updated: 2026-10-19

## Overview

The History screen stacked two floating actions: the primary
add-entry FAB and a smaller measurement button that opened the body-measurement
capture sheet in
`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`.
The dedicated measurements surface in
`app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt`
had charts, history, and goals but no entry point of its own — the user had to
leave that screen, open History, and tap the secondary FAB to log a value.
This feature removes the measurement button and its sheet wiring from History
and gives Measurements its own single add-measurement FAB opening the same
sheet, so History owns journal entries and Measurements owns measurements.
Capture, validation, units, and sync behavior do not change; only where the
sheet is launched from changes.

## Goals / Non-goals

**Goals**

- Give the Measurements screen its own add-measurement entry point.
- Remove the measurement button and capture-sheet hosting from History.
- Keep the capture sheet behavior identical (fields, validation, units, save).
- Keep a clear path from History to logging a measurement via the existing
  measurements navigation action.

**Non-goals**

- No change to measurement validation, bounds, or error strings.
- No change to display-unit conversion or canonical metric storage.
- No change to trend charts, goals, history list, delete/undo, or Drive sync.
- No new navigation destinations or bottom-bar changes.

## User stories

- As a user on the Measurements screen, I want an add button there so I can
  log a measurement without going back to History.
- As a user on the History screen, I want a single add action so I am not
  guessing which FAB logs a journal entry versus a measurement.
- As a user who opens Measurements from the dashboard tile, I want to land
  on a screen where logging is obviously available. (The chart-icon shortcut
  this story originally described was later removed from the History top
  bar; the dashboard tile is the entry point.)

## Functional requirements

- FR-1: The History screen shows a single FAB for adding a journal entry; no
  measurement button, no measurement sheet state, and no measurement
  ViewModel wiring for capture remain on that screen.
- FR-2: The Measurements screen provides a single add-measurement FAB that
  opens the existing measurement entry sheet.
- FR-3: The relocated sheet keeps its current behavior: timestamp plus up to
  seven optional fields (at least one required), decimal keyboard, inline
  validation, preferred display units with canonical metric storage, and
  save/dismiss semantics per `Docs/prd/body-measurements.md`.
- FR-4: The History top-bar measurements navigation action continues to open
  the Measurements screen, preserving the History-to-measurement path.
- FR-5: Empty, error, and success states on the Measurements screen are
  unchanged except for the added FAB; the sheet's own empty/invalid/valid
  states are unchanged.

## Non-functional requirements

- Reactive reads: the measurements list and charts update on save as today.
- Offline-first: capture saves locally first, then syncs; no new network path.
- Accessibility: the new FAB carries the same content description the History
  measurement button used, and remains reachable by screen reader and keyboard.
- No new permissions, background work, or storage.

## Acceptance criteria

- AC-1: On History, exactly one FAB (add entry) is visible; no measurement
  action or sheet can be triggered from that screen.
- AC-2: On Measurements, an add-measurement FAB is visible in both empty and
  populated states and opens the measurement entry sheet.
- AC-3: Saving a valid set from the relocated sheet persists locally
  (pending-sync), appears in the measurements list, and updates the trend
  chart, identical to today's History-launched flow.
- AC-4: Invalid input (future date, negative, over-bound, no values) is still
  blocked with the existing inline guidance.
- AC-5: Navigating History → measurements icon → FAB → save produces a saved
  measurement without passing through the History FAB stack.

## Out of scope

- Redesign of the capture sheet or its fields.
- Editing existing measurements in place.
- Dashboard widgets or Health Connect write-back.

## Cross-references

- `Docs/psd/move-add-measurement-button.md` — the specification (added when the feature is built).
- `Docs/tests/move-add-measurement-button.md` — the test cases (added when the feature is built).
- `Docs/prd/body-measurements.md` — capture, validation, chart, and sync requirements this move preserves.
- `Docs/prd/history-feed.md` — the History feed requirements this move simplifies.
- [[ui-layer]] — the History and Measurements screens.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — current measurement FAB and sheet host to remove.
- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — future FAB and sheet host.
- `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` — capture sheet reused unchanged.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — capture/validation state reused unchanged.
