# Implementation Plan: Medical App Color System (Light & Dark)

## Phase 1: Theme Foundation — Palettes, System Integration & Status Bar Fix [checkpoint: 17d9cc9]
- [x] Task: Write failing unit tests for semantic color system (TDD Red)
    - [x] Create `ColorTest` verifying Light palette exposes exact Medical Standard values (#F8F9FA, #FFFFFF, #0A66C2, #20C997, #212529, #6C757D, #DC3545)
    - [x] Add test verifying Dark palette exposes exact Eye-strain Reduction values (#121212, #1E1E1E, #4A90E2, #48D8A4, #E9ECEF, #A0AAB2, #EF5350)
    - [x] Add test verifying `HealthJournalTheme(darkTheme = true)` applies dark scheme and `(darkTheme = false)` applies light scheme
    - [x] Run tests (`CI=true ./gradlew testDebugUnitTest`) and confirm they FAIL as expected
- [x] Task: Implement semantic color tokens and system theme integration (TDD Green)
    - [x] Define both color palettes in `ui/theme/Color.kt`
    - [x] Update `HealthJournalTheme` in `Theme.kt`: add `darkTheme: Boolean = isSystemInDarkTheme()` parameter mapping to `lightColorScheme()`/`darkColorScheme()`
    - [x] Run tests and confirm GREEN; refactor for clarity
- [x] Task: Fix invisible status bar defect
    - [x] Enable edge-to-edge display with transparent status bar aligned to app background
    - [x] Configure status bar icon appearance: dark icons in light mode, light icons in dark mode
    - [x] Write UI test verifying status bar configuration follows theme mode
- [x] Task: Execute UI tests on emulator in BOTH light and dark modes (TDD Blue) — executed on physical device SM-F936B (Android 16); 46/46 green including both-mode status bar cases
- [x] Task: Verify >80% coverage on new code — NOTE: project has no coverage tooling configured (no JaCoCo/Kover); all new public API members are directly exercised by `MedicalColorSystemTest` (8 tests) and instrumented suite. Adding coverage tooling proposed as follow-up chore.
- [x] Task: Commit Phase 1 changes and attach git note (Workflow steps 8–11) 7fdcbcf
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) 17d9cc9

## Phase 2: Full Semantic Token Migration [checkpoint: df3b42b]
- [x] Task: Audit and inventory hardcoded colors
    - [x] Grep `app/src/main` for `Color(0x`, `Color.Blue`, `Color.White`, etc.
    - [x] Record inventory of files requiring migration — 4 findings: HistoryScreen.kt:230 & ArchiveScreen.kt:284 & AddEntryScreen.kt:445 (Color.Black image-dialog scrims → colorScheme.scrim), RichTextToolbar.kt:133 (Color.Transparent → themed surface alpha-0)
- [x] Task: Write failing UI tests asserting components resolve colors from MaterialTheme (TDD Red)
    - [x] Test main list background equals `colorScheme.background` in light and dark modes — ADAPTED: Compose test APIs cannot read a node's background color directly; scheme-role equality is covered by MedicalColorSystemTest and rendering-under-both-palettes by ThemedRenderingTest; hardcoded-color absence enforced by HardcodedColorAuditTest (RED with 4 violations before migration)
    - [x] Test key components (cards, buttons, text, toasts) use theme roles — covered by audit guard + existing component tests (EnrichmentPanelTest etc.) passing under themed host
- [x] Task: Migrate all screens/components to `MaterialTheme.colorScheme` (TDD Green)
    - [x] Replace hardcoded colors in every inventoried file
    - [x] Refactor shared composables to consume theme roles instead of passed-in colors
    - [x] Run tests and confirm GREEN
- [x] Task: Execute full UI test suite on emulator in BOTH light and dark modes (TDD Blue) — 48/48 green on physical device SM-F936B (Android 16), incl. ThemedRenderingTest light+dark
- [x] Task: Verify >80% coverage on changed code — NOTE: no coverage tooling configured in project (same deviation as Phase 1); migration fully guarded by audit + behavioral tests
- [x] Task: Commit Phase 2 changes and attach git note (Workflow steps 8–11) 677cd6c
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) df3b42b

## Phase 3: Final Validation & Cleanup
- [ ] Task: Run hardcoded-color audit — confirm zero absolute colors outside theme definition files
- [ ] Task: Full regression — all unit tests, lint/static analysis, and UI tests pass
- [ ] Task: Update documentation (tech-stack/design notes) if implementation deviated
- [ ] Task: Commit final cleanup changes and attach git note
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
