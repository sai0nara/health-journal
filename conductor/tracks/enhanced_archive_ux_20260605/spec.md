# Specification: Enhanced Archive & Multi-Delete UX

## Goal
Elevate the Archive and Deletion experience with high-polish UX, including haptic feedback, "Undo" capabilities, and hardened confirmation flows.

## Objectives
- **Soft Exit (Archiving)**: Implement Swipe-to-Archive with Haptic feedback and a "Undo" Snackbar.
- **Archive Management**:
    - **Single Deletion**: Swipe-to-Delete in Archive screen with Undo capability.
    - **Batch Deletion**: Long-press to enter *Selection Mode*, with a Contextual Action Bar (CAB) showing selected count.
    - **Confirmation**: Required for batch deletion via a clear dialog.
    - **The Nuke (Clear All)**: High-friction confirmation (Bottom Sheet) for "Delete All".
- **Visuals & Feedback**:
    - Smooth animations for entry removal.
    - Platform-native haptics (Light for selection, Heavy for deletion).
    - TopAppBar state morphing for selection mode.

## User Flows
1. **Archive Entry**: Swipe left on History -> Haptic feedback -> Snackbar with "Undo" -> Item disappears/returns on undo.
2. **Batch Delete**: Long-press item in Archive -> CAB appears -> Select multiple -> Tap Delete -> Confirm Dialog -> Items gone.
3. **Empty Archive**: Tap "Delete All" in Archive -> Bottom Sheet appears -> Heavy confirmation -> All archived entries permanently deleted.

## Technical Details
- **UI**: Jetpack Compose `SwipeToDismissBox`, `ModalBottomSheet`, and `Scaffold`'s `SnackbarHost`.
- **Haptics**: Use `LocalHapticFeedback.current`.
- **State**: Track `SelectionState` in `ArchiveScreen` to trigger TopAppBar morphing.

## Success Criteria
- Archiving feels responsive and safe due to the Undo Snackbar.
- Batch deletion is efficient but protected against accidental triggers.
- Permanent deletion of all archived data has high enough friction to prevent mistakes.
