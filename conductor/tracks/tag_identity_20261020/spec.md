# Track tag_identity_20261020 — Specification

## Overview

One tag concept currently exists as up to three stored spellings: preset chips
write "FITNESS", inline hashtags lowercased "fitness", system auto-tags
"Fitness". All render as the same "#Fitness" chip, so one entry can show two
visually identical chips, and the Fitness filter chip misses entries tagged
through the other spellings. This track makes the stored value canonical
(lowercase), normalizes every ingestion path, and migrates existing rows.

## Context

- preset chip stores `JournalTag.name` — `ui/components/TagSelectionRow.kt:48-50`
- hashtags store lowercase — `domain/HashtagParser.kt:14-17`
- system auto-tags store capitalized — `domain/HashtagParser.kt:26-28`
- save unions preset + extracted — `ui/screens/AddEntryScreen.kt:585`;
  edit re-unions selection + text + system tags — `viewmodel/JournalViewModel.kt:320-337`
- display resolves every case variant to one label — `ui/components/TagSelectionRow.kt:30-32`
- filters compare exact strings — `data/local/JournalDao.kt:45`
- other ingestion points: `sync/SyncWorker.kt:159`, `export/RestoreRepository.kt:69`

## Functional Requirements

- FR1 Canonical value: every tag is stored as its Unicode lowercase (Locale.ROOT)
  word. Presets, inline hashtags and system auto-tags write the identical value
  ("fitness"), so one concept = one cross-ref.
- FR2 Create path: selecting Fitness and typing "#Fitness" yields exactly one
  tag and one chip.
- FR3 Edit path: an entry carrying the canonical tag from any source opens with
  the Fitness chip selected; saving writes exactly the selected chips plus
  re-parsed hashtags, so deselecting the chip removes the tag (auto-tags
  included).
- FR4 Filtering: tapping the preset Fitness chip returns entries tagged Fitness
  from any source; entry chips pass the canonical value into the filter.
- FR5 Display unchanged: known tags resolve via string resources (ru parity);
  free-form tags render capitalized with "#"; no new strings.
- FR6 Migration v18→19: fold case variants onto the canonical value per entry
  and dedupe cross-refs in a transaction; tag membership otherwise unchanged;
  no entry content/timestamp changes; no backfill of new tags.
- FR7 Ingestion normalization: tags arriving from sync merge and backup/restore
  are canonicalized and deduped before they are written; export keeps writing
  stored canonical values; payload format unchanged.
- FR8 Case-insensitive identity applies to all tags, including free-form:
  "#MorningRun" and "#morningrun" are one tag.
- FR9 Inline text wins on save: if the text still contains "#fitness",
  deselecting the chip does not remove the tag.

## Non-Functional

- Unicode-aware normalization (Locale.ROOT), not ASCII-only SQL LOWER.
- Migration is transactional and idempotent; failure leaves v18 data intact;
  covered by a real-schema migration test.
- No change to tag table shape, sync payload format, or restore manifest.
- Offline; normalization is O(tags per entry), no new I/O.
- `Docs/prd|psd|tests/hashtags.md` and cited wiki pages updated;
  `python3 wiki/lint.py` exits 0.

## Acceptance Criteria

- AC1 create: Fitness chip + "#Fitness" → one `fitness` cross-ref, one
  "#Fitness" chip (JVM + instrumented).
- AC2 filter: preset Fitness chip matches hashtag-tagged, auto-tagged and
  manually tagged entries (instrumented DAO + History screen).
- AC3 edit: chip state reflects canonical membership; no duplicates after save;
  deselected auto-tags are removed (JVM).
- AC4 migration: v18 with `FITNESS` + `fitness` on one entry yields one
  `fitness` row; other entries' tags untouched (migration test).
- AC5 sync/restore: mixed-case incoming tags collapse to canonical (JVM).
- AC6 free-form: case variants merge; display stays capitalized (JVM).
- AC7 full build green, JVM suite green, touched instrumented suites green,
  wiki lint 0.

## Out of Scope

- Tag management UI (rename/merge/delete), autocomplete, translating user tags.
- Changing the seven preset tags or backfilling tags onto untagged entries.
- Live re-parse of hashtags while typing.
- Separate adjacent defect: a free-form inline tag persists on edit even after
  its "#word" is deleted (current behavior; candidate follow-up track).
