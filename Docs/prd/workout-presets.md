# Workout Presets & Progressive Overload — Product Requirements

> Turns the Workout hub from a generic activity logger into a structured
> progressive-overload tracker: day-based presets, an in-session set/rep matrix
> with ordered completion, a searchable exercise catalog, and (planned) weight
> analytics with alternative-exercise weight conversion.

Last updated: 2026-09-18

## Overview

A user can compose named routines (e.g. "Leg Day") from a curated exercise
catalog, start any preset as an active session whose planned set rows are
prefilled with the preset's defaults, complete sets in order with inline
editing and quick-increment controls, swap a movement mid-routine, and finish
into the journal. The finished routine's history card names the routine and its
exercises instead of a bare tonnage figure.

## Goals / Non-goals

**Goals**

- Build, edit, and delete named presets with per-exercise defaults (sets, reps,
  weight, rest) and an optional scheduled day.
- Pick a preset and run it: planned set rows, ordered completion, automatic
  between-sets rest timer, inline weight/reps editing, and quick-increment
  controls that keep the screen on.
- Swap an exercise mid-routine via a searchable dropdown; the swapped-in
  movement starts from its catalog default plan.
- Finish a routine into the journal + Health Connect, with the routine name and
  per-exercise summaries on the history card.
- Recover an interrupted session to the last completed set.
- (Planned, Phase 5) Per-exercise weight trend analytics (Epley 1RM, max volume)
  and alternative-movement weight conversion from logged history.

**Non-goals**

- GPS/route, heart-rate, guided HIIT, or Music sync (see out of scope).
- Replacing the manual (non-preset) v2 set-matrix or HIIT flows — presets are an
  additional entry path.
- Rack-aware weight recommendations beyond 1RM-based conversion.

## User stories

- As a lifter, I want a saved "Leg Day" routine I can start in two taps, so I
  don't rebuild the plan every session.
- As a lifter, I want my sets preloaded with planned weight/reps, so I only
  adjust when the plan changes.
- As a lifter, I want to swap a movement mid-session and get sane starting
  values, so I can adapt without mental math.
- As a lifter, I want to see which exercises and weights I did on a routine's
  history card, so past sessions read as a plan, not a tonnage total.

## Functional requirements

- FR-1: Preset library — list, create, edit, delete named presets; each preset
  holds exercises from a searchable catalog dropdown with target sets, default
  reps, default weight, and rest length. Editing has an explicit cancel that
  discards the draft without saving. Catalog search is a case-insensitive
  substring match on the exercise name; a blank query shows no results.
- FR-2: Preset validation — a preset requires a name and at least one exercise;
  per-exercise defaults must be usable (positive sets/reps/weight,
  non-negative rest). Validators enforce lower bounds only: there are no upper
  bounds on name length, sets, reps, weight, or rest, and no length constraint
  on the stored preset name.
- FR-3: Routine start — starting a preset builds a planned set matrix (all
  sets pending, prefilled with the preset's defaults, catalog names resolved)
  after the same 3-2-1 countdown as manual sessions; the countdown runs to zero
  with no skip/cancel affordance. It records the routine's name on the session.
  The optional scheduled day (`MONDAY`..`SUNDAY`, `ANY`) is a stored label on
  the preset; no scheduling engine consumes it. Exercises whose catalog name
  cannot be resolved (or with non-positive target sets) are skipped; if none
  resolve, an empty but ACTIVE session is still created.
- FR-4: Ordered set completion — a set can only be completed once every earlier
  set in the exercise is done; completing a set persists immediately, starts
  the rest timer with a heavy haptic, and fires a success haptic when the
  exercise's final set completes. Un-completing is allowed: unchecking a set
  persists it as pending and stops the rest period (re-checking restarts it).
  While paused, the session is frozen except Resume/Finish: the screen keeps
  the same controls, but set toggles, edits, swaps, and additions are ignored
  until resumed.
- FR-5: Set editing — each planned set's weight/reps edit inline with
  validation; the quick-increment pad adjusts the value in place, including
  while a field is focused, and updates the displayed text. Weight entry is
  free decimal text (no fixed granularity); the pad steps 1.25 kg in metric
  mode or 5 lb in imperial mode.
- FR-6: Exercise swap — the dropdown replaces the exercise mid-routine and
  resets the row to the new movement's catalog default plan (sets, reps,
  weight, rest).
