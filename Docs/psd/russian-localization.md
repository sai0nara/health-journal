# Russian Localization — Product Specification

> Externalizes every user-facing string into `values/strings.xml`, ships a
> complete `values-ru` translation with Russian plural rules, resolves enum
> taxonomies through resource ids, and localizes the PDF report — with parity,
> audit, and enum-label tests enforcing the contract.

Last updated: 2026-09-26

## Overview

Localization packages only: no new libraries, no network, no permissions, no
SDK changes. Every Compose literal (titles, buttons, labels, menu items,
dialog text, empty states, validation errors, content descriptions) resolves
through `stringResource` / `pluralStringResource`, so the Android resource
system substitutes the Russian catalog on `ru` devices and falls back to
English everywhere else. User-entered data (journal text, names, medications,
raw ZIP bytes) is never translated.

The design decisions:

- **System locale only.** No in-app language switcher in v1 (PRD "Out of
  scope"); follow the platform resource system. See `Docs/prd/russian-localization.md`.
- **Named plurals, one resource per wording.** Count-driven strings are
  declared as `<plurals>` with a `name` (not ad-hoc string keys): English uses
  `one`/`other`, Russian `one`/`few`/`many`. A `<plurals>` *must* carry a
  `name` attribute or AAPT2 strips it from the build, silently removing every
  variant (review finding, fixed).
- **Taxonomy via enum `labelRes`.** Domain enums whose labels are user-facing
  (`WorkoutType`, measurement kinds, presets) expose a `@StringRes labelRes`
  instead of returning strings, so the UI resolves them through resources.
- **Parity as an automated contract.** The JVM parity test reconstructs the
  key sets of both catalogs (including `<plurals>`) and fails if `values-ru`
  misses a default key or the catalogs drift.
- **Hardcoded-text audit.** A regex-based JVM test fails on any `Text("...")`,
  `placeholder="..."`, `contentDescription="..."`, `showSnackbar("...")`, or
  `Toast.makeText(..., "...")` literal in UI sources, keeping the
  externalization rule enforceable rather than by eye.

## Architecture

- No DI framework; the resource system (`stringResource`,
  `pluralStringResource`, `context.getString`) is the only resolution
  mechanism. Non-Compose code (PDF export, `PermissionsRationaleActivity`)
  receives a `Context` already held and calls `context.getString`.
- Catalogs: `values/strings.xml` (English default, 342 strings + 5 plurals)
  and `values-ru/strings.xml` (complete translation, same shape). No third
  locale exists, so default == English fallback.
- Empty-state and dialog surfaces that previously embedded literals now read
  resources through the same ViewModel-verified string keys; ViewModel-driven
  display strings (sync status, toasts) keep the existing `errorResId`-style
  resource-id pattern.
- Date/number formatting: locale-aware where user-visible (the PDF report
  formats dates via `SimpleDateFormat(..., Locale.getDefault())`); machine
  input patterns (e.g. the date-entry hint matching the accepted parse
  format) stay fixed per PRD FR-5.
- Seams for testing: pure-JVM resource-key parsing (parity test), regex audit
  (JVM), labelRes existence/distinctness checks (JVM), per-screen Compose
  tests (instrumented, run in isolation), and locale-specific instrumented
  checks on a device set to Russian.

## Data flow

1. Compose screen calls `stringResource(R.string.<key>)` /
   `pluralStringResource(R.plurals.<key>, count, ...)` at render time.
2. The resource system resolves the key against the device configuration:
   `values-ru` on `ru` devices, `values` otherwise.
3. Parameterized strings apply their args positionally (`%1$s`, `%1$d`, ...);
   placeholders are reorderable per locale so Russian word order stays
   grammatical (PRD FR-7).
4. Residual `%d`-style placeholders that historically received `.toString()`
   String args (which raise `IllegalFormatConversionException` in Java) are
   fed raw `Int`/`Int?` values with `?: 0` fallbacks.
5. Domain enum users call `labelRes` and pass the result into the same
   resource hooks; non-Compose paths call `context.getString(labelRes)`.
6. The PDF export builds its report via `context.getString(R.string.pdf_report_title)`
   and locale-aware date formats so a Russian doctor reads a Russian report;
   entry content and ZIP payloads stay byte-identical.

## Components

| Component | File | Responsibility |
|---|---|---|
| English catalog | `app/src/main/res/values/strings.xml` | default strings + named plurals |
| Russian catalog | `app/src/main/res/values-ru/strings.xml` | full `ru` translation + one/few/many plurals |
| Screen/component UI | `app/src/main/java/com/example/healthjournal/ui/screens/*.kt`, `ui/components/*.kt` | resolve all user-facing text via resources |
| Taxonomy labels | `app/src/main/java/com/example/healthjournal/domain/WorkoutType.kt`, `ValidateMeasurements.kt`, `WorkoutPreset.kt` | `labelRes` on user-facing enums |
| PDF localization | `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` | localized title, locale-aware dates |
| Restore worker | `app/src/main/java/com/example/healthjournal/sync/RestoreWorker.kt` | localized sync/restore status via resource ids |
| Activity context | `app/src/main/java/com/example/healthjournal/PermissionsRationaleActivity.kt` | localized rationale via `Context.getString` |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Device locale ≠ `ru` | default `values` English renders exactly as before; Russian affects nothing |
| Catalog drift (new default key, missing RU key) | parity JVM test fails the build |
| `<plurals>` missing `name` | impossible to regress silently: parity test would lose the keys; wired audit catches it |
| Russian plural classes | `one`/`few`/`many` chosen by quantity (1/2/5 patterns); `%1$d` reorders correctly in RU |
| Placeholder arg type mismatch | raw `Int`/`Int?` (with `?: 0`) passed to `%d`; string-to-`%d` conversion never occurs |
| Longer Russian strings on small screens | existing overflow/ellipsis behaviour is the accepted worst case (PRD NFR) |
| PDF on `ru` device | localized title + locale-aware dates; byte-identical entry content |

## Dependencies

- Android resource system only (string/plural resolution, locale fallback);
  `Locale`/`SimpleDateFormat` via the platform.
- No new libraries, permissions, network, or schema/sync changes.

## Cross-references

- `Docs/prd/russian-localization.md` — the requirements this specification implements.
- `Docs/tests/russian-localization.md` — the test cases that verify this design.
- [[ui-layer]] — the Compose screens whose literals are externalized.

## Sources

- `app/src/main/res/values/strings.xml` — English catalog and plurals.
- `app/src/main/res/values-ru/strings.xml` — Russian catalog and plurals.
- `app/src/main/java/com/example/healthjournal/ui/screens/*.kt`, `ui/components/*.kt` — resource-resolved UI.
- `app/src/main/java/com/example/healthjournal/domain/WorkoutType.kt`, `ValidateMeasurements.kt`, `WorkoutPreset.kt` — `labelRes` enums.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — localized PDF report.
- `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` — parity contract.
- `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` — externalization audit.
- `app/src/test/java/com/example/healthjournal/localization/EnumLabelResTest.kt` — taxonomy-label checks.
- `Docs/prd/russian-localization.md` — requirements this specification implements.
- `Docs/tests/russian-localization.md` — test cases.