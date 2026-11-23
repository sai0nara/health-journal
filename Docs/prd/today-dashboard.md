# Today Dashboard — Product Requirements

> Replace the static 8-tile app launcher on the home screen with an action-first, glanceable Today dashboard featuring live summary cards, a recent activity timeline, a quick-log FAB, and persistent bottom navigation.

Last updated: 2026-11-15

> **Status: PLANNED.** Based on the home-screen redesign brief and dark/light reference screenshots. All grooming questions (`G1`–`G4`) are resolved below; implementation and `Docs/psd/today-dashboard.md` / `Docs/tests/today-dashboard.md` follow in the feature track.

## Overview

The current home screen (`app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt`) functions as a flat 8-tile launcher where high-frequency daily tracking (Vitals, Workouts, Measurements, History) shares identical visual weight with low-frequency utilities (Settings, Archive, Export Data, Personal Card). Tiles show zero live data, `"Body measurements"` wraps onto two lines, and `"Settings"` sits orphaned in the bottom row.

This feature redesigns the `"main"` launch destination into a glanceable **Today** dashboard that surfaces the user's latest metrics, recent activity, and one-tap logging in the thumb zone, while relocating secondary administrative tools into standard mobile navigation patterns.

## Goals / Non-goals

**Goals**

- Declutter the home canvas by moving `Personal Card`, `Settings`, `Export Data`, `Archive`, and `Presets` out of the main grid into contextual navigation entry points.
- Surface live, glanceable health summaries (**Vitals** carousel, **Workouts** weekly progress, **Measurements** sparkline & delta) and a **Recent Activity** timeline directly on the launch screen.
- Introduce semantic color-coded icon containers (coral for Vitals, emerald for Workouts, sky blue for Measurements) with full light/dark theme parity.
- Move primary navigation and logging into the thumb zone via a persistent bottom navigation bar and a `+ Log` Floating Action Button (FAB) opening a quick-entry bottom sheet.

**Non-goals**

- Replacing or redesigning the underlying detail screens (`HistoryScreen`, `VitalsScreen`, `MeasurementsScreen`, `WorkoutScreen`, `PresetLibraryScreen`, `SettingsScreen`, `ExportScreen`, `ArchiveScreen`, `PersonalCardScreen`).
- Changing Room database schemas, Google Drive sync payloads, or Health Connect sync protocols.
- User-customizable dashboard card ordering or home-screen Android widgets.

## User stories

- As a daily tracker, I want to see my latest vitals, workout progress, and weight trend immediately on launch so I don't have to tap into three separate screens to check my status.
- As a returning user, I want to tap a `+ Log` button in the thumb zone to quickly log vitals, a measurement, a journal entry, or start a workout preset without hunting through a tile grid.
- As a multi-profile user, I want to access my Personal Card and switch sibling profiles from the top bar's `Me` pill so profile management stays in one place.
- As a TalkBack and dark/light theme user, I want every card, chart summary, and navigation tab to have clear labels and high contrast in both themes.

## Grooming Decisions (G1–G4)

All grooming gaps from the initial requirements audit have been resolved:

- **G1 (Resolved — 4-tab bottom bar, no top-right gear icon):** Use the **4-tab** bottom navigation bar (`Today`, `History`, `Trends`, `More`) matching the reference screenshots. `Settings`, `Export Data`, and `Presets` live inside the `More` tab, and the redundant top-right gear icon (`⚙`) is omitted from the Top Bar.
- **G2 (Resolved — 5 Vitals parameters, hide empty slides):** The Vitals horizontal pager covers the 5 tracked Vitals parameters from `VitalsScreen` (`Blood Pressure` + HR, `Blood Glucose`, `Heart Rate`, `Body Temperature`, `SpO2`), ordered consistently with `Docs/prd/vitals-trends.md`. Slides with zero logged readings for the active profile are dynamically hidden so the user only swipes through populated metrics; when all 5 parameters have zero readings, a single Vitals empty-state card is shown (minimum 1 slide).
- **G3 (Resolved — Workouts weekly target in `GoalsRepository`):** The weekly workout target denominator in `X/Y this week` defaults to `4` workouts per week and is persisted per profile in `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` (keyed by `WORKOUT_WEEKLY_TARGET`), editable from `WorkoutScreen` and `Settings`.
- **G4 (Resolved — Recent Activity capped at 5 items + tap navigation):** `"Recent Activity"` displays up to the **5 most recent items** across Vitals, Workouts, Measurements, and Journal entries, plus a `"View all"` header action that navigates to `HistoryScreen`. Tapping an individual activity row navigates to its corresponding category screen (`VitalsScreen`, `WorkoutScreen`, `MeasurementsScreen`) or entry detail/edit view.

