# History Feed — Product Specification

> A reactive, DB-driven feed over the non-archived entries couples with a
> swipe/undo archive interaction; the Archive screen reuses the same search and
> tag filters over the archived rows and adds multi-select delete/restore.

Last updated: 2026-09-02

## Overview

Both History and Archive are thin Compose layers over `StateFlow<List<JournalEntry>>`
derived from the current search query and selected tags. The DAO filters by the
`isArchived` flag so the two surfaces never overlap. Archiving merely flips the
flag (plus a `lastModified` bump); permanent deletion is the only destructive
path and leaves a tombstone.

## Architecture

- MVVM with `JournalViewModel` exposing reactive feed state via `StateFlow`
  using `combine` + `flatMapLatest` keyed on search query, sort order, and
  selected tags.
- DAO-level branching: no filters → sorted query; query only → LIKE search;
  tags → combined text+tag query enforced with a distinct-count HAVING clause
  (AND semantics).
- Archive is a boolean column; deletion writes a `DeletedEntry` tombstone before
  removing the row, and removes the row's local media files through a sandboxed
  file helper.

## Data flow

1. `HistoryScreen` observes the composed feed flow; on text/sort/tag change the
   ViewModel switches the DAO query and the flow re-emits.
2. A swipe invokes `archiveEntry(id)` → `updateArchiveStatus(isArchived=1)` +
   `lastModified` bump; a snackbar Undo calls `restoreEntry(id)`.
3. Pull-to-refresh calls `syncNow()` for a manual sync.
4. `ArchiveScreen` observes the archived feed; multi-select collects ids; Restore
   flips flags back; Delete calls `deleteEntries(ids)` which writes tombstones,
   deletes rows/tag refs, and cleans local files; Empty calls the batch variant
   for all archived ids.

## Components

| Component | File | Responsibility |
|---|---|---|
| History screen | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | feed, search/sort/tag, swipe-archive + undo, pull-to-refresh |
| Archive screen | `app/src/main/java/com/example/healthjournal/ui/screens/ArchiveScreen.kt` | archived feed, multi-select, restore/delete/empty |
| Journal ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | feed composition, archive/restore/delete actions |
| Journal repository | `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` | archive-status, tombstone, file cleanup |
| Journal DAO | `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` | feed/search/tag/archive queries |
| Entry item | `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` | entry card in both surfaces |
| Search bar | `app/src/main/java/com/example/healthjournal/ui/components/SharedSearchBar.kt` | shared search input |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Empty feed / archive | dedicated empty states |
| Archived entry reopened via id | Add Entry renders read-only (see `Docs/psd/entry-logging.md`) |
| Permanent deletion | tombstone written first; resurrection suppressed on sync (see `Docs/psd/drive-sync.md`) |
| Local media deletion | only `filesDir`-prefixed paths removed; foreign URIs ignored |
| Undo after archive | restores the `isArchived` flag |

## Dependencies

- Room reactive flows; the shared `JournalViewModel`/fake testing seam.
- Material 3 swipe-to-dismiss and snackbar for archive/undo.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — feed interactions.
- `app/src/main/java/com/example/healthjournal/ui/screens/ArchiveScreen.kt` — archive management.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — feed state + actions.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — archive/delete logic.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — feed/search queries.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — card rendering.
- `Docs/prd/history-feed.md` — requirements.
- `Docs/tests/history-feed.md` — test cases.