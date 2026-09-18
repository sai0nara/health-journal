# Workout Presets & Progressive Overload — Test Cases

> Maps the PRD's acceptance criteria and the PSD's edge cases to concrete
> verification. The "Automated coverage" table cites the real test files; keep
> it in sync when coverage moves.

Last updated: 2026-09-18

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` | preset CRUD, validation rules, UI-state transitions |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/RoutineExecutionTest.kt` | routine start, ordered completion, swap-to-catalog-defaults, rest/haptics, recovery, history-card content |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCaseTest.kt` | Epley single/best-estimate + bounds |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCaseTest.kt` | ratio from history, default-coefficient fallback, bounds |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/StrengthExerciseTest.kt` | planned rows, `isPlanned`, tonnage |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` | session persistence (crash-recovery rows) |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` | preset list/create/edit/delete + dropdown |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` | set matrix, rest timer, pad-while-focused, ordered-completion disable, swap, many-sets reachability, haptics |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt` | hub → preset start → execution flow |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | schema migration preserving workout data + new routine-name column |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Create a preset | library open, catalog seeded | New Preset → name "Leg Day" → add exercise via dropdown → set defaults → save | preset listed with its exercises |
| T-2 | Preset validation | edit a preset | leave name blank / zero exercises / invalid default | inline error; save blocked |
| T-3 | Delete a preset | at least one saved preset | delete | removed from the list |
| T-4 | Start a routine | saved preset, no unfinished session | Start Routine → countdown | planned set rows prefilled, `routineName` recorded |
| T-5 | Ordered completion (skip) | active routine | complete set 2 before set 1 | rejected (VM guard) + checkbox disabled (UI) |
| T-6 | Ordered completion (in order) | active routine | complete set 1, then set 2 | both complete; rest timer + haptic fire |
| T-7 | Final-set haptic | one set pending in an exercise | complete the final set | success haptic |
| T-8 | Pad when focused | active routine | focus weight field → pad + | displayed value updates; cursor preserved for no-op |
| T-9 | Swap resets to defaults | active routine | swap movement to bench press | sets/reps/weight/rest taken from catalog default plan |
| T-10 | Finish routine | fully or partially completed | Finish | journal card shows routine name + per-exercise name/sets/weight; Health Connect record |
| T-11 | Many sets reachable | routine with 8+ sets | scroll to CTAs | Finish/Pause remain visible and reachable |
| T-12 | Crash recovery | unfinished session persisted | relaunch | resumes at last completed set |
| T-13 | 1RM bounds | — | weight ≤ 0 or reps < 1 | rejected |
| T-14 | 1RM estimate | valid (weight, reps) | compute | Epley formula result; single rep returns weight |
| T-15 | Conversion with history | both sides have 1RM history | convert | weight scaled by historical ratio |
| T-16 | Conversion without history | one/both sides missing | convert | default coefficient applied |
| T-17 | Migration 16→17→18 | pre-existing schema DB | open at target version | workout rows survive; routine-name column present |
| T-18 | Uncheck a set | active routine, set completed | uncheck the set | persists pending; rest timer stops; no error |
| T-19 | Start with all exercises unresolvable | preset whose catalog ids are unknown | Start Routine → countdown | empty-matrix ACTIVE session created |
| T-20 | Health Connect failure | Health Connect throws on write | Finish | summary + journal entry still written; not-synced recorded |
| T-21 | Recovery discard vs resume | unfinished session persisted | relaunch → Discard / Resume | discard marks discarded → idle; resume re-activates |
| T-22 | Cancel preset edit | editing a preset draft | Cancel | draft dropped; library unchanged |
| T-23 | Mutation while paused | paused routine | toggle a set / edit weight | ignored; session unchanged until Resume |
| T-24 | Countdown not skippable | routine starting | observe countdown UI | runs to zero; no skip/cancel control |
| T-25 | Catalog search semantics | library open, catalog seeded | type mixed-case substring / clear to blank | case-insensitive substring matches; blank shows no results |

## Manual checks

- Rest timer countdown feels correct on device (see `WorkoutScreenTest` for
  automated countdown coverage).
- Cloud sync of presets/catalog/alternative mappings and Drive full-backup
  round-trip — covered by Phase 6 of `conductor/tracks/workout_presets_20260914/plan.md`
  (work to follow).

## Cross-references

- `Docs/prd/workout-presets.md` — the requirements under test.
- `Docs/psd/workout-presets.md` — the design the cases verify.
- [[ui-layer]] — the screens the instrumented tests drive.
- [[data-layer]] — the DAOs the migration tests exercise.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` — preset unit coverage.
- `app/src/test/java/com/example/healthjournal/viewmodel/RoutineExecutionTest.kt` — routine unit coverage.
- `app/src/test/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCaseTest.kt` — 1RM coverage.
- `app/src/test/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCaseTest.kt` — conversion coverage.
- `app/src/test/java/com/example/healthjournal/domain/StrengthExerciseTest.kt` — set model coverage.
- `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` — persistence coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` — library UI coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` — execution UI coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt` — hub flow coverage.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — migration coverage.
- `Docs/prd/workout-presets.md` — requirements under test.
- `Docs/psd/workout-presets.md` — design the cases verify.