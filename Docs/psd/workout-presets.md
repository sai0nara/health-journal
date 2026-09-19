# Workout Presets & Progressive Overload — Product Specification

> A preset repository + library screen manage named routines; the workout
> ViewModel expands a preset into a planned set matrix, enforces ordered set
> completion with immediate persistence and rest/haptic side effects, swaps
> movements to catalog defaults, and finishes into the journal + Health Connect
> — with (planned) use cases for 1RM and alternative-exercise conversion.

Last updated: 2026-09-18

## Overview

Presets are composed from a searchable exercise catalog and stored as a single
Gson-serialized row (exercises as JSON), keeping a preset atomic. Starting a
preset turns its planned exercises into `StrengthExercise` rows whose set list
is prefilled from the preset defaults. The `WorkoutUiState.Active` machine owns
an ordered-completion guard (VM enforces, UI disables), immediate persistence
on every set toggle, and the rest timer. Progress is journaled on finish with
the routine name and per-exercise summaries. The design keeps the v2 manual
set-matrix and HIIT flows intact beside the routine path.

## Architecture

- MVVM without a DI framework: `PresetViewModel` and `WorkoutViewModel` are
  constructed manually via per-ViewModel `Factory` classes reading DAOs off the
  singleton `JournalDatabase`.
- `PresetRepository` owns preset CRUD over `WorkoutPresetDao`.
- `WorkoutViewModel.startPreset` expands a preset into a planned matrix (catalog
  names resolved via `ExerciseCatalogDao`, unknown/empty exercises skipped).
- `WorkoutViewModel.toggleSetCompleted` implements the ordered-completion guard
  (completion only; unchecking bypasses it) and, on success, persistence + rest
  start + haptics; unchecking persists pending and zeroes the rest timer.
  `toggleSetCompleted`, `updateRoutineSet`, `swapRoutineExercise`, and the
  add paths all require the `Active` state, so mutations issued while `Paused`
  are silent no-ops — the paused screen wires the same callbacks but only
  Resume/Finish take effect.
- `WorkoutViewModel.startPreset` skips planned exercises with an unresolvable
  catalog name or non-positive target sets; an all-skipped preset still yields
  an `ACTIVE` session with an empty matrix, followed by the unskippable 3 s
  countdown (`COUNTDOWN_SECONDS`, no skip/cancel affordance in
  `CountdownContent`).
- `WorkoutViewModel.finishSession` writes the session row and journal entry
  first, then attempts the Health Connect record inside try/catch: failure sets
  `healthSynced = false` on the `Summary` state, never blocking completion.
- `resumeRecovery` re-activates the unfinished session; `discardRecovery` marks
  it `DISCARDED` and returns to `Idle`. No other recovery transition exists.
- Library search (`ExerciseSearchDropdown`) filters with a case-insensitive
  substring predicate (`name.contains(query, ignoreCase = true)`); a blank
  query yields no results. The mid-routine swap menu is not a search: it lists
  the full catalog with the current movement check-marked.
- Validation (`ValidatePreset`, `StrengthExercise.ValidateStrengthExercise`)
  enforces lower bounds only (name non-blank; sets/reps ≥ 1; weight > 0;
  rest ≥ 0; RPE 1..10 where applicable) — no upper bounds anywhere, and the
  preset name column carries no length constraint.
- The weight pad steps `1.25` kg metric / `5` lb converted to kg imperial
  (`padStepWeightKg`); typed entry is free decimal text with no fixed step.
- `CalculateOneRepMaxUseCase` (Epley `weight × (1 + reps/30)`, single rep
  returns weight) and `ConvertAlternativeExerciseWeightUseCase`
  (`DEFAULT_COEFFICIENT = 0.6`) have no production call sites yet — constants
  and formulas exist, wiring is Phase 5.
- `PresetViewModel.cancelEditing` drops the draft and returns to `Library`
  without persisting.
- The seeded catalog (`BuiltInExerciseCatalog`) is a versioned constants source;
  `defaultPlanFor` yields per-movement starting defaults with a generic
  fallback.

## Data flow

1. Library screen → `PresetViewModel` validates (name + ≥1 exercise + usable
   defaults per `ValidatePreset`) and upserts/deletes via `PresetRepository`.
2. User taps Start Routine → `startPreset` resolves catalog names, builds the
   planned matrix, stores `routineName` on the session, saves, enters the 3-2-1
   countdown.
3. Each set toggle → ordered-completion guard → persist immediately → rest timer
   + `SET_COMPLETE` haptic; final set per exercise → `EXERCISE_COMPLETE` haptic.
4. Weight/reps edits validate inline; the quick pad adjusts the persisted value
   so focused fields re-seed from the set (cursor preserved on no-op).
