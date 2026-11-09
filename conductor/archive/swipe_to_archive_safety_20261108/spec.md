# Track swipe_to_archive_safety_20261108 — Specification

## Overview

Implement `Docs/prd/swipe-to-archive-safety.md`: refine the swipe-to-archive
gesture in the History feed (`SwipeToArchiveWrapper` in
`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`) so
that vertical scrolling, diagonal thumb movement, and short/fast flicks never
archive an entry accidentally, while an intentional archive remains a single
deliberate gesture with clear visual/haptic feedback, a longer Undo window, and
non-gesture alternatives (card long-press menu and TalkBack custom action).

## Functional Requirements

- **FR-1 (50 % Width Threshold):** The archive commits only when the card is
  released after being dragged end-to-start by at least **50 % of the card
  width**. Releasing below that distance animates the card back with no state
  change.
- **FR-2 (No Fling-Only Commit):** Fling velocity alone never commits an
  archive. A fling past the FR-1 distance commits; a fling below it springs
  back.
- **FR-3 (Dominant-Axis Lock):** After the platform touch slop is exceeded, the
  gesture is treated as a horizontal swipe only if horizontal travel is at least
  **2×** vertical travel (`|dx| >= 2 * |dy|`, ≈ 27° from horizontal). Otherwise
  the feed scrolls and the row swipe stays inactive until the finger lifts.
- **FR-4 (Scroll-State Guard):** A row swipe cannot start while the feed list is
  scrolling or flinging (`lazyListState.isScrollInProgress`). A touch that stops
  a fling does not start a swipe.
- **FR-5 (Armed Visual + Haptic Tick):** While dragging, the background uses the
  semantic error container role (`errorContainer` / `onErrorContainer`) and
  reveals the archive icon and an `"Archive"` text label at the end edge. When
  the drag crosses the FR-1 threshold the background enters an "armed" style
  (distinct color intensity and icon scale) and one light haptic tick fires;
  dragging back below the threshold returns to the unarmed style with one more
  tick. No archive occurs until release in the armed state.
- **FR-6 (Commit Action):** On commit, the confirm haptic fires once, the entry
  is archived via `viewModel.archiveEntry(entry.entry_id)`, and the card leaves
  the feed.
- **FR-7 (Undo Duration):** The `"Entry archived"` snackbar with `"Undo"` is
  shown for `SnackbarDuration.Long` (≈ 10 s). When an accessibility service
  (TalkBack) is enabled, it uses `SnackbarDuration.Indefinite`.
- **FR-8 (Consecutive Archives):** Archiving a second entry while a snackbar is
  visible replaces the snackbar; Undo on the new snackbar restores only the most
  recent entry.
- **FR-9 (Card Long-Press Menu & Accessibility Action):** Each non-archived feed
  card in `JournalEntryItem` exposes both:
  1. a long-press context menu containing an `"Archive"` item, and
  2. a custom accessibility action `"Archive"`,
  each performing the same archive + Undo snackbar flow as the swipe gesture.
- **FR-10 (RTL & Localization):** The gesture and its visuals mirror in
  right-to-left layouts (archive direction is end-to-start in both). All
  user-visible strings are localized in English and Russian.

## Non-Functional Requirements

- Smooth 60/120 Hz spring-back and armed-state transitions with no degradation
  to vertical `LazyColumn` scroll performance.
- Haptics respect system touch feedback settings via Compose `LocalHapticFeedback`.
- Colors use semantic Material 3 roles (`errorContainer`, `onErrorContainer`).
- Quality gates: TDD red/green/refactor per task, UI tests on emulator, >80 %
  coverage on new code, `./gradlew build` green, and `python3 wiki/lint.py`
  exit 0.

## Acceptance Criteria

- **AC-1:** Given an entry in the feed, when the user drags it end-to-start less
  than 50 % of the card width and releases, then the card springs back and the
  entry stays non-archived.
- **AC-2:** Given an entry, when the user drags it past 50 % and releases, then
  the entry is archived and the Undo snackbar appears.
- **AC-3:** Given an entry, when the user performs a fast end-to-start flick
  shorter than 50 % of the card width, then the entry stays non-archived.
- **AC-4:** Given the feed, when the user scrolls vertically with a gesture
  whose horizontal component is less than 2× its vertical component — including
  fast flings and diagonal drags starting on a card — then no entry is archived.
- **AC-5:** Given the feed is flinging, when the user touches and drags a card
  horizontally to stop the fling, then no swipe starts on that touch.
- **AC-6:** Given a drag in progress, when it crosses the 50 % threshold, then
  the armed style is shown and one haptic tick fires; when it moves back below,
  the unarmed style returns and one more tick fires.
- **AC-7:** Given an entry was archived, then the Undo snackbar remains visible
  for `SnackbarDuration.Long` (`Indefinite` with TalkBack on) and Undo restores
  the entry to the feed.
- **AC-8:** Given a non-archived card in the feed, when the user long-presses
  the card and selects `"Archive"` from the menu (or invokes the `"Archive"`
  custom accessibility action under TalkBack), then the entry is archived and
  the Undo snackbar appears.
- **AC-9:** A start-to-end (right) swipe never archives and fires no haptic.

## Out of Scope

- Swipe-to-delete (permanent deletion stays in the Archive screen only).
- Batch archive from History.
- A user setting to disable the swipe gesture.

## Cross-references

- `Docs/prd/swipe-to-archive-safety.md` — product requirements.
- `Docs/prd/history-feed.md` — parent History feed requirements.
- `Docs/psd/history-feed.md` — History feed specification.
- `Docs/tests/history-feed.md` — History feed test cases.
- [[ui-layer]] — History screen and card components.
