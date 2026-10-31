# Track history_card_vitals_20261028 — Specification

## Overview

The History entry card currently renders only the blood pressure, average heart rate, and sleep hours that Add Entry imports from Health Connect, in a single non-wrapping row. There is no way to enter a vital by hand. This track refactors the card's metric block into wrapping chips and adds a quick-add bottom sheet for manual blood pressure, blood glucose (the request's "glycemic index"), body temperature, heart rate, and SpO2. The same fields are added to Add/Edit Entry so a value can be corrected later. Manual values are stored on the journal entry and replace Health Connect values for that entry; there is no second source column.

## Context

- `JournalEntry` already stores `bp_systolic`, `bp_diastolic`, `heart_rate_avg`, and `sleep_hours`; those columns are filled by Health Connect (`app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`, `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt`).
- `JournalEntryItem` renders the metrics in one `Row` and has no add/edit action (`app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt`).
- `AddEntryScreen` has no manual vital inputs; the Health action only fills the existing BP/HR/sleep state (`app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt`).
- `JournalRepository.insert` delegates to `JournalDao.insertEntry` with `OnConflictStrategy.REPLACE`; tags live in a separate table and are preserved by an entry-only update (`app/src/main/java/com/example/healthjournal/data/JournalRepository.kt`, `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt`).
- Room schema is version 20 after the warm-up track; the newest migration is `MIGRATION_19_20` (`app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt`).
- Journal sync and full backup serialize the whole `JournalEntry` with Gson, so new nullable fields are backward compatible (`app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt`, `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt`).
- The PDF export prints a metrics line only when `bp_systolic` is present (`app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt`).
- The global unit preference is `UnitSettings` / `UnitSystem`; display conversion lives in `UnitConverter` (`app/src/main/java/com/example/healthjournal/data/local/UnitSettings.kt`, `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt`).
- UI tests share `FakeJournalViewModel`; the interface is `IJournalViewModel` (`app/src/androidTest/java/com/example/healthjournal/util/FakeJournalViewModel.kt`, `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt`).

## Decisions

- "Glycemic index" is interpreted as a **blood glucose reading**, per user clarification. The UI label is Blood glucose / Глюкоза, not glycemic index.
- Manual values replace synced values in the same entry columns. There is no source badge and no second value.
- Body temperature, manual heart rate, and SpO2 are included. Weight, steps, respiratory rate, and manual sleep are not.
- Storage is canonical metric; the global metric/imperial preference controls display.

## Functional Requirements

### FR1 — Data model

Add nullable `blood_glucose_mmol REAL`, `body_temperature_c REAL`, and `spo2_percent INTEGER` columns to `journal_entries`, with matching nullable properties on `JournalEntry` defaulting to null. Reuse the existing `bp_systolic`, `bp_diastolic`, and `heart_rate_avg` columns. All vitals remain optional.

### FR2 — Card display

Replace the single metric `Row` in `JournalEntryItem` with a wrapping `FlowRow` of compact chips, rendered in this order when present:

| Metric | Stored column | Display |
|---|---|---|
| Blood pressure | `bp_systolic`, `bp_diastolic` | `120/80` + mmHg |
| Heart rate | `heart_rate_avg` | integer + bpm |
| Blood glucose | `blood_glucose_mmol` | 1 decimal + mmol/L (metric) or integer + mg/dL (imperial) |
| Body temperature | `body_temperature_c` | 1 decimal + °C (metric) or °F (imperial) |
| SpO2 | `spo2_percent` | integer + % |
| Sleep | `sleep_hours` | 1 decimal + h |

Each chip carries a localized content description and a stable test tag (`entry_vitals_bp`, `entry_vitals_hr`, `entry_vitals_glucose`, `entry_vitals_temp`, `entry_vitals_spo2`, `entry_vitals_sleep`). The wrapping row gets `entry_vitals_row`. No chip renders for a null value. The card stays clickable to open the entry.

### FR3 — History quick-add sheet

Every non-archived History card shows a compact action: `Add vitals` when none of the five editable vitals exist, `Edit vitals` when at least one exists (`entry_vitals_action`). Tapping it opens a modal bottom sheet for that entry with fields for systolic, diastolic, heart rate, blood glucose, body temperature, and SpO2, prefilled in display units. Save validates and writes only that entry's five editable vitals; Cancel dismisses without writing. Blank fields clear that vital; saving all blanks removes the five editable vitals but leaves a synced sleep value untouched. When the entry has no editable vitals and every field is blank, Save is disabled. Archived cards (used by the Archive screen) render chips but never the action and stay read-only.

### FR4 — Add/Edit Entry fields

Add a `Health metrics` section to Add/Edit Entry with editable systolic, diastolic, heart rate, glucose, temperature, and SpO2 fields, plus a read-only sleep value when present. Loading an entry prefills the fields; create and update both persist them. The existing Health Connect action continues to fill BP, heart rate, and sleep after permission and does not clear manual glucose, temperature, or SpO2. Read-only archived entries show the values without edit controls or the save button.

