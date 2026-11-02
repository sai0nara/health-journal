# Track history_card_vitals_20261028 — Implementation Plan

## Technical Notes

- New nullable columns on `journal_entries`: `blood_glucose_mmol REAL`, `body_temperature_c REAL`, `spo2_percent INTEGER`. BP, HR (`heart_rate_avg`), and sleep reuse existing columns.
- Schema 20 → 21 via `ALTER TABLE ... ADD COLUMN`; bump `CURRENT_SCHEMA_VERSION` and add `MIGRATION_20_21` to the Room builder. Export the schema JSON.
- Canonical storage: mmHg, bpm, mmol/L, °C, percent. Display follows `UnitSystem` (metric mmol/L + °C; imperial mg/dL + °F).
- `EntryVitals` carries the five editable values. `ValidateVitals` returns sealed per-field `VitalsError` codes so validation stays pure; the UI maps them to localized strings when the sheet and editor land (Phases 2–4).
- `IJournalViewModel.updateVitals(entryId, vitals)` updates only the vitals, marks the row `PENDING_SYNC` / `isSynced = false`, and preserves description, timestamp, tags, media, and archive state.
- New `VitalsEntrySheet` follows the existing `MeasurementEntrySheet` Material 3 pattern. The History screen owns the selected entry; `JournalEntryItem` only reports the action.
- `JournalEntryItem` replaces the single metric row with a `FlowRow` of chips and always renders the action for non-archived entries.
- Gson round-trips the whole `JournalEntry`; no sync payload or backup format change. PDF export gets a metrics line that renders when any vital exists.
- Docs: add the `manual-vitals` feature (PRD/PSD/tests), then update `history-feed` and `entry-logging`.

## Phase 1: Data, validation, and persistence

- [x] Task: Write failing domain tests for vitals validation and units (5591fe6; red→green)
    - [x] Add `app/src/test/java/com/example/healthjournal/domain/ValidateVitalsTest.kt`: BP pair requirement, systolic > diastolic, BP/HR/glucose/temperature/SpO2 bounds, blank fields, comma decimals, malformed input, metric and imperial display units
    - [x] Extend `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` for glucose mmol/L ↔ mg/dL and °C ↔ °F, including round-trip rounding
    - [x] Run `./gradlew :app:testDebugUnitTest --tests '*ValidateVitalsTest' --tests '*UnitConverterTest'` with `JAVA_HOME` on JDK 21 and confirm the new cases fail (Red)
- [x] Task: Implement vitals validation and unit conversion (5591fe6; green)
    - [x] Add `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` with `VitalField`, `EntryVitals`, `VitalsError`, `validate(...)`, and display-unit parse/format helpers
    - [x] Add glucose and temperature conversions to `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt`
    - [x] Validation strings deferred: `ValidateVitals` returns sealed `VitalsError` codes; the `@StringRes` mapping and EN/RU copy ship with the sheet/editor in Phases 2–4
    - [x] Rerun the targeted tests until green
- [x] Task: Write failing persistence and compatibility tests (5591fe6; red→green)
    - [x] Real round-trip belongs to `JournalDaoTest` (the repository test is mock-based): `writeEntryWithVitals_roundTripsCanonicalValues`
    - [x] Add `migrate20To21_AddsVitalsColumnsAndPreservesRows` to `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt`
    - [x] Add Gson backward-compatibility cases (legacy JSON → nulls; new values round-trip) to `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt`
    - [x] Run the targeted JVM tests and confirm they fail (Red); ran the instrumented suites on device `RFCT719Y8BP`
- [x] Task: Implement schema and persistence (5591fe6; green)
    - [x] Add the three nullable fields to `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`
    - [x] Add `MIGRATION_20_21`, bump the database version to 21, and register the migration in `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt`; keep `CURRENT_SCHEMA_VERSION` in sync
    - [x] Confirm the exported schema JSON is generated (`app/schemas/.../21.json`) and no destructive fallback is introduced
    - [x] Rerun the persistence tests until green (`MigrationTest` 13/13, `JournalDaoTest` 15/15)
