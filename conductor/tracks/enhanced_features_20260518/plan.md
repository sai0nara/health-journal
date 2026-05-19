# Implementation Plan: Enhanced Authentication, Sorting, and Search

## Phase 1: Sorting & Search Implementation
- [x] Task: Update `JournalDao` for Sorting and Search [1b8f270]
    - [x] Write unit tests for Room queries with sorting and LIKE filtering.
    - [x] Add Room queries for ASC/DESC sorting and keyword search.
- [x] Task: Enhance `JournalViewModel` for List Management [verified]
    - [x] Write tests for ViewModel filtering and sorting logic.
    - [x] Implement state flows for search query and sort order.
- [x] Task: Update History UI with Search & Sort [verified]
    - [x] Write UI tests for search bar and sort menu on HistoryScreen.
    - [x] Implement SearchBar and Sort Menu in HistoryScreen.
- [x] Task: Conductor - User Manual Verification 'Sorting & Search' (Protocol in workflow.md) [7b94f90]

## Phase 2: Credential-based Authentication
- [x] Task: Research and Implement Legacy Auth Flow [verified]
    - [x] Analyze secure ways to handle username/password for Drive (e.g., manual OAuth2 code entry or App Passwords).
    - [x] Implement UI for credential entry.
- [x] Task: Integrate with `GoogleAuthManager` [verified]
    - [x] Write tests for credential-based session persistence.
    - [x] Update `GoogleAuthManager` to handle manually provided credentials.
- [~] Task: Conductor - User Manual Verification 'Credential-based Auth' (Protocol in workflow.md)
