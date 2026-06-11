# Implementation Plan: Rich Text and Truncation

## Phase 1: Markdown Parsing Logic
- [ ] Task: Create `MarkdownParser` utility to convert raw text to `AnnotatedString`.
    - [ ] Write unit tests for Markdown to `AnnotatedString` conversion (bold, italics, headers).
    - [ ] Implement parsing logic.
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Markdown Parsing Logic' (Protocol in workflow.md)

## Phase 2: Feed View Truncation
- [ ] Task: Update `HistoryScreen` and `ArchiveScreen` card components to use truncated text.
    - [ ] Implement 3-line truncation with ellipsis using `maxLines` and `TextOverflow`.
    - [ ] Write UI tests to verify truncation behavior.
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Feed View Truncation' (Protocol in workflow.md)

## Phase 3: Detail Screen Expansion
- [ ] Task: Update `AddEntryScreen` (or Detail Screen) to render fully parsed content.
    - [ ] Update UI to display full formatted content.
    - [ ] Write UI tests for verifying full rendering.
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Detail Screen Expansion' (Protocol in workflow.md)
