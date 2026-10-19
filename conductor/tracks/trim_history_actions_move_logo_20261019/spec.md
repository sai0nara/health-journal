# Track trim_history_actions_move_logo_20261019 — Specification

## Overview
Declutter the History top bar: remove the Personal Card, Measurements, and
Export shortcut actions, and relocate the app logo from History to the Main
(dashboard) screen. All three destinations stay reachable via existing Main
tiles — the app loses no navigation capability; History becomes feed-focused.

## Context
- `HistoryScreen.kt:95-170` top bar: back arrow + `app_logo` image (leading),
  title "History", actions Person / measurements chart / export / overflow.
- `MainScreen.kt:54-63` already has tiles: `measurements`, `export`, `personal_card`.

## Functional Requirements
- FR1: History top bar shows only back arrow, "History" title, and the existing
  overflow menu (sync / sort / about). No logo, no Person/Measurements/Export icons.
- FR2: `HistoryScreen` drops the `onPersonalCardClick`, `onMeasurementsClick`,
  `onExportClick` parameters; `MainActivity.kt:92-94` stops wiring them.
- FR3: Main screen top bar shows the logo (32dp, testTag `app_logo`,
  contentDescription `cd_app_logo`) in the leading position, before the title.
- FR4: Unused string keys `cd_open_personal_card`, `history_cd_measurements`,
  `history_cd_export` are deleted from both `values/strings.xml` and
  `values-ru/strings.xml`; `cd_app_logo` is retained (now used by Main).
- FR5: All three destinations remain reachable from Main tiles.

## Non-Functional
- Purely UI + nav wiring; no ViewModel, DAO, or data changes.
- en/ru catalog parity maintained; wiki/Docs updated for changed sources;
  `python3 wiki/lint.py` exits 0.

## Acceptance Criteria
- AC1: UI test — Main screen composes and displays `app_logo`.
- AC2: UI test — History screen has no `app_logo`, no `measurements_chart_icon`,
  no Person/Export action icons.
- AC3: `HistoryScreenTest`, `TruncationTest`, `ThemedRenderingTest` compile and
  pass against the slimmer `HistoryScreen` signature.
- AC4: `:app:testDebugUnitTest` green (l10n audit/parity tests included).
- AC5: Instrumented UI tests for touched screens green on device.

## Out of Scope
- Deleting the now-unused `measurements_chart` drawable (kept; may be reused).
- Any change to overflow menu items or Main tile grid.
