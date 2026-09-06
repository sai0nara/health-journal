# History Feed — Product Requirements

> Browse the journal chronologically, search and filter it, and manage an
> archive (archive by swipe, restore, or permanently delete) from the History
> and Archive screens.

Last updated: 2026-09-02

## Overview

The History screen is the app's home: a reverse-chronological feed of journal
entries with search, tag filtering, sorting, swipe-to-archive with undo, and
pull-to-refresh that triggers a sync. The Archive screen holds entries the user
soft-removed via the `isArchived` flag and supports search, tag filtering,
restore, and permanent (batch or empty-all) deletion. Archiving is not deletion:
an archived row remains in the database, just filtered from the active feed.

## Goals / Non-goals

**Goals**

- List non-archived entries newest-first with optional ascending sort.
- Substring-search entries by content, and filter by tags (AND semantics).
- Archive from the feed by swipe (with undo) and restore from the archive.
- Permanently delete selected entries or empty the whole archive, with
  confirmation.
- Leave a tombstone on permanent deletion so the cloud cannot resurrect it.

**Non-goals**

- Pagination (the full non-archived set is streamed via Flow).
- Search over parsed plaintext (the LIKE search runs over stored HTML).
- Editing directly from the feed beyond the entry-detail navigation.

## User stories

- As a user, I want to see my entries newest-first and search them so I can find
  past logs.
- As a user, I want to filter by category tags so I can narrow to relevant
  entries.
- As a user, I want to swipe an entry away and undo if I change my mind.
- As a user, I want a dedicated archive where I can restore or permanently
  delete soft-removed entries.

## Functional requirements

- FR-1: History lists non-archived entries, newest-first by default, with a
  toggle for ascending order.
- FR-2: A search bar filters by matching the entry description.
- FR-3: Tag toggles filter to entries that have all selected tags.
- FR-4: Swiping an entry left archives it and shows an Undo action (with haptic).
- FR-5: Pull-to-refresh triggers a manual sync.
- FR-6: The Archive screen lists archived entries with the same search/tag
  filters, plus multi-select.
- FR-7: From multi-select, entries can be restored or permanently deleted (with
  confirmation); the whole archive can be emptied with confirmation.
- FR-8: Permanent deletion writes a tombstone and cleans up the entry's local
  media files.

## Non-functional requirements

- Reads are reactive: the feed updates as the database changes.
- Permanent deletion is durable against cross-device resurrection (tombstones).
- Media deletion is sandboxed to app private storage only.

## Acceptance criteria

- AC-1: Non-archived entries are listed newest-first; toggling sort order
  reverses it.
- AC-2: A search query narrows the list to matching entries.
- AC-3: Selecting multiple tags shows only entries with all of them.
- AC-4: Swiping archives the entry and Undo restores it.
- AC-5: Archive multi-select can restore, delete (confirmed), and empty all
  (confirmed); deletion removes local media files.

## Out of scope

- Paging and virtualized loading beyond `LazyColumn`.
- Deleting directly from the feed (delete lives in the archive).

## Cross-references

- `Docs/prd/entry-logging.md` — how entries are captured.
- [[data-layer]] — the entry/archive schema and DAO queries.
- [[ui-layer]] — the History and Archive screens.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — the feed UI, search/sort/archive.
- `app/src/main/java/com/example/healthjournal/ui/screens/ArchiveScreen.kt` — archive list, multi-select, delete.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — feed state composition + archive actions.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — archive-status, delete, tombstone, file cleanup.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — feed/search/archive queries.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — entry card rendering.
- `app/src/main/java/com/example/healthjournal/ui/components/SharedSearchBar.kt` — search input.
- `Docs/psd/history-feed.md` — specification.
- `Docs/tests/history-feed.md` — test cases.