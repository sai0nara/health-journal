# Implementation Plan: Review Fixes — Body Measurements Tracking

**Spec:** [./spec.md](./spec.md)

## Phase 1: DatePicker UTC→Local Fix (Review Finding 1) [checkpoint: 94253f5]

- [x] Task: Write failing unit tests for UTC→local conversion helper (ed7c4a2)
    - [x] Add `UtcToLocalDateTest` under `app/src/test/java/com/example/healthjournal/domain/`
    - [x] Cover negative offsets (America/New_York, America/Los_Angeles): UTC-midnight millis maps to same local Y/M/D
    - [x] Cover positive offset (Europe/Berlin) and a DST-boundary date
    - [x] Run tests, confirm Red
- [x] Task: Implement helper and wire into MeasurementEntrySheet (0560cb1)
    - [x] Add pure `UtcToLocalDate.toLocalMillis(utcMillis)` helper in `domain/` (java.util.Calendar, no Android deps)
    - [x] In `MeasurementEntrySheet.kt` (~line 145), convert `selectedDateMillis` via helper before `viewModel.onTimestampChanged(...)`
    - [x] Run tests, confirm Green
- [x] Task: Add instrumented Compose UI test for DatePicker confirm flow (05ffa53)
    - [x] Extend `MeasurementEntrySheetTest.kt`: open picker, select date, tap OK, assert form timestamp renders selected local date (no off-by-one)
    - [x] Execute on emulator until green
- [x] Task: Restrict future-dated measurement saves (manual-verification feedback) [9e9e9c6, 5c9fea4, bd101f2]
    - [x] Stabilize flaky `partialEntry_savesAndDismissesSheet` instrumented test: IME occluded Save tap; close keyboard pre-click + polling `coVerify` [9e9e9c6]
    - [x] TDD Red→Green: reject `timestamp > now` at save in `BodyMeasurementViewModel.onSaveClicked()` (parity with JournalViewModel.addEntry); current time allowed [5c9fea4]
    - [x] TDD Red→Green: inline alert "Future dates cannot be saved" under date row + Save disabled while future-dated (`timestampError` in UiState); unit + instrumented tests (75/75 green) [bd101f2]
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) [94253f5: report in git note, user confirmed YES]

## Phase 2: Multi-Deletion Undo Map (Review Finding 2) [checkpoint: 4329db0]

- [x] Task: Write failing ViewModel tests for undo snapshot map [23b5003]
    - [x] Extend `BodyMeasurementViewModelTest.kt`: two rapid `deleteEntry()` calls keep both snapshots intact
    - [x] Default `undoDelete()` restores most recently deleted entry (LIFO) exactly once
    - [x] `undoDelete(entryId)` restores that specific entry; unknown ID is a safe no-op
    - [x] Run tests, confirm Red (target API absent → compile Red; strict-mock deleteEntry stub added during Green)
- [x] Task: Implement map-based undo snapshots [23b5003]
    - [x] Replace `pendingUndoSnapshot` with `pendingUndoSnapshots: MutableMap<String, BodyMeasurementEntry>`
    - [x] `deleteEntry()` stores per-ID snapshot; `undoDelete(entryId: String? = null)` removes + re-inserts correct snapshot (LIFO default)
    - [x] Confirm existing single-delete snackbar call sites compile unchanged (default param)
    - [x] Run tests, confirm Green (full unit suite BUILD SUCCESSFUL)
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) [4329db0: report in git note, user confirmed YES]

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
