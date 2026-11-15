# Today Dashboard — Product Specification

> Glanceable MVVM dashboard (`TodayDashboardViewModel` + `MainScreen`), 4-tab `TodayBottomBar`, Quick-Log bottom sheet, and relocated secondary navigation (`ProfileSwitcher` Personal Card, `HistoryScreen` Archive overflow, `MoreScreen`, and weekly workout target in `SettingsScreen`).

Last updated: 2026-11-15

## Overview

The Today Dashboard replaces the static 8-tile home launcher with an action-first daily overview while keeping all existing logging and management destinations reachable in one or two taps. See `Docs/prd/today-dashboard.md` for requirements and scope.

Key design trade-offs:
- **ViewModel aggregation vs. per-card queries**: A dedicated `TodayDashboardViewModel` combines Room `Flow`s from `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`, and `ProfileRepository` using `combine` + `stateIn` into a single `TodayDashboardUiState`, keeping `MainScreen` stateless and easy to preview/test.
- **4-tab bottom bar vs. top-bar gear icon**: `TodayBottomBar` provides persistent `Today`, `History`, `Trends`, and `More` tabs across top-level destinations; `MoreScreen` hosts `Settings`, `Export Data`, `Presets`, and `About`, eliminating the top-bar gear icon.
- **Semantic theme tokens**: Category accents (`TodayVitalsAccent`, `TodayWorkoutsAccent`, `TodayMeasurementsAccent`) and FAB gradient tokens (`TodayFabGradientStart`, `TodayFabGradientEnd`) live in `ui/theme/Color.kt` so `HardcodedColorAuditTest` remains green.

## Architecture

- **Data / Goals**: `GoalsRepository` persists `WORKOUT_WEEKLY_TARGET` in `goals` alongside existing body-measurement goals, clamped to the supported weekly range via `setWorkoutWeeklyTarget`.
- **ViewModel**: `TodayDashboardViewModel` (with `TodayDashboardViewModel.Factory` in `MainActivity`) observes active profile changes (`flatMapLatest`), computes up to five `TodayVitalSlide` entries (hiding unlogged parameters while guaranteeing one empty-state slide), builds `TodayWorkoutsCardState` for the current ISO week, builds `TodayMeasurementsCardState` with sparkline points and weekly delta, and maps recent entries into at most five `TodayActivityItem`s.
- **UI**:
  - `MainScreen` renders the top bar (`ProfileSwitcher` with `Personal Card` menu item), `Today's Overview` (`HorizontalPager` Vitals card + pager dots, `Workouts` ring card, `Measurements` single-line card + sparkline), `Recent Activity` timeline, `+ Log` gradient FAB, and `TodayBottomBar`.
  - `QuickLogBottomSheet` presents up to three user `WorkoutPreset` chips (or a `Manage Presets` shortcut when empty) and four quick-log shortcuts (`Vitals`, `Workout`, `Measurement`, `Note / Journal`).
  - `VitalsScreen` accepts an optional `initialMetric: VitalMetricTab?` so tapping a Vitals slide opens `Trends` on the matching tab.
  - `HistoryScreen` exposes `Archive` inside its overflow menu (`onOpenArchive`), and `SettingsScreen` provides a weekly workout goal stepper (`weeklyWorkoutTarget` / `onWeeklyWorkoutTargetChange`).

## Data flow

1. User launches the app on `"main"` (`MainScreen`); `MainActivity` collects `TodayDashboardViewModel.uiState`, `workoutPresets`, and `activeProfileId`.
2. `TodayDashboardViewModel` reacts to `ProfileRepository.activeProfileId` and combines entries, body measurements, and `GoalsRepository.workoutWeeklyTarget` into `TodayDashboardUiState`.
3. Tapping `+ Log` opens `QuickLogBottomSheet`; choosing a preset or shortcut navigates to `addEntry` / `addEntry?workoutType=...` / `vitals` / `measurements`.
4. Tapping a Vitals slide stores the selected `VitalMetricTab` in `MainActivity` and navigates to `"vitals"`, where `VitalsScreen` selects that tab via `LaunchedEffect`.
5. Switching tabs in `TodayBottomBar` navigates between `"main"`, `"history"`, `"vitals"`, and `"more"` with single-top state preservation.
6. Opening `ProfileSwitcher` offers `Personal Card` (`onPersonalCard` -> `"personalCard"`); opening `HistoryScreen` overflow offers `Archive` (`onOpenArchive` -> `"archive"`).

