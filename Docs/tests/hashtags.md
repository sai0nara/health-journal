# Hashtags — Test Cases

> Maps the PRD's requirements and the PSD's contract to concrete
> verification. The "Automated coverage" table cites the real test files;
> keep it in sync when coverage moves.

Last updated: 2026-10-05

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/HashtagParserTest.kt` | extraction (Unicode, dedupe, case, edges) + kind mapping |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | save unions parsed tags; edit re-parses + preserves auto-tags |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` | finish + manual log tag linked entry Fitness |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` | save creates linked entry with Health tag |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/BuiltInExerciseCatalogTest.kt` | untouched (regression) |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HashtagEditorHtmlTest.kt` | plain-text parses; HTML `&num;` must never be parsed |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` | chips render, tap reports tag, no-tags hides row |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Extraction contract | — | run `HashtagParserTest` | 10 cases green |
| T-2 | Save unions tags | — | save entry `#Recovery` + manual `DOCTOR` | both tags written |
| T-3 | Edit stickiness | entry tagged Fitness | edit text, save | Fitness kept; dropped `#words` gone |
| T-4 | Workout auto-tag | — | finish routine / manual log | linked entry tagged Fitness |
| T-5 | Measurement link | — | save measurement | linked entry tagged Health |
| T-6 | Plain-text parse | device | type `#hashtag` in editor, save | chip appears on feed item |
| T-7 | Chip filters | tagged entry in feed | tap chip | history filters to tag |
| T-8 | RU labels | `ru` device | open filter row | Fitness/Health/Medication in Russian |
| T-9 | Cyrillic inline | `ru` device | save `#тренировка` | stored verbatim, chip filters |
| T-10 | Full verification | — | JVM suite, androidTest compile + device spot checks, wiki lint | all green, lint exits 0 |

## Manual checks

- Save entry with `#hashtag` line → chip on the feed item; tap → filtered.
- Finish a workout → linked entry carries Fitness.
- Log a measurement → linked entry carries Health.
- `ru` device: auto-tag labels Russian; Cyrillic inline tags verbatim.
- Edit entry to remove `#word` → tag gone on save; auto-tag stays.

## Cross-references

- `Docs/prd/hashtags.md` — the requirements under test.
- `Docs/psd/hashtags.md` — the design the cases verify.
- [[ui-layer]] — the entry and history surfaces covered.

## Sources

- `app/src/test/java/com/example/healthjournal/domain/HashtagParserTest.kt` — parser contract.
- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — save/update union.
- `app/src/test/java/com/example/healthjournal/viewmodel/WorkoutViewModelTest.kt` — workout auto-tag.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` — measurement link.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HashtagEditorHtmlTest.kt` — editor contract.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` — chip UI.
- `Docs/prd/hashtags.md` — requirements under test.
- `Docs/psd/hashtags.md` — design the cases verify.
