# Entry Logging — Product Specification

> One Add Entry screen drives create, edit, and read-only-archived modes; it
> writes a single Room entry plus its tag cross-references, and merges optional
> Health Connect vitals with manually typed values before the same save.

Last updated: 2026-10-29

## Overview

The feature is a single Compose screen parameterized by an optional entry id:
absent means create, present means edit, and an archived entry renders
read-only. All capture state (text, timestamp, tags, media, vitals) converges on
one ViewModel write that persists the entry and its tags atomically enough for
the app's needs, then schedules a background sync. Manual vitals and health
imports share one draft, so the user can type what Health Connect lacks and
import only the rest.

## Architecture

- MVVM: `AddEntryScreen` is a pure function of `IJournalViewModel` state and
  callbacks; the real `JournalViewModel` is swappable for an in-memory fake in
  instrumented tests.
- Rich text uses a third-party editor whose state converts to/from HTML; the
  non-ASCII named entities it emits are decoded back to UTF-8 before saving.
- The Room model stores the content HTML, an epoch timestamp, JSON lists of
  photo URIs and attachment metadata, the vitals columns (BP, heart rate,
  glucose, temperature, SpO2, sleep), and archive/sync flags. Tags live in a
  separate cross-reference table.
- The manual vitals draft is display-unit text validated by `ValidateVitals` and
  rendered by the shared `VitalsFieldsEditor`; conversion to canonical metric
  happens once, on save.

## Data flow

1. Opening the screen with no id starts `Idle`; with an id, the existing entry
   is loaded (including hydrating its tags) into the editor.
2. The user edits rich text, timestamp, tags, media, and optionally types the
   five manual vitals or taps the Health action, which calls `syncHealthData`
   and merges the returned BP/HR/sleep into local state without clearing manual
   glucose, temperature, or SpO2.
3. Photos and non-`file` attachments are copied into app private storage via
   `savePersistentFile`; failures become empty strings and are filtered, or
   toast for attachments.
4. On save, the ViewModel builds the `JournalEntry` (new UUID when creating,
   fields copied + `PENDING_SYNC` + fresh `lastModified` when updating) and calls
   `repository.insert`; for updates it removes and re-adds the selected tags.
5. A manual sync is (re)enqueued so the change reaches the cloud.

## Components

| Component | File | Responsibility |
|---|---|---|
| Add Entry screen | `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` | create/edit/read-only capture UI; pickers; media attachment |
| Journal ViewModel | `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` | add/update, tag writes, persistent-file save, pending-sync marking |
| Journal repository | `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` | entry insert + tag persistence |
| Entry model | `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` | persisted entry incl. content, timestamp, vitals, flags |
| Journal DAO | `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` | insert/get entry, tag cross-ref methods |
| Rich text toolbar | `app/src/main/java/com/example/healthjournal/ui/components/RichTextToolbar.kt` | formatting controls for the editor |
| HTML entities | `app/src/main/java/com/example/healthjournal/util/HtmlEntities.kt` | decode non-ASCII named entities |
| Enrichment panel | `app/src/main/java/com/example/healthjournal/ui/components/EnrichmentPanel.kt` | camera/gallery/file/health entry points |
| Vitals field editor | `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` | inline BP/HR/glucose/temperature/SpO2 fields with unit labels and errors |
| Vitals validation | `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` | draft validation, range checks, canonical conversion, display formatting |

## Edge cases & failure handling

| Condition | Behaviour |
|---|---|
| Blank description | save is a no-op (blocked by plain-text guard) |
| Future timestamp | rejected in the ViewModel; UI clamps to now |
| Photo/file stream copy fails | photo filtered to empty; attachment toasts failure |
| Rich-text entities | non-ASCII named entities decoded to UTF-8 |
| Archived entry opened for edit | read-only; toolbar/capture/save hidden; Unarchive shown |
| Archived entry with vitals | values render as a read-only summary; no fields or Save |
| Invalid vitals field | inline message; Save disabled until the draft validates |
| Health import returns no value for a field | that draft field is left unchanged |
| Already-synced entry edited | copied with `PENDING_SYNC` + bumped `lastModified` |

## Dependencies

- Jetpack Compose (Material 3) UI; a third-party rich text editor (HTML round-trip).
- Room for entry + tag persistence; the shared `JournalViewModel`/fake pattern.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — capture UI and mode switching.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — capture orchestration and save.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — persistence and tags.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — entry model.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — DAO methods.
- `app/src/main/java/com/example/healthjournal/ui/components/RichTextToolbar.kt` — formatting controls.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsFieldsEditor.kt` — inline vitals fields.
- `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` — vitals validation/conversion.
- `Docs/prd/entry-logging.md` — requirements.
- `Docs/tests/entry-logging.md` — test cases.