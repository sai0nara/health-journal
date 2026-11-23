# Implementation Plan: Today Dashboard

## Phase 1: Dashboard Data Layer & ViewModel Aggregation

- [x] Task: Add `WORKOUT_WEEKLY_TARGET` goal support (default `4`) to `GoalsRepository`
    - [x] Write failing unit tests for reading, updating, and per-profile isolation of `WORKOUT_WEEKLY_TARGET` (Red)
    - [x] Implement `WORKOUT_WEEKLY_TARGET` constant, default `4` fallback, and update methods in `GoalsRepository` (Green)
    - [x] Refactor and verify unit tests pass
- [x] Task: Create `TodayDashboardUiState` and `TodayDashboardViewModel` aggregating active-profile metrics
    - [x] Write failing unit tests for: 5-parameter Vitals slide ordering & hiding unlogged slides (minimum 1 empty slide), weekly workout count vs target, latest weight + weekly delta + sparkline points (metric/imperial), top-5 Recent Activity merge/sort across Vitals/Workouts/Measurements/Journal, and reactive profile-switching updates (Red)
    - [x] Implement `TodayDashboardViewModel` + `Factory` observing `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`, `ProfileRepository`, and `PresetRepository` via Kotlin `Flow`s (Green)
    - [x] Refactor helper mappers and verify unit tests pass (>80% coverage)
- [x] Task: Conductor - User Manual Verification 'Phase 1: Dashboard Data Layer & ViewModel Aggregation' (Protocol in workflow.md)

## Phase 2: Navigation Shell, Bottom Bar & Decluttered Destinations

- [x] Task: Introduce 4-tab persistent bottom navigation (`Today`, `History`, `Trends`, `More`) and update Top Bar
    - [x] Write failing UI/navigation tests verifying cold launch on `Today`, switching between `Today`/`History`/`Trends`/`More`, top bar `ProfileSwitcher` opening profile switching / `PersonalCardScreen`, and absence of top-right gear icon (Red)
    - [x] Implement bottom navigation bar scaffold in `MainActivity` / navigation host, `MoreScreen` (`Settings`, `Export Data`, `Presets`), and `ProfileSwitcher` Personal Card action (Green)
    - [x] Run instrumented UI tests on emulator and verify green (Blue)
- [x] Task: Nest `Archive` access inside `HistoryScreen` and expose `WORKOUT_WEEKLY_TARGET` editing in `WorkoutScreen` / `SettingsScreen`
    - [x] Write failing UI tests for opening `Archive` from `HistoryScreen` and editing weekly workout target from `WorkoutScreen` / `SettingsScreen` (Red)
    - [x] Implement `Archive` filter/menu entry in `HistoryScreen` and weekly workout target editor UI (Green)
    - [x] Run instrumented UI tests on emulator and verify green (Blue)
- [x] Task: Conductor - User Manual Verification 'Phase 2: Navigation Shell, Bottom Bar & Decluttered Destinations' (Protocol in workflow.md)

## Phase 3: Today Dashboard UI, Quick-Log Sheet, Localization & Docs

- [x] Task: Implement `Today's Overview` cards (Vitals carousel, Workouts progress ring, Measurements sparkline)
    - [x] Write failing Compose UI tests for Vitals pager & dynamic dots, single-line `"Measurements"` title, `"Workouts"` `X/Y this week` ring, empty states, card drill-down navigation, and Light/Dark semantic colors (Red)
    - [x] Implement semantic color-coded overview cards, inline sparklines, and circular progress ring in `MainScreen` / `TodayScreen` (Green)
    - [x] Run instrumented UI tests on emulator and verify green (Blue)
- [x] Task: Implement `Recent Activity` timeline and `+ Log` FAB quick-entry bottom sheet
    - [x] Write failing Compose UI tests for `Recent Activity` (<= 5 items, `"View all"` -> `HistoryScreen`, row tap navigation) and `+ Log` FAB opening the quick-entry bottom sheet with Presets and logging shortcuts (Red)
    - [x] Implement `Recent Activity` timeline component and `+ Log` FAB + `QuickLogBottomSheet` (Green)
    - [x] Run instrumented UI tests on emulator and verify green (Blue)
- [x] Task: Add EN/RU string resources, TalkBack descriptions, PSD/Test docs, and run full verification
    - [x] Add all new strings and content descriptions to `values/strings.xml` and `values-ru/strings.xml`
    - [x] Write `Docs/psd/today-dashboard.md`, `Docs/tests/today-dashboard.md`, update `Docs/index.md`, and run `python3 wiki/lint.py`
    - [x] Run full `./gradlew :app:testDebugUnitTest` and `./gradlew connectedAndroidTest` on emulator (then `adb emu kill`)
- [x] Task: Conductor - User Manual Verification 'Phase 3: Today Dashboard UI, Quick-Log Sheet, Localization & Docs' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 6c1efc3
