# Track hashtags_20261002 — Implementation Plan

## Phase 1: Hashtag parser + auto-tag mapping [TDD red→green]

- [x] Task: Write failing tests (Red)
    - [x] JVM test: `#word` extraction (Unicode, dedupe, case-insensitive, edge cases)
    - [x] JVM test: kind→tag mapping (workout→Fitness, measurement→Health, medication→Medication)
    - [x] Run both; confirm red
- [x] Task: Implement pure parser + mapping in domain (Green)
    - [x] No Android dependencies; JVM suite green
- [x] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md) [user verified 2026-10-02]

## Phase 2: Wire into entry save + History filter

- [x] Task: Auto-tag on workout finish/manual log; parse on entry save; chips filter on tap (Green)
    - [x] System-owned auto-tags survive edits; inline tags follow text
    - [x] Existing manual picker untouched; UI tests green
- [x] Task: RU parity for auto-tag labels; Cyrillic inline round trip
    - [x] Parity test green

> Scope note: measurements create linked journal entries tagged Health (user-approved); medications have no log-event flow, so the Medication mapping exists in domain but wires to nothing yet.
- [x] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md) [user verified on device; &num; root cause fixed + device-tested]

## Phase 3: Verification + documentation

- [x] Task: Full regression — JVM suite + affected instrumented classes green [JVM green; chip + editor-HTML tests pass on device RFCT719Y8BP]
- [x] Task: Write Docs/psd/hashtags.md and Docs/tests/hashtags.md; drop planned marker [33056c5]
    - [x] wiki lint exits 0
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [user signed off 2026-10-05]

## Phase: Review Fixes
- [x] Task: Apply review suggestions 73252e1
