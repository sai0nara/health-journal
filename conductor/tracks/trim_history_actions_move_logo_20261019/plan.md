# Track trim_history_actions_move_logo_20261019 — Implementation Plan

## Phase 1: Failing tests (Red)

- [x] Task: History top-bar cleanup failing tests (36340d1)
    - [x] Add absence assertions to `HistoryScreenTest`: no `app_logo`, no `measurements_chart_icon`, no personal-card/export actions (call-site signature updates deferred to Phase 2)
    - [x] Run the new History assertions and confirm they fail (Red) with `JAVA_HOME` on JDK 21
- [x] Task: Main-screen logo failing tests (36340d1)
    - [x] Extend `MainScreenTest` with a failing assertion: `app_logo` composed and displayed
    - [x] Run the new Main assertions and confirm they fail (Red) with `JAVA_HOME` on JDK 21
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Failing tests (Red)' (Protocol in workflow.md)

## Phase 2: Implementation (Green + Refactor)

- [ ] Task: Move the logo to Main screen
    - [ ] Add the 32dp `app_logo` image (testTag `app_logo`, cd `cd_app_logo`) to the `MainScreen` TopAppBar leading slot, before the "Health Journal" title
    - [ ] Re-run Phase 1 Main tests and confirm green
- [ ] Task: Remove History actions and wiring
    - [ ] Remove logo Image and Person/Measurements/Export `IconButton`s from `HistoryScreen.kt`; drop the three callback params
    - [ ] Update `MainActivity.kt`; strip the params from `HistoryScreenTest`, `TruncationTest`, `ThemedRenderingTest`
    - [ ] Delete `cd_open_personal_card`, `history_cd_measurements`, `history_cd_export` from both catalogs; keep `cd_app_logo`
    - [ ] Re-run Phase 1 tests and confirm green; refactor and re-run to stay green
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md)

## Phase 3: Docs + verification (Blue + gates)

- [ ] Task: Ship docs and wiki updates
    - [ ] Update `Docs/psd/history-feed.md`, `Docs/tests/history-feed.md` (no top-bar shortcuts)
    - [ ] Update `Docs/psd/main-page-tiles.md`, `Docs/tests/main-page-tiles.md` (logo in bar)
    - [ ] Update `Docs/prd|psd/russian-localization.md` string counts after the catalog trim
    - [ ] Update wiki pages citing the touched sources (start `wiki/modules/ui-layer.md`)
- [ ] Task: Full verification
    - [ ] `./gradlew :app:testDebugUnitTest` green (JDK 21, `CI=true`)
    - [ ] UI tests green on the physical device (`-PcomposeRunner=true`, one class per run): `MainScreenTest`, `HistoryScreenTest`, `TruncationTest`, `ThemedRenderingTest`
    - [ ] `./gradlew build` green; `python3 wiki/lint.py` exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Docs + verification (Blue + gates)' (Protocol in workflow.md)
