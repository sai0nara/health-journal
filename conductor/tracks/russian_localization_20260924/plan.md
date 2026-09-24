# Track russian_localization_20260924 — Implementation Plan

## Phase 1: Failing checks + string externalization

- [ ] Task: Write failing localization checks (Red)
    - [ ] JVM test: values-ru key parity vs values/strings.xml (fails: no values-ru)
    - [ ] JVM test: scan of ui/ composables for user-facing Text literals (fails: literals present)
    - [ ] Run both; confirm red
- [ ] Task: Externalize History, Workout, Presets literals to values/strings.xml (Green)
    - [ ] Replace literals with stringResource; keep English text identical
    - [ ] Rerun literal-scan test for touched screens; JVM suite green
- [ ] Task: Externalize Export, Restore, Archive, Measurements, remaining screens + dialogs (Green)
    - [ ] Same replace-and-verify per screen
    - [ ] Literal-scan test fully green; no English behavior change
- [ ] Task: Refactor duplicated format patterns into shared string/plural keys
    - [ ] Rerun JVM suite; still green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Russian translation (blocked on user glossary)

- [ ] Task: Collect user glossary (blocking input; externalization in Phase 1 proceeds regardless)
    - [ ] Glossary received and recorded in track notes
- [ ] Task: Author values-ru/strings.xml (Green)
    - [ ] Glossary terms applied verbatim; remainder derived consistently
    - [ ] Parity test green
- [ ] Task: Convert count-driven wording to plurals with one/few/many forms
    - [ ] pluralStringResource call sites updated; JVM suite green
- [ ] Task: Present full Russian string set for user review; apply corrections
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Locale formatting + PDF

- [ ] Task: Route user-visible dates/times/numbers through active-locale formatters (Red: UI test asserting ru-RU date/decimal rendering fails; Green: implement)
    - [ ] Keep machine input patterns (e.g. date-entry hint) intact
- [ ] Task: Localize PDF report headers/labels/units to active locale; entry text and ZIP payloads untouched
    - [ ] JVM/PDF tests green
- [ ] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md)

## Phase 4: Verification + documentation

- [ ] Task: Locale-matrix UI tests (Blue)
    - [ ] ru-RU: Russian asserted on all screens + dialogs; en-US: English unchanged
    - [ ] Run until green on device RFCT719Y8BP (device unlocked, classes individually)
- [ ] Task: Full regression — JVM suite + all instrumented classes green
- [ ] Task: Write Docs/psd/russian-localization.md and Docs/tests/russian-localization.md; drop planned marker in Docs/index.md
    - [ ] wiki lint exits 0
- [ ] Task: Manual device pass in Russian (no English chrome, no clipped buttons) + user string sign-off
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)
