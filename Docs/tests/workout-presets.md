# Workout Presets & Progressive Overload — Test Cases

> Maps the PRD's acceptance criteria and the PSD's edge cases to concrete
> verification. The "Automated coverage" table cites the real test files; keep
> it in sync when coverage moves.

Last updated: 2026-10-27

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` | preset CRUD, validation rules (including warm-up name/duration), UI-state transitions |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/RoutineExecutionTest.kt` | routine start, ordered completion, swap-to-catalog-defaults, rest/haptics, recovery, history-card content |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` | finish-prompt lifecycle, warm-up timer/skip/finish/recovery, history line, and no extra set/tonnage/Health Connect record |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ValidatePresetTest.kt` | warm-up and preset validation bounds |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupWriterTest.kt`, `BackupDataReaderTest.kt`, `FullBackupUseCaseTest.kt`, `RestoreRepositoryTest.kt` | preset/session warm-up backup round-trip and legacy null fields |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCaseTest.kt` | Epley single/best-estimate + bounds |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCaseTest.kt` | ratio from history, default-coefficient fallback, bounds |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/StrengthExerciseTest.kt` | planned rows, `isPlanned`, tonnage |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` | session persistence (crash-recovery rows) |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` | preset list/create/edit/delete + dropdown + warm-up editor and library summary |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` | set matrix, rest timer, pad-while-focused, 1 kg dumbbell step (metric + imperial), ordered-completion disable, swap, many-sets reachability, haptics |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/FinishRoutinePromptTest.kt` | prompt copy, Keep going, confirm-to-Summary, manual Finish bypass, manual/HIIT exclusion |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/WarmupExecutionScreenTest.kt` | warm-up panel/timer, Skip, Finish, Pause disabled, summary line |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/localization/WarmupLocalizationTest.kt` | warm-up validation messages and persisted history line in Russian and English |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` | preset warm-up fields round-trip through a real Room database |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt` | hub → preset start → execution flow |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | schema migrations preserving workout data + routine-name and warm-up columns |

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
| T-26 | Dumbbell step | active routine, 60 kg set, metric units | pad +1 kg → 61; pad −1 kg twice → 59 | weight adjusts by exactly 1 kg per tap (2.5 lb in imperial) |
| T-27 | Finish prompt trigger | active preset routine, partial completion | check the final pending set | dialog with "Finish routine?", body, Finish and Keep going; Keep going leaves the session Active with the rest timer running |
| T-28 | Finish prompt scope and re-show | preset routine / manual strength / HIIT | confirm Finish → Summary; uncheck and recheck the final set; complete manual/HIIT sets; resume a recovered complete routine | Finish persists COMPLETED and writes one journal entry; prompt returns only after uncheck→recheck; no prompt for manual, HIIT, or recovery |
| T-29 | Configure a warm-up | preset editor | enable warm-up → name "Rowing" → duration 5 → save | library summary shows "Warm-up · 5 min"; reopening the editor restores the values |
| T-30 | Warm-up validation | preset editor, warm-up enabled | blank name / duration 0 / 61 / non-numeric | inline error; save blocked; disabling clears the fields |
| T-31 | Warm-up countdown and completion | preset with 1-minute warm-up | Start Routine → wait out 3-2-1 → advance 60 s | panel shows timer; work sets appear only after completion; no warm-up set in the matrix |
| T-32 | Skip and Finish during warm-up | active warm-up | Skip; repeat and press Finish | outcome SKIPPED with elapsed retained; Finish reaches Summary with a warm-up line |
| T-33 | No tonnage, set, or Health Connect side effects | preset with warm-up and planned sets | complete warm-up → finish without completing work sets | set count and tonnage unchanged; one Health Connect record; no finish prompt from warm-up |
| T-34 | Pause and crash recovery | active warm-up | press Pause; separately kill/recover during warm-up | Pause disabled (Compose); recovery resumes Active with the warm-up marked SKIPPED (JVM) |
| T-35 | History and summary line | completed/skipped warm-up session | open Summary and history | exactly one localized warm-up line containing name, planned duration, and Completed/Skipped outcome (English template: `Warm-up: <name> · <min> min · <Completed / Skipped>`); absent for presets without warm-up |
| T-36 | Backup and legacy restore | preset/session with warm-up; legacy archive without presets file or warm-up keys | full backup → restore; restore legacy archive | warm-up fields survive; legacy presets/sessions restore with null warm-up fields |

## Manual checks

- Rest timer countdown feels correct on device (see `WorkoutScreenTest` for
  automated countdown coverage).
- Cloud sync of presets/catalog/alternative mappings; full-backup round-trip is
  covered by the JVM backup tests and `RestoreIntegrationTest`.

## Cross-references

- `Docs/prd/workout-presets.md` — the requirements under test.
- `Docs/psd/workout-presets.md` — the design the cases verify.
- [[ui-layer]] — the screens the instrumented tests drive.
- [[data-layer]] — the DAOs the migration tests exercise.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel/PresetViewModelTest.kt` — preset unit coverage.
- `app/src/test/java/com/example/healthjournal/viewmodel/RoutineExecutionTest.kt` — routine unit coverage.
- `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` — finish-prompt, warm-up execution, history-line, and side-effect coverage.
- `app/src/test/java/com/example/healthjournal/domain/ValidatePresetTest.kt` — warm-up validation bounds.
- `app/src/test/java/com/example/healthjournal/domain/CalculateOneRepMaxUseCaseTest.kt` — 1RM coverage.
- `app/src/test/java/com/example/healthjournal/domain/ConvertAlternativeExerciseWeightUseCaseTest.kt` — conversion coverage.
- `app/src/test/java/com/example/healthjournal/domain/StrengthExerciseTest.kt` — set model coverage.
- `app/src/test/java/com/example/healthjournal/data/WorkoutRepositoryTest.kt` — persistence coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt` — library UI coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/RoutineExecutionScreenTest.kt` — execution UI coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/FinishRoutinePromptTest.kt` — finish-prompt UI coverage.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/WarmupExecutionScreenTest.kt` — warm-up panel, skip/finish, Pause-disabled, and summary coverage.
- `app/src/androidTest/java/com/example/healthjournal/localization/WarmupLocalizationTest.kt` — warm-up validation and history-line localization.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt` — hub flow coverage.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — migration coverage.
- `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` — preset warm-up restore integration.
- `Docs/prd/workout-presets.md` — requirements under test.
- `Docs/psd/workout-presets.md` — design the cases verify.