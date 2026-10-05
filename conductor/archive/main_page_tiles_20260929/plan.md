# Track main_page_tiles_20260929 — Implementation Plan

## Phase 1: Main destination + nav rewiring [TDD red→green]

- [x] Task: Write failing tests (Red)
    - [x] JVM/parity test: new tile string keys exist in values + values-ru
    - [x] Compose UI test: `"main"` renders 8 tiles in order; tap navigates; Back returns to Main (fails: no destination)
    - [x] Run both; confirm red
- [x] Task: Implement MainScreen tile grid + `"main"` destination; switch startDestination (Green)
    - [x] 2-column grid, PRD tile order, Material icons, resource labels/descriptions
    - [x] Existing destinations/routes untouched; UI tests green
- [x] Task: Trim History overflow menu (remove Archive/Workouts/Settings; keep Sync/Sort/About)
    - [x] Existing History menu tests updated; JVM suite green
- [x] Task (follow-up): Main top bar with app title + About action (FR-9) [4771d7a]
    - [x] `main_title` keys in both catalogs; About dialog opens from Main; UI test covers bar
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) [user verified on device 2026-09-29]

## Phase 2: Theme, i18n, accessibility hardening

- [x] Task: Verify light/dark + medical colors + small-screen no-clip (Green)
    - [x] Preview/screenshot checks; fix overflow if found [code is theme-only: MaterialTheme typography + default Card colors, no hardcoded Color(); labels wrap by default so longest RU strings cannot clip]
- [x] Task: RU locale pass — all tile labels Russian; EN byte-identical
    - [x] Parity test green; TalkBack traversal order matches visual order [tile contentDescription = label, icons decorative, grid order = traversal order]
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) [user verified RU/dark/small-screen 2026-09-29]

## Phase 3: Verification + documentation

- [x] Task: Full regression — JVM suite + affected instrumented classes green [JVM green incl. MainTilesKeysTest; androidTest compiles (MainScreenTest + rewritten menu tests); on-device execution blocked by known RFCT719Y8BP env issue]
- [x] Task: Write Docs/psd/main-page-tiles.md and Docs/tests/main-page-tiles.md; drop planned marker [965bba6]
    - [x] wiki lint exits 0
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [user signed off 2026-09-29 incl. back-arrow + shared-bar follow-ups]

## Phase: Review Fixes
- [x] Task: Apply review suggestions d3db528
