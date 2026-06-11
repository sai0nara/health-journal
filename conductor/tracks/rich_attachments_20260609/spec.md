# Specification: Rich Attachments & Archive Integration

## Overview
Allows users to build a comprehensive medical and personal archive. Users can capture medical prescriptions, lab reports, or visual symptoms and attach them directly to historical journal entries. Unarchiving items from the Archive screen and viewing item cards will be supported. 

## Functional Requirements
- **Flow 1 (Viewing/Unarchiving)**: Archived items open in `JournalDetailScreen` in "Read-Only/Archived" mode. Displays a grid of static thumbnails that expand when tapped. Tapping "Unarchive" smoothly transitions the screen state to active editable mode.
- **Flow 2 (Adding/Saving Attachments)**: In an active/unarchived entry, users can click "Add Attachment". It uses the Android Photo Picker or StorageAccessFramework to select files. Thumbnails with a loading/processing state appear instantly.
- **Local Storage**: Files are copied directly into the app's internal storage directory (`context.filesDir`) to ensure privacy and prevent accidental deletion by the system.
- **Image Compression**: Aggressively compress raw camera photos (JPEG, 80 quality) before saving. If compression fails, save the uncompressed file locally and warn the user.
- **Data Flow**: Room database stores local file URIs and sets sync status to `PENDING_SYNC`.
- **Sync Mechanism**: A `PeriodicSyncWorker` runs opportunistically (aiming for ~15 mins, prioritizing Wi-Fi/unmetered networks). It uploads binary files to cloud storage, appends the cloud URLs to text metadata, and pushes the payload to the main database.
- **UI State Management**: Implement `AttachmentUiState` with properties: `uri`, `fileType`, `isLocalOnly`, and `isUploading`.

## Non-Functional Requirements
- **Performance**: Use Coil for asynchronous media loading and caching.
- **Security**: Scoped internal storage for sensitive health documents.
- **Permissions**: Use modern contracts (`PickVisualMedia`, `OpenDocument`) to avoid broad storage permissions.

## Acceptance Criteria
- [ ] User can view archived entries with media thumbnails in read-only mode.
- [ ] User can unarchive an entry and transition to edit mode seamlessly.
- [ ] User can attach photos and PDFs without granting blanket storage permissions.
- [ ] Attachments are compressed and stored securely in the app's internal storage.
- [ ] WorkManager opportunistically uploads attachments and updates the remote and local database schemas with permanent URLs.
- [ ] Fallback warning is shown if an image fails to compress.

## Out of Scope
- Editing documents directly within the app.
- Optical Character Recognition (OCR) on the uploaded documents.
