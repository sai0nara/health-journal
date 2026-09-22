# Unit Conversion — Product Requirements

> Every measurement in the app renders and accepts input in the user's
> preferred unit system, converting to and from canonical metric storage
> without data loss.

Last updated: 2026-09-22

> **Status: BUILT.** Implemented as documented; the specification is
> `Docs/psd/unit-conversion.md` and the test cases are
> `Docs/tests/unit-conversion.md`. Implementing this feature reversed the
> metric-only decision formerly recorded in `Docs/prd/body-measurements.md`.

## Overview

The app stores all measurements in canonical metric units (kg, cm) and offers
a global unit-system preference. Conversion support is currently fragmented:
the personal-card editor converts height/weight on entry, and the workout
quick pad steps 5 lb in imperial mode — but routine set rows, tonnage,
preset defaults, the measurements screen with its charts and goals, and the
personal-card read-only rows all display metric regardless of the preference.
This feature makes conversion uniform: each surface renders stored metric
values in the preferred units and parses user input back to metric, following
the display-layer pattern the personal-card editor already uses. No stored
value, schema column, or sync payload changes.

## Goals / Non-goals

**Goals**

- Render every user-facing measurement in the preferred unit system, driven
  by the single global preference.
- Parse imperial input back to canonical metric storage without losing the
  stored value when the preference toggles.
- Validate numeric input in display units, mirroring the existing
  personal-card validation pattern.
- Fix the personal-card read-only rows, which hardcode metric display while
  the editor converts.

**Non-goals**

- Change canonical storage units, schema columns, or sync/backup payloads.
- Convert Health Connect import units (imports land in metric as today).
- Convert non-measurement quantities: RPE, reps, durations, laps.
- Add per-surface unit toggles; the global preference is the only switch.

## User stories

- As an imperial-units user, I want my body measurements, charts, and goals
  in lb/in so I can read my own data.
- As an imperial-units user, I want routine set rows, tonnage, and preset
  defaults in lb so plates match what I load.
- As a user switching systems mid-draft, I want my entered values preserved
  through the toggle so nothing I typed is lost.
- As a user, I want the personal-card summary rows to match the units I
  chose in the editor.

## Functional requirements

- FR-1: The measurements screen, its per-parameter charts, and goal lines
  render stored metric values in the preferred units.
- FR-2: Measurement capture parses imperial input back to metric storage.
- FR-3: Routine set rows, routine headers, tonnage, and the preset editor
  render and parse weights in the preferred units.
- FR-4: The personal-card read-only height/weight rows respect the
  preference instead of hardcoding metric display.
- FR-5: Journal vitals display converts stored metric values to the
  preferred units.
- FR-6: Toggling the preference re-renders in-progress drafts through the
  shared converter, preserving the underlying stored values.
- FR-7: All rounding goes through the shared converter; no surface keeps
  its own conversion factor.

## Non-functional requirements

- Reliability: a metric→imperial→metric round trip stays within display
  rounding; conversion never crashes on missing or partial values.
- Performance: conversion is arithmetic at render time; no caching layer
  or background work.
- Offline behaviour: fully local; no network dependency.
- Privacy/security: no new data collected; units are a display concern.

## Acceptance criteria

(To be defined with the PSD when the feature is built.)

## Out of scope

- Migrating stored values or adding per-record unit columns (rejected in
  favour of display-layer conversion).
- `Docs/psd/unit-conversion.md` and `Docs/tests/unit-conversion.md` do not
  exist yet; they are added when the feature is built.

## Cross-references

- `Docs/psd/unit-conversion.md` — the specification (added when the feature is built).
- `Docs/tests/unit-conversion.md` — the test cases (added when the feature is built).
- `Docs/prd/body-measurements.md` — the metric-only decision this feature reverses.
- `Docs/prd/personal-card.md` — the existing entry-conversion pattern to follow.
- `Docs/prd/workout-presets.md` — the pad-step conversion already specified.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` — the shared converter all surfaces must use.
- `app/src/main/java/com/example/healthjournal/data/local/UnitSettings.kt` — the global preference backing the toggle.
- `app/src/main/java/com/example/healthjournal/data/local/UnitSystem.kt` — the unit-system values.
- `app/src/main/java/com/example/healthjournal/ui/screens/SettingsScreen.kt` — the existing global toggle.
- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — the metric-only capture/chart target.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — the kg-hardcoded set rows and pad step.
- `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt` — the editor pattern and the hardcoded read-only rows.
- `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt` — display-unit validation to mirror.