## Components

| Component | File | Responsibility |
|---|---|---|
| Weekly workout target | `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` | Stores and clamps `WORKOUT_WEEKLY_TARGET` |
| Dashboard ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/TodayDashboardViewModel.kt` | Aggregates Vitals slides, Workouts progress, Measurements sparkline, and Recent Activity |
| Today screen | `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` | Renders Today's Overview, Recent Activity, `+ Log` FAB, and Quick-Log sheet host |
| Bottom navigation | `app/src/main/java/com/example/healthjournal/ui/components/TodayBottomBar.kt` | Persistent 4-tab bar (`Today`, `History`, `Trends`, `More`) |
| Quick-Log sheet | `app/src/main/java/com/example/healthjournal/ui/components/QuickLogBottomSheet.kt` | Preset chips and category logging shortcuts |
| More hub | `app/src/main/java/com/example/healthjournal/ui/screens/MoreScreen.kt` | Houses `Settings`, `Export Data`, `Presets`, and `About` |
| Profile menu | `app/src/main/java/com/example/healthjournal/ui/components/ProfileSwitcher.kt` | Adds `Personal Card` entry above `Manage Profiles` |
| History overflow | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | Exposes `Archive` in the top-bar overflow menu |
| Settings stepper | `app/src/main/java/com/example/healthjournal/ui/screens/SettingsScreen.kt` | Edits `WORKOUT_WEEKLY_TARGET` |
| Workout stepper | `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` | Edits `WORKOUT_WEEKLY_TARGET` on the Workout hub |
| Vitals deep-link | `app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt` | Honors `initialMetric` when opened from a Vitals slide |
| Navigation wiring | `app/src/main/java/com/example/healthjournal/MainActivity.kt` | Wires `TodayDashboardViewModel`, `TodayBottomBar`, `"more"`, and callbacks |
| Theme tokens | `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` | Defines Today Dashboard category accents and FAB gradient colors |

## Edge cases & failure handling

| Case | Expected behaviour |
|---|---|
| No vitals logged for active profile | Carousel renders a single empty-state slide (`Blood Pressure`) with prompt to log |
| Partial vitals logged (e.g. only Heart Rate) | Carousel hides unlogged parameters and shows only logged slides |
| Zero workouts this week | Workouts card renders `0/<target> this week` with empty progress arc |
| Fewer than 2 weight measurements | Measurements card hides weekly delta and shows empty sparkline or latest single value |
| Zero presets configured | `QuickLogBottomSheet` shows `Manage Presets` shortcut instead of preset chips |
| Weekly target out of bounds | `GoalsRepository.setWorkoutWeeklyTarget` clamps input to valid bounds |

## Dependencies

- Room & Kotlin Coroutines `Flow` (`combine`, `flatMapLatest`, `stateIn`).
- Jetpack Compose Material 3 (`HorizontalPager`, `ModalBottomSheet`, `NavigationBar`, `DropdownMenu`).
- Jetpack Navigation (`NavHost` single-top tab switching).

## Cross-references

- `Docs/prd/today-dashboard.md` — requirements this specification implements.
- `Docs/tests/today-dashboard.md` — test cases verifying this design.

## Sources

- `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` — weekly workout goal persistence.
- `app/src/main/java/com/example/healthjournal/viewmodel/TodayDashboardViewModel.kt` — dashboard state aggregation.
- `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` — Today Dashboard UI.
- `app/src/main/java/com/example/healthjournal/ui/components/TodayBottomBar.kt` — 4-tab bottom navigation bar.
- `app/src/main/java/com/example/healthjournal/ui/components/QuickLogBottomSheet.kt` — Quick-Log bottom sheet.
- `app/src/main/java/com/example/healthjournal/ui/screens/MoreScreen.kt` — More navigation hub.
- `app/src/main/java/com/example/healthjournal/ui/components/ProfileSwitcher.kt` — Personal Card menu entry.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — Archive overflow menu entry.
- `app/src/main/java/com/example/healthjournal/ui/screens/SettingsScreen.kt` — weekly workout target stepper.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — weekly workout target stepper and preset launch target.
- `app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt` — `initialMetric` selection.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — navigation and ViewModel wiring.
- `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` — semantic accent tokens.
- `Docs/prd/today-dashboard.md` — product requirements.
- `Docs/tests/today-dashboard.md` — verification suite.
