# Implementation Plan: Enhanced Authentication, Sorting, and Search

## Phase 1: Sorting & Search Implementation
- [x] Task: Update `JournalDao` for Sorting and Search [1b8f270]
    - [x] Write unit tests for Room queries with sorting and LIKE filtering.
    - [x] Add Room queries for ASC/DESC sorting and keyword search.
- [~] Task: Enhance `JournalViewModel` for List Management
    - [ ] Write tests for ViewModel filtering and sorting logic.
    - [ ] Implement state flows for search query and sort order.
- [ ] Task: Update History UI with Search & Sort
    - [ ] Write UI tests for search bar and sort menu on HistoryScreen.
    - [ ] Implement SearchBar and Sort Menu in HistoryScreen.
- [ ] Task: Conductor - User Manual Verification 'Sorting & Search' (Protocol in workflow.md)

## Phase 2: Credential-based Authentication
- [ ] Task: Research and Implement Legacy Auth Flow
    - [ ] Analyze secure ways to handle username/password for Drive (e.g., manual OAuth2 code entry or App Passwords).
    - [ ] Implement UI for credential entry.
- [ ] Task: Integrate with `GoogleAuthManager`
    - [ ] Write tests for credential-based session persistence.
    - [ ] Update `GoogleAuthManager` to handle manually provided credentials.
- [ ] Task: Conductor - User Manual Verification 'Credential-based Auth' (Protocol in workflow.md)
