# Personal Card - Implementation Plan

## Phase 1: Data Layer Setup [checkpoint: 1973002]
- [x] Task: Create PersonalCard Room Entity with all embedded data classes
- [x] Task: Create PersonalCardDao with CRUD operations
- [x] Task: Add Gson TypeConverters for list fields
- [x] Task: Update JournalDatabase to version 12 with migration
- [x] Task: Create PersonalCardRepository
- [x] Task: Write unit tests for PersonalCardDao
- [x] Task: Conductor - User Manual Verification 'Data Layer' (Protocol in workflow.md)

## Phase 2: ViewModel & Business Logic [checkpoint: d4487df]
- [x] Task: Create PersonalCardViewModel with view/edit state management
- [x] Task: Implement add/remove operations for list fields
- [x] Task: Implement save/cancel with validation
- [x] Task: Integrate with Google Drive sync via SyncWorker
- [x] Task: Write unit tests for PersonalCardViewModel
- [x] Task: Conductor - User Manual Verification 'ViewModel' (Protocol in workflow.md)

## Phase 3: UI - View Mode [checkpoint: 69d5e56]
- [x] Task: Create PersonalCardScreen with card-based grid layout
- [x] Task: Implement DemographicsCard (view mode)
- [x] Task: Implement MedicalProfileCard (view mode)
- [x] Task: Implement MedicalHistoryCard (view mode)
- [x] Task: Implement EmergencyContactsCard (view mode)
- [x] Task: Add top app bar icon in HistoryScreen
- [x] Task: Write UI tests for PersonalCardScreen
- [x] Task: Conductor - User Manual Verification 'UI View Mode' (Protocol in workflow.md)

## Phase 4: UI - Edit Mode [checkpoint: 3dfb727]
- [x] Task: Implement edit mode toggle with save/cancel buttons
- [x] Task: Create list management components (add/remove items)
- [x] Task: Implement DemographicsCard (edit mode)
- [x] Task: Implement MedicalProfileCard (edit mode)
- [x] Task: Implement MedicalHistoryCard (edit mode)
- [x] Task: Implement EmergencyContactsCard (edit mode)
- [x] Task: Write UI tests for edit mode interactions
- [ ] Task: Conductor - User Manual Verification 'UI Edit Mode' (Protocol in workflow.md)

## Phase 5: Integration & Polish
- [x] Task: Wire navigation route in MainActivity
- [ ] Task: Add haptic feedback for destructive actions
- [ ] Task: Test light/dark theme rendering
- [ ] Task: Test cloud sync flow end-to-end
- [ ] Task: Write integration tests
- [ ] Task: Conductor - User Manual Verification 'Integration' (Protocol in workflow.md)

## Review Log

### 2026-08-27 — Code Review (Conductor protocol)
Reviewed range `8ab46f1~1..HEAD` (`feat/body-measurements`, 23 commits). Findings resolved in follow-up commit:

- **High:** `PersonalCardScreenTest.cardWithDemographics_displaysCorrectly` asserted `180.0 cm`/`75.0 kg` but UI renders truncated `180 cm`/`75 kg` (formatDouble strips `.0`) — assertion corrected.
- **High:** `editMode_disablesSaveWhenInvalid` asserted Save disabled on an empty card (empty demographics are valid, so Save was enabled) — test now seeds a future DOB and relies on validation-on-enter-edit.
- **Medium:** Out-of-range error strings used `%1$s`/`%2$s` placeholders but were rendered without format args (literal placeholders shown to user) — `ValidationResult.Invalid` now carries `formatArgs`.
- **Medium:** Removed dead "latest weight override" feature (`bodyMeasurementRepository` in ViewModel/Factory, `getLatestWeight()` in DAO/repository) — not spec'd and never wired in MainActivity.
- **Medium:** Fixed domain → viewmodel layering violation by moving `UnitConverter` to `data.local`.
- **Medium:** De-duplicated `formatDouble` (moved to `UnitConverter`); removed unused `error_invalid_number`.
- **Medium:** `validateDraft()` now runs on `startEditing()` so previously-stored invalid values disable Save.
- **Low:** Moved all PersonalCard UI literals into `strings.xml`; singleton `id` default; trailing newlines; `Features.md` section reformatted.

### 2026-08-27 — Edit-mode UX defect fixes (user-reported, cleanup decision)
- **Defect 1:** DOB field auto-inserted `-` moved the cursor before the last typed digit (`1986-|0`) — the field was bound to a `String`, so the reformatted text lost the cursor. Now bound to `TextFieldValue` with explicit selection computed from the digits typed before the cursor (`onDateOfBirthChanged(TextFieldValue)`); DatePicker path uses `onDateOfBirthSelected`.
- **Defect 2/3:** Height/weight fields regenerated their value from the parsed canonical `Double` on every keystroke, so trailing input like `185.`/`85.` was reformatted away and decimals could never be entered. Edit fields now bind to raw draft text (`draftHeightText`/`draftWeightText`); the parsed value is stored in `draftDemographics` for validation/save; raw text is re-baselined on load/start/cancel/unit-system change.

### 2026-08-27 — Follow-up defect fixes (Defects.md #1, #2)
- **DOB day digit:** cursor still jumped one position before the day digit (`1986-01-|0`) — the second dash exists once 7 digits are typed (`1986-01-0`), but the caret math only counted it at 8 digits. Dash-before-cursor now triggers at `digits.length >= 7`.
- **Height/weight precision:** `formatDouble` and metric `parseInput` now cap to 2 decimal places (HALF_UP) and strip trailing zeros, so over-precise values display as `178.35`, not `178.35745332432423`.