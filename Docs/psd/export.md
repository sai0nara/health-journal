# Export — Product Specification

> An `ExportViewModel` state machine drives two use cases — PDF (date-ranged
> report) and full ZIP backup — producing a `FileProvider` URI that the Export
> screen hands to the system share sheet.

Last updated: 2026-09-02

## Overview

The feature is a Compose screen (two tabs: Export and Restore) backed by
`ExportViewModel`. On the Export tab the user picks a format and the PDF-only
date range, then `exportData` runs the matching use case in a coroutine and
emits a ready-to-share URI. The ZIP path writes a versioned manifest plus JSON
snapshots and media, which is exactly the layout the restore feature reads back.

## Architecture

- MVVM: `ExportViewModel` (an `AndroidViewModel`) exposes `ExportState` as a
  `StateFlow` (`Idle | Generating | ReadyToShare | Error`).
- The two use cases encapsulate generation; `FullBackupUseCase` is injected,
  `PdfExportUseCase` and `ImageResizer` are constructed internally.
- Output goes through a `FileProvider` URI; the screen triggers
  `Intent.ACTION_SEND` on `ReadyToShare`.

## Data flow

1. User selects format + (PDF) range and taps Generate → `Generating`.
2. `exportData(startDate, endDate, format)` runs in `viewModelScope`.
3. PDF: `PdfExportUseCase.execute(range)` filters entries to the range, newest-
   first, and builds a PDF (downsampling photos via `ImageResizer`).
4. ZIP: `FullBackupUseCase.execute()` collects every data domain + media and
   writes the versioned manifest and snapshots via `BackupWriter`.
5. The resulting file is exposed as a `FileProvider` URI → `ReadyToShare`.
6. The screen opens the share sheet; `ExportService` cleans cache on expiry.

## Components

| Component | File | Responsibility |
|---|---|---|
| Export screen | `app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt` | format/range UI + share intent |
| Export ViewModel | `app/src/main/java/com/example/healthjournal/export/ExportViewModel.kt` | state machine + orchestration |
| Full backup use case | `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` | ZIP assembly incl. media |
| PDF use case | `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` | medical report PDF |
| Backup writer | `app/src/main/java/com/example/healthjournal/export/BackupWriter.kt` | ZIP layout + manifest |
| Manifest model | `app/src/main/java/com/example/healthjournal/export/BackupDataManifest.kt` | manifest + backup data DTOs |
| Image resizer | `app/src/main/java/com/example/healthjournal/export/ImageResizer.kt` | photo downsampling for PDF |
| Export service | `app/src/main/java/com/example/healthjournal/export/ExportService.kt` | generated-file cache cleanup |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| PDF range narrower/all | range filters; empty range yields no entries |
| ZIP ignores range | `FullBackupUseCase` always fetches the full set |
| Missing media file in backup | skipped rather than fatal |
| Duplicate media filename | deduplicated by name |
| Generation failure | `Error` state surfaced with a message |
| Rotation during generation | `ExportState` + saved form state survive |

## Dependencies

- PDF toolkit for the medical report; a ZIP writer for the backup.
- `FileProvider` + `Intent.ACTION_SEND` for delivery.
- Room/repository access for all data domains.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt` — Export UI + share.
- `app/src/main/java/com/example/healthjournal/export/ExportViewModel.kt` — orchestration.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — ZIP assembly.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — PDF generation.
- `app/src/main/java/com/example/healthjournal/export/BackupWriter.kt` — manifest/ZIP layout.
- `app/src/main/java/com/example/healthjournal/export/BackupDataManifest.kt` — manifest model.
- `app/src/main/java/com/example/healthjournal/export/ExportService.kt` — cache cleanup.
- `Docs/prd/export.md` — requirements.
- `Docs/tests/export.md` — test cases.