# Track history_dual_units_20260922 — Specification

## Overview

Fix the two open defects in `Docs/Defects.md`: workout history stays
single-unit in Imperial mode, and history lines show planned defaults instead
of performed values. (Defect #3, personal-card metric rows, is already fixed
by the unit-conversion track; this track adds regression coverage for it.)

## Functional Requirements

- FR-1: Workout history cards render weights dual-unit, metric primary
  (e.g. `40 kg (88.2 lb)`), in both preferences.
- FR-2: Routine headers render weights dual-unit, metric primary, in both
  preferences.
- FR-3: History per-exercise lines show performed values (actual completed
  sets) instead of planned defaults.
- FR-4: A regression test locks defect #3: personal-card rows follow the unit
  preference.
- FR-5: Canonical metric storage, validation, and parsing are unchanged.

## Non-Functional Requirements

- Reliability: dual rendering never crashes on missing values; performed-only
  lines fall back sensibly when nothing was completed.
- Coverage: >80% on new code; UI tests on the changed surfaces (per
  workflow).

## Acceptance Criteria

- AC-1: In Imperial mode a finished routine's history card shows dual-unit
  weights.
- AC-2: Routine headers show dual-unit weights.
- AC-3: Raising set weights changes the history line values accordingly.
- AC-4: Card rows follow the preference (regression green).
- AC-5: `./gradlew :app:testDebugUnitTest` green; touched UI classes green in
  isolation; wiki lint exits 0.

## Out of Scope

- Changing which unit is primary per preference; new preferences or settings.
- Touching storage, sync, or validation logic.
