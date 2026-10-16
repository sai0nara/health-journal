# Move Add Measurement Button from History Screen — Test Cases

> Maps the PRD's acceptance criteria and the PSD's edge cases to concrete
> verification. The "Automated coverage" table cites the real test files; keep
> it in sync when coverage moves.

Last updated: 2026-10-16

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | single add-entry FAB, measurement action absent |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` | FAB visible empty + populated, tap opens capture sheet |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` | sheet driven through the relocated FAB: render, save, validation, units |

No new JVM unit tests: the relocation adds no business logic, and the
existing `:app:testDebugUnitTest` suite guards the untouched validation,
conversion, and repository layers against regressions.

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | AC-1 History has one FAB | History with entries | open History | "Add Entry" FAB exists and displays; "Add body measurements" node absent |
| T-2 | AC-2 FAB on empty Measurements | no measurements | open Measurements | FAB exists and displays |
| T-3 | AC-2 FAB opens sheet | no measurements | tap FAB | waist field and save button appear |
| T-4 | AC-2 FAB with populated list | one saved entry | open Measurements | FAB exists and displays above the list |
| T-5 | AC-3 save from relocated sheet | sheet open | enter waist only, save | repository insert with waist value; sheet dismissed; entry listed |
| T-6 | AC-4 invalid input still blocked | sheet open | enter malformed weight | inline "Invalid decimal format", save disabled, typed value retained |
| T-7 | AC-4 future date blocked | future timestamp preset | open sheet | future-date alert renders, save disabled |
| T-8 | AC-3 imperial entry converts | imperial preference | enter weight in lb, save | metric kg persisted; sheet dismissed |
| T-9 | AC-5 end-to-end path | History | tap chart icon, tap FAB, save valid set | measurement saved without a History FAB |

## Manual checks

- On a real device: screen-reader focus order reaches the relocated FAB and
  announces its label; keyboard navigation can activate it.
- On an emulator: run the three instrumented classes until green, then shut
  the emulator down (`adb emu kill`).

## Cross-references

- `Docs/prd/move-add-measurement-button.md` — the requirements under test.
- `Docs/psd/move-add-measurement-button.md` — the design the cases verify.
- [[ui-layer]] — the History and Measurements screens.

## Sources

- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — History coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` — Measurements FAB coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` — sheet coverage.
- `Docs/prd/move-add-measurement-button.md` — requirements under test.
- `Docs/psd/move-add-measurement-button.md` — design the cases verify.
