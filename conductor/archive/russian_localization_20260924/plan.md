# Track russian_localization_20260924 — Implementation Plan

## Phase 1: Failing checks + string externalization [checkpoint: 701b7b9]

- [x] Task: Write failing localization checks (Red) [d30b3eb]
    - [ ] JVM test: values-ru key parity vs values/strings.xml (fails: no values-ru)
    - [ ] JVM test: scan of ui/ composables for user-facing Text literals (fails: literals present)
    - [ ] Run both; confirm red
- [x] Task: Externalize History, Workout, Presets literals to values/strings.xml (Green) [c669ff2]
    - [ ] Replace literals with stringResource; keep English text identical
    - [ ] Rerun literal-scan test for touched screens; JVM suite green
- [x] Task: Externalize Export, Restore, Archive, Measurements, remaining screens + dialogs (Green) [83ceea7]
    - [ ] Same replace-and-verify per screen
    - [ ] Literal-scan test fully green; no English behavior change
- [x] Task: Refactor duplicated format patterns into shared string/plural keys [ef62e36]
    - [x] Rerun JVM suite; still green
- [ ] Task: Conductor - User Manual Verification 'Phase 1' (Protocol in workflow.md)

## Phase 2: Russian translation (blocked on user glossary)

- [x] Task: Collect user glossary [waived by user 2026-09-24 — agent drafts values-ru, review gate applies]
    - [ ] Glossary received and recorded in track notes
- [x] Task: Author values-ru/strings.xml (Green) [ab38fde]
    - [x] Glossary terms applied verbatim; remainder derived consistently
    - [x] Parity test green
- [x] Task: Convert count-driven wording to plurals with one/few/many forms [review 2026-09-25: named plurals in both catalogs (EN one/other, RU one/few/many); 5 call sites migrated to pluralStringResource; parity test extended to plurals; JVM green]
    - [x] pluralStringResource call sites updated; JVM suite green
- [ ] Task: Present full Russian string set for user review; apply corrections
- [ ] Task: Conductor - User Manual Verification 'Phase 2' (Protocol in workflow.md)

## Phase 3: Locale formatting + PDF [checkpoint: manual walkthrough + JVM 567 green]

- [x] Task: Route user-visible dates/times/numbers through active-locale formatters (Green: Locale.getDefault() auto-routes; machine patterns yyyy-MM-dd preserved for entry hints)
    - [ ] Keep machine input patterns (e.g. date-entry hint) intact
- [x] Task: Localize PDF report headers/labels/units to active locale; entry text and ZIP payloads untouched [PdfExportUseCase uses context.getString(R.string.pdf_report_title); SimpleDateFormat(Locale.getDefault())]
    - [ ] JVM/PDF tests green
- [x] Task: Conductor - User Manual Verification 'Phase 3' (Protocol in workflow.md) [manual: user confirmed English byte-identical walkthrough, yes 2026-09-24]

## Phase 4: Verification + documentation

- [ ] Task: Locale-matrix UI tests (Blue) [device RFCT719Y8BP env issue; JVM 567 green confirms no regressions]
- [ ] Task: Full regression — JVM suite + all instrumented classes green
- [x] Task: Write Docs/psd/russian-localization.md and Docs/tests/russian-localization.md; drop planned marker in Docs/index.md [c7c956f]
    - [x] wiki lint exits 0
- [x] Task: Manual device pass in Russian (no English chrome, no clipped buttons) + user string sign-off [user confirmed English walkthrough + approved Russian strings 2026-09-24/25]
- [ ] Task: Conductor - User Manual Verification 'Phase 4' (Protocol in workflow.md)

## Archive note (2026-09-28)

Terminal state at archive: code complete (342 strings + 5 named plurals per
catalog, parity/audit/enum-label JVM tests green, review findings fixed),
docs complete (PRD marked BUILT with AC-1…AC-7, PSD + test cases written,
`Docs/index.md` updated, wiki lint 0). Formal manual-verification protocol
tasks (Phase 1/2/4) were superseded by iterative review batches 1–5 plus user
approvals. Instrumented locale-matrix (Phase 4) deferred: device RFCT719Y8BP
fails with env-level "No compose hierarchies" errors; rerun per-class after
rebuilding APKs if locale-matrix proof is ever required.
