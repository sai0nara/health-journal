# Unit Conversion — Test Cases

> Maps the PRD's requirements and the PSD's edge cases to concrete
> verification. The "Automated coverage" table cites the real test files; keep
> it in sync when coverage moves. A QA pipeline suite derived from these docs
> holds 52 cases (18 P0); all P0s are automated in the files cited below.

Last updated: 2026-09-22

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` | ft/in split and combine, field-kind format/parse, round-trip rounding |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ValidateMeasurementsTest.kt` | imperial capture validation, display-unit cap messages, malformed/injection guards |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/GoalValidatorTest.kt` | imperial goal validation, metric parse-back, unit labels |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` | imperial entry persistence, draft conversion on toggle, double-submit guard |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` | ft/in entry parsing, malformed/out-of-range guards, split-text seeding, metric restore |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` | imperial entry labels and metric save round trip |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` | imperial history, converted goal line, imperial goal save |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` | imperial set rows, header, tonnage, pad step, invalid-edit guard |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` | imperial preset default render and metric save |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt` | imperial manual weight entry, metric labels and manual editor unchanged |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PersonalCardScreenTest.kt` | converted card rows, ft/in entry save |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | vitals identical under both systems |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Imperial body-measurement entry | imperial preference | enter weight in lb, save | metric kg persisted |
| T-2 | Imperial history render | metric record stored, imperial preference | open measurements | cards show lb/in |
| T-3 | Converted chart and goal | metric goal stored, imperial preference | open tab; edit goal in lb and save | line/label converted; metric target stored |
| T-4 | ft+in height entry | imperial preference, card editor | enter ft/in, save | metric cm persisted; same split redisplays |
| T-5 | Imperial routine and preset weights | imperial preference | run routine; edit preset default | rows/header/tonnage in lb; kg persisted |
| T-6 | Personal-card rows follow preference | saved height/weight | switch to imperial | rows show in/lbs |
| T-7 | Mid-draft toggle preserves values | partial draft typed | flip preference, reopen | values preserved in new units |
| T-8 | Journal vitals invariance | entry with BP/HR/sleep | render under both preferences | identical text both times |
| T-9 | Display-unit validation | imperial preference | over-cap lb/in input | display-unit cap message; save blocked |
| T-10 | Full verification | — | JVM suite, each UI class in isolation, wiki lint | all green, lint exits 0 |
| T-11 | Manual editor imperial weight | imperial preference | enter lb in the manual add-set field, save | metric kg persisted; lb re-rendered |
| T-12 | Routine invalid edit guard | imperial preference, active routine | type malformed weight | never commits; stored row untouched, no crash |
| T-13 | Capture double-submit guard | valid draft entered | tap Save twice rapidly | exactly one row persisted |

## Manual checks

- Settings toggle to imperial re-renders history, entry, charts, goals, routines, presets, and card rows with no mixed units.
- Mid-draft toggle on every touched surface preserves typed values.
- Kill-and-relaunch with imperial set opens directly in imperial.
- Metric mode behaves exactly as before; no crashes on missing or partial values.

## Cross-references

- `Docs/prd/unit-conversion.md` — the requirements under test.
- `Docs/psd/unit-conversion.md` — the design the cases verify.
- `Docs/prd/body-measurements.md` — the reversed metric-only decision.

## Sources

- `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` — converter contract.
- `app/src/test/java/com/example/healthjournal/domain/ValidateMeasurementsTest.kt` — capture rules.
- `app/src/test/java/com/example/healthjournal/domain/GoalValidatorTest.kt` — goal rules.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` — entry state.
- `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` — ft/in entry state.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` — entry UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` — history/chart/goal UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` — routine UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` — preset UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PersonalCardScreenTest.kt` — card UI.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — vitals invariance.
- `Docs/prd/unit-conversion.md` — requirements under test.
- `Docs/psd/unit-conversion.md` — design the cases verify.
