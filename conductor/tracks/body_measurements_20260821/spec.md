# Track Specification: Body Measurements Tracking

## Overview
Add dedicated body-composition tracking (weight + circumferential measurements) to complement journal logs and Health Connect metrics. Users capture granular measurements (chest, waist, glute, thighs, calves, biceps) via a speed-dial FAB and bottom-sheet form, browse them chronologically on a dedicated screen with a weight trend chart, and stay protected by undo-based deletion. Data persists locally first (offline-first) and joins the existing Google Drive sync pipeline.

## Functional Requirements

### FR1: Entry Point — Speed-Dial FAB
- History screen hosts a stacked FAB group: existing primary FAB (add journal entry) plus a new secondary `SmallFloatingActionButton` (tape measure icon) directly opening the Body Measurements sheet.

### FR2: Measurement Entry Sheet
- Compose Material 3 `ModalBottomSheet` containing:
    - Timestamp (defaults to current date/time, editable).
    - Fields: Weight, Chest, Waist, Glute, Thighs, Calves, Biceps (metric KG/CM only).
    - **Partial entries supported** — any single field may be saved alone.

### FR3: Input Ergonomics
- Soft keyboard defaults to `KeyboardType.Decimal`; fields chain `ImeAction.Next` for rapid cycling.
- Out-of-bounds/malformed input triggers INLINE field warnings (e.g., "Invalid decimal format") WITHOUT dismissing the sheet or wiping typed data.

### FR4: Domain Validation Rules
- Non-negative decimals only; sanity upper bounds (weight ≤ 500 kg, girths ≤ 300 cm) enforced in a domain validation component before persistence.
- At least one measurement value required to enable Save.

### FR5: Success Feedback
- On save: `HapticFeedbackType` medium-impact tactile confirmation, sheet dismissal, reactive refresh of downstream UI.

### FR6: Persistence (Offline-First)
- New Room entity (`BodyMeasurementEntry`: id, timestamp, nullable measurement columns) + DAO + database migration.
- Writes go to local Room immediately; UI observes reactively.

### FR7: Cloud Backup Integration
- Measurements join the EXISTING Google Drive sync pipeline (SyncWorker/SyncMerge) using the same sync-status patterns as journal entries.

### FR8: Measurements Screen
- Dedicated chronological list (timestamp + summary of non-null values, e.g., "78.5 kg · Waist 85 cm"), reachable from the History top bar; friendly empty state.

### FR9: Weight Trend Chart
- Basic line chart of weight over time rendered on the Measurements screen (Compose Canvas or lightweight chart library).

### FR10: Deletion with Undo
- Delete from the list triggers an Undo snackbar per Product Guidelines' "Safety Nets & Undo" principle; permanent deletion after snackbar expiry.

## Non-Functional Requirements
- Architecture: existing MVVM + manual ViewModel-factory DI; new `BodyMeasurementViewModel` exposes a single immutable `BodyMeasurementUiState` (StateFlow); no Hilt/MVI introduction.
- Form field values survive configuration changes (rotation/theme toggle) via ViewModel-scoped state.
- Fully functional offline; background sync via WorkManager when connectivity allows.
- All colors via the Medical Color System semantic tokens (no absolute colors).
- TDD per workflow; >80% coverage on new logic; UI tests pass in BOTH light and dark modes.

## Acceptance Criteria
1. Secondary tape-measure FAB is visible above the primary FAB and opens the measurement sheet.
2. A partial entry (e.g., waist only) saves successfully.
3. Invalid input shows an inline warning, retains all typed values, and does not dismiss the sheet.
4. Valid save produces haptic feedback, closes the sheet, and the entry appears on the Measurements screen.
5. The weight trend chart renders the chronological weight series including the newest point.
6. Deleting an entry shows an Undo snackbar; Undo restores the record.
7. Saved measurements are included in Google Drive backup/sync operations.
8. Rotating the device mid-entry preserves typed values.
9. All flows work with airplane mode ON (no connectivity).
10. All unit + instrumented tests green in light and dark modes.

## Out of Scope
- Imperial units / unit-system preference (DataStore) — follow-up track.
- At-rest encryption (SQLCipher) — dedicated security chore track covering the whole database.
- In-place editing of saved measurements (delete + re-create instead).
- Dashboard widgets; charts beyond the weight trend line.
- Health Connect write-back of body measurements.
