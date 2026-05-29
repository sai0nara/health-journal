# Implementation Plan: Support Multiple Photos and Attachments with Cloud Sync

## Phase 1: Data Layer & Persistence
- [x] Task: Refactor JournalEntry Schema
    - Replace `photo_url: String?` with `photo_urls: List<String>` and `attachments: List<AttachmentData>`.
    - Create `AttachmentData` data class (name, uri, mimeType).
- [x] Task: Implement TypeConverters
    - Add JSON converters for lists in `JournalDatabase`.
- [x] Task: Migration Strategy
    - Ensure existing `photo_url` data is migrated to the new `photo_urls` list.

## Phase 2: UI Implementation (Add Entry)
- [x] Task: Multi-Photo Selection
    - Update `AddEntryScreen` to allow multiple image picking.
- [x] Task: General File Attachment Support
    - Integrate a file picker for common document formats.
- [x] Task: Attachment Preview List
    - Implement a horizontal/vertical list in `AddEntryScreen` to show and remove attachments.

## Phase 3: Cloud Synchronization
- [x] Task: Update DriveServiceHelper
    - Ensure robust handling of many small files.
- [x] Task: Refactor SyncWorker
    - Update the merging and upload/download logic to iterate through lists of photos and attachments.
    - Maintain path mapping for all files.

## Phase 4: Verification
- [x] Task: Instrumented Sync Test for Multiple Files
    - Update `SyncDownloadTest` to verify entries with multiple attachments.
- [x] Task: Conductor - User Manual Verification 'Multi-Attachments' (Protocol in workflow.md)
