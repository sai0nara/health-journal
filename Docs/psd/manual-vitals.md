# Manual Vitals — Product Specification

> One canonical set of vitals columns on the entry, one shared Compose field
> editor, and two entry points — the card quick-add sheet and the Add/Edit Entry
> screen — with display-unit conversion at the edges only.

Last updated: 2026-10-30

## Overview

The design keeps the journal entry as the single owner of its vitals and treats
manual input as a write to the same columns Health Connect already fills. Draft
text lives in display units, validation runs on that draft, and conversion to
canonical metric happens once on save. The card, the sheet, and the entry screen
all read the stored values back through the same formatter, so a value looks the
same wherever it appears.

## Architecture

- MVVM: the screens own draft `VitalsInput` state; `ValidateVitals` is a pure
  domain object providing validation, canonical conversion, and display
  formatting; `JournalViewModel` owns persistence and sync marking.
- Shared UI: `VitalsFieldsEditor` renders the five fields with unit-aware labels
  and inline errors. `VitalsEntrySheet` wraps it in a Material 3 bottom sheet;
  `AddEntryScreen` embeds it in the Health metrics card and renders sleep there
  read-only when present.
- Storage: nullable columns on the entry row. Blood pressure and heart rate reuse
  the columns Health Connect already filled; glucose, temperature, and SpO2 are
  new columns added by a forward-only migration registered in
  `JournalDatabase.kt`.
- Units: `UnitConverter` converts glucose (mmol/L ↔ mg/dL) and temperature
  (°C ↔ °F); blood pressure and heart rate have one display unit. The global
  `UnitSettings` preference selects the display system.
- Sync: the entry is serialized whole by Gson, so no payload or backup codec
  changes are needed; older payloads deserialize the new fields as null.

## Data flow

1. The History screen reads `UnitSettings` and passes it to `JournalEntryItem`,
   which formats present values with `ValidateVitals.formatVitals` and renders
   one chip per value.
2. Tapping the card action stores the selected entry; `VitalsEntrySheet` seeds a
   `VitalsInput` from the stored entry in display units.
3. Each keystroke replaces the draft; `ValidateVitals.validate` returns a map of
   field errors that drives inline messages and Save enablement.
4. Save calls `ValidateVitals.toEntryVitals` (display units → canonical metric)
   and `JournalViewModel.updateVitals`, which copies only the vitals onto the
   entry, bumps `lastModified`, sets `PENDING_SYNC` / `isSynced = false`, and
   enqueues sync when signed in.
5. Add/Edit Entry seeds the same draft from the loaded entry; on save it converts
   once and passes the values through `addEntry` or the `JournalEntry.copy` used
   by `updateEntry`.
6. Health Connect import patches only the BP/HR fields of the draft and the sleep
   value, leaving glucose, temperature, and SpO2 untouched.
7. PDF export lists every present vital in canonical metric units.

## Components

| Component | File | Responsibility |
|---|---|---|
| Vitals domain | `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` | draft model, typed errors, ranges, canonical conversion, display formatting |
| Unit converter | `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` | glucose and temperature conversions, numeric formatting |
| Entry model | `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` | nullable BP/HR/glucose/temperature/SpO2/sleep columns |
| Database | `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` | forward-only migration adding the new columns |
| Field editor | `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` | five labeled fields, inline errors, entry↔vitals mapping helpers |
| Quick-add sheet | `app/src/main/java/com/example/healthjournal/ui/components/VitalsEntrySheet.kt` | prefill, Save/Cancel, clearing all values |
| Entry card | `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` | unit-aware chips and `Add`/`Edit vitals` action |
| History screen | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | owns sheet state and calls `updateVitals` |
| Archive screen | `app/src/main/java/com/example/healthjournal/ui/screens/ArchiveScreen.kt` | read-only chips with display units |
| Add/Edit entry | `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` | inline editor, validation gating, archived summary |
| Journal ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | `updateVitals`, create/update wiring, pending-sync marking |
| PDF export | `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` | metrics line covering all present vitals |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Only one BP value entered | pair error on the missing side; Save blocked |
| Systolic ≤ diastolic | systolic error; Save blocked |
| Out-of-range or malformed field | typed error mapped to a localized message; Save blocked |
| All fields cleared on an entry with values | save removes the vitals |
| Empty untouched sheet | Save disabled (nothing to write) |
| Archived entry | chips and summary only; no action, no editable fields |
| Health import returns a null field | that draft field is left unchanged |
| Imperial input | parsed and validated in display units, stored metric |
| Legacy entry/backup payload | new columns deserialize as null; chips simply omit them |
| Unit preference changes | next read of `UnitSettings` reformats values and bounds; stored values are untouched |

## Dependencies

- Room (schema migration and nullable columns) and Gson (payload compatibility).
- Jetpack Compose Material 3 (bottom sheet, text fields, chips).
- The app-wide `UnitSettings` preference and `UnitConverter`.

## Cross-references

- `Docs/prd/manual-vitals.md` — the requirements this specification implements.
- `Docs/tests/manual-vitals.md` — the test cases that verify this design.
- `Docs/psd/unit-conversion.md` — the shared display-unit rules.
- `Docs/psd/health-connect.md` — the synced values that share the BP/HR columns.
- [[data-layer]] — the entry schema and migration.
- [[ui-layer]] — the card, sheet, and entry screen.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` — validation and conversion API.
- `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` — glucose/temperature conversions.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — vitals columns.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` — migration registration.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` — shared editor and mapping helpers.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsEntrySheet.kt` — quick-add sheet.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — card chips/action.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — sheet ownership and update call.
- `app/src/main/java/com/example/healthjournal/ui/screens/ArchiveScreen.kt` — read-only rendering.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — inline editor and archived summary.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — vitals persistence.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — export line.
- `Docs/prd/manual-vitals.md` — requirements.
- `Docs/tests/manual-vitals.md` — test cases.
