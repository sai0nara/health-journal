# Implementation Plan: Fix NullPointerException in List Iteration

## Phase 1: Diagnostics & Data Layer Fix
- [x] Task: Harden TypeConverters
    - Update `JournalTypeConverters.kt` to return `emptyList()` instead of `null` if the JSON string is null or parsing fails.
- [x] Task: Entity Defaults Verification
    - Double check `JournalEntry.kt` to ensure non-nullable types match Room's expectations for new columns.

## Phase 2: UI Hardening
- [x] Task: Safe Iteration in UI
    - Audit `HistoryScreen.kt` and `AddEntryScreen.kt` for any iteration over `photo_urls` or `attachments` and ensure null-safety (using `?.forEach` or similar).

## Phase 3: Verification
- [x] Task: Reproduction Test
    - Create a unit test in `JournalDaoTest.kt` that simulates inserting a record with null list columns and reading it back.
- [x] Task: Conductor - User Manual Verification 'Crash Fix' (Protocol in workflow.md)