## Functional requirements

### 1. Top App Bar & Decluttered Utilities
- **FR-1 (Top Bar):** Display the app icon, `"Health Journal"` title, and active profile pill (`M Me ▾`, reusing `ProfileSwitcher`) on the top-right; omit the redundant top-right gear icon (`G1`).
- **FR-2 (Personal Card & Profile Pill):** Tapping the active profile pill opens the `ProfileSwitcher` dropdown, which includes a `"Personal Card"` menu item above `"Manage Profiles"` that navigates to the active profile's `PersonalCardScreen` (removing the standalone Personal Card tile from the home grid).
- **FR-3 (More Tab — Settings, Export Data & Presets):** Move `Settings`, `Export Data`, and `PresetLibraryScreen` management out of the home grid into the `More` tab of the bottom navigation bar (`G1`).
- **FR-4 (Archive):** Remove the standalone Archive tile from the home screen and nest Archive access inside `HistoryScreen` as a top-bar overflow menu option (`⋮` → `Archive`).

### 2. Today's Overview (Glanceable Metric Cards)
- **FR-5 (Vitals Carousel Card):** Render a full-width horizontal pager card under `"Today's Overview"` covering the 5 tracked Vitals parameters (`Blood Pressure` + HR, `Blood Glucose`, `Heart Rate`, `Body Temperature`, `SpO2`) with pagination dots matching the active slide count (`G2`). Dynamically hide any parameter slide that has zero readings for the active profile (minimum 1 slide shown when all 5 are empty). Each populated slide displays a coral-tinted heart icon container, parameter title, relative timestamp (e.g., `Today, 08:30 AM`), latest reading (e.g., `118/76 mmHg • 64 bpm`), and an inline mini sparkline plotting up to the last `7` readings (or a single dot when `1` reading exists).
- **FR-6 (Workouts Summary Card):** Render a half-width card with an emerald-tinted dumbbell icon container, `"Workouts"` label, most recent workout name + relative date (e.g., `Strength Training • Yesterday`), and a circular weekly progress ring showing `X/Y this week` across a rolling 7-day window (today + prior 6 calendar days in local timezone), where `Y` is read from `GoalsRepository` (`WORKOUT_WEEKLY_TARGET`, valid range `1..14`, defaulting to `4` per week and editable in `WorkoutScreen` / `Settings` per `G3`). When `X > Y`, the progress ring caps at `100%` fill while the label displays the actual count (e.g., `5/4 this week`).
- **FR-7 (Measurements Summary Card):** Shorten the card title from `"Body measurements"` to `"Measurements"` so it fits on a single line. Display a sky-blue ruler icon container, latest weight + date (e.g., `82.4 kg • Nov 13`, respecting user metric/imperial unit preferences), an area/line mini sparkline (up to the last `7` readings, or a single dot when `1` reading exists), and rolling 7-day delta (latest minus oldest weight in the 7-day window when `>= 2` entries exist, e.g., `-0.6 kg this week`, or `"—"` when `< 2` entries exist).
- **FR-8 (Card Drill-Down & Empty States):** Tapping Vitals, Workouts, or Measurements cards navigates to `VitalsScreen` (pre-selecting the visible slide's parameter tab, or `Blood Pressure` tab 0 when in the empty state), `WorkoutScreen`, or `MeasurementsScreen` respectively. When a profile has zero entries for a category, the card displays `"No data yet • Tap to log"` (RU: `"Нет данных • Нажмите, чтобы добавить"`).

### 3. Recent Activity Timeline
- **FR-9 (Recent Activity List):** Below `"Today's Overview"`, render a `"Recent Activity"` section header with a `"View all"` action (navigating to `HistoryScreen`) and a timeline list capped at the **5 most recent items** across Vitals, Workouts, Measurements, and Journal entries (`G4`), or `"No recent activity"` when 0 items exist. Render a vertical connector line and category-colored dots (coral for Vitals, emerald for Workout, sky blue for Weight/Measurements, primary accent for Journal). Each row shows category/summary (e.g., `Vitals - 118/76 mmHg`, `Workout - 45 min`, `Weight - 82.4 kg`), relative date using local midnight boundaries (`Today`, `Yesterday`, `d MMM` for current year, `d MMM yyyy` for prior years), and a locale-aware time pill (`12h` e.g. `08:30 AM` in EN, `24h` e.g. `08:30` in RU) when logged today. Tapping a row navigates to `VitalsScreen` (matching tab), `WorkoutScreen`, `MeasurementsScreen`, or the Edit Entry screen (for Journal rows) (`G4`).

### 4. Thumb-Zone Navigation & Quick Logging
- **FR-10 (Quick-Log FAB & Bottom Sheet):** Render a primary `+ Log` FAB in the bottom-right corner. Tapping it opens a quick-entry bottom sheet displaying up to `3` recent workout Presets (tapping a preset opens `WorkoutScreen` pre-loaded with that preset; when `0` presets exist, a `"Manage Presets"` link is shown) alongside single-tap logging shortcuts (Vitals, Workout, Measurements, Journal Entry), replacing the standalone Presets grid tile.
- **FR-11 (Persistent Bottom Navigation Bar):** Render a persistent 4-tab bottom navigation bar for thumb-zone switching between top-level destinations: `Today`, `History`, `Trends`, and `More` (`G1`).
- **FR-12 (Localization & Theme Parity):** All labels, relative dates, and accessibility descriptions resolve via string resources with full `values` (EN) and `values-ru` (RU) parity, honouring both light and dark themes as shown in the reference screenshots.

## Non-functional requirements

- **Performance:** Dashboard queries run asynchronously via Room `Flow`s on IO/default dispatchers scoped to the active profile; initial frame renders without blocking the main thread.
- **Offline-first:** All overview cards, sparklines, and recent activity rows read exclusively from local Room storage and work with zero network connectivity.
- **Accessibility:** Every icon container, sparkline, progress ring, FAB, and bottom-bar destination exposes TalkBack content descriptions and meets Material 3 touch-target (`>= 48dp`) and contrast guidelines in light and dark modes.
- **Profile isolation:** Switching the active profile via `ProfileSwitcher` immediately updates all overview cards, sparklines, weekly workout goal progress, and recent activity to the selected profile's data.

## Acceptance criteria

- AC-1: Cold launch lands on the `Today` tab with the Top Bar (`Health Journal`, `ProfileSwitcher`, no top-right gear icon), `"Today's Overview"`, `"Recent Activity"`, `+ Log` FAB, and 4-tab bottom navigation bar (`Today`, `History`, `Trends`, `More`); zero static launcher tiles remain (`G1`).
- AC-2: `"Measurements"` title renders on a single line without wrapping on standard phone widths.
- AC-3: The Vitals carousel renders slides only for Vitals parameters that have at least one logged reading for the active profile (up to 5 slides), or a single empty-state slide when no vitals are logged (`G2`).
- AC-4: The Workouts card displays `X/Y this week` using the profile's `WORKOUT_WEEKLY_TARGET` from `GoalsRepository` (default `4`), updating immediately when workouts are logged or the weekly target is changed (`G3`).
- AC-5: `"Recent Activity"` shows at most 5 newest items across all categories; tapping an item navigates to its category screen or entry detail, and tapping `"View all"` navigates to `HistoryScreen` (`G4`).
- AC-6: Logging a new vital, workout, or weight measurement updates the corresponding Overview card, sparkline/progress ring, and Recent Activity timeline without restarting the app; switching profiles updates all dashboard metrics and recent activity to the active profile.
- AC-7: Tapping `+ Log` opens the quick-entry bottom sheet with Presets and logging shortcuts; tapping `History` in the bottom bar navigates to `HistoryScreen` (where `Archive` is accessible); tapping `More` exposes `Settings`, `Export Data`, and `Presets`.
- AC-8: Both Light and Dark modes match the semantic color accents and layout hierarchy of the reference designs; EN and RU strings render without missing resources.

## Out of scope

- AI-generated daily insights on the Today screen (covered separately by `Docs/prd/ai-insights.md`).
- Home-screen Android AppWidgets or Wear OS tiles.
- Customizing/reordering dashboard cards.

## Cross-references

- `Docs/prd/main-page-tiles.md` — previous static 8-tile home screen replaced by this feature.
- `Docs/prd/vitals-trends.md` — Vitals & Trends screen linked from the Vitals card and `Trends` tab.
- `Docs/prd/profiles-for-siblings.md` — `ProfileSwitcher` and multi-profile data isolation.
- `Docs/prd/personal-card.md` — Personal Card accessed from the profile pill.
- `Docs/prd/workout-presets.md` — Presets surfaced in the `+ Log` quick-entry bottom sheet and `More` tab.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` — current home screen implementation.
- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — navigation graph and `startDestination`.
- `app/src/main/java/com/example/healthjournal/ui/components/ProfileSwitcher.kt` — top-bar profile pill component.
- `app/src/main/java/com/example/healthjournal/data/GoalsRepository.kt` — per-profile goal persistence backing `WORKOUT_WEEKLY_TARGET`.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — History destination and Archive integration point.
- `app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt` — Vitals & Trends destination.
- `app/src/main/java/com/example/healthjournal/ui/screens/MeasurementsScreen.kt` — Body Measurements destination.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — Workouts destination.
- `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt` — Workout presets source.
