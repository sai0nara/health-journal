# Implementation Plan: Body Measurements Tracking

## Phase 1: Data Foundation — Entity, Migration, Validation & Repository
- [~] Task: Write failing unit tests for data model and validation (TDD Red)
    - [ ] `BodyMeasurementEntryTest`: defaults (`syncStatus=PENDING_SYNC`, `isLocalOnly=true`), all 7 measurement fields nullable
    - [ ] `ValidateMeasurementsTest`: rejects malformed decimals, negatives, weight > 500 kg, girths > 300 cm with field-level errors; requires ≥ 1 value; accepts partial entries
    - [ ] Run tests and confirm FAIL as expected
- [ ] Task: Implement entity, validator and repository to pass tests (TDD Green)
    - [ ] `BodyMeasurementEntry` Room entity + `BodyMeasurementDao` (insert, observe chronological, getById, tombstone delete/restore)
    - [ ] `ValidateMeasurements` domain component returning `Map<MeasurementField, String>` errors
    - [ ] `BodyMeasurementRepository` wrapping the DAO
    - [ ] Run tests and confirm GREEN; refactor for clarity
- [ ] Task: Database migration (TDD)
    - [ ] Failing instrumented `MigrationTest` case 9→10 preserving existing journal data (RED)
    - [ ] Implement `MIGRATION_9_10` creating `body_measurements` table; bump DB version; confirm GREEN
- [ ] Task: Instrumented DAO roundtrip test (insert/observe/delete-restore)
- [ ] Task: Verify >80% coverage on new code (note tooling deviation if applicable)
- [ ] Task: Commit Phase 1 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Capture Flow — ViewModel, Speed-Dial FAB & Bottom Sheet
- [ ] Task: Write failing unit tests for `BodyMeasurementViewModel` (TDD Red)
    - [ ] Immutable `BodyMeasurementUiState` defaults (timestamp=now, empty fields, metric)
    - [ ] `OnFieldChanged` updates value and clears that field's error; malformed input sets inline error WITHOUT clearing typed value
    - [ ] Save enabled only when ≥ 1 valid value; `OnSaveClicked` persists via repository, emits save-success event, resets form
- [ ] Task: Implement ViewModel to pass tests (TDD Green); refactor
- [ ] Task: Write failing UI tests for capture flow (TDD Red)
    - [ ] Secondary tape-measure FAB stacked above primary FAB; opens `ModalBottomSheet`
    - [ ] Decimal keyboard hint + ImeAction.Next chaining present
    - [ ] Partial entry (waist only) saves end-to-end; invalid input shows inline warning and retains text
    - [ ] Form state survives configuration change (rotation simulation)
- [ ] Task: Implement speed-dial FAB group on History screen + `MeasurementEntrySheet` with haptic success feedback (TDD Green)
- [ ] Task: Execute UI tests on device in BOTH light and dark modes (TDD Blue)
- [ ] Task: Commit Phase 2 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Measurements Screen — List, Trend Chart & Undo Delete
- [ ] Task: Write failing unit tests for screen logic (TDD Red)
    - [ ] Chronological ordering of observed entries
    - [ ] Summary formatting "78.5 kg · Waist 85 cm" (non-null fields only)
    - [ ] Weight trend series mapping (sorted, non-null weights only)
- [ ] Task: Implement screen state/formatting to pass tests (TDD Green)
- [ ] Task: Write failing UI tests (TDD Red)
    - [ ] Navigation entry from History top bar opens Measurements screen
    - [ ] Empty state shown when no records
    - [ ] Saved entries render as cards with summaries; newest first
    - [ ] Delete triggers Undo snackbar; tapping Undo restores the record
- [ ] Task: Implement `MeasurementsScreen`: list cards, empty state, `WeightTrendChart` line chart via Compose Canvas (no new dependency), delete-with-Undo (TDD Green)
- [ ] Task: Execute UI tests on device in BOTH light and dark modes (TDD Blue)
- [ ] Task: Commit Phase 3 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Drive Sync Integration & Final Validation
- [ ] Task: Write failing unit tests for sync inclusion (TDD Red)
    - [ ] Measurement records serialize into the existing sync payload format
    - [ ] `SyncMerge` merges remote measurement records and processes measurement tombstones
    - [ ] New records marked `PENDING_SYNC` are uploaded by `SyncWorker` flow
- [ ] Task: Extend SyncWorker/SyncMerge/DriveServiceHelper pipeline for body measurements (TDD Green)
- [ ] Task: Full regression — hardcoded-color audit, lint, all unit tests, all instrumented tests in both modes
- [ ] Task: Update documentation (tech-stack/product notes) if implementation deviated
- [ ] Task: Commit Phase 4 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
