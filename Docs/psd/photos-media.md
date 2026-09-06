# Photos & Media — Product Specification

> Media is captured via platform pickers, persisted into app-private storage
> through a compression/token joined by filename, stored on the entry as JSON
> URI lists, remapped across Drive sync and backup/restore, and deleted through
> a sandboxed local-file helper.

Last updated: 2026-09-02

## Overview

`JournalEntry` carries two JSON lists: `photo_urls` (image URIs) and
`attachments` (`AttachmentData` rows with name/uri/mime/status). The capture
flow is in `AddEntryScreen`; a single `savePersistentFile` in the ViewModel
routes photos through the compression service and files to a raw copy. Across
systems, the last URI path segment is the durable key for remapping.

## Architecture

- MVVM: capture/cleanup in `JournalViewModel`; rendering in Add Entry and the
  entry card.
- `MediaCompressionService` (Android implementation) decodes, compresses to JPEG
  q80, re-writes EXIF orientation, and writes `filesDir/photos/media_<UUID>.<ext>`;
  failures fall back to a raw copy.
- Persistence via Room type converters (Gson) for the two JSON lists, with
  normalization of legacy rows.
- Sync (`SyncWorker`) and restore (`RestoreRepository`) both re-import binaries
  into `filesDir/photos|attachments` and rewrite URIs to `file://` local paths.
- Cleanup guards against deleting anything outside `filesDir`.

## Data flow

1. User taps Camera/Gallery/File in the Enrichment panel; pickers return URIs.
2. On save, each photo/file goes through `savePersistentFile`: photos compressed
   by `compressAndSaveImage`, files copied verbatim; failures filtered/toasted.
3. The final URI lists are persisted on the entry via `addEntry`/`updateEntry`.
4. On sync, missing binaries download into `filesDir` and URIs remap to local
   paths; on upload, local files go up and cloud URIs normalize status.
5. On backup, `FullBackupUseCase` collects media by filename under `media/`; on
   restore, `RestoreRepository` re-imports and remaps.
6. On entry deletion, `deleteLocalFiles` removes only `filesDir`-prefixed paths.

## Components

| Component | File | Responsibility |
|---|---|---|
| Media compression | `app/src/main/java/com/example/healthjournal/media/MediaCompressionService.kt` | photo compress + store |
| ViewModel capture/cleanup | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | `savePersistentFile`, `deleteLocalFiles` |
| Capture UI | `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` | camera/gallery/file pickers, thumbnails, per-item remove |
| Entry rendering | `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` | photo thumbnails + attachment count |
| Entry media model | `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` | `photo_urls`/`attachments` |
| Type converters | `app/src/main/java/com/example/healthjournal/data/local/JournalTypeConverters.kt` | JSON persistence + legacy normalization |
| Sync media | `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` | download/upload + URI remap |
| Restore media | `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` | re-import + remap |
| PDF resizer | `app/src/main/java/com/example/healthjournal/export/ImageResizer.kt` | export-side downsampling |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Decode/compress failure | raw-byte fallback or skip (never a 0-byte file) |
| Empty input stream | returns null; no file written |
| EXIF non-normal orientation | orientation tag rewritten onto the saved JPEG |
| Non-`file` URI on save | copied persistent; `file://`-scheme URIs kept as-is |
| Sync/restore remap | filename-keyed; cloud rows normalized to `SYNCED` |
| Foreign path in cleanup | prefix guard blocks deletion outside filesDir |

## Dependencies

- AndroidX activity pickers (`TakePicture`, `PickMultipleVisualMedia`,
  `OpenDocument`); Coil image loading for thumbnails.
- Gson via Room type converters; Bitmap/ExifInterface for compression.

## Sources

- `app/src/main/java/com/example/healthjournal/media/MediaCompressionService.kt` — compression.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — save/cleanup.
- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — capture + thumbnails.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — rendering.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — media model.
- `app/src/main/java/com/example/healthjournal/data/local/JournalTypeConverters.kt` — JSON.
- `app/src/main/java/com/example/healthjournal/sync/SyncWorker.kt` — sync media.
- `app/src/main/java/com/example/healthjournal/export/RestoreRepository.kt` — restore media.
- `Docs/prd/photos-media.md` — requirements.
- `Docs/tests/photos-media.md` — test cases.