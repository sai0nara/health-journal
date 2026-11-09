# Swipe-to-Archive Safety — Product Specification

> Pure gesture policy with 50 % width commit threshold, dominant-axis lock, scroll guard, and pre-commit armed feedback, paired with card long-press menu and accessibility actions.

Last updated: 2026-11-08

## Overview

Refines the soft-archive gesture in History (`Docs/prd/swipe-to-archive-safety.md`) to prevent accidental archiving while scrolling. Replaces the default `SwipeToDismissBox` behavior with a pointer-driven interaction governed by a pure, unit-tested decision model (`SwipeArchivePolicy` and `SwipeAxisLockTracker`), while adding card long-press menu and TalkBack accessibility action parity.

## Architecture

- **Domain Policy Layer (`domain/SwipeArchivePolicy.kt`):** Pure Kotlin classes free of Compose UI dependencies.
  - `SwipeArchivePolicy`: Computes clamped drag offsets, 50 % width threshold armed state, threshold crossing tick detection, and commit-on-release decisions (suppressing fling velocity commits below 50 % width).
  - `SwipeAxisLockTracker`: Analyzes touch delta relative to touch slop. Locks to `HORIZONTAL` only if `|dx| >= 2 * |dy|`; otherwise locks to `VERTICAL` (delegating to list scroll) or `BLOCKED` (if list was scrolling when touch started).
- **Presentation Layer (`ui/screens/HistoryScreen.kt` & `ui/components/JournalEntryItem.kt`):**
  - `SwipeToArchiveWrapper`: Consumes horizontal drag deltas when `HORIZONTAL`, animates the card offset, animates the background alpha and icon scale, fires light haptic ticks on armed/disarmed transition, and springs back on release < 50 % or animates out and calls `onArchive()` on release >= 50 %.
  - `JournalEntryItem`: Offers long-press context menu (`DropdownMenu`) with `"Archive"` and a TalkBack `customActions` entry for non-archived cards.
  - `HistoryScreen`: Hoists `LazyListState` to supply `isListScrollInProgress`, and shows the Undo snackbar with `SnackbarDuration.Long` (or `Indefinite` with accessibility enabled).

## Data flow

1. Touch Down: `SwipeToArchiveWrapper` records touch and checks `isListScrollInProgress`. If scrolling, tracker locks to `BLOCKED`.
2. Touch Move: Delta updates tracker. If `|dx| >= 2 * |dy|` after touch slop, gesture locks to `HORIZONTAL`, consumes pointer event, and adjusts offset via `policy.clampOffset`.
3. Threshold Crossing: When offset crosses 50 % card width, armed animation updates and `HapticFeedbackType.TextHandleMove` fires.
4. Release: `policy.onRelease` evaluates target offset. If >= 50 %, card animates out, confirm haptic fires, `viewModel.archiveEntry(id)` is called, and the Undo snackbar is presented. If < 50 %, card springs back to 0.
5. Menu / Accessibility: Long-press menu or TalkBack custom action invokes the same `archiveEntryWithUndo(id)` callback.

## Components

| Component | File | Responsibility |
|---|---|---|
| Swipe policy & axis lock | `app/src/main/java/com/example/healthjournal/domain/SwipeArchivePolicy.kt` | Math and state transitions for drag clamping, threshold detection, and axis locking |
| History screen & swipe wrapper | `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` | Pointer input, animated layout, armed background, haptic feedback, and snackbar orchestration |
| Journal entry card | `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` | Long-press menu and custom accessibility action |

## Edge cases & failure handling

| Edge case | Expected behaviour |
|---|---|
| User flings feed vertically | List scrolls smoothly; touches landing during fling do not start a horizontal swipe |
| Diagonal swipe (thumb arc) | If vertical delta exceeds half horizontal delta (`|dx| < 2 * |dy|`), swipe is locked out and list scrolls |
| Short flick < 50 % width | Card springs back to 0; no archive commits |
| Drag past 50 % then dragged back | Disarms, fires second haptic tick, springs back to 0 on release |
| Right-to-left layout | Clamps end-to-start positive drag; mirrors background content and armed state |
| Card already archived (Archive screen) | No swipe wrapper; no Archive long-press menu or accessibility action |

## Dependencies

- Jetpack Compose Foundation gestures & pointer input (`pointerInput`, `awaitEachGesture`, `VelocityTracker`).
- Android `AccessibilityManager` for touch exploration detection.
- Compose Material 3 theme and semantic colors (`errorContainer`, `onErrorContainer`).

## Cross-references

- `Docs/prd/swipe-to-archive-safety.md` — requirements.
- `Docs/tests/swipe-to-archive-safety.md` — verification scenarios.
- [[ui-layer]] — UI structure and interactions.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/SwipeArchivePolicy.kt` — pure gesture decision policy and axis tracker.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — swipe wrapper and undo snackbar.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — long-press menu and custom accessibility action.
- `Docs/prd/swipe-to-archive-safety.md` — requirements.
- `Docs/tests/swipe-to-archive-safety.md` — test cases.
