# Implementation Plan: Enhanced Archive & Multi-Delete UX

## Phase 1: Archiving Polish
- [x] Task: Implement Undo Snackbar for Archiving
    - Update `HistoryScreen.kt` to use `Scaffold` with `SnackbarHost`.
    - Trigger Snackbar after archiving an entry.
    - Implement the "Undo" action (calling `viewModel.restoreEntry`).
- [x] Task: Add Haptics to Archiving
    - Use `LocalHapticFeedback` to provide feedback on swipe completion.

## Phase 2: Archive Screen UX
## Phase 2: Archive Screen UX
- [x] Task: Refine Selection Mode (CAB)
    - Morph the `TopAppBar` to show "X Selected" with contextual icons (Delete, Restore).
- [x] Task: Implement Batch Deletion Confirmation
    - Add an `AlertDialog` for confirming permanent deletion of selected items.
- [x] Task: Implement "Empty Archive" Bottom Sheet
    - Use `ModalBottomSheet` for the "Delete All" confirmation.
    - Add a "Clear All" action in the Archive overflow menu.

## Phase 3: Swipe-to-Delete in Archive
- [x] Task: Add Swipe-to-Delete with Undo
    - Implement swipe actions in `ArchiveScreen.kt`.
    - Provide a Snackbar for permanent deletion with a brief Undo window (if possible before DB purge, or just local-only undo).


## Phase 4: Verification
- [x] Task: Manual UX Review 1f590f8
    - Verify haptic feedback feels appropriate.
    - Verify Snackbar persistence and Undo logic.
- [x] Task: Conductor - User Manual Verification 'Enhanced UX' (Protocol in workflow.md) 1f590f8
