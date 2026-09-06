# Body Measurements — Product Requirements

> Track up to seven body measurements (weight, chest, waist, glute, thigh,
> calf, bicep) with per-parameter trend charts, goals, and deletion undo, synced
> to the cloud like the rest of the health data.

Last updated: 2026-09-02

## Overview

A dedicated measurements screen lets a user log a dated set of body metrics
(metric-only), see a per-parameter trend chart with an optional goal line, and
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

**Non-goals**

- Imperial units (metric kg/cm only for body measurements).
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
- FR-2: The sheet captures a timestamp and up to seven metric fields; partial
  entries are allowed (at least one value required).
- FR-3: Input uses a decimal keyboard and inline validation warnings.
- FR-4: Validation blocks negative, malformed, over-bound, and future-dated
  values.
- FR-5: A chronological list shows the measurement history with cloud status,
  and supports delete with undo.
- FR-6: A per-parameter trend chart (auto-scaled, goal line shown) is shown per
  tab.
- FR-7: Goals can be set/cleared per parameter and reflected on the chart.
- FR-8: Measurements and goals sync to Drive via the existing sync pipeline.

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

- Imperial units and unit-system preference for body measurements.
- Health Connect write-back.

## Cross-references

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