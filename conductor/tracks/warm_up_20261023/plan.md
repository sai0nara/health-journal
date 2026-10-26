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

## Phase 3: Routine execution, recovery, and warm-up lifecycle

- [ ] Task: Write failing JVM tests for the active warm-up
    - [ ] Starting a configured preset copies the warm-up and initializes the remaining time
    - [ ] `advanceTime` completes the warm-up at zero and records elapsed/outcome
    - [ ] Skip advances immediately and records skipped
    - [ ] Pause is ignored while warming up; Finish during warm-up records skipped
    - [ ] Recovery starts at work sets and records skipped
    - [ ] Warm-up completion does not show the finish prompt
    - [ ] Run `WorkoutViewModelTest`; confirm expected failures
- [ ] Task: Implement warm-up state and lifecycle
    - [ ] Extend `WorkoutUiState.Active` with warm-up remaining time
    - [ ] Initialize from the preset in `startPreset` after the countdown
    - [ ] Handle completion, skip, pause guard, finish, and recovery
    - [ ] Run targeted JVM tests until green
- [ ] Task: Write failing Compose tests for the warm-up panel
    - [ ] Panel shows name, remaining time, and Skip before work sets
    - [ ] Skipping reveals the work sets; zero countdown reveals them automatically
    - [ ] Pause is unavailable during the warm-up; Finish still works
    - [ ] Presets without a warm-up remain unchanged
    - [ ] Run the new/extended instrumented class; confirm expected failures
- [ ] Task: Implement the active warm-up panel
    - [ ] Hide work-set controls until the warm-up ends or is skipped
    - [ ] Wire Skip and Finish; disable Pause during the warm-up
    - [ ] Run targeted instrumented tests until green
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Routine execution and recovery' (Protocol in workflow.md)

## Phase 4: History, backup, docs, and verification

- [ ] Task: Write failing integration tests
    - [ ] Finished history includes one warm-up line with name, duration, and outcome
    - [ ] Warm-up adds no tonnage, strength set, PR/analytics input, or separate Health Connect record
    - [ ] Full backup/restore round-trips preset and session warm-up fields
    - [ ] Old backups/presets without warm-up fields restore with nulls
    - [ ] Run targeted JVM tests; confirm expected failures
- [ ] Task: Implement history, summary, and backup compatibility
    - [ ] Add the warm-up line to `describe()` and the Summary when present
    - [ ] Confirm backup/restore handles nullable warm-up fields
    - [ ] Run targeted JVM tests until green
- [ ] Task: Update docs and wiki
    - [ ] Update `Docs/prd/workout-presets.md` (FR/AC), `Docs/psd/workout-presets.md` (state/schema/flow), and `Docs/tests/workout-presets.md` (cases)
    - [ ] Update cited wiki pages and run `python3 wiki/lint.py` to exit 0
- [ ] Task: Full verification
    - [ ] JVM suite green, `./gradlew build` green, touched instrumented suites green
    - [ ] Manual device pass: create/edit warm-up, start, complete/skip, Finish during warm-up, history, old preset
- [ ] Task: Conductor - User Manual Verification 'Phase 4: History, backup, docs, and verification' (Protocol in workflow.md)
