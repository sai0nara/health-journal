# Specification: Support Multiple Photos and Attachments with Cloud Sync

## Goal
Enhance journal entries to support multiple photo attachments and general file attachments (common formats like PDF, DOCX), ensuring all media is preserved across devices via Google Drive synchronization.

## Problem Statement
The current implementation only supports a single photo per entry. Users need the ability to attach multiple photos (e.g., progress pictures) and technical documents (e.g., medical reports) to a single journal event.

## Objectives
- Update the `JournalEntry` schema to store multiple photo URIs and attachment metadata.
- Implement a `TypeConverter` to handle list-to-string serialization for Room.
- Modify the "Add Entry" UI to support:
    - Multiple photo selection.
    - File selection for common formats (PDF, DOCX, TXT, etc.).
    - Interactive lists showing attached items with deletion support.
- Update Cloud Synchronization:
    - Iterate through and upload all attached files to Google Drive.
    - Synchronize and download all referenced files to new devices.
    - Handle relative path re-mapping for multiple files.

## Success Criteria
- Users can add 3+ photos and 2+ documents to a single entry.
- All attachments are visible and accessible in the entry details.
- Synchronizing a second device restores all photos and documents.
