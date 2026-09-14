# Implementation Plan: Workout Presets & Progressive Overload

**Prerequisite:** v2 workspace (this branch), schema 16 already present; this track adds migration 16→17. Work happens on a feature branch and merges to main via PR after completion.

> **Deviation note (Phase 1):** schema moves 16→17 and the v2 manual in-session set-matrix is replaced by preset-driven routine execution (spec), so the existing `StrengthSet` gains an optional RPE field and the `Active`-state set-matrix editing is superseded by a routine-execution state machine. Vico is added to the tech stack (documented in `tech-stack.md`) before any chart code is written.

## Phase 1: Data Model + Persistence (schema 16→17) [checkpoint: 0a3ebcf]
- [x] Task: Write failing unit tests for new entities: WorkoutPreset (name, scheduled day, exercises with default sets/reps/weight/rest), ExerciseCatalog (name, muscle category, alternative-movement ids), logged-set RPE extension; converters (setMatrix + preset data classes) (prereq: define model shapes first) [red] (2825745)
- [x] Task: Implement entities + Room schema 16→17 (migration with explicit `ALTER TABLE`/recreate path for the RPE column + new tables); DAOs (preset CRUD, catalog search, alternative mapping, per-exercise historical weight query with indexes); update exported schema JSON; green JVM tests [green] (ed6db16)
- [x] Task: Seed the built-in curated exercise catalog (muscle categories + pre-mapped alternative movements) as a versioned constants source (not a migration); test seeding is idempotent + catalog count > threshold (047e994)
- [x] Task: Conductor - User Manual Verification 'Data Model + Persistence' (Protocol in workflow.md) [0a3ebcf]

## Phase 2: Domain Use Cases (1RM + Conversion) [checkpoint: 4f3f64f]
- [x] Task: Write failing unit tests for CalculateOneRepMaxUseCase (Epley formula, bounds: weight>0, reps>=1), ConvertAlternativeExerciseWeightUseCase (1RM ratio from historical logs, default coefficient fallback when history insufficient) (b4f2694)
- [x] Task: Implement both use cases in `domain/`; green unit tests (b4f2694)
- [x] Task: Conductor - User Manual Verification 'Domain Use Cases' (Protocol in workflow.md) [4f3f64f]

## Phase 3: Preset Management (Repository + ViewModel)
- [x] Task: Write failing ViewModel tests for preset CRUD + validation (name required, at least one exercise, per-exercise defaults respected, inline errors) [red] (4903e05)
- [x] Task: Implement PresetRepository + PresetViewModel (MVI sealed states Idle/Editing/List) + manual Factory; green unit tests (4903e05)
- [x] Task: Write Compose UI tests for preset library screen (list, create/edit/delete, exercise search dropdown); red before implementation
- [~] Task: Implement preset library UI (M3, semantic tokens, searchable dropdown); green UI tests on device
- [ ] Task: Conductor - User Manual Verification 'Preset Management' (Protocol in workflow.md)

## Phase 4: Routine Execution (Active-State Machine)
- [ ] Task: Write failing unit tests: routine start from preset, set completion (weight/reps/RPE/checkbox), rest-timer auto-start + screen-awake flag, exercise dropdown swap mid-routine, crash-recovery resumes at last completed set
- [ ] Task: Implement routine-execution state machine in WorkoutViewModel (replaces v2 set-matrix editing; keeps finish→journal+Health Connect path); green unit tests
- [ ] Task: Write failing Compose UI tests: exercise cards, set matrix fields (Weight/Reps/RPE/completion), automatic rest timer countdown, haptic calls on set/exercise completion, exercise dropdown, custom numeric quick-increment pad (+2.5 kg / +5 lb)
- [ ] Task: Implement execution UI (M3, keyed lazy lists, keep-screen-on flag during active routine); green UI tests on device
- [ ] Task: Conductor - User Manual Verification 'Routine Execution' (Protocol in workflow.md)

## Phase 5: Weight Analytics + Alternative Conversion (Vico overlay)
- [ ] Task: Document Vico charting in tech-stack.md (deviation note) BEFORE chart code; add dependency
- [ ] Task: Write failing unit tests: analytics state composition (weight-over-time series, Epley 1RM, max volume per exercise), alternative-conversion dialog state (base/target exercise, equivalent target weight)
- [ ] Task: Implement analytics bottom-sheet overlay (Vico chart) + conversion dialog wiring in ViewModel; green unit tests
- [ ] Task: Write Compose UI tests for the analytics overlay + alternative conversion flow; implement UI; green on device
- [ ] Task: Conductor - User Manual Verification 'Weight Analytics + Alternative Conversion' (Protocol in workflow.md)

## Phase 6: Integration + Backup/Health Connect + Docs
- [ ] Task: Extend backup/restore payload (presets, catalog additions, alternative mappings) so Drive full-backup round-trip still holds; tests for BackupWriter/BackupDataReader round-trip + restore
- [ ] Task: Verify routine finish still writes journal entry + Health Connect EXERCISE record (denied/airplane-mode paths never block logging); update affected wiki pages + run wiki lint (exit 0); commit wiki + code together
- [ ] Task: Conductor - User Manual Verification 'Integration + Backup/Health Connect' (Protocol in workflow.md)

## Phase 7: End-to-End Verification + Review Fixes
- [ ] Task: End-to-end JVM + instrumented pass; manual verification of all 3 user flows (A: preset build, B: routine execute, C: analytics/convert) on device; checkpoint commits per phase per workflow
- [ ] Task: Code review of changes; apply suggestions; record SHA (protocol in workflow.md)