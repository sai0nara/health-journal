# Hashtags — Product Requirements

> Entries tag themselves: every special event carries an automatic tag
> (workouts → Fitness), `#words` typed into entry text become tags on save,
> and all of it filters through the existing tag search.

Last updated: 2026-10-05

> **Status: BUILT.** All functional requirements below are implemented:
> kind auto-tags and inline `#word` parsing feed the shared tag table,
> feed chips filter on tap, and labels carry Russian parity. Measurement
> saves create linked entries (user-approved); medications have no log
> flow, so that mapping stays unwired. See
> `Docs/psd/hashtags.md` for the design and `Docs/tests/hashtags.md` for
> the verification.

## Overview

Journal entries already support manual tags (`EntryTagCrossRef`, picked in
`app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt`
and used to filter in
`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`).
But tagging is all-or-nothing manual: a workout log is just text unless the
user remembers to tag it, and `#words` typed into entries are dead text.

This feature adds two automatic tag sources on top of the manual picker,
all stored in the same tag table so filter, search, sync, and backup treat
them identically:

1. **Auto-tags by entry kind** — creating a special event tags it
   (workout → Fitness; the full kind→tag mapping is defined in the PSD).
2. **Inline hashtags** — `#word` tokens in entry text are parsed into tags
   when the entry is saved.

## Goals / Non-goals

**Goals**

- Every special event is findable by tag without manual tagging effort.
- `#word` syntax in entry text works the way users expect from social apps.
- One unified tag set: auto, inline, and manual tags share storage,
  filtering, search, sync, and backup.
- Tag chips in the feed filter history when tapped.

**Non-goals**

- A tag-management surface (rename, merge, delete tags globally).
- Tag suggestions or completions while typing.
- Changing how manual tags work today.

## User stories

- As a user logging workouts, I want them auto-tagged Fitness so I can
  pull up every training log with one tap.
- As a user writing entries, I want `#words` I type to become real tags
  so my own vocabulary is searchable.
- As a user browsing history, I want to tap a tag chip and see everything
  with that tag regardless of which source created it.

## Functional requirements

- FR-1: Each entry kind maps to an auto-tag applied at creation
  (workout → Fitness; full mapping in the PSD).
- FR-2: `#word` tokens (letters/digits/underscore, case-insensitive,
  deduplicated) in entry text become tags on save.
- FR-3: Auto, inline, and manual tags are stored identically in the entry
  tag table and are indistinguishable to filter, search, sync, backup,
  and restore.
- FR-4: Auto-tags are system-owned: they follow the entry kind and cannot
  be removed manually. Inline tags are removed by deleting the `#word`
  from the text.
- FR-5: Auto-tag display labels resolve via string resources with full
  `values-ru` parity; inline tags are stored verbatim in the user's own
  language.
- FR-6: Tapping a tag chip anywhere filters history to that tag.
- FR-7: Parsing runs on save only (no live re-parse while typing); tagged
  state recomputes on edit-and-save.

## Non-functional requirements

- Offline-first: parsing and auto-tagging need no connectivity.
- No save-time regression: tag extraction is a pure string scan, no I/O
  beyond the existing tag writes.
- Existing entries gain no tags retroactively unless edited and re-saved
  (no backfill migration).

## Acceptance criteria

- AC-1 (FR-1): Logged workout's entry carries Fitness; filter shows it.
- AC-2 (FR-2/FR-3): Entry saved with `#Recovery and #recovery` yields one
  tag; chip filters.
- AC-3 (FR-5): Russian auto-tag labels render; Cyrillic inline tags stored
  verbatim.
- AC-4 (FR-4): Editing text to drop `#word` removes that tag on save;
  auto-tag remains.
- AC-5: JVM suite green; wiki lint exits 0.

## Out of scope

- Tag rename/merge/delete management UI.
- Autocomplete or suggested tags while typing.
- Retroactive tagging of historical entries.
- Translating user-typed tags.

## Cross-references

- `Docs/psd/hashtags.md` — the specification (added when the feature is built).
- `Docs/tests/hashtags.md` — the test cases (added when the feature is built).
- [[ui-layer]] — the entry and history surfaces this extends.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/EntryTagCrossRef.kt` — the tag table this feature writes through.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — manual tag picker and entry save path.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — tag filter this feature feeds.