- [x] Task: Write failing ViewModel tests for vitals updates (5591fe6; red→green)
    - [x] Extend `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt`: `updateVitals` writes the values, bumps `lastModified`, sets `isSynced = false` / `PENDING_SYNC`, preserves description/timestamp/tags/media/archive state, and enqueues sync when signed in
    - [x] Cover `addEntry` with glucose/temperature/SpO2 and `updateEntry` setting `isSynced = false`
    - [x] Update `app/src/androidTest/java/com/example/healthjournal/util/FakeJournalViewModel.kt` with the new interface members so compilation fails for the right reason
    - [x] Run the targeted ViewModel tests and confirm they fail (Red)
- [x] Task: Implement ViewModel vitals update and create/update wiring (5591fe6; green)
    - [x] Add `EntryVitals` and `updateVitals(...)` to `IJournalViewModel` and `JournalViewModel`
    - [x] Extend `addEntry` with glucose/temperature/SpO2 parameters (defaults keep existing call sites source-compatible)
    - [x] Mark `updateEntry` entries `isSynced = false` alongside `PENDING_SYNC`, and keep the sync enqueue behavior
    - [x] Rerun the targeted tests until green; full JVM suite 702/702
- [x] Task: Conductor - User Manual Verification 'Phase 1: Data, validation, and persistence' (Protocol in workflow.md)

## Phase 2: History card and quick-add sheet

- [x] Task: Write failing card and sheet tests (5591fe6; red→green on device `RFCT719Y8BP`)
    - [x] Extend `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt`: vitals chips and units, no chip for nulls, `entry_vitals_row`, action label `Add vitals` / `Edit vitals`, action hidden for archived entries
    - [x] Add `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsEntrySheetTest.kt`: prefill, inline validation, Save callback with parsed canonical values, Cancel leaves data unchanged, all-blank clearing, Save disabled for an empty untouched entry
    - [x] Extend `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt`: tapping `entry_vitals_action` opens the sheet, saving updates the card, cancel does not
    - [x] Run the targeted instrumented classes on a device/emulator and confirm they fail (Red)
- [x] Task: Implement the vitals sheet and card refactor (5591fe6; green)
    - [x] Add `app/src/main/java/com/example/healthjournal/ui/components/VitalsEntrySheet.kt` (fields, unit labels, inline errors, Save/Cancel)
    - [x] Refactor `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt`: `FlowRow` chips with units and content descriptions, action button, no action when `entry.isArchived == true`
    - [x] Wire the sheet state and `viewModel.updateVitals(...)` in `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`; keep `ArchiveScreen` read-only and pass display units to both cards
    - [x] Rerun the targeted instrumented classes until green: card 9/9, sheet 9/9, History 12/12; Archive 3/3 and HistoryFeedDeletionP0 9/9 unaffected
- [x] Task: Conductor - User Manual Verification 'Phase 2: History card and quick-add sheet' (Protocol in workflow.md)

## Phase 3: Add/Edit Entry manual fields

- [x] Task: Write failing Add/Edit Entry tests (5591fe6; red→green on device `RFCT719Y8BP`)
    - [x] Extend `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt`: fields render, edit prefills, create/update persists, validation errors block Save, Health Connect fills BP/HR/sleep without clearing manual glucose/temperature/SpO2, archived entries are read-only
    - [x] Add a focused JVM test for the screen's unit-aware initial formatting/parsing if the logic is extracted into a testable helper (not needed: the screen reuses the already-covered `ValidateVitals.formatVitals`/`toEntryVitals`)
    - [x] Run the targeted instrumented class and confirm it fails (Red: 7 new cases failed, 10 existing passed)
- [x] Task: Implement the Health metrics section (5591fe6; green)
    - [x] Extract the shared `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` and make `VitalsEntrySheet` reuse it
    - [x] Add editable BP/HR/glucose/temperature/SpO2 state and fields to `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt`, with unit-aware labels and inline errors
    - [x] Keep the Health Connect action and its BP/HR/sleep fill behavior; preserve manual glucose/temperature/SpO2
    - [x] Wire the fields into `addEntry` / `updateEntry` and the read-only archived rendering
    - [x] Rerun the targeted instrumented class until green (17/17); sheet 9/9, card 9/9, History 12/12 unaffected
