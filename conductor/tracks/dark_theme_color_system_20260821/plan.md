# Implementation Plan: Medical App Color System (Light & Dark)

## Phase 1: Theme Foundation — Palettes, System Integration & Status Bar Fix
- [ ] Task: Write failing unit tests for semantic color system (TDD Red)
    - [ ] Create `ColorTest` verifying Light palette exposes exact Medical Standard values (#F8F9FA, #FFFFFF, #0A66C2, #20C997, #212529, #6C757D, #DC3545)
    - [ ] Add test verifying Dark palette exposes exact Eye-strain Reduction values (#121212, #1E1E1E, #4A90E2, #48D8A4, #E9ECEF, #A0AAB2, #EF5350)
    - [ ] Add test verifying `HealthJournalTheme(darkTheme = true)` applies dark scheme and `(darkTheme = false)` applies light scheme
    - [ ] Run tests (`CI=true ./gradlew testDebugUnitTest`) and confirm they FAIL as expected
- [ ] Task: Implement semantic color tokens and system theme integration (TDD Green)
    - [ ] Define both color palettes in `ui/theme/Color.kt`
    - [ ] Update `HealthJournalTheme` in `Theme.kt`: add `darkTheme: Boolean = isSystemInDarkTheme()` parameter mapping to `lightColorScheme()`/`darkColorScheme()`
    - [ ] Run tests and confirm GREEN; refactor for clarity
- [ ] Task: Fix invisible status bar defect
    - [ ] Enable edge-to-edge display with transparent status bar aligned to app background
    - [ ] Configure status bar icon appearance: dark icons in light mode, light icons in dark mode
    - [ ] Write UI test verifying status bar configuration follows theme mode
- [ ] Task: Execute UI tests on emulator in BOTH light and dark modes (TDD Blue)
- [ ] Task: Verify >80% coverage on new code
- [ ] Task: Commit Phase 1 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Full Semantic Token Migration
- [ ] Task: Audit and inventory hardcoded colors
    - [ ] Grep `app/src/main` for `Color(0x`, `Color.Blue`, `Color.White`, etc.
    - [ ] Record inventory of files requiring migration
- [ ] Task: Write failing UI tests asserting components resolve colors from MaterialTheme (TDD Red)
    - [ ] Test main list background equals `colorScheme.background` in light and dark modes
    - [ ] Test key components (cards, buttons, text, toasts) use theme roles
- [ ] Task: Migrate all screens/components to `MaterialTheme.colorScheme` (TDD Green)
    - [ ] Replace hardcoded colors in every inventoried file
    - [ ] Refactor shared composables to consume theme roles instead of passed-in colors
    - [ ] Run tests and confirm GREEN
- [ ] Task: Execute full UI test suite on emulator in BOTH light and dark modes (TDD Blue)
- [ ] Task: Verify >80% coverage on changed code
- [ ] Task: Commit Phase 2 changes and attach git note (Workflow steps 8–11)
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Final Validation & Cleanup
- [ ] Task: Run hardcoded-color audit — confirm zero absolute colors outside theme definition files
- [ ] Task: Full regression — all unit tests, lint/static analysis, and UI tests pass
- [ ] Task: Update documentation (tech-stack/design notes) if implementation deviated
- [ ] Task: Commit final cleanup changes and attach git note
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
