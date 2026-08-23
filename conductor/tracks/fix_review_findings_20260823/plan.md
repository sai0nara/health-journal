# Implementation Plan: Review Fixes — Body Measurements Tracking

**Spec:** [./spec.md](./spec.md)

## Phase 1: DatePicker UTC→Local Fix (Review Finding 1)

- [x] Task: Write failing unit tests for UTC→local conversion helper (ed7c4a2)
    - [x] Add `UtcToLocalDateTest` under `app/src/test/java/com/example/healthjournal/domain/`
    - [x] Cover negative offsets (America/New_York, America/Los_Angeles): UTC-midnight millis maps to same local Y/M/D
    - [x] Cover positive offset (Europe/Berlin) and a DST-boundary date
    - [x] Run tests, confirm Red
- [ ] Task: Implement helper and wire into MeasurementEntrySheet
    - [ ] Add pure `UtcToLocalDate.toLocalMillis(utcMillis)` helper in `domain/` (java.util.Calendar, no Android deps)
    - [ ] In `MeasurementEntrySheet.kt` (~line 145), convert `selectedDateMillis` via helper before `viewModel.onTimestampChanged(...)`
    - [ ] Run tests, confirm Green
- [ ] Task: Add instrumented Compose UI test for DatePicker confirm flow
    - [ ] Extend `MeasurementEntrySheetTest.kt`: open picker, select date, tap OK, assert form timestamp renders selected local date (no off-by-one)
    - [ ] Execute on emulator until green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Multi-Deletion Undo Map (Review Finding 2)

- [ ] Task: Write failing ViewModel tests for undo snapshot map
    - [ ] Extend `BodyMeasurementViewModelTest.kt`: two rapid `deleteEntry()` calls keep both snapshots intact
    - [ ] Default `undoDelete()` restores most recently deleted entry (LIFO) exactly once
    - [ ] `undoDelete(entryId)` restores that specific entry; unknown ID is a safe no-op
    - [ ] Run tests, confirm Red
- [ ] Task: Implement map-based undo snapshots
    - [ ] Replace `pendingUndoSnapshot` with `pendingUndoSnapshots: MutableMap<String, BodyMeasurementEntry>`
    - [ ] `deleteEntry()` stores per-ID snapshot; `undoDelete(entryId: String? = null)` removes + re-inserts correct snapshot
    - [ ] Confirm existing single-delete snackbar call sites compile unchanged (default param)
    - [ ] Run tests, confirm Green
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Tombstone Purge Ordering (Review Finding 3)

- [ ] Task: Write failing SyncWorker purge-ordering test
    - [ ] New unit test verifying `clearDeletedEntries()` is invoked strictly after both journal upload and body-measurements upload complete
    - [ ] Run tests, confirm Red
- [ ] Task: Relocate purge call in SyncWorker.doWork()
    - [ ] Move `repository.clearDeletedEntries()` from step 7 (~line 200) to after successful measurements upload, immediately before `Result.success()`
    - [ ] Preserve tombstone grace-period comment; update wording if needed
    - [ ] Run tests, confirm Green
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Full Regression & Coverage Verification

- [ ] Task: Run complete unit test suite (`CI=true ./gradlew testDebugUnitTest`) — zero failures
- [ ] Task: Run complete instrumented suite on emulator (`./gradlew connectedDebugAndroidTest`) — zero failures
- [ ] Task: Verify coverage >80% for changed code paths
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