- [x] Task: Conductor - User Manual Verification 'Phase 3: Add/Edit Entry manual fields' (Protocol in workflow.md)

## Phase 4: Localization, export, docs, and verification

- [x] Task: Add English and Russian strings (5591fe6; green)
    - [x] Add labels, units, sheet copy, content descriptions, and validation messages to `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ru/strings.xml` (catalog parity verified: no missing/extra keys)
    - [x] Add the Russian instrumentation coverage used by the existing localization suite (vitals labels and validation): `app/src/androidTest/java/com/example/healthjournal/localization/VitalsLocalizationTest.kt`, 3/3
- [x] Task: Extend export (5591fe6; green)
    - [x] Update `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` so the metrics line renders when any vital exists and includes BP, HR, glucose, temperature, SpO2, and sleep
    - [x] Confirm full backup/restore needs no codec change and cover the new fields in the existing round-trip test (`BackupDataReaderTest` legacy-null and new-value round-trip)
- [x] Task: Write product docs (5591fe6)
    - [x] Add `Docs/prd/manual-vitals.md`, `Docs/psd/manual-vitals.md`, and `Docs/tests/manual-vitals.md`
    - [x] Add the feature to `Docs/index.md`
    - [x] Update `Docs/prd|psd|tests/history-feed.md` for the chips/action and `Docs/prd|psd|tests/entry-logging.md` for the manual fields
- [x] Task: Update the wiki (5591fe6)
    - [x] Update pages citing `JournalEntryItem.kt`, `HistoryScreen.kt`, `AddEntryScreen.kt`, `JournalViewModel.kt`, `JournalEntry.kt`, `JournalDatabase.kt`, and `PdfExportUseCase.kt` (`data-layer`, `ui-layer`, `viewmodel-layer`, `integrations/health-connect`)
    - [x] Run `python3 wiki/lint.py` and resolve findings until it exits 0 (new files marked intent-to-add so the `UNTRACKED` check passes; only advisory `REVIEW` findings remain)
- [x] Task: Full verification (automated)
    - [x] `./gradlew :app:testDebugUnitTest` green on JDK 21 (702/702)
    - [x] `./gradlew build` green
    - [x] Touched instrumented suites green on device `RFCT719Y8BP`: AddEntry 17/17, sheet 9/9, card 9/9, History 12/12, Archive 3/3, Migration 13/13, DAO 15/15, P0 deletion 9/9, VitalsLocalization 3/3, CyrillicSpacing 2/2, TagEditRemoval 1/1
    - [x] Manual device pass on `RFCT719Y8BP` (real install with 93 pre-existing entries): DB migrated 20→21 in place with rows preserved; card action opens the prefillable sheet; save renders five chips + `Edit vitals` + `(Edited)` + pending-sync badge; blank-draft Save is a no-op; metric→imperial switch re-renders chips as mg/dL/°F and the sheet prefills/round-trips them (100 mg/dL saved as 5.55 mmol/L, 36.6 °C unchanged); clear-all removes the values and the action reverts to `Add vitals`; archived card shows chips with no action and the archived entry opens read-only; generated PDF contains `BP … | HR … | Glucose … | Temp … | SpO2 …`. Found and fixed one copy gap: `settings_units_hint` omitted entry vitals.
- [x] Task: Conductor - User Manual Verification 'Phase 4: Localization, export, docs, and verification' (Protocol in workflow.md)

## Phase: Review Fixes

- [x] Task: Apply review suggestions (5591fe6)
    - [x] Render sleep read-only in the editable Add/Edit Health metrics card (`AddEntryScreen.kt`) with instrumented coverage
    - [x] Isolate `AddEntryScreenTest` unit preferences and make `HistoryScreenTest` assert a real imperial conversion
    - [x] Extend `updateVitals` unit coverage for preserved sleep and sync enqueue
    - [x] Align archived BP/HR labels with the spec (`BP`, `HR`, `АД`, `Пульс`)
    - [x] Full verification after review fixes: JVM 702/702, build green, wiki lint exit 0 (REVIEW only), AddEntry 18/18, History 12/12, sheet 9/9, card 9/9, VitalsLocalization 3/3
