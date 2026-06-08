# Implementation Plan: Archive Management Refinement (Search & Unarchive)

## Phase 1: Data & Logic
- [x] Task: Update DAO for Archived Search
    - Add query for searching archived entries by title/content.
- [x] Task: Update Repository
    - Add search method for archived entries.
- [x] Task: Refactor ViewModel
    - Implement debounced search logic for archived entries.

## Phase 2: UI Implementation
- [x] Task: Extract SharedSearchBar
    - Create reusable search component.
- [x] Task: Update ArchiveScreen
    - Integrate `SharedSearchBar`.
    - Add "Unarchive" action in detail view (requires updating Detail navigation/screen).

## Phase 3: Verification
- [x] Task: Unit & Instrumented Tests
    - Test archiving, unarchiving, and searching.
- [x] Task: Conductor - User Manual Verification 'Archive Refinement' (Protocol in workflow.md)

