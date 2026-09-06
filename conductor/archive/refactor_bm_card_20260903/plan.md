# Plan: Refactor Body Measurements Card View

## Phase 1: Tests

- [x] Task: Write UI tests for refactored `MeasurementCard`
    - [ ] Test: card shows weight on its own line when present
    - [ ] Test: card shows body circumference params in grid rows
    - [ ] Test: card omits null params from display
    - [ ] Test: card with only weight displays correctly (no empty grid)
    - [ ] Test: card with only circumferences displays correctly

## Phase 2: Implementation

- [x] Task: Refactor `MeasurementCard` composable in `MeasurementsScreen.kt`
    - [ ] Replace summary string with structured layout (weight line + FlowRow grid + date)
    - [ ] Extract circumference params into a grid composable
    - [ ] Move date to bottom-left
    - [ ] Verify existing tests still pass

## Phase 3: Verification

- [x] Task: Run full test suite and verify
    - [x] Run `./gradlew :app:testDebugUnitTest`
    - [x] Run `./gradlew connectedAndroidTest` if emulator available
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase: Review Fixes

- [x] Task: Apply review suggestions 4e728e5
