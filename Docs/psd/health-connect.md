# Health Connect — Product Specification

> A thin `HealthConnectManager` reads the day's blood pressure, heart rate, and
> sleep; the ViewModel derives the date window and the Add Entry screen drives
> the platform permission flow before calling it.

Last updated: 2026-09-02

## Overview

`HealthConnectManager` wraps the Health Connect SDK's read/aggregate APIs for
three record types. `syncHealthData(timestamp)` computes the day-window (and a
previous-night window for sleep), calls the manager, and returns a
`HealthSyncResult` that the Add Entry screen merges into local state before
saving the entry. Permissions and SDK availability gate the whole flow.

## Architecture

- `HealthConnectManager` owns SDK access: availability, permission checks, and
  the three reads; every read is wrapped so a failure degrades to `null`.
- `JournalViewModel.syncHealthData` derives the UTC day window and converts the
  manager's results into `HealthSyncResult` (bp pair, hr int, sleep hours float).
- `AddEntryScreen` owns the permission interaction via the Health Connect
  permission controller contract, and renders the returned metrics for saving.
- A `PermissionsRationaleActivity` satisfies the platform's rationale/usage
  intent filters.

## Data flow

1. User taps the Health action in the Enrichment panel.
2. Screen checks SDK availability; unavailable → toast and stop.
3. If permissions not yet granted, launch the permission controller with the
   three read permissions; on grant, proceed, on deny, toast guidance.
4. `syncHealthData` runs: builds start/end of the selected day, calls the three
   manager reads (sleep uses a 12h-shifted previous-night window), returns the
   result.
5. The returned vitals populate the entry's metric fields for the eventual save.

## Components

| Component | File | Responsibility |
|---|---|---|
| Health Connect manager | `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` | availability, permission check, BP/HR/sleep reads |
| Journal ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | date-window derivation + `HealthSyncResult` |
| Add Entry screen | `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` | permission launcher + metric display |
| Rationale activity | `app/src/main/java/com/example/healthjournal/PermissionsRationaleActivity.kt` | platform rationale/usage surface |
| Manifest / permissions | `app/src/main/AndroidManifest.xml`, `app/src/main/res/values/health_permissions.xml` | declared read permissions + intent filters |
| Entry columns | `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` | persisted metric fields |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Health Connect unavailable | toast; no permission flow |
| Permissions denied/incomplete | toast guidance; no vitals saved |
| Read fails / no record in range | metric is `null`; entry still saves |
| Sleep window | previous-night window via 12h shift from day start |

## Dependencies

- Health Connect SDK (`connect-client`); the permission controller contract.
- The three read scopes: blood pressure, heart rate, sleep.

## Sources

- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — reads.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — window + result.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — permission flow.
- `app/src/main/java/com/example/healthjournal/PermissionsRationaleActivity.kt` — rationale.
- `app/src/main/AndroidManifest.xml` — permissions/filters.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — metric columns.
- `Docs/prd/health-connect.md` — requirements.
- `Docs/tests/health-connect.md` — test cases.