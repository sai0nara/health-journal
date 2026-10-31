# Manual Vitals — Test Cases

> Maps manual-vitals requirements to the pure validation/unit tests, the
> persistence and migration tests, and the instrumented card, sheet, and entry
> screen suites.

Last updated: 2026-10-30

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ValidateVitalsTest.kt` | BP pair/order, field ranges, integer fields, comma decimals, display-unit parse/format, canonical conversion |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` | glucose mmol/L ↔ mg/dL and °C ↔ °F round-trips |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | `updateVitals` writes/marks dirty/preserves other fields, create/update carry the new values |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` | legacy payload → null vitals; new values round-trip |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | schema migration adds the columns and preserves rows |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` | canonical vitals round-trip through Room |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` | chips, units, metric/imperial rendering, action labels, archived action hidden |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsEntrySheetTest.kt` | prefill (metric/imperial), validation messages, Save payload, Cancel, clearing all |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | card action opens the sheet, save updates the card, cancel writes nothing |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` | fields render, edit prefill, create/update persistence, invalid BP blocks Save, health import preserves manual fields, archived read-only |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/localization/VitalsLocalizationTest.kt` | Russian labels/units/errors and English fallback |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | FR-6 | BP pair | only systolic entered | pair error on diastolic, Save disabled |
| T-2 | FR-6 | BP ordering | systolic ≤ diastolic | systolic error, Save disabled |
| T-3 | FR-6 | Range/integer checks | malformed, fractional, or out-of-range input | typed error per field; Save disabled |
| T-4 | FR-8 | Metric display | stored 5.4 mmol/L, 36.6 °C | fields/formatted values show mmol/L and °C |
| T-5 | FR-8 | Imperial display | stored metric values, imperial preference | glucose shown in mg/dL, temperature in °F, bounds converted |
| T-6 | FR-4 | Sheet save | sheet opened from card, values entered, Save | canonical `EntryVitals` callback; card chips update |
| T-7 | FR-4 | Sheet cancel | values changed, Cancel | no callback, card unchanged |
| T-8 | FR-4 | Clear all | entry with values, every field cleared, Save | empty `EntryVitals` (values removed) |
| T-9 | AC-5 | Archived card | archived entry with vitals | chips render, no `Add`/`Edit vitals` action |
| T-10 | FR-2 | Null chips | entry with only BP | only the BP chip renders; sleep-only entry renders sleep and still offers `Add vitals` |
| T-11 | FR-3 | Create entry | Add Entry, description + five values, Save | `addEntry` receives canonical values |
| T-12 | FR-5 | Edit entry | existing entry with sleep, change glucose, update | sleep renders read-only; saved entry carries the new canonical glucose |
| T-13 | FR-7 | Health import merge | manual glucose/temperature/SpO2 present, import returns BP/HR/sleep | BP/HR fields fill; manual values remain |
| T-14 | FR-9 | Archived entry screen | archived entry with vitals | values render read-only, no fields, no Save |
| T-15 | FR-10 | Dirty marking | `updateVitals` on a synced entry | `PENDING_SYNC`, `isSynced = false`, `lastModified` bumped, other fields preserved |
| T-16 | FR-1 | Migration | database at the prior schema with rows | rows preserved, new columns read null |
| T-17 | FR-11 | Export | entry with all vitals | export line contains BP, HR, glucose, temperature, SpO2, sleep |

## Manual checks

- Physical layout of the six-field editor on a narrow phone with the keyboard open.
- Unit toggle from Settings reformats an existing card, sheet, and entry screen.
- Backup/restore round-trip on a device after entering manual vitals.
- PDF output renders the metrics line legibly.

## Cross-references

- `Docs/prd/manual-vitals.md` — requirements under test.
- `Docs/psd/manual-vitals.md` — design the cases verify.
- `Docs/tests/unit-conversion.md` — shared conversion coverage.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/domain/ValidateVitalsTest.kt` — validation/conversion unit coverage.
- `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` — glucose/temperature conversions.
- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — vitals persistence and dirty marking.
- `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` — payload compatibility.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — schema migration.
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — Room round-trip.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` — card chips/action.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsEntrySheetTest.kt` — quick-add sheet.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — card→sheet flow.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` — inline fields and archived mode.
- `app/src/androidTest/java/com/example/healthjournal/localization/VitalsLocalizationTest.kt` — Russian copy.
- `Docs/prd/manual-vitals.md` — requirements.
- `Docs/psd/manual-vitals.md` — design.
