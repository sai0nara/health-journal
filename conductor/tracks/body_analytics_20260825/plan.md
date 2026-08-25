# Implementation Plan: Body Analytics — Per-Parameter Trends & Goal Lines

**Spec:** [./spec.md](./spec.md)

## Phase 1: Goal Persistence Layer

- [x] Task: Write failing unit tests for goal validation rules (Red) [ad2ad41]
    - [x] `GoalValidatorTest`: positive-value rule; upper sanity caps (Weight ≤ 400 kg, girths ≤ 300 cm); metric unit label mapping per parameter (kg/cm) [caps set to 500/300 for parity with ValidateMeasurements]
    - [x] Run tests, confirm Red [compile Red: Unresolved reference GoalValidator]
- [ ] Task: Add Room `goals` table with schema migration (Green)
    - [ ] `GoalEntity(parameter_id PK, target REAL, lastModified INTEGER)` + `GoalDao` (upsert, getAll, clear, deleteById)
    - [ ] Bump `JournalDatabase` version with written Migration (no destructive fallback)
    - [ ] Implement `GoalValidator` to Green; full unit suite green
- [ ] Task: Implement `GoalsRepository` following existing repository conventions
    - [ ] Unit tests with strict-mock DAO (save/clear/getAll/observe) Red→Green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Tabbed Analytics Section & Extended Chart

- [ ] Task: Write failing instrumented tests for the analytics section (Red)
    - [ ] Extend `MeasurementScreenTest`: seven parameter tabs render; tapping a tab swaps the plotted series; chart exposes per-param test tag; empty-param page shows friendly empty message
    - [ ] Run on device, confirm Red
- [ ] Task: Extract and extend the Canvas chart into `ParamTrendChart` component
    - [ ] Generalize existing weight chart plotting (ascending series, min/max scaling) for any parameter column
    - [ ] Dashed horizontal goal line + value/unit label when goal present (FR3)
    - [ ] Translucent delta area fill between polyline and goal line using semantic tokens (FR4)
- [ ] Task: Replace chart section with `ScrollableTabRow` + `HorizontalPager`
    - [ ] Two-way sync of tab selection and pager page (FR1)
    - [ ] `BodyAnalyticsViewModel`: per-param point derivation (ascending re-sort), selected-tab state, goals flow injection (FR9); unit tests Red→Green
    - [ ] Instrumented tests Green on device
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Goal Setting Flow

- [ ] Task: Write failing instrumented tests for goal dialog (Red)
    - [ ] "Set Goal" affordance opens bottom sheet pre-filled with current goal; Save persists via repository (mockk verify); Clear deletes; out-of-bounds input shows inline error and blocks save (FR5/FR6)
    - [ ] Run on device, confirm Red
- [ ] Task: Implement goal bottom-sheet dialog + view-model intents (Green)
    - [ ] Validation wiring reuses `GoalValidator`; inline error parity with measurement fields
    - [ ] All instrumented tests Green; full unit suite green
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Goal Sync via Google Drive

- [ ] Task: Write failing unit tests for `GoalSyncPayload` codec (Red)
    - [ ] null/garbage → empty list; JSON array roundtrip mirrors `MeasurementSyncPayload` style
    - [ ] Confirm Red, then implement to Green
- [ ] Task: Write failing SyncWorker goal-sync instrumented tests (Red)
    - [ ] Remote goal with newer lastModified upserts local row
    - [ ] Local goal absent from valid cloud snapshot is pruned (cleared goals propagate)
    - [ ] Goals snapshot uploaded as sibling file `body_measurements_goals.json` (`MEASUREMENTS_GOALS_FILE`); upload failure → `Result.retry()`
    - [ ] Confirm Red on device
- [ ] Task: Integrate goals pipeline into SyncWorker (Green)
    - [ ] Import+merge+prune after measurements pipeline, before ledger publish/final success; failures → retry
    - [ ] Existing payload contracts untouched (NFR2); all sync tests Green
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

## Phase 5: Full Regression & Coverage Verification

- [ ] Task: Run complete unit test suite (`CI=true ./gradlew testDebugUnitTest`) — zero failures
- [ ] Task: Run complete instrumented suite on device (`CI=true ./gradlew connectedDebugAndroidTest`) — zero failures
- [ ] Task: Verify coverage >80% for changed code paths [note tooling status]
- [ ] Task: Lint gate (`CI=true ./gradlew :app:lintDebug`) + hardcoded-color audit on new UI files
- [ ] Task: Conductor - User Manual Verification 'Phase 5' (Protocol in workflow.md)
