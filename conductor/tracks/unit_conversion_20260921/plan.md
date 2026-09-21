# Track unit_conversion_20260921 — Implementation Plan

## Phase 1: Body measurements conversion

- [x] Task: Shared ft/in + imperial helpers (Red/Green/Refactor) (d1c81fc)
    - [ ] Write failing unit tests for ft+in parse/format and imperial measurement format/parse helpers
    - [ ] Implement helpers alongside `UnitConverter`
    - [ ] Refactor for clarity; rerun unit tests green
- [ ] Task: Measurement entry converts (Red/Green/Refactor + UI)
    - [ ] Write failing VM tests: imperial input persists metric; render converts stored metric
    - [ ] Implement entry render/parse via shared converter + `UnitSettings`
    - [ ] Write UI tests for imperial entry round trip; run green in isolation
- [ ] Task: Charts + goals convert (Red/Green/Refactor + UI)
    - [ ] Write failing tests: goal line renders converted; goal editor imperial input stores metric
    - [ ] Implement chart/goal render + goal editor parse
    - [ ] Write UI tests for converted chart + goal edit; run green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Workout weights conversion

- [ ] Task: Routine set rows + headers + tonnage convert (Red/Green/Refactor + UI)
    - [ ] Write failing tests: imperial render of set rows/headers/tonnage; input parses to kg
    - [ ] Implement render/parse via shared converter (replace hardcoded kg labels)
    - [ ] Write UI tests for imperial routine run; run green in isolation
- [ ] Task: Preset editor converts (Red/Green/Refactor + UI)
    - [ ] Write failing tests: preset defaults render/parse in preferred units
    - [ ] Implement preset editor render/parse
    - [ ] Write UI tests for imperial preset edit; run green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Personal card + journal vitals

- [ ] Task: Personal-card read-only rows respect preference (Red/Green/Refactor + UI)
    - [ ] Write failing UI/VM tests: summary rows match editor unit choice
    - [ ] Replace hardcoded metric display with converter-driven render
    - [ ] Run UI tests green in isolation
- [ ] Task: Journal vitals display converts (Red/Green/Refactor + UI)
    - [ ] Write failing tests: stored metric vitals render in preferred units
    - [ ] Implement vitals render conversion
    - [ ] Run UI tests green in isolation
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Docs + final verification

- [ ] Task: Lift metric-only decision in body-measurements docs
    - [ ] Update `Docs/prd`, `Docs/psd`, `Docs/tests` body-measurements clauses
    - [ ] Run wiki lint to exit 0
- [ ] Task: Full verification
    - [ ] Run `./gradlew :app:testDebugUnitTest` green with `CI=true` and `JAVA_HOME` on JDK 21
    - [ ] Confirm mid-draft preference toggle preserves values on all touched surfaces
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
