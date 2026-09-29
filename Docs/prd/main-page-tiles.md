# Main Page with Tiles — Product Requirements

> The app opens on a tile dashboard — one tap to every section — instead of
> dropping the user straight into the History feed.

Last updated: 2026-09-29

> **Status: PLANNED.** This PRD records the agreed requirements. The feature
> has not been implemented: the nav graph in `MainActivity.kt` still starts
> at `"history"`, and no dashboard destination exists. It is documented
> up-front so the scope is captured before work starts; the product
> specification and test cases are added when the feature is built.

## Overview

The app launches directly into the History feed (`startDestination =
"history"` in `app/src/main/java/com/example/healthjournal/MainActivity.kt`).
Every other section — Workout, Measurements, Presets, Archive, Export,
Personal Card, Settings — is reachable only through menus from wherever the
user happens to be. There is no home: no single place that shows what the
app offers or that orients a returning user.

This feature adds a main page: a grid of static tiles, one per section,
that becomes the launch screen. Tapping a tile navigates to its section;
the back stack returns to the main page. v1 tiles carry icon + label only
— live summary data on tiles (last workout, entries this week) is an
explicit named follow-up, not this feature.

## Goals / Non-goals

**Goals**

- Give the app a home: a tile per section (History, Workout, Measurements,
  Presets, Archive, Export, Personal Card, Settings).
- Make the main page the launch screen (`startDestination`).
- Keep v1 tiles static (icon + label, zero data queries) so launch stays
  instant and offline.
- Preserve every existing destination, route, and back-stack behaviour —
  only the entry point changes.

**Non-goals**

- Live summary content on tiles (deferred follow-up, see Out of scope).
- Tile customization, reordering, hiding, or badges.
- Redesigning any section reached through a tile.
- Any change to sync, export payloads, database, or notifications.

## User stories

- As a returning user, I want the app to open on an overview of everything
  it does so I can jump to today's workout without hunting through menus.
- As a new user, I want to see all sections at a glance so I learn what the
  app offers on first launch.
- As a TalkBack user, I want each tile announced with a clear label so the
  dashboard is navigable by ear.

## Functional requirements

- FR-1: A new `"main"` destination renders a tile grid with one tile per
  section: History, Workout, Measurements, Presets, Archive, Export,
  Personal Card, Settings.
- FR-2: The nav graph starts at `"main"`; the app launches on the tile page.
- FR-3: Tapping a tile navigates to its existing destination with no
  argument or behaviour change; system Back from a section returns to
  `"main"`.
- FR-4: v1 tiles are static — icon plus label, no per-tile data loading or
  observation of repositories.
- FR-5: Every tile label and content description resolves via string
  resources with full `values-ru` parity (same contract as the
  localization feature).
- FR-6: Tiles honour light/dark theme and the medical color system; layout
  adapts to small screens without clipped tiles.

## Non-functional requirements

- Launch time does not regress: the main page performs no I/O, and no
  section ViewModel is created until its tile is tapped.
- Offline-first: the dashboard renders fully with no connectivity.
- Accessibility: each tile exposes a TalkBack content description matching
  its label; grid order matches visual order.

## Acceptance criteria

(To be defined with the PSD when the feature is built.)

## Out of scope

- Live tile summaries (per-tile queries, e.g. last workout, this week's
  entries) — named follow-up after v1.
- Customizable tile order, hidden tiles, folders, or notification badges.
- Widgets, shortcuts, or deep-link changes.
- An in-app language switcher (system locale only, per the localization
  feature).

## Cross-references

- `Docs/psd/main-page-tiles.md` — the specification (added when the feature is built).
- `Docs/tests/main-page-tiles.md` — the test cases (added when the feature is built).
- [[ui-layer]] — the Compose navigation whose entry point this changes.

## Sources

- `app/src/main/java/com/example/healthjournal/MainActivity.kt` — the nav graph whose `startDestination` this feature changes.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — the current launch screen, reached via tile after this feature.
