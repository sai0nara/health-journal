# Implementation Plan: Body Analytics — Per-Parameter Trends & Goal Lines

**Spec:** [./spec.md](./spec.md)

## Phase 1: Goal Persistence Layer [checkpoint: 34e5125]

- [x] Task: Write failing unit tests for goal validation rules (Red) [ad2ad41]
    - [x] `GoalValidatorTest`: positive-value rule; upper sanity caps (Weight ≤ 400 kg, girths ≤ 300 cm); metric unit label mapping per parameter (kg/cm) [caps set to 500/300 for parity with ValidateMeasurements]
    - [x] Run tests, confirm Red [compile Red: Unresolved reference GoalValidator]
- [x] Task: Add Room `goals` table with schema migration (Green) [9fc7275]
    - [x] `GoalEntity(parameter_id PK, target REAL, lastModified INTEGER)` + `GoalDao` (upsert, getAll, clear, deleteById) [+ observeAll Flow]
    - [x] Bump `JournalDatabase` version with written Migration (no destructive fallback) [v10→v11, MIGRATION_10_11]
    - [x] Implement `GoalValidator` to Green; full unit suite green
- [x] Task: Implement `GoalsRepository` following existing repository conventions [112e73c]
    - [x] Unit tests with strict-mock DAO (save/clear/getAll/observe) Red→Green
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) [34e5125: report in git note, user confirmed YES]

## Phase 2: Tabbed Analytics Section & Extended Chart [checkpoint: a01fbb5]

- [x] Task: Write failing instrumented tests for the analytics section (Red) [66f62bf]
    - [x] Extend `MeasurementScreenTest`: seven parameter tabs render; tapping a tab swaps the plotted series; chart exposes per-param test tag; empty-param page shows friendly empty message
    - [x] Run on device, confirm Red [compile Red: Unresolved BodyAnalyticsViewModel; two test-side bugs fixed via semantics dump — pager waitUntil polling, off-screen tab scroll, CALF vs 'Calves' tag]
- [x] Task: Extract and extend the Canvas chart into `ParamTrendChart` component [66f62bf]
    - [x] Generalize existing weight chart plotting (ascending series, min/max scaling) for any parameter column [toParamTrend(field); scaling includes goal so line always visible]
    - [x] Dashed horizontal goal line + value/unit label when goal present (FR3)
    - [x] Translucent delta area fill between polyline and goal line using semantic tokens (FR4)
- [x] Task: Replace chart section with `ScrollableTabRow` + `HorizontalPager` [c523fa5]
    - [x] Two-way sync of tab selection and pager page (FR1)
    - [x] `BodyAnalyticsViewModel`: per-param point derivation (ascending re-sort), selected-tab state, goals flow injection (FR9); unit tests Red→Green [5 tests; setMain/resetMain for viewModelScope]
    - [x] Instrumented tests Green on device [11/11 MeasurementScreenTest]
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) [user: YES]

## Phase 3: Goal Setting Flow [checkpoint: 47482c5]

- [x] Task: Write failing instrumented tests for goal dialog (Red) [3ff496c]
    - [x] "Set Goal" affordance opens bottom sheet pre-filled with current goal; Save persists via repository (mockk verify); Clear deletes; out-of-bounds input shows inline error and blocks save (FR5/FR6) [coVerify for suspend DAO; Red: 3 new tests fail, legacy 11 pass]
    - [x] Run on device, confirm Red
- [x] Task: Implement goal bottom-sheet dialog + view-model intents (Green) [695ed5a, revision 3e04d99]
    - [x] Validation wiring reuses `GoalValidator`; inline error parity with measurement fields
    - [x] All instrumented tests Green; full unit suite green [14/14 MeasurementScreenTest; unit BUILD SUCCESSFUL]
    - [x] Revision from user feedback: per-field realistic caps (calf/bicep 75cm, thigh 120cm, torso 200cm) shared by capture+goal validation; pager page wrapped in Column — header/chart overlap was hiding the graph [0995a1b]
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [round 1 NO → caps/layout revision; round 2 user: YES]

## Phase 4: Goal Sync via Google Drive

- [x] Task: Write failing unit tests for `GoalSyncPayload` codec (Red) [d264dfe]
    - [x] null/garbage → empty list; JSON array roundtrip mirrors `MeasurementSyncPayload` style [GoalSyncPayloadTest: 4 tests]
    - [x] Confirm Red, then implement to Green
- [x] Task: Write failing SyncWorker goal-sync instrumented tests (Red) [d264dfe, 3b85690]
    - [x] Remote goal with newer lastModified upserts local row
    - [x] Local goal absent from valid cloud snapshot is pruned (cleared goals propagate) [GoalSyncMergeTest: 7 tests]
    - [x] Goals snapshot uploaded as sibling file `body_measurements_goals.json` (`MEASUREMENTS_GOALS_FILE`); upload failure → `Result.retry()`
    - [x] Confirm Red on device [compile Red → Green]
- [x] Task: Integrate goals pipeline into SyncWorker (Green) [7def8ba]
    - [x] Import+merge+prune after measurements pipeline, before ledger publish/final success; failures → retry
    - [x] Existing payload contracts untouched (NFR2); all sync tests Green [full unit suite BUILD SUCCESSFUL]
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

## Phase 5: Full Regression & Coverage Verification

- [ ] Task: Run complete unit test suite (`CI=true ./gradlew testDebugUnitTest`) — zero failures
- [ ] Task: Run complete instrumented suite on device (`CI=true ./gradlew connectedDebugAndroidTest`) — zero failures
- [ ] Task: Verify coverage >80% for changed code paths [note tooling status]
- [ ] Task: Lint gate (`CI=true ./gradlew :app:lintDebug`) + hardcoded-color audit on new UI files
- [ ] Task: Conductor - User Manual Verification 'Phase 5' (Protocol in workflow.md)
