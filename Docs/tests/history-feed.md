# History Feed — Test Cases

> Maps the feed, search, tag, and archive behaviors to unit and instrumented
> coverage, including the swipe-archive/undo interaction and batch operations.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | archive/restore/delete/empty call-through, tag-toggle sync, feed query selection |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | tombstone grace-period cleanup, tag/query delegation, import |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | feed display, swipe-to-archive + undo |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ArchiveScreenTest.kt` | multi-select batch delete, empty-archive flow, search |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` | sort asc/desc, LIKE search, tag AND-filter, tag lifecycle |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-1 | Newest-first list | two entries inserted | both render in DESC order |
| T-2 | AC-2 | Search filter | query text entered | feed switches to LIKE-search DAO path |
| T-3 | AC-3 | Tag AND filter | two tags selected | only entries with both returned |
| T-4 | AC-4 | Swipe to archive + undo | entry in feed | swipe archives; Undo restores flag |
| T-5 | AC-5 | Batch delete | multi-select two entries, confirm | tombstones written, rows/tag refs gone, sync triggered |
| T-6 | AC-5 | Empty archive | entries archived, confirm bottom sheet | all archived rows deleted, tombstones written |
| T-7 | AC-5 | Restore selected | archived entries selected | `isArchived` flipped back |
| T-8 | FR-8 | Local media cleanup | archived entry with photos | files removed (see photos-media) |

## Manual checks

- Haptic feedback on archive swipe and on batch operations.
- Empty-archive confirmation copy and destructive-action color.
- Search behavior with multi-word queries and leading/trailing spaces.

## Cross-references

- `Docs/prd/history-feed.md` — requirements under test.
- `Docs/psd/history-feed.md` — design the cases verify.
- `Docs/prd/photos-media.md` — file-cleanup coverage.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — archive actions + feed queries.
- `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` — tombstone/tag logic.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — feed + swipe/undo.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ArchiveScreenTest.kt` — multi-select/delete/empty/search.
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — query-level behavior.
- `Docs/prd/history-feed.md` — requirements.
- `Docs/psd/history-feed.md` — design.