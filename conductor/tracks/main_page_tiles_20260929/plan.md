# Track main_page_tiles_20260929 — Implementation Plan

## Phase 1: Main destination + nav rewiring [TDD red→green]

- [ ] Task: Write failing tests (Red)
    - [ ] JVM/parity test: new tile string keys exist in values + values-ru
    - [ ] Compose UI test: `"main"` renders 8 tiles in order; tap navigates; Back returns to Main (fails: no destination)
    - [ ] Run both; confirm red
- [ ] Task: Implement MainScreen tile grid + `"main"` destination; switch startDestination (Green)
    - [ ] 2-column grid, PRD tile order, Material icons, resource labels/descriptions
    - [ ] Existing destinations/routes untouched; UI tests green
- [ ] Task: Trim History overflow menu (remove Archive/Workouts/Settings; keep Sync/Sort/About)
    - [ ] Existing History menu tests updated; JVM suite green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Theme, i18n, accessibility hardening

- [ ] Task: Verify light/dark + medical colors + small-screen no-clip (Green)
    - [ ] Preview/screenshot checks; fix overflow if found
- [ ] Task: RU locale pass — all tile labels Russian; EN byte-identical
    - [ ] Parity test green; TalkBack traversal order matches visual order
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Verification + documentation

- [ ] Task: Full regression — JVM suite + affected instrumented classes green
- [ ] Task: Write Docs/psd/main-page-tiles.md and Docs/tests/main-page-tiles.md; drop planned marker
    - [ ] wiki lint exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
