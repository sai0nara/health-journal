# Russian Localization — Product Requirements

> The full app UI reads in Russian on Russian-locale devices: every
> user-facing string externalized to resources with a complete `values-ru`
> translation, Russian plural rules, and locale-aware dates and numbers.
>
Last updated: 2026-09-26
>
> **Status: BUILT.** All functional requirements below are implemented: the
> English catalog holds 342 strings plus 5 named plurals, `values-ru` covers
> every key, taxonomy enums resolve through `labelRes`, and the PDF report
> localizes. See `Docs/psd/russian-localization.md` for the design and
> `Docs/tests/russian-localization.md` for the verification.

## Overview

The app's UI is English-only. The Personal Card and Settings screens already
resolve their labels, errors, and content descriptions through
`app/src/main/res/values/strings.xml` via `stringResource`, which proves the
pattern — but every other screen (Workout, History menus, Export, Restore,
Presets, Measurements, Archive, dialogs) embeds English text directly in
composables. A Russian-speaking user therefore gets a half-English app even
with the device set to Russian.

This feature externalizes all user-facing text into string resources and
ships a complete Russian translation, so the Android resource system renders
the whole UI in Russian when the device locale is Russian and falls back to
English otherwise. User-entered data (entry text, names, medications) is
never translated — only the app's own chrome is localized.

## Goals / Non-goals

**Goals**

- Externalize every user-facing literal in Compose UI (titles, buttons,
  labels, menu items, dialog text, empty states, validation errors, content
  descriptions) into `values/strings.xml`.
- Ship a complete `values-ru/strings.xml` covering all externalized strings.
- Apply Russian plural rules (`one`/`few`/`many` via `<plurals>`) wherever a
  count drives wording (sets, reps, entries, contacts, files).
- Format user-visible dates, times, and numbers with the active locale
  instead of hard-coded patterns.
- Localize the generated PDF medical report to match the active locale.

**Non-goals**

- Any language beyond Russian and English.
- An in-app language switcher; v1 follows the system locale (deferred, see
  Out of scope).
- Translating user-entered content (journal text, names, drug names) or raw
  data inside ZIP exports.
- Redesigning layouts; strings must fit existing components (ellipsis and
  wrapping behaviour unchanged).

## User stories

- As a Russian-speaking user, I want the whole app UI in Russian so I can
  log workouts and read my history without translating in my head.
- As a Russian-speaking user, I want counts and dates to read naturally
  (correct plurals, day-month order) so the app feels native, not
  machine-translated.
- As a user showing my PDF report to a Russian doctor, I want the report in
  Russian so it is immediately usable.
- As an English-speaking user, I want nothing to change: English remains the
  complete fallback.

## Functional requirements

- FR-1: No user-facing `Text("...")` literal remains in `ui/` composables;
  all such text resolves via `stringResource`/`pluralStringResource`.
- FR-2: A `values-ru/strings.xml` exists and covers 100% of the string keys
  in `values/strings.xml` (verified by a lint/resource check, not by eye).
- FR-3: On a device set to Russian, every screen (including dialogs,
  validation errors, TalkBack content descriptions, and notifications, if
  any) renders Russian; on any other locale the app renders exactly as
  today.
- FR-4: Count-dependent wording uses `<plurals>` with correct Russian forms
  (e.g. 1 подход / 2 подхода / 5 подходов patterns).
- FR-5: User-visible dates, times, and decimal numbers format via the active
  locale; hard-coded `YYYY-MM-DD`-style patterns survive only where they
  are machine input (e.g. the date-entry hint tied to the accepted format).
- FR-6: The PDF medical report renders in the active locale (section
  headers, labels, units); raw entry text and ZIP payloads stay byte-identical.
- FR-7: Parameterized strings (`%1$s`, positional args) keep argument order
  correct in Russian word order; translators may reorder placeholders.

## Non-functional requirements

- Offline-first and dependency-free: localization is packaged resources only,
  no network, no new permissions, no SDK changes.
- No layout regressions: Russian strings (typically longer) must not clip
  action buttons or dialog titles on small screens; existing overflow
  behaviour is the worst acceptable case.
- Consistent with the medical color system and light/dark UI (no string
  change may alter semantics of color-coded states).
- Translation quality: native-level Russian, medical terms consistent with
  the Personal Card glossary already established (e.g. demographics and
  medical-history labels).

## Acceptance criteria

- AC-1 (FR-1): `HardcodedStringAuditTest` is green — no user-facing literal
  remains in `ui/` composables.
- AC-2 (FR-2): `LocalizationParityTest` is green — `values-ru` covers 100%
  of the default string and plural keys.
- AC-3 (FR-3): on a `ru` device every screen, dialog, validation error, and
  content description renders Russian; on any other locale the app renders
  English exactly as before.
- AC-4 (FR-4): count-driven wording uses `<plurals>` with `one`/`few`/`many`
  (renders 1/2/5 correctly).
- AC-5 (FR-5): user-visible dates and numbers format via the active locale;
  machine-input patterns are unchanged.
- AC-6 (FR-6): the PDF report renders in the active locale while entry text
  and ZIP payloads stay byte-identical.
- AC-7 (FR-7): parameterized strings keep argument order correct in Russian
  word order; no format crash on any localized surface.

## Out of scope

- In-app language override deferred past v1; v1 follows the system locale
  via the platform resource system. (A per-app preference would add a
  settings surface, persistence, and restart semantics — revisit only on
  explicit request.)
- Additional locales, RTL layouts, and locale-specific artwork.
- Translating historical PDF/ZIP exports already on disk or in Drive.
- Machine-translating the user's own journal content.

## Cross-references

- `Docs/psd/russian-localization.md` — the specification (added when the feature is built).
- `Docs/tests/russian-localization.md` — the test cases (added when the feature is built).
- [[ui-layer]] — the Compose screens whose literals are externalized.

## Sources

- `app/src/main/res/values/strings.xml` — the existing string catalog (Personal Card + Settings keys) that the Russian translation extends.
- `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` — representative screen still using hard-coded English literals.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — menu and empty-state literals pending externalization.