5. Swap → `swapRoutineExercise` rebuilds the row from `defaultPlanFor` targets.
6. Finish → journal entry (routine name + per-exercise "name: N sets · X kg")
   and Health Connect EXERCISE record.
7. Interrupted session → recovery surfaces the last persisted state.

## Components

| Component | File | Responsibility |
|---|---|---|
| Preset library screen | `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` | list/create/edit/delete + dropdown |
| Workout screen | `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` | hub, routine execution, pad, swap, summary |
| Preset ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/PresetViewModel.kt` | CRUD + validation state |
| Workout ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` | routine start/complete/swap/finish, rest timer, journaling |
| UI state | `app/src/main/java/com/example/healthjournal/viewmodel/PresetUiState.kt`, `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutUiState.kt` | Idle/Editing/List and Isolations/Active/Paused/Recovery |
| Preset repository | `app/src/main/java/com/example/healthjournal/data/PresetRepository.kt` | preset CRUD |
| Preset entity | `app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt` | atomic JSON-backed preset row |
| Catalog entity | `app/src/main/java/com/example/healthjournal/data/local/ExerciseCatalogItem.kt` | movement, category, alternatives |
| Catalog source | `app/src/main/java/com/example/healthjournal/data/local/BuiltInExerciseCatalog.kt` | curated seeds + `defaultPlanFor` |
| Set domain | `app/src/main/java/com/example/healthjournal/domain/StrengthExercise.kt` | planned targets, `isPlanned`, tonnage |
| Validation | `app/src/main/java/com/example/healthjournal/domain/ValidatePreset.kt` | preset + per-exercise rules |
| 1RM use case | `app/src/main/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCase.kt` | Epley estimate (planned wiring) |
| Conversion use case | `app/src/main/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCase.kt` | history-based ratio + fallback (planned wiring) |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Preset missing at start | typed `Error("Preset not found")` |
| Unfinished session exists | start is blocked; `RecoveryRequired` surfaces |
| Skip-ahead completion | VM rejects with `setMatrixError`; UI disables later checkboxes |
| Editing invalid set values | inline error; no mutation |
| Swap to unknown movement | `defaultPlanFor` falls back to the generic `PresetDefaults()` |
| Over-long routine list | content area takes remaining height; CTA row stays reachable |
| Routine from before v18 | `routineName` null → journal card omits the routine-name line |
| Process kill mid-set | last persisted toggle is the resume point |
| Uncheck a completed set | allowed: persists pending, rest timer zeroed, no error |
| Mutation while paused | silently ignored (VM requires `Active`); Resume/Finish only |
| Countdown visible | runs 3 s to zero; no skip/cancel control |
| Start with all exercises unresolvable | empty-matrix `ACTIVE` session still created |
| Health Connect write throws | `Summary` with `healthSynced = false`; journal entry kept |
| Recovery choice | resume re-activates; discard marks `DISCARDED` → `Idle` |
| Blank catalog search | no results shown |
| Cancel preset edit | draft dropped, back to library, nothing persisted |

## Dependencies

- Room (preset/catalog/session DAOs + explicit migrations with schema export).
- Jetpack Compose Material 3; `FlowRow` for the quick pad.
- Gson typing for the serialized preset exercises.
- No DI framework; manual factories.
- Health Connect write path reused from the v2 finish flow.
- (Planned) Vico charting for Phase 5 analytics — documented in `tech-stack.md`
  before chart code lands.

## Cross-references

- `Docs/prd/workout-presets.md` — the requirements this specification implements.
- `Docs/tests/workout-presets.md` — the test cases that verify this design.
- [[ui-layer]] — preset library + routine execution UI.
- [[data-layer]] — entities and DAOs.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` — library UI.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — execution UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/PresetViewModel.kt` — preset state.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` — routine state machine.
- `app/src/main/java/com/example/healthjournal/viewmodel/PresetUiState.kt` — preset UI states.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutUiState.kt` — workout UI states.
- `app/src/main/java/com/example/healthjournal/data/PresetRepository.kt` — persistence.
- `app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt` — entity.
- `app/src/main/java/com/example/healthjournal/data/local/ExerciseCatalogItem.kt` — entity.
- `app/src/main/java/com/example/healthjournal/data/local/BuiltInExerciseCatalog.kt` — seeds/defaults.
- `app/src/main/java/com/example/healthjournal/domain/StrengthExercise.kt` — set model.
- `app/src/main/java/com/example/healthjournal/domain/WorkoutPreset.kt` — scheduled-day enum + preset exercise model.
- `app/src/main/java/com/example/healthjournal/domain/ValidatePreset.kt` — validation.
- `app/src/main/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCase.kt` — 1RM.
- `app/src/main/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCase.kt` — conversion.
- `Docs/prd/workout-presets.md` — requirements.
- `Docs/tests/workout-presets.md` — test cases.