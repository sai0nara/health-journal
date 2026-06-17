# Specification: Data Export (PDF & ZIP Archive)

## Overview
Implement a robust, memory-efficient data export feature allowing users to export their health journal history as human-readable PDF reports or structured ZIP archives. The implementation must use stream-based processing to avoid Out-Of-Memory (OOM) crashes when handling many high-resolution attachments.

## User Value Proposition
Complete data sovereignty. Users can extract specific records or generate comprehensive medical reports for sharing with healthcare providers.

## Functional Requirements
- **Single Attachment Export:** Save a single attachment from an entry to a public device folder (e.g., Downloads).
- **Batch Export (Date Range):** Export entries within a user-defined date range.
- **Formats:**
    - **PDF:** Human-readable report with formatted text and embedded images (downsampled).
    - **ZIP:** Structured archive containing `data.json` (text entries) and a `media/` folder (original attachments).
- **Safe Sharing:** Use `FileProvider` to share generated files via the Android Share Sheet.
- **Progress Tracking:** Show a progress bar during export generation.
- **Cleanup:** Automatically delete temporary export files from cache before starting a new export.

## Technical Requirements
- **OOM Prevention:** Stream-based processing for PDF and ZIP generation.
- **Background Processing:** Use Coroutines (and potentially WorkManager for long-running batch exports) to keep the UI responsive.
- **Libraries:**
    - PDF Generation: iText7 or Apache PDFBox for Android.
    - ZIP Generation: `java.util.zip.ZipOutputStream`.
- **File Management:** Generate files in `context.cacheDir` and expose via `FileProvider`.

## UX Design
- **Export Screen:** Simple UI to select date range and format.
- **Share Sheet:** Trigger native Android share sheet upon completion.
- **Feedback:** "Generating Report..." message with progress indicator.

## Out of Scope
- Direct backup to cloud services (handled by existing Google Drive sync).
- Advanced PDF styling (e.g., custom themes per user).
