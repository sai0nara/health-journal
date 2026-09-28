# Track russian_localization_20260924 — Specification

## Overview

Implement `Docs/prd/russian-localization.md`: the full app UI reads in Russian
on Russian-locale devices. Today only Personal Card and Settings resolve
strings via `values/strings.xml`; all other screens embed hard-coded English
literals and no `values-ru` exists. The track externalizes every user-facing
literal, ships a complete Russian translation, applies Russian plurals and
locale-aware formatting, and localizes the PDF report — in a single pass over
all screens.

Agreed decisions: locale follows the system (no in-app switcher in v1); all
screens at once (no phasing); the user supplies the terminology glossary
(still pending — translation work is blocked until it arrives,
externalization is not); the PDF report follows the active locale.

## Functional Requirements

- FR-1: Every user-facing literal in `ui/` composables (titles, buttons,
  labels, menus, dialogs, empty states, validation errors, content
  descriptions) resolves via `stringResource` / `pluralStringResource`; no
  `Text("...")` literal with user-facing text remains.
- FR-2: `values-ru/strings.xml` covers 100% of keys in `values/strings.xml`,
  enforced by an automated parity check.
- FR-3: On a ru-RU device the full UI (including dialogs, errors, TalkBack
  descriptions) renders Russian; on any other locale the UI is behaviorally
  identical to today (English).
- FR-4: Count-driven wording uses `<plurals>` with Russian one/few/many forms.
- FR-5: User-visible dates, times, and decimals format via the active locale;
  fixed machine patterns survive only where they are input syntax.
- FR-6: The PDF medical report renders section headers, labels, and units in
  the active locale; raw entry text and ZIP payloads are untouched.
- FR-7: Positional format args (`%1$s`) keep correct argument order under
  Russian word order.
- FR-8: User-supplied glossary terms are applied verbatim; remaining strings
  are derived consistently and presented for user review before merge.

## Non-Functional Requirements

- NFR-1: Resources only — no new dependencies, permissions, network, or SDK
  changes.
- NFR-2: No layout regressions from longer Russian strings on small screens
  (existing ellipsis/wrapping is the worst acceptable case).
- NFR-3: Semantics of color-coded states unchanged; light/dark themes
  unaffected.
- NFR-4: Quality gates per workflow: full JVM + instrumented suites green,
  wiki lint exits 0, `Docs/` PSD/tests written when built, index updated.

## Acceptance Criteria

- AC-1: Automated parity check proves every `values` key exists in
  `values-ru` (and keeps it so).
- AC-2: A locale-matrix UI test sets ru-RU and asserts Russian text on
  History, Workout, Export, Restore, Presets, Measurements, Archive,
  Personal Card, and dialogs; the same test on en-US asserts English.
- AC-3: Manual device pass in Russian shows no English chrome and no clipped
  action buttons/dialog titles.
- AC-4: A PDF generated under ru-RU carries Russian headers/labels; entry
  content unchanged.
- AC-5: User reviews and signs off the Russian strings (glossary verbatim +
  derived remainder).
- AC-6: Full test suite (JVM + all instrumented classes) green; lint exits 0.

## Out of Scope

- In-app language override (deferred past v1 by explicit decision).
- Additional locales, RTL, locale-specific artwork.
- Translating historical exports or user-entered content.
- Layout redesigns.

## Inputs & Dependencies

- Blocking input: the user glossary (English = Russian pairs). Translation
  tasks wait for it; string externalization proceeds immediately.
- Source of truth: `Docs/prd/russian-localization.md`; PSD/tests docs
  (`Docs/psd/russian-localization.md`, `Docs/tests/russian-localization.md`)
  are written during this track.
