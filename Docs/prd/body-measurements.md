# Body Measurements — Product Requirements

> Track up to seven body measurements (weight, chest, waist, glute, thigh,
> calf, bicep) with per-parameter trend charts, goals, and deletion undo, synced
> to the cloud like the rest of the health data.

Last updated: 2026-09-21

## Overview

A dedicated measurements screen lets a user log a dated set of body metrics
in their preferred display units (metric or imperial, converted to canonical
metric storage), see a per-parameter trend chart with an optional goal line, and
review a chronological history. Each measurement set is stored locally and
synced to Drive as its own snapshot. Validation enforces non-negative decimals
within realistic bounds and rejects future dates; future-dated and over-bound
inputs are blocked before save.

## Goals / Non-goals

**Goals**

- Log a dated set of body measurements (weight + up to six girths).
- Show a per-parameter trend chart with a goal line.
- Set, adjust, and clear a target goal per parameter.
- Delete a measurement with undo.
- Sync measurements and goals to Drive.
- Render and accept measurements, charts, and goals in the preferred display
  units per `Docs/prd/unit-conversion.md` (storage stays canonical metric).

**Non-goals**

- Height, BMI, or blood pressure in this feature (height/weight in the Personal
  Card demographics are separate).
- In-place editing or dashboard widgets.
- Health Connect write-back of body measurements.

## User stories

- As a user, I want to log my weight and girths so I can follow changes over
  time.
- As a user, I want a trend chart with a goal so I can see progress.
- As a user, I want my measurements and goals available on another device.

## Functional requirements

- FR-1: A speed-dial button opens the measurement entry sheet from the History
  screen.
- FR-2: The sheet captures a timestamp and up to seven fields; partial
  entries are allowed (at least one value required). Fields render in the
  preferred display units and parse back to canonical metric storage.
- FR-3: Input uses a decimal keyboard and inline validation warnings.
- FR-4: Validation blocks negative, malformed, over-bound, and future-dated
  values.
- FR-5: A chronological list shows the measurement history with cloud status,
  and supports delete with undo.
- FR-6: A per-parameter trend chart (auto-scaled, goal line shown) is shown per
  tab.
- FR-7: Goals can be set/cleared per parameter and reflected on the chart.
- FR-8: Measurements and goals sync to Drive via the existing sync pipeline.

### Validation bounds (G1)

Inclusive ranges enforced by
`app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt`
(`MAX_BOUNDS`, `value > maxFor(field)` blocks); boundary values at the cap are
accepted. Capture allows zero; goals require strictly positive input per
`app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt`
(`value <= 0.0` blocks).

| Parameter | Unit | Capture range (inclusive) | Goal range |
|---|---|---|---|
| Weight | kg | [0, 500] | (0, 500] |
| Chest | cm | [0, 200] | (0, 200] |
| Waist | cm | [0, 200] | (0, 200] |
| Glute | cm | [0, 200] | (0, 200] |
| Thigh | cm | [0, 120] | (0, 120] |
| Calf | cm | [0, 75] | (0, 75] |
| Bicep | cm | [0, 75] | (0, 75] |

Step size: none (continuous decimal). Input is free decimal text accepting `.`
or `,` as the separator, parsed to display-unit `Double` and stored as
canonical metric; toggling the unit system re-renders drafts through the
shared converter without loss, and display strips trailing zeros per
`app/src/main/java/com/example/healthjournal/domain/MeasurementFormatters.kt`.
There is no quantized step to normalize to.

### Sync determinism (G8)

- Clock source: `System.currentTimeMillis()` wall-clock millis. Capture stamps
  `timestamp`/`lastModified` at save in
  `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt`;
  goal writes stamp `lastModified` at set in
  `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt`; edits
  re-stamp via `markEntryDirty`.
- Merge key and tie-break per
  `app/src/main/java/com/example/healthjournal/sync/SyncMerge.kt`:
  measurements merge per `entry_id`, goals per `parameterId`; the strictly newer
  `lastModified` wins and cloud wins ties (`local.lastModified > cloud.lastModified`
  keeps local, otherwise cloud). Tombstones remove a copy only when
  `deletedAt >= lastModified` (see `Docs/prd/drive-sync.md` FR-4, AC-2).

### Literal strings, English locale (G10)

Single-locale (en) literals; no per-locale table exists. Date row format is the
only locale-sensitive rendering (`EEE, d MMM yyyy` in the device locale).

| String | Owner |
|---|---|
| `Invalid decimal format` | `ERROR_INVALID_FORMAT` in `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` |
| `Cannot be negative` | `ERROR_NEGATIVE` in `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` |
| `Too large (max 500 kg)` for weight; `Too large (max {200\|120\|75} cm)` for girths (metric; imperial shows converted bounds with `lb`/`in`) | `maxExceededMessage` in `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` |
| `Future dates cannot be saved` | `ERROR_FUTURE_DATE` in `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` |
| `Enter a goal value` | `ERROR_REQUIRED` in `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` |
| `Body measurements`, `Save measurements`, `Close measurements sheet`, `Pick measurement date` | `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` |
| `{Label} goal`, `Target ({kg\|cm\|lb\|in})`, `Clear`, `Save`, `Close goal sheet` | `app/src/main/java/com/example/healthjournal/ui/components/GoalSheet.kt` |

## Non-functional requirements

- Offline-first persistence (local Room, then sync).
- Medical color system and light/dark UI.
- Deterministic validation bounds shared by capture and goals.

## Acceptance criteria

- AC-1: A valid measurement set saves locally (pending-sync).
- AC-2: Invalid input (future date, negative, over-bound, no values) is blocked
  with inline guidance.
- AC-3: Deleting a measurement shows an Undo that restores it.
- AC-4: A goal set for a parameter renders on the chart and persists.
- AC-5: Measurements and goals converge across devices via Drive sync.

## Out of scope

- Health Connect write-back.

## Cross-references

- `Docs/prd/unit-conversion.md` — display-unit conversion across surfaces.
- `Docs/prd/drive-sync.md` — sync of measurements/goals.
- [[data-layer]] — the measurement/goal entities and DAO.
- [[ui-layer]] — the measurements screen and sheets.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — measurements/analytics UI.
- `app/src/main/java/com/example/healthjournal/ui/components/MeasurementEntrySheet.kt` — capture sheet.
- `app/src/main/java/com/example/healthjournal/ui/components/GoalSheet.kt` — goal entry.
- `app/src/main/java/com/example/healthjournal/ui/components/ParamTrendChart.kt` — trend/goal chart.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — capture/validation state.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModel.kt` — series/goals.
- `app/src/main/java/com/example/healthjournal/data/BodyMeasurementRepository.kt` — measurement persistence + tombstones.
- `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` — goal persistence.
- `app/src/main/java/com/example/healthjournal/data/local/BodyMeasurementEntry.kt` — measurement entity.
- `app/src/main/java/com/example/healthjournal/data/local/GoalEntity.kt` — goal entity.
- `app/src/main/java/com/example/healthjournal/domain/ValidateMeasurements.kt` — capture validation.
- `app/src/main/java/com/example/healthjournal/domain/GoalValidator.kt` — goal validation.
- `app/src/main/java/com/example/healthjournal/domain/MeasurementFormatters.kt` — summaries/trend projection.
- `Docs/psd/body-measurements.md` — specification.
- `Docs/tests/body-measurements.md` — test cases.