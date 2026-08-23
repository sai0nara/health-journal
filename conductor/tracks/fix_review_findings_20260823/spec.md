# Specification: Review Fixes — Body Measurements Tracking

## Overview

Address the three findings from the code review (Approved with Recommendations) of track `body_measurements_20260821` documented in `Docs/Review.md`: a timezone-dependent off-by-one date bug in the measurement capture sheet, loss of undo data on rapid successive deletions, and premature tombstone cleanup during background sync.

## Functional Requirements

### FR1 — Finding 1 [Medium]: Local-date correctness in DatePicker
- **FR1.1:** Selecting a date in the entry-sheet DatePicker must pass a timestamp representing that calendar date in the **device's local timezone** to `BodyMeasurementViewModel.onTimestampChanged()`, not raw UTC midnight epoch millis.
- **FR1.2:** In negative UTC-offset timezones (UTC-5, UTC-8, ...), the selected date must display identically in the form and saved entry after confirmation (no backward one-day shift).
- **FR1.3:** The UTC→local conversion must be extracted into a pure, unit-testable helper (no Android framework dependency).
- **FR1.4:** Existing behavior preserved: picker opens preselected to current form timestamp; Cancel discards changes.

### FR2 — Finding 2 [Low]: Multi-deletion undo integrity
- **FR2.1:** Replace the single `pendingUndoSnapshot` with a map keyed by entry ID (`pendingUndoSnapshots: MutableMap<String, BodyMeasurementEntry>`).
- **FR2.2:** Each `deleteEntry(entryId)` snapshots its own entry before deletion; successive deletions never overwrite other pending snapshots.
- **FR2.3:** `undoDelete()` accepts an optional `entryId`; when omitted it restores the most recently deleted entry (LIFO). Restored snapshots are removed from the map.
- **FR2.4:** Single-deletion Undo snackbar flow continues working unchanged.

### FR3 — Finding 3 [Low]: Tombstone purge ordering
- **FR3.1:** In `SyncWorker.doWork()`, `repository.clearDeletedEntries()` must run only **after** both the journal sync pipeline and the body measurements sync pipeline complete.
- **FR3.2:** No tombstone within the 30-day grace window may be purged while either pipeline still needs it.

### FR4 — Testing (TDD workflow)
- **FR4.1:** Failing unit tests first (Red), then implementation (Green), for:
  - UTC→local date conversion helper (including simulated negative-offset timezones)
  - ViewModel undo map: rapid successive deletions, LIFO default restore, restore by explicit ID, unknown-ID no-op
  - SyncWorker purge ordering (purge invoked strictly after both sync steps)
- **FR4.2:** New instrumented Compose UI test verifying the DatePicker confirm flow yields a locally-correct timestamp.
- **FR4.3:** Full existing unit + instrumented regression suites remain green (zero failures).

## Non-Functional Requirements
- **NFR1:** No Room schema/migration changes; Google Drive payload contracts (`body_measurements.json`) unchanged.
- **NFR2:** Semantic `MaterialTheme.colorScheme` tokens only; no hardcoded colors.
- **NFR3:** Maintain MVVM + manual ViewModelFactory patterns; no new dependencies (`java.util.Calendar` per review snippet is acceptable).
- **NFR4:** Follow `code_styleguides/general.md`; KDoc on public members.

## Acceptance Criteria
- **AC1:** Device at UTC-5: picking "August 23" shows/saves August 23 (not August 22).
- **AC2:** Two rapid deletions → tapping Undo on each snackbar restores both distinct entries exactly once; no wrong-entry restoration.
- **AC3:** A sync run finishes journal + measurement processing before removing any rows from `deleted_entries`.
- **AC4:** New unit tests pass; new instrumented DatePicker UI test passes on emulator; all existing suites green.
- **AC5:** No schema version bump, no Drive payload change.

## Out of Scope
- Batch-delete UI or undo-history list features.
- Refactors beyond what the findings require.
- Journal-side undo/sync logic changes beyond the purge-call relocation.
- Editing `Docs/Review.md` (the review artifact remains untouched).