### FR5 — Units and formatting

Canonical storage is mmHg, bpm, mmol/L, °C, and percent. Display follows `UnitSystem`: metric shows mmol/L and °C; imperial shows mg/dL and °F. BP, heart rate, and SpO2 are unit-invariant. Input accepts both `.` and `,` decimal separators and is sanitized to the field's precision. Unit suffixes appear in field labels and card chips. The card label for `heart_rate_avg` is `HR` / `Пульс` because a manual value is a point reading, not necessarily an average.

### FR6 — Validation

A pure `ValidateVitals` returns per-field error codes; the UI resolves localized messages. Rules:

- Blood pressure: both fields blank or both present; systolic 50–300 mmHg; diastolic 30–200 mmHg; systolic must exceed diastolic.
- Heart rate: integer 20–300 bpm.
- Blood glucose: 1.0–50.0 mmol/L canonical (display-unit input is parsed to canonical first, then checked against the canonical bounds; messages show the display-unit equivalent).
- Body temperature: 30.0–45.0 °C canonical (display-unit input is parsed to canonical first, then checked against the canonical bounds; messages show the display-unit equivalent).
- SpO2: integer 0–100%.
- Malformed or non-numeric input produces an inline format error.
- Save is blocked while any error exists. Every field is optional.

### FR7 — Persistence and sync

Add `IJournalViewModel.updateVitals(entryId, vitals)`. It loads the entry, copies the vitals, sets `lastModified = now`, `isSynced = false`, and `syncStatus = PENDING_SYNC`, then inserts it. It preserves the description, timestamp, tags, photos, attachments, and archive state, and enqueues periodic sync when signed in. `AddEntryScreen` passes the same values through `addEntry` / `updateEntry`; the existing `updateEntry` path also marks the entry `isSynced = false` so the card reflects a pending change.

### FR8 — Backup, restore, and export

The new nullable fields are Gson-compatible; no sync payload version change. Old cloud payloads and full backups deserialize with nulls, and new values round-trip through full backup/restore. PDF export includes BP, heart rate, glucose, temperature, SpO2, and sleep when present, and its metrics line renders when any vital exists (not only when BP exists).

### FR9 — Localization

All new labels, units, sheet copy, content descriptions, and validation messages have English and Russian strings. Clinical values stay numeric.

### FR10 — Documentation

Add a `manual-vitals` feature to `Docs/index.md` with `Docs/prd/manual-vitals.md`, `Docs/psd/manual-vitals.md`, and `Docs/tests/manual-vitals.md`. Update `Docs/prd|psd|tests/history-feed.md` for the card chips/action and `Docs/prd|psd|tests/entry-logging.md` for the entry fields. Update cited wiki pages and run `python3 wiki/lint.py` to exit 0.

## Non-Functional Requirements

- No new libraries or tech-stack changes.
- Room migration 20→21 is transactional, adds nullable columns with `ALTER TABLE`, preserves existing rows, and is covered by a real migration test and exported schema.
- Card rendering adds no database query: chips use the entry already emitted by the feed.
- The quick-add sheet keeps draft state locally; no write happens until Save.
- Validation is pure JVM-testable and returns codes, not hardcoded English strings.
- New code targets >80% coverage; JVM and Compose UI tests cover the card, sheet, and editor flows.
- Offline-first; no network dependency.

## Acceptance Criteria

- AC1: A History card shows wrapping chips for whichever of BP, HR, glucose, temperature, SpO2, and sleep are present, with units in the selected unit system.
- AC2: The card action opens a prefilled sheet; Save updates the card without a manual reload and persists the values; Cancel leaves the entry unchanged.
- AC3: Add/Edit Entry saves and reloads all five editable vitals; the Health Connect action still fills BP/HR/sleep.
- AC4: Invalid BP pairs, out-of-range values, and malformed numbers show inline localized errors and block Save.
- AC5: A manual value replaces a synced value; clearing fields removes the corresponding chips.
- AC6: Archived cards show values without the quick-add action and remain read-only.
- AC7: Migration 20→21 preserves old entries; old backups restore with null vitals; new fields round-trip through backup and PDF export.
- AC8: English and Russian strings are present; JVM suite, `./gradlew build`, touched instrumented suites, and `python3 wiki/lint.py` are green.

## Out of Scope

- Vitals time-series, trend charts, goals, or a separate vitals table.
- Manual weight, steps, respiratory rate, or manual sleep entry.
- Health Connect reads/writes for glucose, temperature, or SpO2.
- Per-entry source badges, dual synced/manual values, or conflict UI.
- Per-metric unit overrides beyond the global metric/imperial preference.
- Changes to workout, body-measurement, or personal-card analytics.
