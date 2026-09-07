# Implementation Plan: Workout Tracking (v1)

## Phase 1: Domain + Data Layer
- [ ] Task: Write failing unit tests for workout/session entities, validation, and calorie estimation
- [ ] Task: Add Room workout session entity + DAO + versioned migration (exportSchema = true)
- [ ] Task: Implement workout repository (Room) + validation use cases; green unit tests
- [ ] Task: Write failing tests for the Health Connect exercise data source (behind an interface; fake in unit tests)
- [ ] Task: Implement EXERCISE permission request + exercise record read/write data source
- [ ] Task: Conductor - User Manual Verification 'Workout Data Layer' (Protocol in workflow.md)

## Phase 2: Session Engine + ViewModel (MVI)
- [ ] Task: Write failing ViewModel tests for state transitions (Idle→Configuring→Active→Paused→Summary→Error) and crash-recovery detection
- [ ] Task: Implement WorkoutViewModel with sealed WorkoutUiState over StateFlow plus manual Factory (no DI framework)
- [ ] Task: Implement timer scope, haptic events, batched session persistence (every few seconds), and Resume/Discard recovery
- [ ] Task: Rerun unit tests to green; verify >80% coverage on new code
- [ ] Task: Conductor - User Manual Verification 'Workout Session Engine' (Protocol in workflow.md)

## Phase 3: UI + Navigation
- [ ] Task: Write failing Compose UI tests for discovery, configuration, active session, manual-log form, and summary screens
- [ ] Task: Implement Workout screens in Material 3 (semantic color tokens) + History entry point + navigation wiring
- [ ] Task: Run instrumented UI tests until green on device
- [ ] Task: Conductor - User Manual Verification 'Workout UI' (Protocol in workflow.md)

## Phase 4: Integration & Polish
- [ ] Task: End-to-end test: timed session → summary → journal entry → Drive sync; kill mid-session → Resume restores state
- [ ] Task: Verify Health Connect write with permission granted and graceful denial path; airplane-mode logging pass
- [ ] Task: Test light/dark theme rendering of workout UI
- [ ] Task: Update affected wiki pages (agent owns the vault) and run wiki lint (exit 0); commit wiki + code together
- [ ] Task: Conductor - User Manual Verification 'Workout Integration' (Protocol in workflow.md)