- FR-7: Session finish — the finished routine writes the journal entry (routine
  name + per-exercise name, set count, weight) and the Health Connect EXERCISE
  record. A Health Connect failure never blocks completion: the journal entry
  and summary are still written, with the failure recorded as not-synced.
- FR-8: Crash recovery — relaunching with an unfinished session resumes at the
  last persisted set state. Recovery offers exactly two actions: resume (back
  to active) or discard (marked discarded); there is no finish-from-recovery.
- FR-9: (Planned) Weight analytics — per-exercise trend chart of weight over
  time with Epley 1RM and max volume, in a bottom-sheet overlay. 1RM uses the
  Epley formula `weight × (1 + reps/30)` (a single rep returns the weight).
- FR-10: (Planned) Alternative conversion — convert a working weight to an
  alternative movement using the user's historical 1RM ratio, with a default
  coefficient fallback of 0.6 when history is insufficient.

## Non-functional requirements

- Offline-first local persistence (Room), consistent with the rest of the app.
- >80% coverage on new code; a UI test per user-facing flow.
- Keyed lazy lists and timer/math off the main thread.
- Schema migrations are explicit and validated; a routine session's name column
  is added via a versioned migration with schema export.
- Medical color system / Material 3; haptics for set/exercise completion;
  keep-screen-on during an active routine.

## Acceptance criteria

- AC-1: A preset can be created, edited, started, and deleted.
- AC-2: Starting a preset runs sets in order; a skipped set blocks the ones
  after it (UI disabled + state-machine guard).
- AC-3: Swapping an exercise resets weight/reps/sets/rest to the catalog
  default plan.
- AC-4: The quick pad updates a set even when its field is focused.
- AC-5: Finishing a routine shows the routine name and per-exercise name/sets/
  weight on the journal card.
- AC-6: An unfinished session survives a process kill at the last completed set.
- AC-7: (Planned) A weight trend chart and alternative conversion render from
  logged history, with a sensible fallback for a fresh user.

## Out of scope

- GPS/location routes, heart-rate/sensor integration, foreground service/
  notification, music/air-play sync, automated/guided HIIT plans, band/relative-
  strength comparisons, SQLCipher encryption.

## Cross-references

- `Docs/psd/workout-presets.md` — the specification that implements these
  requirements.
- `Docs/tests/workout-presets.md` — the test cases that verify them.
- [[ui-layer]] — the preset library and routine-execution screens.
- [[data-layer]] — preset, catalog, and session entities/DAOs.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` — preset library UI.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — routine execution, pad, swap UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/PresetViewModel.kt` — preset CRUD/validation state.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` — routine start/completion/swap/finish.
- `app/src/main/java/com/example/healthjournal/data/PresetRepository.kt` — preset persistence.
- `app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt` — preset entity.
- `app/src/main/java/com/example/healthjournal/data/local/ExerciseCatalogItem.kt` — catalog entity.
- `app/src/main/java/com/example/healthjournal/data/local/BuiltInExerciseCatalog.kt` — curated catalog + default plans.
- `app/src/main/java/com/example/healthjournal/domain/StrengthExercise.kt` — planned set row model + tonnage.
- `app/src/main/java/com/example/healthjournal/domain/WorkoutPreset.kt` — scheduled-day enum + preset exercise model.
- `app/src/main/java/com/example/healthjournal/domain/ValidatePreset.kt` — preset validation.
- `app/src/main/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCase.kt` — Epley 1RM.
- `app/src/main/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCase.kt` — conversion.
- `Docs/psd/workout-presets.md` — specification.
- `Docs/tests/workout-presets.md` — test cases.