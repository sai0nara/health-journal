# Track history_dual_units_20260922 — Implementation Plan

## Phase 1: Dual-unit history + headers [checkpoint: 63ff6b5/afde77a — recorded on disk only; no checkpoint commit per conductor-untracked decision]

- [x] Task: Dual-unit history lines (Red/Green/Refactor + UI) (63ff6b5)
    - [ ] Write failing tests: history description renders `40 kg (88.2 lb)` dual lines
    - [ ] Implement dual rendering in history description builder
    - [ ] Write UI tests for history card dual text; run green in isolation
- [x] Task: Dual-unit routine headers (Red/Green/Refactor + UI) (afde77a)
    - [ ] Write failing tests: routine header shows dual units
    - [ ] Implement dual rendering in routine header
    - [ ] Write UI tests for header dual text; run green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Performed values in history [checkpoint: f821e54 — recorded on disk only; no checkpoint commit per conductor-untracked decision]

- [x] Task: History shows performed sets (Red/Green/Refactor + UI) (f821e54)
    - [ ] Write failing tests: raised set weights appear on history lines; sensible fallback when nothing completed
    - [ ] Implement performed-value history lines
    - [ ] Write UI tests; run green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Regression + verification [checkpoint: 63ff6b5/afde77a/f821e54 + verification runs — recorded on disk only; no checkpoint commit per conductor-untracked decision]

- [x] Task: Defect #3 regression test (Red/Green + UI) (existing `demographicsCard_imperialShowsConvertedRows` verified green; no code change)
    - [ ] Write failing test: card rows follow the preference
    - [ ] Confirm green on current implementation (fix if red indicates regression)
- [x] Task: Full verification
    - [x] Run `./gradlew :app:testDebugUnitTest` green with `CI=true` and `JAVA_HOME` on JDK 21
    - [x] Run wiki lint to exit 0
    - [ ] Run `./gradlew :app:testDebugUnitTest` green with `CI=true` and `JAVA_HOME` on JDK 21
    - [ ] Run wiki lint to exit 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
