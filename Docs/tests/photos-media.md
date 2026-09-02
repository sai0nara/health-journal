# Photos & Media — Test Cases

> Verifies compression behavior, sandboxed cleanup, JSON persistence, and the
> sync/restore media remap across the JVM unit and instrumented suites.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/media/MediaCompressionServiceTest.kt` | compress, raw fallback, null on empty, EXIF orientation |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | attachment file cleanup; sandbox guard |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | attachment append/save delegation |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/JournalTypeConvertersTest.kt` | photo/attachment JSON round-trip + normalization |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` | media re-import + URI remap |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` | media collection into backup |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` | attachment update/status |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/AttachmentSchemaTest.kt` | full `AttachmentData` round-trip incl. remote url |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` | media re-import + remap end-to-end |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/sync/SyncDownloadTest.kt` | worker media merge + remap |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` | Enrichment buttons present/enabled |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-3 | Compress success | valid image | JPEG q80 saved; file URI returned |
| T-2 | AC-3 | Raw fallback | decode/compress failure | raw copy written |
| T-3 | AC-3 | Empty input | empty stream | returns null, no 0-byte file |
| T-4 | AC-3 | EXIF orientation | non-normal orientation | orientation tag preserved on output |
| T-5 | AC-5 | Local cleanup | entry with files under filesDir | files removed on delete |
| T-6 | AC-5 | Sandbox guard | `file://` pointing outside filesDir | untouched |
| T-7 | AC-1/AC-2 | Attachment persistence | AttachmentData with remote url/status | round-trips through converters |
| T-8 | AC-4 | Sync media remap | 2 photos + 1 attachment | files survive + URIs remapped to filesDir |
| T-9 | AC-4 | Restore media remap | staged media | media copied + URIs remapped |
| T-10 | AC-1 | Enrichment UI | Add Entry shown | Camera/Gallery/File buttons present/enabled |

## Manual checks

- Camera capture with CAMERA permission grant/deny on a device.
- Captured photo orientation correct after compression and display.
- Adding a large image and an arbitrary file, saving, then verifying the files
  exist under `filesDir/photos` and `filesDir/attachments`.

## Cross-references

- `Docs/prd/photos-media.md` — requirements under test.
- `Docs/psd/photos-media.md` — design the cases verify.
- `Docs/prd/drive-sync.md` / `Docs/prd/restore-from-backup.md` — media remap cases.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/media/MediaCompressionServiceTest.kt` — compression.
- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — cleanup/sandbox.
- `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` — attachment persistence.
- `app/src/test/java/com/example/healthjournal/data/local/JournalTypeConvertersTest.kt` — JSON.
- `app/src/test/java/com/example/healthjournal/export/RestoreRepositoryTest.kt` — restore media.
- `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` — backup media.
- `app/src/androidTest/java/com/example/healthjournal/data/local/JournalDaoTest.kt` — attachment DAO.
- `app/src/androidTest/java/com/example/healthjournal/data/local/AttachmentSchemaTest.kt` — schema round-trip.
- `app/src/androidTest/java/com/example/healthjournal/export/RestoreIntegrationTest.kt` — restore media.
- `app/src/androidTest/java/com/example/healthjournal/sync/SyncDownloadTest.kt` — sync media.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/AddEntryScreenTest.kt` — capture UI.
- `Docs/prd/photos-media.md` — requirements.
- `Docs/psd/photos-media.md` — design.