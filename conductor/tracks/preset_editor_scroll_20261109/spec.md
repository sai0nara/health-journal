# Track preset_editor_scroll_20261109 — Specification

## Overview

In `PresetLibraryScreen.kt`, the preset create/edit form (`EditingContent`) previously placed the preset name field, day dropdown, warm-up section, and catalog search as fixed children of a non-scrollable outer `Column` directly above a `LazyColumn(Modifier.weight(1f))` that contained only draft exercise rows. When warm-up was enabled and/or search results expanded, these static upper controls took up ~80% of screen height, leaving only ~20% of the viewport at the bottom for exercise list scrolling.

This track refactors `EditingContent` so that all form elements (Preset name, Day dropdown, Warm-up section, Exercise validation error, Exercises section header + Catalog search, and Draft exercise rows) live inside a single unified `LazyColumn` (`preset_draft_list`) taking `Modifier.weight(1f)` between the `TopAppBar` and the bottom pinned `Cancel` / `Save Preset` action bar.

## Context

- UI implementation: `app/src/main/java/com/example/healthjournal/ui/screens/PresetLibraryScreen.kt`
- Test coverage: `app/src/androidTest/java/com/example/healthjournal/ui/screens/PresetLibraryScreenTest.kt`
- Product Requirements Document: `Docs/prd/workout-presets.md` (`FR-13`, `AC-10`)
- Product Specification Document: `Docs/psd/workout-presets.md`
- Test Cases Document: `Docs/tests/workout-presets.md`

## Functional Requirements

- **FR1 — Unified LazyColumn:** `EditingContent` layout consists of a parent `Column` with two main areas:
  1. A single unified `LazyColumn` (`preset_draft_list`) taking `Modifier.weight(1f).fillMaxWidth()` containing:
     - Preset name field (`preset_name_field`)
     - Scheduled day dropdown
     - Warm-up section (`preset_warmup_toggle`, and when enabled `preset_warmup_name` & `preset_warmup_duration`)
     - Exercise error text (when present)
     - Exercises title & catalog search dropdown (`exercise_search_field`)
     - Draft exercise rows (`DraftExerciseRow`)
  2. A pinned bottom `Row` containing the `Cancel` and `Save Preset` (`save_preset_button`) buttons.
- **FR2 — Full Viewport Allocation:** The scroll container occupies the entire height between the `TopAppBar` and the pinned bottom action buttons (≥ 80% of portrait screen height with soft keyboard closed). Zero form controls are pinned above the scroll container.
- **FR3 — Keyed Lazy Items:** Every item in the `LazyColumn` uses stable keys (`"header_name"`, `"header_day"`, `"header_warmup"`, `"header_exercise_error"`, `"header_search"`, and index/id-based keys for draft exercise items) to prevent focus loss or scroll jumping during recomposition.
- **FR4 — Search & Collapse:** Search results remain inline or collapsible above draft exercises, clearing on item selection.
- **FR5 — Test Tag & Test Compatibility:** All existing test tags (`preset_name_field`, `preset_warmup_toggle`, `preset_warmup_name`, `preset_warmup_duration`, `exercise_search_field`, `exercise_search_result`, `preset_draft_list`, `preset_field_sets`, `preset_field_reps`, `preset_field_weight`, `preset_field_rest`, `save_preset_button`) remain fully functional.

## Non-Functional Requirements

- Keyed lazy composition is preserved; off-screen exercise rows are not eagerly composed.
- No changes to ViewModel, Repository, or database schema.
- JVM unit tests and Compose UI tests remain 100% passing.
- `python3 wiki/lint.py` exits 0.

## Acceptance Criteria

- **AC1:** In preset create/edit mode with warm-up enabled, the scroll container spans the full height between top app bar and bottom action buttons.
- **AC2:** When scrolling down toward exercises, top metadata (name, day, warm-up, search) scrolls off-screen smoothly, giving exercises the full screen height.
- **AC3:** `Cancel` and `Save Preset` remain pinned and clickable at the bottom regardless of scroll position.
- **AC4:** All existing tests in `PresetLibraryScreenTest` pass without regressions.
- **AC5:** A new Compose test confirms that scrolling to an exercise down the list scrolls top header items off-screen while the save button remains visible at the bottom.
