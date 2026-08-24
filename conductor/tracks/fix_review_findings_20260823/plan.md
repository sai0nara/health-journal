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

## Phase 3: Tombstone Purge Ordering (Review Finding 3) [checkpoint: 2ddb4a2]

- [x] Task: Write failing SyncWorker purge-ordering test [01d090a]
    - [x] New instrumented test verifying `clearDeletedEntries()` is invoked strictly after both journal upload and body-measurements upload complete (observes tombstone state inside mocked Drive download/upload answers; expired + fresh tombstones seeded)
    - [x] Run tests, confirm Red on device
- [x] Task: Relocate purge call in SyncWorker.doWork() [01d090a]
    - [x] Move `repository.clearDeletedEntries()` from step 7 (~line 200) to after successful measurements upload, immediately before `Result.success()`
    - [x] Preserve tombstone grace-period comment; wording updated to cover both pipelines
    - [x] Run tests, confirm Green (SyncDownloadTest 6/6)
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [2ddb4a2: report in git note, user confirmed YES; first round surfaced deletion-propagation gap → Phase 3 Expansion]

### Phase 3 Expansion: Cross-Device Deletion Propagation (user-reported, 2026-08-24)
Manual verification revealed deletions never propagate between devices:
tombstones stayed local and merges keep cloud-absent local rows. Journal
pipeline shares the defect but stays OUT OF SCOPE (future track); the ledger
is shared so journals benefit once a future track wires them in.

- [x] Task: TDD Red — codec + worker propagation tests [15baab8]
    - [x] `MeasurementTombstonePayloadTest`: null/garbage → empty, roundtrip (unit Red)
    - [x] `SyncDownloadTest.testSyncWorker_RemoteMeasurementTombstoneRemovesLocalEntry` (Red on device)
    - [x] `SyncDownloadTest.testSyncWorker_NewerLocalEditBeatsRemoteTombstone` (LWW guard)
- [x] Task: Implement tombstone ledger sync [15baab8]
    - [x] New sibling Drive file `body_measurements_tombstones.json` (`MEASUREMENTS_TOMBSTONES_FILE`) — existing payload contracts untouched (NFR1 preserved)
    - [x] `MeasurementTombstonePayload` Gson codec (null/garbage-safe, mirrors MeasurementSyncPayload style)
    - [x] SyncWorker: import remote ledger before filtering (newest deletedAt wins), LWW-guarded explicit removal of locally tombstoned rows (`importAll` is upsert-only), publish merged ledger after measurements upload; failure → `Result.retry()`
    - [x] Green: unit suite BUILD SUCCESSFUL; SyncDownloadTest 8/8 on SM-F936B

## Phase 4: Full Regression & Coverage Verification

- [ ] Task: Run complete unit test suite (`CI=true ./gradlew testDebugUnitTest`) — zero failures
- [ ] Task: Run complete instrumented suite on emulator (`./gradlew connectedDebugAndroidTest`) — zero failures
- [ ] Task: Verify coverage >80% for changed code paths
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
