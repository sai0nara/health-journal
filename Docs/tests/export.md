# Export — Test Cases

> Verifies the export use cases at JVM level and the Export screen's format,
> range-visibility, and state-restoration behavior at the instrumented level.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` | full backup with all entities, schema version, media inclusion |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupWriterTest.kt` | ZIP layout, media entries, manifest |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` | reading the backup data back |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/ExportServiceTest.kt` | generated-file cache cleanup |
| JVM unit | `app/src/test/java/com/example/healthjournal/export/ExportUseCaseCompilationTest.kt` | PDF/ZIP use-case instantiation smoke |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenTest.kt` | screen renders via MainActivity |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenDefectsTest.kt` | rotation state, scroll-to, format labels, date-card visibility |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-3 | Full backup content | entries + measurements + goals + card + tombstones + tags + media | all domains in ZIP; manifest references them |
| T-2 | AC-3 | Schema version in manifest | current DB | manifest `schemaVersion` reflects DB version |
| T-3 | AC-3 | Media included | entry with attachments/photos | files under `media/`, deduped |
| T-4 | AC-1 | PDF range visibility | PDF selected | date-range card visible; ZIP hides it |
| T-5 | AC-4 | Rotation | generating state across config change | state restored |
| T-6 | AC-2 | ZIP ignores range | ZIP selected with a range | full set exported (range not used) |

## Manual checks

- Generate a PDF and share to a real target; verify formatting and photo
  downsampling.
- Generate a ZIP and open it to confirm the manifest and `media/` layout.
- Empty-date-range PDF produces a sensible (empty) report, not a crash.

## Cross-references

- `Docs/prd/export.md` — requirements under test.
- `Docs/psd/export.md` — design the cases verify.
- `Docs/prd/restore-from-backup.md` — the ZIP/reader counterpart.
- [[unit-tests]] / [[instrumented]] — test stacks.
- [[export-restore]] — the pipeline page.

## Sources

- `app/src/test/java/com/example/healthjournal/export/FullBackupUseCaseTest.kt` — backup assembly.
- `app/src/test/java/com/example/healthjournal/export/BackupWriterTest.kt` — ZIP layout.
- `app/src/test/java/com/example/healthjournal/export/BackupDataReaderTest.kt` — data reading.
- `app/src/test/java/com/example/healthjournal/export/ExportServiceTest.kt` — cache cleanup.
- `app/src/test/java/com/example/healthjournal/export/ExportUseCaseCompilationTest.kt` — use-case smoke.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenTest.kt` — screen render.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/ExportScreenDefectsTest.kt` — rotation/visibility.
- `Docs/prd/export.md` — requirements.
- `Docs/psd/export.md` — design.