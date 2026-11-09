# Track workout_hub_routines_top_20261109 — Specification

## Overview

In the Workout screen idle state (`IdleContent` in `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt`), the 10 generic activity buttons (`Run`, `Walk`, `Fitness`, etc.) currently render at the top under "Choose a workout", followed by the "Presets" button and the saved "Routines" list. For users running structured routines, this requires scrolling past 10 large buttons every session to find their routine.

This track refactors `IdleContent` so that the **Routines** section (routine cards + "Presets" library button `presets_entry`) is placed at the very top of the scrollable catalog (`workout_catalog`), followed by "Choose a workout" (10 activity buttons) and "Recent" workout history.

## Functional Requirements

- **FR1 — Top Placement for Routines:** In `IdleContent`, the Routines section and the "Presets" button (`presets_entry`) render at the top of the `LazyColumn` (`workout_catalog`), preceding "Choose a workout" (`workout_choose`) and the 10 activity buttons.
- **FR2 — With Presets:** When the active profile has presets, display:
  1. "Routines" header (`R.string.workout_routines`)
  2. Saved routine cards (`routine_start_<presetId>`)
  3. "Presets" button (`presets_entry`) to manage/create presets
  4. "Choose a workout" header (`R.string.workout_choose`)
  5. 10 activity buttons (`WorkoutType.entries`)
  6. Recent workouts list (if present)
- **FR3 — Without Presets:** When the active profile has no presets:
  1. "Routines" header (`R.string.workout_routines`)
  2. "Presets" button (`presets_entry`) as the primary CTA
  3. "Choose a workout" header + 10 activity buttons
  4. Recent workouts list (if present)
- **FR4 — Test Tags & Interactions:** All existing test tags (`workout_catalog`, `presets_entry`, `routine_start_<presetId>`) and click callbacks (`onStartRoutine`, `onPresetsClick`, `onSelectType`) remain intact and functional.

## Non-Functional Requirements

- Zero regression in routine execution, countdown, warm-up, and manual logging flows.
- JVM unit tests and Compose UI tests remain 100% green.
- `python3 wiki/lint.py` exits 0.

## Acceptance Criteria

- **AC1:** On opening the Workout hub, saved routine cards and the Presets button are visible at the top without scrolling.
- **AC2:** When no presets exist, the Routines header and Presets button appear at the top above the 10 activity buttons.
- **AC3:** Tapping a routine card starts the countdown/warm-up routine runner.
- **AC4:** Tapping the Presets button navigates to the Preset Library screen.
- **AC5:** Tapping any activity button opens the configuring screen as before.
