# Implementation Plan: Data Export (PDF & ZIP Archive)

## Phase 1: Infrastructure & Safe Sharing [checkpoint: 0894fea]
- [x] Task: Configure `FileProvider` and `filepaths.xml` for safe internal cache sharing.
    - [x] Create `xml/file_paths.xml`.
    - [x] Update `AndroidManifest.xml` with `<provider>` declaration.
- [x] Task: Create `ExportService` interface and basic implementation.
    - [x] Implement `cleanupCache()` to remove old temporary exports.
    - [x] Write unit tests for cache cleanup logic.
- [x] Task: Conductor - User Manual Verification 'Phase 1: Infrastructure & Safe Sharing' (Protocol in workflow.md)

## Phase 2: ZIP Export Implementation
- [ ] Task: Implement `ZipExportUseCase` using `ZipOutputStream`.
    - [ ] Implement text entry serialization to `data.json`.
    - [ ] Implement stream-based media file copying.
    - [ ] Write unit tests for ZIP structure and data integrity.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: ZIP Export Implementation' (Protocol in workflow.md)

## Phase 3: PDF Export Implementation
- [ ] Task: Integrate PDF library (e.g., PDFBox Android) and implement basic report layout.
    - [ ] Implement text wrapping and pagination.
- [ ] Task: Implement image downsampling for PDF embedding.
    - [ ] Create `ImageResizer` utility.
    - [ ] Write unit tests for downsampling logic (verifying reduced memory footprint).
- [ ] Task: Conductor - User Manual Verification 'Phase 3: PDF Export Implementation' (Protocol in workflow.md)

## Phase 4: UI Integration
- [ ] Task: Create `ExportViewModel` and `ExportScreen`.
    - [ ] Implement date range picker and format selector.
    - [ ] Bind progress state to UI indicator.
- [ ] Task: Implement "Share" interaction using `ACTION_SEND`.
    - [ ] Write UI tests for the complete export-to-share flow.
- [ ] Task: Conductor - User Manual Verification 'Phase 4: UI Integration' (Protocol in workflow.md)

## Phase 5: OOM Validation & Final Polish
- [ ] Task: Perform stress testing with many high-res attachments.
    - [ ] Verify memory profile remains stable during large exports.
- [ ] Task: Final UI/UX polish (loading animations, success/error feedback).
- [ ] Task: Conductor - User Manual Verification 'Phase 5: OOM Validation & Final Polish' (Protocol in workflow.md)
