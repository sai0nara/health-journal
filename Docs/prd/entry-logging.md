# Entry Logging — Product Requirements

> Capture a dated journal entry with rich text, date/time, tags, attached media,
> and optional health metrics pulled from Health Connect — and edit or view it
> (read-only, if archived) later.

Last updated: 2026-09-02

## Overview

Entry logging is the core capture path of the app. A user writes what happened
and when, marking relevant category tags, attaching photos and generic files,
and (via Health Connect) pulling that day's blood-pressure, heart-rate, and
sleep numbers into the entry. The Add Entry screen doubles as the Edit screen
for a chosen entry, and shows an archived entry read-only so it can be
unarchived rather than edited in place.

## Goals / Non-goals

**Goals**

- Create and edit a dated entry with rich text (headings, bold/italic/underline,
  strikethrough, lists, links) and plain-text fallback handling.
- Select categories from a fixed set of tags.
- Attach photos and generic files.
- Import that day's health metrics into the entry from Health Connect.
- Clamp/reject future timestamps so entries are never dated in the future.

**Non-goals**

- A separate title or mood field (the description is the whole content).
- Editing archived entries in place (they are read-only and only unarchivable).

## User stories

- As a user, I want to log a dated entry with formatted text and tags so I can
  record events richly.
- As a user, I want to attach photos and files so my entry includes evidence and
  documents.
- As a user with Health Connect, I want today's vitals pulled into my entry so I
  do not type them by hand.
- As a returning user, I want to edit a past entry and see any archived one as
  read-only so I can unarchive it without accidental edits.

## Functional requirements

- FR-1: The Add Entry screen creates a new entry; passing an existing id opens
  it for editing.
- FR-2: The description is the entry content, captured as rich text and stored
  as HTML. A save with a blank description is ignored.
- FR-3: Date and time are selectable with pickers; the chosen timestamp is the
  entry's date. Future timestamps are rejected on save.
- FR-4: Up to four category tags (ILLNESS, CHECKUP, DOCTOR, EXERCISES) can be
  toggled and persisted per entry.
- FR-5: Multiple photos and generic file attachments are captured (camera,
  gallery multi-pick, file picker) and attached.
- FR-6: The Enrichment panel's Health action imports that day's blood-pressure,
  heart-rate, and sleep from Health Connect into the entry.
- FR-7: Editing an archived entry is read-only and offers an Unarchive action.

## Non-functional requirements

- Persistence is offline-first: entries save to local Room immediately.
- Edits mark the entry pending-sync so a later sync uploads the change.
- Non-ASCII HTML entities are decoded back to UTF-8 text for clean rendering.

## Acceptance criteria

- AC-1: Saving a non-blank entry persists description, timestamp, tags, media,
  and any health metrics.
- AC-2: Saving a blank description does nothing.
- AC-3: A future timestamp cannot be saved.
- AC-4: Selected tags round-trip: re-opening the entry shows the chosen tags.
- AC-5: Health Connect data, when granted, fills blood pressure, heart rate, and
  sleep on the entry.
- AC-6: An archived entry opens read-only with an Unarchive action.

## Out of scope

- Sync transport (see `Docs/prd/drive-sync.md`).
- Media compression specifics (see `Docs/prd/photos-media.md`).
- Health Connect specifics (see `Docs/prd/health-connect.md`).

## Cross-references

- `Docs/prd/photos-media.md` — capture and storage of the attached media.
- `Docs/prd/health-connect.md` — the vitals pulled into the entry.
- `Docs/prd/history-feed.md` — how captured entries are listed, searched, archived.
- [[data-layer]] — the entry schema and DAO.
- [[ui-layer]] — the Add Entry screen.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/AddEntryScreen.kt` — create/edit/read-only capture UI.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — add/update/tag/save-persistent logic.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — insert and tag persistence.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — the persisted entry model.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — entry and tag queries.
- `app/src/main/java/com/example/healthjournal/ui/components/RichTextToolbar.kt` — rich text controls.
- `app/src/main/java/com/example/healthjournal/util/HtmlEntities.kt` — non-ASCII entity decoding.
- `Docs/psd/entry-logging.md` — specification.
- `Docs/tests/entry-logging.md` — test cases.