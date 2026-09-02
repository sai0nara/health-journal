# Health Connect — Product Requirements

> Pull a day's blood pressure, heart rate, and sleep from Google Health Connect
> into a journal entry, with a clear permission flow and a rationale screen.

Last updated: 2026-09-02

## Overview

From the Add Entry screen's Health action, the app reads the selected date's
health metrics from Health Connect and attaches them to the entry: latest
blood pressure for the day, average heart rate, and the previous night's sleep
duration. Access requires Health Connect read permissions, requested through
the platform permission controller, backed by a compatibility rationale screen.

## Goals / Non-goals

**Goals**

- Read blood pressure, heart rate, and sleep from Health Connect for a given
  day and populate the entry's metric columns.
- Request the three read permissions and handle grant/deny with guidance.
- Show a rationale screen for the platform's permission flow.

**Non-goals**

- Writing data back to Health Connect.
- Reading steps (the app moved from steps to blood pressure).
- Body measurements from Health Connect (separate feature; not synced in).

## User stories

- As a user, I want my day's vitals pulled in automatically so I do not type
  them.
- As a user, I want to grant or deny Health permission with clear explanation.

## Functional requirements

- FR-1: Blood pressure is the latest reading in the day (mmHg).
- FR-2: Heart rate is the day's average (BPM).
- FR-3: Sleep is the total for the previous-night window (hours).
- FR-4: The three metrics land on the entry's `bp_systolic`, `bp_diastolic`,
  `heart_rate_avg`, and `sleep_hours` columns.
- FR-5: Health Connect availability is checked; if unavailable, a message shows.
- FR-6: Read permissions are requested via the platform permission controller;
  denial shows guidance.
- FR-7: A rationale activity is registered for the platform's permission flow.

## Non-functional requirements

- Degradation: any read failure returns a null metric, never a crash.
- Privacy: only the three requested read scopes are used.

## Acceptance criteria

- AC-1: With permissions granted, tapping Health fills the entry's vitals for
  the selected date.
- AC-2: A denied/incomplete permission shows guidance and saves no vitals.
- AC-3: Health Connect not available shows a message and does not launch the
  permission flow.

## Out of scope

- Health Connect write-back.
- Body measurements via Health Connect.

## Cross-references

- `Docs/prd/entry-logging.md` — where the vitals are attached.
- [[health-connect]] — the Health Connect integration page.
- [[ui-layer]] — the Add Entry / rationale UI.

## Sources

- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — reading vitals from Health Connect.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — `syncHealthData` + date-window logic.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — permission launcher + metric display.
- `app/src/main/java/com/example/healthjournal/PermissionsRationaleActivity.kt` — rationale screen.
- `app/src/main/AndroidManifest.xml` — health permissions and the rationale intent filters.
- `app/src/main/res/values/health_permissions.xml` — declared permission array.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — the metric columns.
- `Docs/psd/health-connect.md` — specification.
- `Docs/tests/health-connect.md` — test cases.