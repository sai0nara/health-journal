# Export — Product Requirements

> Export journal data as a PDF medical report or a full ZIP backup of the raw
> data and media, delivering the output through the system share sheet.

Last updated: 2026-09-02

## Overview

The Export tab produces either a PDF medical report (with an optional date
range) or a complete ZIP backup of the raw journal data and media. The ZIP path
is the backup half of the backup/restore story: it bundles a manifest plus JSON
snapshots of every data domain and the photos/attachments, and may be encrypted
for the restore flow. The shared sheet hands the generated file to the user.

## Goals / Non-goals

**Goals**

- Produce a PDF summarizing journal entries over a chosen date range.
- Produce a full ZIP backup of all raw data + media.
- Deliver output via the system share sheet.
- Record a versioned manifest so the backup can be validated on restore.

**Non-goals**

- CSV or XML export (not implemented).
- Saving to a user-chosen location via SAF (the share sheet is used instead).
- Encrypting the default export (encryption is a separate restore-path concern).

## User stories

- As a user, I want a medical-style PDF of my entries for a date range to share
  with a care provider.
- As a user, I want a complete ZIP backup of my data and media to keep or move
  to another device.
- As a user, I want the file handed to a share target so I choose where it goes.

## Functional requirements

- FR-1: The Export screen offers PDF and ZIP formats.
- FR-2: The date-range picker is shown for PDF only; the range filters the PDF's
  entries and is ignored for a full ZIP backup.
- FR-3: PDF export sorts entries newest-first within the range.
- FR-4: ZIP export bundles a versioned manifest, JSON snapshots of entries,
  measurements, goals, personal card, tombstones, and tags, plus the media files.
- FR-5: Output is a `FileProvider` URI that triggers the share sheet.
- FR-6: A progress/error state is shown while generating.

## Non-functional requirements

- Deterministic manifest (format + schema version + backup timestamp) for
  restore validation.
- Media resolved by filename and deduplicated within a backup.
- Missing media files are skipped, not fatal.

## Acceptance criteria

- AC-1: Selecting PDF and a date range generates a shareable PDF of that range.
- AC-2: Selecting ZIP generates a shareable full backup regardless of the range.
- AC-3: The ZIP contains all data domains + media referenced by the manifest.
- AC-4: Export state survives a configuration change (rotation).
- AC-5: Errors surface rather than silently failing.

## Out of scope

- CSV/XML formats.
- Encrypting the on-demand export (see `Docs/prd/restore-from-backup.md`).

## Cross-references

- `Docs/prd/restore-from-backup.md` — the counterpart that reads back the ZIP.
- [[export-restore]] — the export/restore pipeline page.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/ExportScreen.kt` — Export/format UI + share.
- `app/src/main/java/com/example/healthjournal/export/ExportViewModel.kt` — export state machine.
- `app/src/main/java/com/example/healthjournal/export/FullBackupUseCase.kt` — full ZIP backup assembly.
- `app/src/main/java/com/example/healthjournal/export/PdfExportUseCase.kt` — PDF medical report.
- `app/src/main/java/com/example/healthjournal/export/BackupWriter.kt` — ZIP layout + manifest writing.
- `app/src/main/java/com/example/healthjournal/export/BackupDataManifest.kt` — manifest/backup data model.
- `app/src/main/java/com/example/healthjournal/export/ExportService.kt` — generated-cache cleanup.
- `Docs/psd/export.md` — specification.
- `Docs/tests/export.md` — test cases.