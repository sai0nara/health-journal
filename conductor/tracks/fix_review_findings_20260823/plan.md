# Implementation Plan: Review Fixes — Body Measurements Tracking

**Spec:** [./spec.md](./spec.md)

## Phase 1: DatePicker UTC→Local Fix (Review Finding 1)

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
