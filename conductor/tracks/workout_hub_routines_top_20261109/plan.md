# Track workout_hub_routines_top_20261109 — Implementation Plan

## Technical Notes

- Target file: `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt`
- Test file: `app/src/androidTest/java/com/example/healthjournal/ui/screens/WorkoutScreenTest.kt`
- Documentation files:
  - `Docs/prd/workout-presets.md` (`FR-14`, `AC-11`)
  - `Docs/psd/workout-presets.md`
  - `Docs/tests/workout-presets.md`
- Work strictly follows TDD: Red → Green → Refactor → Verification.

## Phase 1: Test & Documentation Alignment [checkpoint: 20afe76]

- [x] Task: Update PSD and Tests documentation (86305c7)
    - [x] Update `Docs/psd/workout-presets.md` with top-placed Routines section in `IdleContent`
    - [x] Update `Docs/tests/workout-presets.md` with new test case for top routine placement
- [x] Task: Write failing Compose UI test in `WorkoutScreenTest.kt` (20afe76)
    - [x] Add test verifying that in idle state with saved presets, routine cards and presets button appear at the top without scrolling past generic activity buttons
    - [x] Run test and confirm expected failure against current bottom placement
- [x] Task: Conductor - User Manual Verification 'Phase 1: Test & Documentation Alignment' (Protocol in workflow.md)

## Phase 2: Implementation & Verification [checkpoint: fb387a5]

- [x] Task: Reorder sections in `IdleContent` in `WorkoutScreen.kt` (fb387a5)
    - [x] Move Routines header, routine cards, and `presets_entry` button above the "Choose a workout" header and generic type buttons
    - [x] Verify both empty-presets and with-presets states render correctly
    - [x] Run `WorkoutScreenTest` and verify all tests pass
- [x] Task: Run JVM unit tests and wiki lint (fb387a5)
    - [x] Run `./gradlew :app:testDebugUnitTest`
    - [x] Run `python3 wiki/lint.py` and confirm 0 errors
- [~] Task: Conductor - User Manual Verification 'Phase 2: Implementation & Verification' (Protocol in workflow.md)
