# Entry Logging — Test Cases

> Maps the capture behavior and its edge cases to concrete verification across
> the JVM ViewModel tests, Room DAO tests, and the instrumented Add Entry screen
> tests.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | add/update entry, future-date rejection, entity write |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | insert, tag add/remove, tag hydration on read |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/JournalTypeConvertersTest.kt` | JSON lists for media/vitals round-trip |
| JVM unit | `app/src/test/java/com/example/healthjournal/util/HtmlEntitiesTest.kt` | non-ASCII entity decoding |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/UtcToLocalDateTest.kt` | UTC↔local date handling |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` | save/back/empty/pickers/Unarchive render + callbacks |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` | entry insert/read, tag lifecycle, type converters |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-1/AC-2 | Save entry | typed non-blank description + timestamp + tags | ViewModel insert called with description; blank → not called |
| T-2 | AC-3 | Future timestamp | future epoch passed to `addEntry` | insert not called |
| T-3 | AC-1 | Edit entry | existing entry + new description | repository insert called with copied fields + `PENDING_SYNC` |
| T-4 | AC-4 | Tag round-trip | entry with tags saved then read | tags hydrated from cross-ref table |
| T-5 | AC-4 | Tag add on save | new tag not yet present | cross-ref inserted, entry marked dirty + sync triggered |
| T-6 | AC-6 | Archived entry read-only | entry with `isArchived` true | screen shows Unarchive; toolbar/capture hidden |
| T-7 | AC-2 | Empty description UI | Add Entry with blank text | tapping save calls nothing, stays |

## Manual checks

- Camera capture with the CAMERA permission grant/deny flow on a device.
- Pasting rich content with emoji/arrows verifies entity decoding renders cleanly.
- Date/time pickers on a device across time zones (no off-by-one).

## Cross-references

- `Docs/prd/entry-logging.md` — requirements under test.
- `Docs/psd/entry-logging.md` — design the cases verify.
- [[unit-tests]] — the JVM stack.
- [[instrumented]] — the instrumented stack.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — add/update/future-date.
- `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` — insert/tags/hydration.
- `app/src/test/java/com/example/healthjournal/data/local/JournalTypeConvertersTest.kt` — media/vitals JSON.
- `app/src/test/java/com/example/healthjournal/util/HtmlEntitiesTest.kt` — entity decoding.
- `app/src/test/java/com/example/healthjournal/domain/UtcToLocalDateTest.kt` — date handling.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` — capture UI.
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — entry/tag DAO.
- `Docs/prd/entry-logging.md` — requirements.
- `Docs/psd/entry-logging.md` — design.