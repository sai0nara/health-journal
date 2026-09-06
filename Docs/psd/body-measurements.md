# Body Measurements — Product Specification

> A ViewModel-held capture sheet with per-field validation writes a metric-only
> measurement row; an analytics ViewModel projects per-parameter trend series and
> goals to a dependency-free chart, and both rows and goals sync as Drive
> snapshots with tombstone + last-write-wins merge.

Last updated: 2026-09-02

## Overview

Body measurements are a sibling domain in Room and Drive. Capture is a
`ModalBottomSheet` whose form state lives in the ViewModel (so it survives
rotation) and whose `canSave` matrix combines per-field errors, the at-least-one
rule, and a future-date guard. Display is a paged tab per parameter: a
dependency-free `Canvas` line chart with a goal line, and a chronological card
list. Goals are one target per parameter with LWW sync; measurements delete via
a tombstone-first path.

## Architecture

- MVVM without DI framework: manually constructed ViewModels/factories.
- `BodyMeasurementViewModel` owns the entry-sheet form + save; a future timestamp
  or any field error blocks save and resets via a `justSaved` signal.
- `BodyAnalyticsViewModel` combines the measurement feed and goals into a
  per-parameter ascending series for the chart.
- Measurementvalues are stored metric-only; the `UnitSystem`/`UnitConverter`
  imperial support is used only by the Personal Card demographics, not here.
- Chart is a pure Compose `Canvas`: auto min/max including the goal target,
  flat-series guard, dashed goal line, translucent fill.

## Data flow

1. Secondary FAB on History opens the sheet; or the top bar opens the
   measurements screen.
2. User enters values; each keystroke validates; save is enabled only when
   `canSave` (no errors, ≥1 value, not future, not already saving).
3. Save maps text to metric `Double?` columns, stamps pending-sync, persists the
   row as `BodyMeasurementEntry`, and resets the form.
4. The analytics feed + goals project the active tab's series into the chart.
5. Delete writes a tombstone then removes the row; Undo re-inserts it.
6. Goal set/clear upserts/clears the goal entity (LWW) and updates the chart.

## Components

| Component | File | Responsibility |
|---|---|---|
| Measurements screen | `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` | tabs, chart, history list, delete/undo, goals entry |
| Capture sheet | `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` | field input + save |
| Goal sheet | `app/src/main/java/com/example/healthjournal/ui/components/GoalSheet.kt` | goal set/clear |
| Trend chart | `app/src/main/java/com/example/healthjournal/ui/components/ParamTrendChart.kt` | dependency-free line/goal chart |
| Body measurement ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` | form/validation/save/undo |
| Analytics ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModel.kt` | series + goal targets |
| Measurement repository | `app/src/main/java/com/example/healthjournal/data/BodyMeasurementRepository.kt` | persistence + tombstones |
| Goals repository | `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` | goal persistence |
| Validation | `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt`, `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` | capture + goal rules |
| Formatters | `app/src/main/java/com/example/healthjournal/domain/MeasurementFormatters.kt` | summaries + trend projection |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Over-bound value | field error "too large" per-field cap; save blocked |
| Negative / malformed | field error; save blocked |
| Future timestamp | block + inline alert; save blocked |
| No values | save disabled (at-least-one) |
| Flat series | chart guards divide-by-zero |
| Delete then undo | undo re-inserts the row from a retained snapshot |

## Dependencies

- Room (measurements + goals DAOs); the shared sync pipeline (Drive snapshot).
- No chart library; a dependency-free `Canvas` line chart.
- No DI framework; manual factories.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — measurements UI.
- `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` — capture.
- `app/src/main/java/com/example/healthjournal/ui/components/GoalSheet.kt` — goals.
- `app/src/main/java/com/example/healthjournal/ui/components/ParamTrendChart.kt` — chart.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — capture state.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModel.kt` — analytics.
- `app/src/main/java/com/example/healthjournal/data/BodyMeasurementRepository.kt` — persistence.
- `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` — goals.
- `app/src/main/java/com/example/healthjournal/data/local/BodyMeasurementEntry.kt` — entity.
- `app/src/main/java/com/example/healthjournal/data/local/GoalEntity.kt` — goal entity.
- `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` — bounds.
- `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` — goal rules.
- `Docs/prd/body-measurements.md` — requirements.
- `Docs/tests/body-measurements.md` — test cases.