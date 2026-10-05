# Track Spec — Hashtags

Implements `Docs/prd/hashtags.md` (planned).

## Overview

Two automatic tag sources feed the existing entry-tag table: kind-based
auto-tags (Workout→Fitness, Measurement→Health, Medication→Medication)
attached to the entry created alongside the event (workouts already insert
a journal entry on finish), and inline `#word` parsing (Unicode letters,
case-insensitive, deduplicated) on save. All tags — auto, inline, manual —
share storage, filtering, search, sync, and backup; UI renders them
identically.

## Functional Requirements

- FR-1: Finishing/logging a workout tags its journal entry `Fitness`;
  measurements tag `Health`, medications tag `Medication` (attachment
  mechanism per PSD where no linked entry exists).
- FR-2: On entry save, `#word` tokens (Unicode letters/digits/underscore)
  become tags; matching is case-insensitive with dedupe.
- FR-3: Tags persist via EntryTagCrossRef; History filter/search treats all
  sources identically; tag chips filter on tap.
- FR-4: Auto-tags are system-owned (follow the kind, not removable);
  inline tags die with their `#word` on edit-and-save.
- FR-5: Auto-tag labels from resources with `values-ru` parity; inline tags
  stored verbatim.
- FR-6: No retroactive tagging of unedited history.

## Non-Functional Requirements

- Parse on save only; no I/O beyond existing tag writes; fully offline.

## Acceptance Criteria

- AC-1: Logged workout's entry carries Fitness; filter shows it.
- AC-2: Entry saved with `#Recovery and #recovery` yields one tag; chip
  filters.
- AC-3: Russian auto-tag labels render; Cyrillic inline tags stored
  verbatim.
- AC-4: Editing text to drop `#word` removes that tag on save; auto-tag
  remains.
- AC-5: JVM suite green; wiki lint exits 0.

## Out of Scope

- Tag management UI, autocomplete/suggestions, backfill, tag translation.
