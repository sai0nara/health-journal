# Track warm_up_20261023 — Implementation Plan

## Technical Notes

- Preset storage: add nullable `warmupName` and `warmupDurationSeconds` columns to `workout_presets`.
- Session storage: add nullable `warmupName`, `warmupDurationSeconds`, `warmupElapsedSeconds`, and `warmupOutcome` columns to `workout_sessions`; `warmupOutcome` is `COMPLETED` or `SKIPPED`.
- Warm-up is metadata, never a `StrengthExercise` or `StrengthSet`; it cannot affect tonnage, PRs, analytics, or the finish-prompt trigger.
- Room schema moves 19 → 20 with nullable `ALTER TABLE` columns. Backups remain Gson-compatible; no sync payload change.
- Pause is disabled during the warm-up; Finish and Skip remain available. Recovery from process death skips the warm-up.
- No new libraries or tech-stack changes.

## Phase 1: Warm-up data and persistence [checkpoint: 786ca6c]

- [x] Task: Write failing tests for warm-up data and validation (a5becf8)
    - [x] Add `ValidatePreset` boundary tests: blank name, 60-char limit, 1/60-minute bounds, invalid values
    - [x] Add repository round-trip tests for preset warm-up fields and nullable defaults
    - [x] Add `MigrationTest` 19→20 coverage for both tables and existing rows
    - [x] Run targeted JVM and migration tests; confirm expected failures
- [x] Task: Implement model and schema 19→20 (a5becf8)
    - [x] Add preset warm-up fields and session warm-up fields with nullable defaults
    - [x] Add the `WarmupOutcome` domain enum and validation rules
    - [x] Add `MIGRATION_19_20`, bump the database to version 20, and export the schema
    - [x] Run targeted tests until green
- [x] Task: Refactor without behavior change (a5becf8)
    - [x] Keep validation and persistence helpers small and covered
    - [x] Re-run the full JVM suite
- [x] Task: Conductor - User Manual Verification 'Phase 1: Warm-up data and persistence' (Protocol in workflow.md)

## Phase 2: Preset editor and library [checkpoint: 17b846e]

- [x] Task: Write failing JVM tests for preset warm-up editing (93952b7)
    - [x] Create/edit draft defaults: disabled, 5-minute default when enabled
    - [x] Update name/duration, toggle off clears values, validation blocks save
    - [x] Saving and reopening a preset preserves the warm-up
    - [x] Run `PresetViewModelTest`; confirm expected failures
- [x] Task: Implement preset warm-up draft state and ViewModel actions (93952b7)
    - [x] Extend `PresetUiState.Editing` with enabled/name/duration/error fields
    - [x] Load warm-up in `openEdit` and persist it in `save`
    - [x] Add EN/RU labels, hints, and validation messages
    - [x] Run targeted JVM tests until green
- [x] Task: Write failing Compose tests for the editor and library (93952b7)
    - [x] Toggle reveals name/duration fields; invalid values show inline errors
    - [x] Create, edit, remove, and reopen a warm-up
    - [x] Library card shows the warm-up summary only when configured
    - [x] Run `PresetLibraryScreenTest`; confirm expected failures
- [x] Task: Implement the editor section and library summary (93952b7)
    - [x] Add the optional Warm-up section to the create/edit form
    - [x] Add the library card summary and test tags
    - [x] Run targeted instrumented tests until green
- [x] Task: Conductor - User Manual Verification 'Phase 2: Preset editor and library' (Protocol in workflow.md)

## Phase 3: Routine execution, recovery, and warm-up lifecycle [checkpoint: c697617]

- [x] Task: Write failing JVM tests for the active warm-up (d853a95)
    - [x] Starting a configured preset copies the warm-up and initializes the remaining time
    - [x] `advanceTime` completes the warm-up at zero and records elapsed/outcome
    - [x] Skip advances immediately and records skipped
    - [x] Pause is ignored while warming up; Finish during warm-up records skipped
    - [x] Recovery starts at work sets and records skipped
    - [x] Warm-up completion does not show the finish prompt
    - [x] Run `WorkoutViewModelTest`; confirm expected failures
- [x] Task: Implement warm-up state and lifecycle (d853a95)
    - [x] Extend `WorkoutUiState.Active` with warm-up remaining time
    - [x] Initialize from the preset in `startPreset` after the countdown
    - [x] Handle completion, skip, pause guard, finish, and recovery
    - [x] Run targeted JVM tests until green
- [x] Task: Write failing Compose tests for the warm-up panel (d853a95)
    - [x] Panel shows name, remaining time, and Skip before work sets
    - [x] Skipping reveals the work sets; zero countdown reveals them automatically
    - [x] Pause is unavailable during the warm-up; Finish still works
    - [x] Presets without a warm-up remain unchanged
    - [x] Run the new/extended instrumented class; confirm expected failures
- [x] Task: Implement the active warm-up panel (d853a95)
    - [x] Hide work-set controls until the warm-up ends or is skipped
    - [x] Wire Skip and Finish; disable Pause during the warm-up
    - [x] Run targeted instrumented tests until green
- [x] Task: Conductor - User Manual Verification 'Phase 3: Routine execution and recovery' (Protocol in workflow.md)

## Phase 4: History, backup, docs, and verification

- [x] Task: Write failing integration tests (2f98261)
    - [x] Finished history includes one warm-up line with name, duration, and outcome
    - [x] Warm-up adds no tonnage, strength set, PR/analytics input, or separate Health Connect record
    - [x] Full backup/restore round-trips preset and session warm-up fields
    - [x] Old backups/presets without warm-up fields restore with nulls
    - [x] Run targeted JVM tests; confirm expected failures
- [x] Task: Implement history, summary, and backup compatibility (2f98261)
    - [x] Add the warm-up line to `describe()` and the Summary when present
    - [x] Confirm backup/restore handles nullable warm-up fields
    - [x] Run targeted JVM tests until green
- [x] Task: Update docs and wiki (2f98261)
    - [x] Update `Docs/prd/workout-presets.md` (FR/AC), `Docs/psd/workout-presets.md` (state/schema/flow), and `Docs/tests/workout-presets.md` (cases)
    - [x] Update cited wiki pages and run `python3 wiki/lint.py` to exit 0
- [ ] Task: Full verification
    - [x] JVM suite green, `./gradlew build` green, touched instrumented suites green
    - [ ] Manual device pass: create/edit warm-up, start, complete/skip, Finish during warm-up, history, old preset
- [ ] Task: Conductor - User Manual Verification 'Phase 4: History, backup, docs, and verification' (Protocol in workflow.md)
