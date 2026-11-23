# Specification: Today Dashboard

## Overview
Replace the static 8-tile app launcher on the home screen (`MainScreen`) with an action-first, glanceable **Today** dashboard featuring live summary cards, a recent activity timeline, a `+ Log` quick-entry FAB, and a persistent 4-tab bottom navigation bar.

## Functional Requirements

### 1. Top App Bar & Decluttered Utilities
- **FR-1 (Top Bar):** Display the Health Journal app icon, `"Health Journal"` title, and active profile pill (`M Me ▾`, reusing `ProfileSwitcher`) on the top-right. Omit the redundant top-right gear icon (`⚙`).
- **FR-2 (Personal Card & Profile Pill):** Tapping the active profile pill opens profile switching and/or the active profile's `PersonalCardScreen` (removing the standalone Personal Card tile from the home grid).
- **FR-3 (More Tab — Settings, Export Data & Presets):** Move `Settings`, `Export Data`, and `PresetLibraryScreen` management out of the home grid into the `More` tab of the bottom navigation bar.
- **FR-4 (Archive):** Remove the standalone Archive tile from the home screen and nest Archive access inside `HistoryScreen` as a filter chip or overflow menu option.

### 2. Today's Overview (Glanceable Metric Cards)
- **FR-5 (Vitals Carousel Card):** Render a full-width horizontal pager card under `"Today's Overview"` covering the 5 tracked Vitals parameters from `VitalsScreen` (`Blood Pressure` + HR, `Blood Glucose`, `Heart Rate`, `Body Temperature`, `SpO2`), ordered consistently with `Docs/prd/vitals-trends.md`, with pagination dots matching the active slide count. Dynamically hide any parameter slide with zero logged readings for the active profile; when all 5 parameters are empty, show a single Vitals empty-state slide. Each populated slide displays a coral-tinted heart/metric icon container, parameter title, relative timestamp (e.g., `Today, 08:30 AM`), latest reading (e.g., `118/76 mmHg • 64 bpm`), and an inline mini sparkline.
- **FR-6 (Workouts Summary Card):** Render a half-width card with an emerald-tinted dumbbell icon container, `"Workouts"` label, most recent workout name + relative date (e.g., `Strength Training • Yesterday`), and a circular weekly progress ring showing `X/Y this week`, where `Y` is persisted per profile in `GoalsRepository` (`WORKOUT_WEEKLY_TARGET`, defaulting to `4` per week, editable from `WorkoutScreen` and `Settings`).
- **FR-7 (Measurements Summary Card):** Shorten the card title from `"Body measurements"` to `"Measurements"` so it fits on a single line. Display a sky-blue ruler icon container, latest weight + date (e.g., `82.4 kg • Nov 13`, respecting user metric/imperial unit preferences), an area/line mini sparkline, and weekly delta (e.g., `-0.6 kg this week`).
- **FR-8 (Card Drill-Down & Empty States):** Tapping Vitals, Workouts, or Measurements cards navigates to `VitalsScreen` (pre-selecting the visible slide's parameter tab), `WorkoutScreen`, or `MeasurementsScreen` respectively. When a profile has zero entries for a category, the card displays a defined empty state with a prompt to log.

### 3. Recent Activity Timeline
- **FR-9 (Recent Activity List):** Below `"Today's Overview"`, render a `"Recent Activity"` section header with a `"View all"` action (navigating to `HistoryScreen`) and a timeline list capped at the **5 most recent items** across Vitals, Workouts, Measurements, and Journal entries. Render a vertical connector line and category-colored dots (coral for Vitals, emerald for Workout, sky blue for Weight/Measurements, primary accent for Journal). Each row shows category/summary, relative date (`Today`, `Yesterday`, `Nov 13`), and time pill (e.g., `08:30 AM`) when logged today. Tapping a row navigates to its category screen or entry detail/edit view.

### 4. Thumb-Zone Navigation & Quick Logging
- **FR-10 (Quick-Log FAB & Bottom Sheet):** Render a primary `+ Log` FAB in the bottom-right corner. Tapping it opens a quick-entry bottom sheet containing workout Presets and single-tap logging shortcuts (Vitals, Workout, Measurements, Journal Entry), replacing the standalone Presets grid tile.
- **FR-11 (Persistent Bottom Navigation Bar):** Render a persistent 4-tab bottom navigation bar (`Today`, `History`, `Trends`, `More`) for thumb-zone switching across top-level destinations.
- **FR-12 (Localization & Theme Parity):** All labels, relative dates, and accessibility descriptions resolve via string resources with full `values` (EN) and `values-ru` (RU) parity, honouring both light and dark themes.

## Non-Functional Requirements
- Dashboard queries run asynchronously via Room `Flow`s scoped to the active profile; initial frame renders without blocking the main thread.
- Offline-first: all overview cards, sparklines, and recent activity rows read exclusively from local Room storage.
- Accessibility: every icon container, sparkline, progress ring, FAB, and bottom-bar tab exposes TalkBack content descriptions and meets Material 3 touch-target (`>= 48dp`) and contrast guidelines.
- Profile isolation: switching the active profile via `ProfileSwitcher` immediately updates all overview cards, sparklines, weekly workout target progress, and recent activity.

## Acceptance Criteria
- AC-1: Cold launch lands on `Today` with Top Bar (`Health Journal`, `ProfileSwitcher`, no gear icon), `"Today's Overview"`, `"Recent Activity"`, `+ Log` FAB, and 4-tab bottom bar (`Today`, `History`, `Trends`, `More`); zero static launcher tiles remain.
- AC-2: `"Measurements"` title renders on a single line without wrapping on standard phone widths.
- AC-3: Vitals carousel renders slides only for Vitals parameters with `>= 1` reading for the active profile (up to 5 slides), or 1 empty-state slide when none are logged.
- AC-4: Workouts card displays `X/Y this week` using `WORKOUT_WEEKLY_TARGET` from `GoalsRepository` (default `4`), updating live when workouts or target change.
- AC-5: `"Recent Activity"` shows `<= 5` newest items across categories; tapping a row opens its category screen or entry detail/edit, and `"View all"` navigates to `HistoryScreen`.
- AC-6: Logging new entries or switching profiles updates all dashboard metrics and recent activity immediately.
- AC-7: `+ Log` opens the quick-entry bottom sheet with Presets and logging shortcuts; `History` exposes `Archive`; `More` exposes `Settings`, `Export Data`, and `Presets`.
- AC-8: Light & Dark modes match the reference designs; EN and RU strings render cleanly.

## Out of Scope
- AI-generated daily insights on Today (`Docs/prd/ai-insights.md`).
- Home-screen Android AppWidgets or Wear OS tiles.
- User-customizable dashboard card ordering.
