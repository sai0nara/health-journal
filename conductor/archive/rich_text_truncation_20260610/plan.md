# Implementation Plan: Rich Text and Truncation (Pivoted to HTML)

## Phase 1: HTML Parsing Logic
- [ ] Task: Create `HtmlParser` utility to convert HTML text to `AnnotatedString`.
    - [ ] Write unit tests for HTML to `AnnotatedString` conversion (<b>, <i>, <h1>-<h3>).
    - [ ] Implement parsing logic using `Html.fromHtml`.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: HTML Parsing Logic' (Protocol in workflow.md)

## Phase 2: Feed View Truncation
- [~] Task: Update `HistoryScreen` and `ArchiveScreen` card components to use truncated HTML text.
    - [x] Implement 3-line truncation with ellipsis using `maxLines` and `TextOverflow`.
    - [ ] Update components to use `HtmlParser`.
    - [ ] Write UI tests to verify truncation behavior.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Feed View Truncation' (Protocol in workflow.md)

## Phase 3: Detail Screen Expansion
- [~] Task: Update `AddEntryScreen` (or Detail Screen) to render fully parsed HTML content.
    - [x] Update UI to display full formatted content.
    - [x] Update to use `HtmlParser`.
    - [ ] Write UI tests for verifying full rendering.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Detail Screen Expansion' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions [e16a721]
