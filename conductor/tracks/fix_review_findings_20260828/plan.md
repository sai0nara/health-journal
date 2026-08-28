# Fix Review Findings - Implementation Plan

## Phase 1: Replace Hardcoded Cancel Strings (FR-1)
- [ ] Task: Verify R.string.action_cancel exists in strings.xml; add if missing
    - [ ] Check app/src/main/res/values/strings.xml for action_cancel resource
- [ ] Task: Replace Text("Cancel") with Text(stringResource(R.string.action_cancel)) in AddStringDialog
- [ ] Task: Replace Text("Cancel") with Text(stringResource(R.string.action_cancel)) in AddMedicationDialog
- [ ] Task: Replace Text("Cancel") with Text(stringResource(R.string.action_cancel)) in AddContactDialog
- [ ] Task: Write failing UI test verifying dialogs use localized cancel resource
    - [ ] Run test and confirm it fails (Red phase)
- [ ] Task: Implement to pass tests (Green phase)
    - [ ] Run UI tests and confirm they pass
- [ ] Task: Conductor - User Manual Verification 'Replace Hardcoded Cancel Strings' (Protocol in workflow.md)

## Phase 2: Fix Compiler Warnings in ValidationResult Tests (FR-2)
- [ ] Task: Write failing test verifying explicit ValidationResult typing compiles without warnings
    - [ ] Run test and confirm it fails (Red phase)
- [ ] Task: Implement explicit type annotations in ValidationResultTest.kt
    - [ ] Change `val result = ValidationResult.Valid` to `val result: ValidationResult = ValidationResult.Valid` in the Valid test
    - [ ] Change `val result = ValidationResult.Invalid(...)` to `val result: ValidationResult = ValidationResult.Invalid(...)` in the Invalid test
    - [ ] Update assertion `(result as ValidationResult.Invalid)` to `(result as? ValidationResult.Invalid)?.errorResId`
    - [ ] Run tests and confirm they pass (Green phase)
- [ ] Task: Verify no new compiler warnings introduced
    - [ ] Run gradle compile task and check for warnings
- [ ] Task: Conductor - User Manual Verification 'Fix Compiler Warnings' (Protocol in workflow.md)

## Phase 3: Regression & Final Verification
- [ ] Task: Run full unit test suite
- [ ] Task: Run UI test suite on emulator
- [ ] Task: Verify code coverage and style compliance
- [ ] Task: Conductor - User Manual Verification 'Regression & Final Verification' (Protocol in workflow.md)
