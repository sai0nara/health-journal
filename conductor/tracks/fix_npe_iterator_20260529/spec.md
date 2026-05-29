# Specification: Fix NullPointerException in List Iteration

## Goal
Resolve the `NullPointerException` occurring after the application update/data wipe.

## Problem Statement
The app crashes immediately with `NullPointerException: Attempt to invoke interface method 'java.util.Iterator java.lang.Iterable.iterator()' on null object reference`.
This likely happens when Room attempts to load `JournalEntry` records where the new `photo_urls` or `attachments` columns are null or empty, and the `TypeConverter` or entity defaults are bypassed.

## Objectives
- Harden `JournalTypeConverters` to never return `null` lists.
- Update `JournalEntry` to ensure default values are used if columns are missing or null.
- Verify that `HistoryScreen` and `AddEntryScreen` safely handle empty or null media lists.

## Success Criteria
- The app launches without crashing after a data wipe.
- Existing and new entries display correctly in the history list.
