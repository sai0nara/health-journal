# Hashtags — Product Specification

> A pure domain parser plus entry-kind mapping feeds the existing tag
> table: the editor screen parses plain text (never HTML — the editor
> encodes `#` as `&num;`), the ViewModels union tags at save, and feed
> items render tappable chips.

Last updated: 2026-10-05

## Overview

Two tag sources, one table. `HashtagParser` (pure Kotlin, no Android
dependencies) extracts `#word` tokens — Unicode letters, digits,
underscore, lowercased with `Locale.ROOT`, deduplicated — and
`EntryKindTag` maps kinds to canonical labels (Fitness/Health/
Medication). The critical integration fact: the rich-text editor
serializes `#` as the `&num;` HTML entity, which the app's entity decoder
leaves encoded — so parsing MUST read `annotatedString.text`, never the
stored HTML. See `Docs/prd/hashtags.md` for the requirements.

## Architecture

- No new layers: domain owns parsing/mapping; `JournalViewModel` unions
  tags at the single save/update choke point; `WorkoutViewModel` and
  `BodyMeasurementViewModel` attach kind tags to the entries they create.
- Auto-tags are sticky without schema change: on edit, existing tags in
  the known `SYSTEM_TAGS` set are preserved while inline tags re-derive
  from current text.
- Feed chips read a reactive `entryId → tags` map (`observeAllTags` flow,
  single query); tapping toggles the existing History tag filter.
- Display labels for the three auto-tags resolve via resources (Russian
  parity); all other tags render capitalized verbatim.
- Seams for testing: pure parser/mapping (JVM), VM tag union with mocked
  repositories (JVM), chip render/tap and editor-HTML contract
  (instrumented).

## Data flow

1. User saves an entry: screen parses plain text, unions with manual
   tags, calls `addEntry`/`updateEntry`.
2. ViewModel re-parses defensively, unions manual + inline + sticky
   auto-tags, writes via `EntryTagCrossRef`.
3. Workout finish/manual log inserts the linked entry, then tags it
   Fitness; measurement save inserts its entry, then tags it Health.
4. History collects the tags map; feed items render `#chips`; tap
   toggles the tag filter; filter row offers all enum categories.

## Components

| Component | File | Responsibility |
|---|---|---|
| Tag parser + mapping | `app/src/main/java/com/example/healthjournal/domain/HashtagParser.kt` | `#word` extraction, kind→tag constants |
| Save/update union | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | parsed + manual + sticky auto-tags; `tagsByEntry` map |
| Workout auto-tag | `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` | Fitness tag on linked entries (finish + manual) |
| Measurement link | `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` | linked entry + Health tag on save |
| Tag categories | `app/src/main/java/com/example/healthjournal/data/JournalTag.kt` | enum incl. new auto-tag categories |
| Display labels | `app/src/main/java/com/example/healthjournal/ui/components/TagSelectionRow.kt` | `tagDisplayName` resolver |
| Feed chips | `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` | tappable `#chips` per entry |
| Reactive tags | `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` | `observeAllTags` flow |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| `#` typed in editor | stored as `&num;` in HTML, renders as `#`; parsing uses plain text so nothing is missed |
| Duplicate/differing-case tags | lowercased + deduped (`#Recovery` + `#recovery` → one tag) |
| Auto-tagged entry edited | auto-tag preserved via known-set stickiness; inline tags follow current text |
| Lone `#` / `C#` | no word characters → no tag |
| Cyrillic tags | `\p{L}` matches; stored verbatim; filter compares raw strings |
| Unparseable stored tag data | existing per-site fallbacks unchanged (empty list, never crash) |

## Dependencies

- Rich-text editor's `annotatedString.text` (plain-text source); Room
  `Flow` for reactive tags; Navigation/Compose unchanged.
- No new libraries, permissions, network, schema migration, or
  sync/backup payload changes (tags already covered).

## Cross-references

- `Docs/prd/hashtags.md` — the requirements this specification implements.
- `Docs/tests/hashtags.md` — the test cases that verify this design.
- [[ui-layer]] — the entry and history surfaces this extends.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/HashtagParser.kt` — parser and mapping.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — save/update union, tags map.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` — workout auto-tag.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — measurement link.
- `app/src/main/java/com/example/healthjournal/data/JournalTag.kt` — categories.
- `app/src/main/java/com/example/healthjournal/ui/components/TagSelectionRow.kt` — display resolver.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — feed chips.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — reactive tags query.
- `app/src/test/java/com/example/healthjournal/domain/HashtagParserTest.kt` — parser contract.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HashtagEditorHtmlTest.kt` — editor-HTML contract.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` — chip UI.
- `Docs/prd/hashtags.md` — requirements this specification implements.
- `Docs/tests/hashtags.md` — test cases.
