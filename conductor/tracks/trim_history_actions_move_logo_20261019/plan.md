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

- [x] Task: Move the logo to Main screen (f82d477)
    - [x] Add the 32dp `app_logo` image (testTag `app_logo`, cd `cd_app_logo`) to the `MainScreen` TopAppBar leading slot, before the "Health Journal" title
    - [x] Re-run Phase 1 Main tests and confirm green
- [x] Task: Remove History actions and wiring (bbfad8c)
    - [x] Remove logo Image and Person/Measurements/Export `IconButton`s from `HistoryScreen.kt`; drop the three callback params
    - [x] Update `MainActivity.kt`; strip the params from `HistoryScreenTest`, `TruncationTest`, `ThemedRenderingTest`
    - [x] Delete `cd_open_personal_card`, `history_cd_measurements`, `history_cd_export` from both catalogs; keep `cd_app_logo`
    - [x] Re-run Phase 1 tests and confirm green; refactor and re-run to stay green
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md)

## Phase 3: Docs + verification (Blue + gates)

- [x] Task: Ship docs and wiki updates (239b35a)
    - [x] Update `Docs/psd/history-feed.md`, `Docs/tests/history-feed.md` (top bar: back + title + actions-only overflow; stale workout-hub case replaced)
    - [x] Update `Docs/psd/main-page-tiles.md`, `Docs/tests/main-page-tiles.md` (dashboard brand mark, T-11)
    - [x] Update `Docs/prd|psd/russian-localization.md` string counts (367 → 364) after the catalog trim
    - [x] Update wiki pages citing the touched sources (`wiki/modules/ui-layer.md`) and bump dates on remaining citing pages
- [x] Task: Full verification
    - [x] `./gradlew :app:testDebugUnitTest` green (612 tests, 0 failures, JDK 21, `CI=true`)
    - [x] UI tests green on the physical device (`-PcomposeRunner=true`, one class per run): `MainScreenTest` 5/5, `HistoryScreenTest` 10/10, `TruncationTest` 1/1, `ThemedRenderingTest` 2/2
    - [x] `./gradlew build` green; `python3 wiki/lint.py` exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Docs + verification (Blue + gates)' (Protocol in workflow.md)
