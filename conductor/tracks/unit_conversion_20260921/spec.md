# Track unit_conversion_20260921 — Specification

## Overview

Implement `Docs/prd/unit-conversion.md` (approved 2026-09-21): every
user-facing measurement renders and parses in the preferred unit system via
display-layer conversion. Canonical storage stays metric (kg/cm); no schema
migration and no sync/backup payload changes. One global preference
(`UnitSettings`) drives all surfaces. Rollout is phased inside a single
track: body measurements, then workouts, then personal-card read-only rows +
journal vitals, then docs.

## Functional Requirements

- FR-1: Measurements entry screen parses imperial input to metric storage
  and renders stored values in preferred units (weight lb, circumferences
  in).
- FR-2: Per-parameter charts and goal lines render converted values; the
  goal editor accepts imperial input and stores metric.
- FR-3: Imperial height entry uses ft+in split fields backed by new
  parse/format helpers alongside `UnitConverter`.
- FR-4: Routine set rows, routine headers, tonnage, and the preset editor
  render and parse weights in preferred units (imperial: lb).
- FR-5: Personal-card read-only height/weight rows respect the preference
  (fix hardcoded metric display).
- FR-6: Journal vitals display converts stored metric values to preferred
  units.
- FR-7: Toggling the preference re-renders in-progress drafts through the
  shared converter with no value loss.
- FR-8: Numeric validation runs in display units, mirroring the
  personal-card pattern.
- FR-9: All rounding/conversion factors live in the shared converter; no
  surface keeps its own factor.
- FR-10: Body-measurements docs (`Docs/prd`, `Docs/psd`, `Docs/tests`) are
  updated to lift the metric-only decision; wiki lint stays green.

## Non-Functional Requirements

- Reliability: metric→imperial→metric round trip within display rounding;
  no crash on missing/partial values.
- Performance: render-time arithmetic only; no caching or background work.
- Offline: fully local, no network dependency.
- Coverage: >80% on new code; UI tests on every user-facing change (per
  workflow).

## Acceptance Criteria

- AC-1: With imperial preference, a body-measurement entry in lb/in persists
  metric values identical (within rounding) to the metric-entered
  equivalents.
- AC-2: Charts and goal lines show converted values; editing a goal in
  imperial stores the correct metric value.
- AC-3: Height entered as ft+in stores correct cm and redisplays as the same
  ft+in.
- AC-4: Routine set rows, headers, tonnage, and preset defaults show lb in
  imperial mode and persist kg.
- AC-5: Personal-card summary rows match the editor's unit choice.
- AC-6: Preference toggle mid-draft preserves values on every touched
  surface.
- AC-7: `./gradlew :app:testDebugUnitTest` green; new/updated Compose UI
  tests green in isolation; wiki lint exits 0.

## Out of Scope

- Storage migration or per-record unit columns.
- Health Connect import units; RPE, reps, durations, laps.
- Per-surface unit toggles.
- `Docs/psd/unit-conversion.md` and `Docs/tests/unit-conversion.md`
  (follow-up docs track).
