# Track hashtags_20261002 — Implementation Plan

## Phase 1: Hashtag parser + auto-tag mapping [TDD red→green]

- [ ] Task: Write failing tests (Red)
    - [ ] JVM test: `#word` extraction (Unicode, dedupe, case-insensitive, edge cases)
    - [ ] JVM test: kind→tag mapping (workout→Fitness, measurement→Health, medication→Medication)
    - [ ] Run both; confirm red
- [ ] Task: Implement pure parser + mapping in domain (Green)
    - [ ] No Android dependencies; JVM suite green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Wire into entry save + History filter

- [ ] Task: Auto-tag on workout finish/manual log; parse on entry save; chips filter on tap (Green)
    - [ ] System-owned auto-tags survive edits; inline tags follow text
    - [ ] Existing manual picker untouched; UI tests green
- [ ] Task: RU parity for auto-tag labels; Cyrillic inline round trip
    - [ ] Parity test green
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Verification + documentation

- [ ] Task: Full regression — JVM suite + affected instrumented classes green
- [ ] Task: Write Docs/psd/hashtags.md and Docs/tests/hashtags.md; drop planned marker
    - [ ] wiki lint exits 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
