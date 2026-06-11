# Specification: Archive Management Refinement (Search & Unarchive)

## Goal
Enable users to search through archived entries and restore them back to the main journal.

## Objectives
- **Unarchive Flow**: Add an "Restore" or "Unarchive" action in the `JournalDetailScreen` when viewing an archived entry.
- **Search Archive**: Implement a search bar in `ArchiveScreen` that filters archived entries.
- **UI Consistency**: Extract the Search Bar from `HistoryScreen` into a reusable component.

## User Flows
1. **Unarchiving**: Open archived entry -> Tap "Unarchive" -> Entry returns to main journal -> Snackbar "Entry restored".
2. **Searching**: Open Archive -> Tap Search -> Type query -> List updates.

## Technical Requirements
- Reusable Search Component: Create `SharedSearchBar`.
- ViewModel Update: `ArchiveViewModel` (or shared logic) needs search filtering (debounce) and unarchive method.
- Database: `JournalDao` search query for `isArchived = 1`.

## Success Criteria
- User can successfully find and restore archived entries.
- Search performance is responsive with debouncing.
- UI components are consistent between History and Archive screens.
