# Unit Conversion — Product Specification

> Display-layer conversion: every measurement renders and parses in the
> preferred unit system through the shared converter, while storage stays
> canonical metric with no schema or sync changes.

Last updated: 2026-09-22

## Overview

The design rejected per-record unit columns and storage migration in favour
of converting at the display boundary. The single global preference backs all
surfaces; each ViewModel holds display-unit state and parses input back to
metric before validation and persistence. Validation runs in display units,
mirroring the personal-card pattern, and toggling mid-draft re-renders drafts
through the converter without loss. Journal vitals needed no production
change: the entry columns are unit-invariant. See `Docs/prd/unit-conversion.md`
for the requirements.

## Architecture

- MVVM without DI framework: manually constructed ViewModels/factories.
- Unit state lives in ViewModel `StateFlow` (`BodyMeasurementUiState`,
  `PersonalCardUiState`); screens read the global preference and sync it into
  the ViewModel via `LaunchedEffect`, so a toggle re-renders without losing
  drafts.
- The shared converter owns every factor and rounding rule; no surface keeps
  its own. Field-kind-aware format/parse helpers separate weight (kg↔lb) from
  length (cm↔in); ft+in helpers back the split imperial height entry.
- Validation overloads parse display-unit text to metric first, then apply the
  existing metric caps with display-unit messages; existing single-arg
  validators delegate with metric so prior behaviour is unchanged.
- Seams for testing: pure converter/validator functions (JVM unit), ViewModel
  state with injectable dispatchers (JVM), per-screen Compose tests with the
  preference seeded via `UnitSettings` (instrumented, run in isolation).

## Data flow

1. Screen reads the global preference and syncs it into the ViewModel; draft
   texts convert (parse old system, format new system), blanks staying blank.
2. Entry fields render stored metric via the field-kind helpers with
   unit-suffixed labels; imperial height splits into ft+in fields.
3. Each keystroke parses display-unit text back to metric and validates
   against the metric caps with display-unit messages; save persists metric.
4. Charts project the metric series and convert points, goal line, header,
   and unit labels at render time; the goal editor prefills converted and
   saves parsed metric.
5. Routine set rows, routine headers, tonnage, the manual editor, and the
   preset editor render converted values and parse edits back to kg.
6. Personal-card read-only rows render converted values on launch (the saved
   preference seeds initial ViewModel state); journal vitals render unchanged
   in both systems.

## Components

| Component | File | Responsibility |
|---|---|---|
| Shared converter | `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` | factors, rounding, ft/in + field-kind format/parse |
| Unit preference | `app/src/main/java/com/example/healthjournal/data/local/UnitSettings.kt` | global preference backing |
| Unit values | `app/src/main/java/com/example/healthjournal/data/local/UnitSystem.kt` | metric/imperial values |
| Capture validation | `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` | unit-aware bounds + messages |
| Goal validation | `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` | unit-aware goal rules + labels |
| Measurement entry state | `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` | draft units, toggle conversion, metric save |
| Measurements screen | `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` | converted cards, charts, goal wiring |
| Capture sheet | `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` | unit-suffixed field labels |
| Goal sheet | `app/src/main/java/com/example/healthjournal/ui/components/GoalSheet.kt` | converted prefill/labels, metric save |
| Routine runner | `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` | converted rows/headers/tonnage/manual editor |
| Preset editor | `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` | converted default-weight field |
| Personal card state | `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` | ft/in entry state, preference load |
| Personal card screen | `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` | converted rows, split height entry |
| Preference wiring | `app/src/main/java/com/example/healthjournal/MainActivity.kt` | seeds saved preference into card state |
| Unit strings | `app/src/main/res/values/strings.xml` | in/lbs display formats |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Unit toggle mid-draft | texts re-render via parse-then-format; blanks stay blank; stored values untouched |
| Partial ft/in entry | height stays unset (optional-field semantics); no crash |
| Blank/malformed input | blank skipped or required-error (goals); malformed surfaces inline format error |
| Display round trip | metric→imperial→metric stays within display rounding; never crashes on missing values |
| Preference unset/corrupt | global read falls back to metric |
| Journal vitals | unit-invariant columns render identically; locked by regression test |

## Dependencies

- No new libraries or platform services; Room, Compose Material 3, and the
  existing sync pipeline unchanged.
- No schema migration; no sync/backup payload change.

## Cross-references

- `Docs/prd/unit-conversion.md` — the requirements this specification implements.
- `Docs/tests/unit-conversion.md` — the test cases (added when the test-case doc is written).
- `Docs/prd/body-measurements.md` — the metric-only decision this work reversed.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` — shared conversion contract.
- `app/src/main/java/com/example/healthjournal/data/local/UnitSettings.kt` — global preference.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — entry units state.
- `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` — ft/in entry state.
- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — converted history/charts/goals.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — converted routine surfaces.
- `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` — converted preset field.
- `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` — converted rows and height entry.
- `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` — unit-aware capture rules.
- `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` — unit-aware goal rules.
- `Docs/prd/unit-conversion.md` — requirements this specification implements.
