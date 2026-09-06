# Photos & Media — Product Requirements

> Attach photos and file attachments to journal entries, store them safely in
> app-private storage with image compression, and manage that media across
> capture, sync, restore, and deletion.

Last updated: 2026-09-02

## Overview

Every journal entry can carry photos and generic file attachments. Photos are
captured by camera or picked from the gallery; generic files are attached from
storage. On save the app copies them into private storage — photos through the
image compression service (JPEG, quality 80, EXIF-preserved), attachments
verbatim — and records them on the entry as URI lists. That media then travels
with the entry through Drive sync and backup/restore (URIs remapped), and is
cleaned up from local storage on deletion, sandboxed to the app's own folder.

## Goals / Non-goals

**Goals**

- Capture one or more photos by camera and multi-pick from the gallery.
- Attach generic files from storage.
- Store media in app-private storage, compressing photos and preserving
  orientation.
- Remap media URIs through Drive sync and backup/restore.
- Remove an entry's local media safely when the entry is deleted.

**Non-goals**

- A media gallery/browser beyond the per-entry thumbnails.
- Editing/deleting an already-saved file when it is removed from an entry in the
  editor (only the DB reference is dropped).

## User stories

- As a user, I want to add photos to an entry so I record visual detail.
- As a user, I want to attach documents or other files so the entry has context.
- As a user, I want my media to survive sync and restore on another device.

## Functional requirements

- FR-1: Photos are captured via camera (with CAMERA permission) or picked
  multi-select from the gallery.
- FR-2: Generic files are attached via the system file picker (any mime).
- FR-3: Photos are compressed to JPEG quality 80, with EXIF orientation
  preserved; decode/compress failures fall back to a raw copy or are skipped.
- FR-4: Media is stored in `filesDir/photos` (photos) and `filesDir/attachments`
  (files) with unique names; URIs are stored on the entry as JSON lists.
- FR-5: Thumbnails render per entry, expandable full-screen; attachment count is
  shown.
- FR-6: Drive sync downloads/upload media binaries and remaps URIs to local
  paths.
- FR-7: Backup/restore includes media and remaps URIs.
- FR-8: Deleting an entry removes its local media, only within app-private
  storage (sandboxed).

## Non-functional requirements

- Storage safety: only `filesDir`-prefixed paths are ever deleted locally.
- Deterministic filename keying by last URL path segment across systems.
- Deduplication of media within a backup by filename.

## Acceptance criteria

- AC-1: Camera and gallery photos attach and persist on save.
- AC-2: Generic files attach and persist with name/mime.
- AC-3: Photos are compressed (JPEG q80) and oriented correctly.
- AC-4: Sync and restore bring back media accessible via remapped local URIs.
- AC-5: Deleting an entry removes its local media but never a foreign path.

## Out of scope

- Cloud photo publishing distinct from Drive app-data sync.
- Per-photo sizing beyond the compress path (PDF export downsizes separately).

## Cross-references

- `Docs/prd/entry-logging.md` — where media is captured alongside an entry.
- `Docs/prd/drive-sync.md` — media sync/remap.
- `Docs/prd/restore-from-backup.md` — media re-import on restore.
- [[domain-media]] — the media service page.
- [[data-layer]] — the media fields on the entry.

## Sources

- `app/src/main/java/com/example/healthjournal/media/MediaCompressionService.kt` — photo compression/storage.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — persistent-file save + cleanup.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — capture pickers + thumbnails.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — thumbnail/attachment rendering.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — `photo_urls`/`attachments` model.
- `app/src/main/java/com/example/healthjournal/data/local/JournalTypeConverters.kt` — JSON list persistence.
- `app/src/main/java/com/example/healthjournal/export/ImageResizer.kt` — PDF-export downsampling.
- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — media sync/remap.
- `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` — media re-import/remap.
- `Docs/psd/photos-media.md` — specification.
- `Docs/tests/photos-media.md` — test cases.