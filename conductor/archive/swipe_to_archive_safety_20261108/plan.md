# Track swipe_to_archive_safety_20261108 — Implementation Plan

> Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent accidental archiving while scrolling the History feed by
enforcing a 50 % card-width commit threshold, disabling fling-only commits,
locking gesture axis (`|dx| >= 2 * |dy|`), blocking swipe initiation during list
scroll, adding pre-commit armed visual/haptic feedback, extending the Undo
snackbar duration, and adding long-press menu + accessibility Archive actions.

**Architecture:** Extract pure gesture math and state transitions into
`domain/SwipeArchivePolicy.kt` (unit-tested on JVM). Replace default
`SwipeToDismissBox` behavior in `SwipeToArchiveWrapper` (`HistoryScreen.kt`)
with pointer/drag handling driven by `SwipeArchivePolicy` and the parent
`LazyListState`. Add a long-press context menu and custom accessibility action
to `JournalEntryItem.kt`.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Coroutines, JUnit 4, Compose
UI Test.

**Spec:** `conductor/tracks/swipe_to_archive_safety_20261108/spec.md`

## Global Constraints

- Export `JAVA_HOME` before every Gradle command:
  `export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home`.
- Follow TDD: red → green → refactor → UI test for every task.
- Run `./gradlew :app:testDebugUnitTest` and `./gradlew build` after each task.
- Always shut down the emulator after instrumented tests: `adb emu kill`.
- Run `python3 wiki/lint.py` at the end of every phase; exit 0 is required.

## Phase 1: Pure Gesture Decision Policy & Safe Swipe Composable

- [x] Task: Extract pure swipe gesture policy and write JVM unit tests (Red/Green/Refactor)
    - [x] Write failing `SwipeArchivePolicyTest` unit tests covering:
        - 50 % fractional width threshold (`isArmed` and `shouldCommitOnRelease`);
        - fling velocity alone never commits when dragged distance < 50 % of width;
        - dominant-axis lock (`|dx| >= 2 * |dy|` after touch slop; vertical lock persists until pointer up);
        - scroll-in-progress guard (`isListScrolling == true` blocks swipe start for the entire pointer gesture);
        - LTR vs RTL end-to-start delta normalization (start-to-end clamped to 0);
        - armed/disarmed transition detection so haptic ticks fire once per threshold crossing.
    - [x] Run `./gradlew :app:testDebugUnitTest --tests "com.example.healthjournal.domain.SwipeArchivePolicyTest"` and confirm it fails (Red).
    - [x] Implement `app/src/main/java/com/example/healthjournal/domain/SwipeArchivePolicy.kt` to pass all unit tests (Green).
    - [x] Refactor for clarity and immutability while keeping tests green.

- [x] Task: Refactor `SwipeToArchiveWrapper` in `HistoryScreen.kt` (Red/Green + UI test)
    - [x] Write failing Compose UI tests in `HistoryScreenTest` and `HistoryFeedDeletionP0Test` covering:
        - dragging end-to-start by < 50 % of card width and releasing springs back without archiving;
        - performing a fast end-to-start flick shorter than 50 % of card width does not archive;
        - scrolling vertically or diagonally (`|dx| < 2 * |dy|`) across cards scrolls the list without archiving;
        - dragging >= 50 % reveals the armed state (`"Archive"` label + icon), fires one tick haptic on crossing and one on uncrossing, and archives on release;
        - start-to-end swipe never archives and fires no haptic.
    - [x] Hoist `rememberLazyListState()` in `HistoryScreen` and pass `isListScrolling = lazyListState.isScrollInProgress` into `SwipeToArchiveWrapper`.
    - [x] Implement the custom horizontal drag & spring-back behavior in `SwipeToArchiveWrapper` using `SwipeArchivePolicy`, `errorContainer` / `onErrorContainer` armed/unarmed visuals, `"Archive"` text label, and threshold haptic tick.
    - [x] Run JVM unit tests and emulator Compose UI tests (`HistoryScreenTest`, `HistoryFeedDeletionP0Test`) to green.

- [x] Task: Conductor - User Manual Verification 'Phase 1: Pure Gesture Decision Policy & Safe Swipe Composable' (Protocol in workflow.md)

## Phase 2: Long-Press Menu, Accessibility Action, Longer Undo & Docs

- [x] Task: Add card long-press Archive menu, custom accessibility action, and longer Undo snackbar (Red/Green + UI test)
    - [x] Write failing Compose UI tests in `JournalEntryItemTest` and `HistoryScreenTest` covering:
        - long-pressing a non-archived card in History opens a context menu with `"Archive"`, and tapping it archives the entry and shows the Undo snackbar;
        - invoking the `"Archive"` custom accessibility action on a non-archived card archives the entry and shows the Undo snackbar;
        - archived cards do not expose the Archive long-press menu or custom accessibility action;
        - Undo snackbar uses `SnackbarDuration.Long` (or `SnackbarDuration.Indefinite` when accessibility is active) and restores the archived entry when tapped;
        - `LocalizationParityTest` and `HardcodedStringAuditTest` pass for all strings in `values/strings.xml` and `values-ru/strings.xml`.
    - [x] Update `JournalEntryItem.kt`, `HistoryScreen.kt`, `values/strings.xml`, and `values-ru/strings.xml`.
    - [x] Run JVM unit tests and emulator Compose UI tests to green.

- [x] Task: Create/update feature PSD, test docs, and wiki pages
    - [x] Create `Docs/psd/swipe-to-archive-safety.md` and `Docs/tests/swipe-to-archive-safety.md`.
    - [x] Update `Docs/prd/swipe-to-archive-safety.md` (remove planned banner), `Docs/psd/history-feed.md`, `Docs/tests/history-feed.md`, `Docs/index.md` (list all three paths for `swipe-to-archive-safety`), and `wiki/modules/ui-layer.md`.
    - [x] Run `python3 wiki/lint.py` and ensure it exits 0.

- [x] Task: Conductor - User Manual Verification 'Phase 2: Long-Press Menu, Accessibility Action, Longer Undo & Docs' (Protocol in workflow.md)
