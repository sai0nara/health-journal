# Track personal_card_verify_20260923 — Implementation Plan

## Phase 1: JVM verification + gap fixes [checkpoint: no code changes; suites green — recorded on disk only]

- [x] Task: JVM verification sweep (Red/Green/Refactor) (no failures: VM/validator/converter suites green, no gaps found)
    - [ ] Run PersonalCardViewModel/validator/converter suites; record failures
    - [ ] Write failing tests for each gap found
    - [ ] Implement minimal fixes; rerun green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: UI verification + gap fixes [checkpoint: no code changes; 17/17 green — recorded on disk only]

- [x] Task: UI verification sweep (Red/Green/Refactor + UI) (no failures: 17/17 green, no gaps found)
    - [ ] Run PersonalCardScreenTest in isolation; record failures
    - [ ] Write failing UI tests for each gap found (ft/in, rows, persistence)
    - [ ] Implement minimal fixes; rerun green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Device pass + docs [checkpoint: e5d9c66 — recorded on disk only; no checkpoint commit per conductor-untracked decision]

- [~] Task: Manual device pass
    - [ ] ft/in entry, toggle, relaunch, validation bounds on device
    - [ ] Fix defects found with regression tests
- [ ] Task: Docs + lint
    - [ ] Update personal-card docs if behavior changed; run wiki lint to exit 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)
