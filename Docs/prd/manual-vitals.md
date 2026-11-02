# Manual Vitals — Product Requirements

> Record blood pressure, heart rate, blood glucose, body temperature, and SpO2
> directly on a journal entry — from the History card or the Add/Edit Entry
> screen — with or without Health Connect.

Last updated: 2026-10-30

## Overview

An entry can carry five editable vitals: blood pressure (systolic/diastolic),
heart rate, blood glucose, body temperature, and SpO2. Sleep remains a
display-only value supplied by Health Connect. The user can add or edit the five
values from a quick-add sheet on the History card, or inline on the Add/Edit
Entry screen; both paths write the same canonical-metric columns and mark the
entry pending sync. Manual input is not a second source of truth: it occupies the
same columns as synced data, so a saved manual value replaces a synced one.

## Goals / Non-goals

**Goals**

- Capture the five vitals manually on any non-archived entry.
- Show each present value as a unit-aware chip on the History and Archive cards.
- Offer quick add/edit from the card without opening the full editor.
- Show the same fields inline in Add/Edit Entry, with inline validation.
- Store canonical metric values and display them in the app's unit system.

**Non-goals**

- A separate vitals entity, trend chart, or vitals history screen.
- Source badges or side-by-side manual-vs-synced values.
- Manual sleep entry (sleep stays display-only).
- Writing manual values back to Health Connect.

## User stories

- As a user without a wearable, I want to type my blood pressure on an entry so
  my journal still records it.
- As a user reading the feed, I want to see the vitals I recorded without
  opening the entry.
- As a user correcting a value, I want to edit it from the card in one tap.
- As an imperial-units user, I want glucose in mg/dL and temperature in °F while
  the app stores metric values.

## Functional requirements

- FR-1: An entry stores optional systolic, diastolic, heart rate, blood glucose,
  body temperature, and SpO2 values.
- FR-2: The History and Archive cards render one chip per present value with its
  unit; sleep renders as a chip when present.
- FR-3: A non-archived card shows `Add vitals` when no editable vital exists and
  `Edit vitals` otherwise; tapping it opens a sheet prefilled with current values.
- FR-4: Saving the sheet replaces all five values; clearing every field removes
  them from the entry. Cancel writes nothing.
- FR-5: Add/Edit Entry exposes the same five fields inline, shows a read-only
  sleep value when present, and blocks Save while any field is invalid.
- FR-6: Validation enforces a systolic/diastolic pair, systolic greater than
  diastolic, whole numbers for heart rate and SpO2, and plausible ranges for
  every field.
- FR-7: A manual save replaces synced values in the same columns. Importing from
  Health Connect fills blood pressure, heart rate, and sleep without clearing
  manual glucose, temperature, or SpO2.
- FR-8: Display follows the app unit system (mmol/L and °C, or mg/dL and °F);
  storage stays metric.
- FR-9: Archived entries render the values read-only and offer no vitals action.
- FR-10: Any vitals write marks the entry pending sync.
- FR-11: The PDF export lists every present vital for the entry.

## Non-functional requirements

- Manual entry is offline-first and needs no additional permissions.
- Validation is pure and maps typed errors to localized messages.
- Adding the columns is a forward-only Room migration; existing entries read as
  null vitals and older cloud/backup payloads remain readable.

## Acceptance criteria

- AC-1: An entry with all five vitals renders BP, heart rate, glucose,
  temperature, and SpO2 chips with units on the History card.
- AC-2: The card action opens the sheet prefilled; Save persists the values and
  the card updates without a reload; Cancel changes nothing.
- AC-3: Add/Edit Entry prefills stored values, saves new values on create and
  update, and blocks Save on invalid input with an inline message.
- AC-4: Switching the unit system converts glucose and temperature labels,
  values, bounds, and input parsing; blood pressure and heart rate are unchanged.
- AC-5: An archived entry shows the values but no edit action and read-only
  fields on the entry screen.
- AC-6: Health Connect import fills BP/HR/sleep while preserving manually entered
  glucose, temperature, and SpO2.
- AC-7: A vitals write sets the entry pending sync.

## Out of scope

- Health Connect specifics (see `Docs/prd/health-connect.md`).
- Card layout and feed behavior beyond the vitals chips (see
  `Docs/prd/history-feed.md`).
- Capture fields unrelated to vitals (see `Docs/prd/entry-logging.md`).

## Cross-references

- `Docs/psd/manual-vitals.md` — the specification that implements these requirements.
- `Docs/tests/manual-vitals.md` — the test cases that verify them.
- `Docs/psd/unit-conversion.md` — the shared display-unit system.
- [[data-layer]] — the entry schema and migration.
- [[ui-layer]] — the card, sheet, and entry screen.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` — validation, parsing, and display formatting of the five vitals.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` — shared field editor used by the sheet and the entry screen.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsEntrySheet.kt` — card quick-add/edit sheet.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — entry-card vitals chips and action.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — inline manual vitals and read-only archived rendering.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — vitals persistence and pending-sync marking.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — the persisted vitals columns.
- `Docs/psd/manual-vitals.md` — specification.
- `Docs/tests/manual-vitals.md` — test cases.
