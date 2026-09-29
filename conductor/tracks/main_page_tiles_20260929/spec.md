# Track Spec — Main Page with Tiles

Implements `Docs/prd/main-page-tiles.md` (planned).

## Overview

A new `"main"` dashboard destination: a 2-column grid of 8 static tiles in
PRD order (History, Workout, Measurements, Presets, Archive, Export,
Personal Card, Settings). It becomes the launch screen
(`startDestination`). Tiles use Material icons (implementer picks) plus
resource-backed labels with full `values-ru` parity. Sections are
top-level: system Back from any section returns to Main. The History
overflow menu loses its three navigation entries (Archive, Workouts,
Settings) and keeps Sync, Sort, About.

## Functional Requirements

- FR-1: `"main"` destination renders 8 tiles in order: History, Workout,
  Measurements, Presets, Archive, Export, Personal Card, Settings, in a
  2-column grid.
- FR-2: Nav graph `startDestination` changes `"history"` → `"main"`.
- FR-3: Tile tap navigates to the existing destination unchanged; Back
  returns to `"main"`.
- FR-4: Tiles are static (icon + label); Main performs no I/O and creates
  no section ViewModels.
- FR-5: Tile labels and content descriptions come from string resources
  with `values-ru` parity.
- FR-6: Tiles honour light/dark theme and the medical color system; no
  clipping on small screens.
- FR-7: History overflow menu keeps Sync (signed-in only), Sort, About;
  Archive/Workouts/Settings entries are removed. Other screens' menus are
  untouched.
- FR-8: Leading tile icons are decorative (`contentDescription = null`);
  the tile itself exposes the label as its TalkBack description.

## Non-Functional Requirements

- Launch time does not regress (no I/O on Main).
- Fully offline; full TalkBack traversal in visual order.

## Acceptance Criteria

- AC-1: Cold launch lands on the tile grid with all 8 tiles in order.
- AC-2: Every tile reaches its section; Back from each returns to Main.
- AC-3: History menu shows only Sync/Sort/About.
- AC-4: RU device renders all tile labels in Russian; EN unchanged.
- AC-5: JVM suite green; wiki lint exits 0.

## Out of Scope

- Live tile summaries, tile customization/reorder/badges,
  widgets/shortcuts, in-app language switcher.
