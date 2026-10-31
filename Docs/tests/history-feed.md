# History Feed — Test Cases

> Maps the feed, search, tag, and archive behaviors to unit and instrumented
> coverage, including the swipe-archive/undo interaction, batch operations,
> and the trimmed top bar.

Last updated: 2026-10-29

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | archive/restore/delete/empty call-through, tag-toggle sync, feed query selection |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | tombstone grace-period cleanup, tag/query delegation, import |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | feed display, swipe-to-archive + undo, trimmed top bar, card vitals + quick-add flow |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` | vitals chips, units, action labels, archived action hidden |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsEntrySheetTest.kt` | sheet prefill, validation, save/cancel, clearing |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ArchiveScreenTest.kt` | multi-select batch delete, empty-archive flow, search |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryFeedDeletionP0Test.kt` | P0 deletion flows on the real viewmodel over in-memory Room: selection vs filters, confirmed delete + tombstones, sandbox media cleanup, cancel/dismiss purity, empty archive, History gesture guards, no resurrection |
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
| T-9 | — | Trimmed top bar | History screen | no logo, no Personal Card/Measurements/Export shortcuts; title and overflow remain |
| T-10 | AC-6 | Vitals chips | entry with BP/HR/glucose/temperature/SpO2/sleep | one chip per value with units; metric and imperial variants |
| T-11 | AC-6 | Card quick-add | entry without vitals, action tapped, values saved | `updateVitals` receives canonical values, card updates, action becomes `Edit vitals`; Cancel writes nothing |
| T-12 | FR-11 | Archived card | archived entry with vitals | chips visible, no action button |

## Manual checks

- Physical feel of haptics (the P0 suite asserts which gestures request feedback).
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
- `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` — card chips/action.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsEntrySheetTest.kt` — quick-add sheet.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ArchiveScreenTest.kt` — multi-select/delete/empty/search.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryFeedDeletionP0Test.kt` — P0 deletion flows (selectors in `pages`, haptic recorder in `util`).
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — query-level behavior.
- `Docs/prd/history-feed.md` — requirements.
- `Docs/psd/history-feed.md` — design.