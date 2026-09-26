# Russian Localization — Test Cases

> Maps the PRD's requirements and the PSD's contract to concrete verification.
> The "Automated coverage" table cites the real test files; automated checks
> enforce parity, externalization, and taxonomy labels at build time, while
> device-locale rendering is covered by instrumented and manual checks.

Last updated: 2026-09-26

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` | `values-ru/strings.xml` covers every default `<string>` and `<plurals>` key |
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` | no user-facing `Text/placeholder/contentDescription/showSnackbar/Toast` literals in UI sources |
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/EnumLabelResTest.kt` | taxonomy `labelRes` ids are distinct (25) and exist in both catalogs |
| JVM unit (app suite) | full `:app:testDebugUnitTest` | regression safety after externalization (validation, viewmodel, converter suites) |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/SettingsScreenTest.kt` | settings renders, dropdown options via resource-backed labels |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PermissionsRationaleActivityTest.kt` | rationale activity renders localized text |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ComponentPreviewScreenTest.kt` | component gallery renders with resources resolved |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | RU catalog parity | both catalogs present | run `LocalizationParityTest` | default key set (strings + plurals) is a subset of RU keys |
| T-2 | Externalization audit | full UI externalized | run `HardcodedStringAuditTest` | no hit from any carrier regex in `ui/` sources |
| T-3 | Taxonomy labels | enums adopted `labelRes` | run `EnumLabelResTest` | 25 distinct ids; every id resolves in both catalogs |
| T-4 | Plural resource wiring | workflow, archive, attachments surfaces | run app suite JVM tests | `pluralStringResource` call sites compile and render counts |
| T-5 | Russian plurals render | device set to ru | open sets/rounds/attachments with counts 1, 2, 5 | `подход/подхода/подходов` (etc.) by quantity |
| T-6 | English fallback | device on any non-ru locale | walk core screens | every surface renders English exactly as pre-change |
| T-7 | Parameterized strings | RU device | open workout routine with target/rest | `%1$d x %2$d @ %3$s · Rest %4$ds` renders with real numbers, no crash |
| T-8 | Locale-aware dates in PDF | RU device | export PDF report | title and dates in Russian/locale format; entry text byte-identical |
| T-9 | Longer RU strings | small screen, RU device | open dialog/detail screens | no clipped action buttons; overflow behaviour unchanged (worst acceptable) |
| T-10 | Full verification | — | JVM suite, each UI class in isolation, wiki lint | all green, lint exits 0 |

## Manual checks

- Set device locale to Russian (Settings → System → Languages) and relaunch the app; every screen incl. dialogs, empty states, validation errors, TalkBack content descriptions, and the permissions rationale renders in Russian.
- Same device back to English; app renders exactly as before localization.
- Drill into workout routine, archive select-all, and entry attachments with 1 / 2 / 5 items; Russian plural forms read natively in every case.
- Export a PDF report on a Russian-locale device; title, section headers, and dates are Russian while user journal text and any attached images stay exactly as entered.
- Kill-and-relaunch with Russian set stays Russian; no mixed-language strings anywhere.

## Cross-references

- `Docs/prd/russian-localization.md` — the requirements under test.
- `Docs/psd/russian-localization.md` — the design the cases verify.
- [[ui-layer]] — the Compose screens covered by the audit and instrumented checks.

## Sources

- `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` — catalog parity.
- `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` — externalization audit.
- `app/src/test/java/com/example/healthjournal/localization/EnumLabelResTest.kt` — taxonomy-label checks.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/SettingsScreenTest.kt` — settings rendering.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PermissionsRationaleActivityTest.kt` — rationale rendering.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ComponentPreviewScreenTest.kt` — component gallery.
- `Docs/prd/russian-localization.md` — requirements under test.
- `Docs/psd/russian-localization.md` — design the cases verify.