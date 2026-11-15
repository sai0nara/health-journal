# Today Dashboard — Test Cases

> Maps the Today Dashboard PRD (`Docs/prd/today-dashboard.md`) and PSD (`Docs/psd/today-dashboard.md`) to JVM unit tests and Compose instrumented UI tests.

Last updated: 2026-11-15

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/data/GoalsRepositoryTest.kt` | Default, persistence, clamping, and observation of `WORKOUT_WEEKLY_TARGET` |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/TodayDashboardViewModelTest.kt` | Empty state, multi-vital carousel filtering, weekly workout progress, weight sparkline/delta, Recent Activity 5-item cap, and unit formatting |
| JVM unit | `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` | Verifies Today Dashboard UI files use `ui/theme/` semantic tokens rather than literal colors |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` | Today Dashboard layout (no gear icon, no legacy static tiles), populated cards, Recent Activity tap/`View all`, `+ Log` Quick-Log bottom sheet, `MoreScreen`, `TodayBottomBar`, and `ProfileSwitcher` Personal Card item |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | `Archive` menu item in `HistoryScreen` overflow menu |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/SettingsScreenTest.kt` | Weekly workout goal stepper callbacks in `SettingsScreen` |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Weekly workout goal defaults and clamps | Clean `GoalsRepository` | Read `workoutWeeklyTarget`; call `setWorkoutWeeklyTarget` with out-of-range and in-range values | Defaults to configured target; out-of-range values clamp to bounds |
| T-2 | Vitals carousel hides unlogged parameters | Profile has only Blood Pressure or no vitals | Observe `TodayDashboardViewModel.uiState` | Empty profile yields 1 empty slide; partial vitals yields only logged slides |
| T-3 | Workouts progress & Measurements sparkline | Weekly workouts and weight entries exist | Observe `TodayDashboardViewModel.uiState` | Weekly count/target, sparkline points, and weekly delta match entries and unit system |
| T-4 | Recent Activity capped at 5 items | Profile has >5 entries | Observe `TodayDashboardViewModel.uiState` | `recentActivity` contains at most 5 items sorted newest-first |
| T-5 | Today Dashboard renders without legacy launcher tiles | `MainScreen` composed | Inspect top bar and overview | App logo/title shown, no gear icon, no legacy static tiles (`Archive`, `Settings`, `Export Data`) |
| T-6 | Quick-Log sheet presets & shortcuts | `+ Log` FAB tapped | Open sheet with presets and without presets | Shows up to 3 presets (or `Manage Presets` fallback) plus 4 logging shortcuts |
| T-7 | Persistent bottom bar & More hub | `TodayBottomBar` / `MoreScreen` composed | Tap each of the 4 tabs; tap `MoreScreen` items | Navigates to `Today`, `History`, `Trends`, `More`; `More` opens `Settings`, `Export Data`, `Presets`, `About` |
| T-8 | Relocated Personal Card, Archive, and Weekly Goal | `ProfileSwitcher`, `HistoryScreen`, `SettingsScreen` composed | Open profile menu, History overflow, and Settings stepper | `Personal Card` opens card; `Archive` opens archive; stepper updates weekly workout target |

## Manual checks

- Visual inspection of light and dark theme rendering on device/emulator against the design mockups (`Today's Overview` gradient tints, `Workouts` ring, `Measurements` sparkline, `+ Log` gradient FAB).
- End-to-end swipe gesture across all 5 Vitals carousel pages and deep-link navigation to the matching `Trends` tab.

## Cross-references

- `Docs/prd/today-dashboard.md` — requirements under test.
- `Docs/psd/today-dashboard.md` — design the cases verify.

## Sources

- `app/src/test/java/com/example/healthjournal/data/GoalsRepositoryTest.kt` — weekly workout target unit tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/TodayDashboardViewModelTest.kt` — dashboard ViewModel unit tests.
- `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` — hardcoded color audit.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` — Today Dashboard, Quick-Log, BottomBar, More, and ProfileSwitcher UI tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — History overflow Archive test.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/SettingsScreenTest.kt` — Settings weekly workout goal stepper test.
- `Docs/prd/today-dashboard.md` — requirements under test.
- `Docs/psd/today-dashboard.md` — design the cases verify.
