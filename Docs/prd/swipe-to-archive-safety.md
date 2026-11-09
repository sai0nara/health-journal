# Swipe-to-Archive Safety — Product Requirements

> Make archiving from the History feed a deliberate gesture, so that scrolling
> the feed never archives an entry by accident.

Last updated: 2026-11-09

## Overview

The History feed archives an entry when the user swipes its card from end to
start (`SwipeToArchiveWrapper` in
`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`). The
wrapper uses the Material 3 `SwipeToDismissBox` with library defaults and
commits the archive inside `confirmValueChange`. With the defaults:

- a short horizontal drag (a small fixed distance, not a fraction of the card
  width) commits the archive;
- a fast flick commits regardless of distance, because the fling-velocity
  threshold also triggers dismissal;
- nothing locks the gesture direction, so a diagonal thumb movement made while
  scrolling the feed vertically can start a horizontal swipe;
- feedback (haptic) arrives only at commit time, so the user has no warning
  before the action happens.

Result reported by the user: the gesture is "too quick", and entries get
archived accidentally while scrolling History. Undo exists, but the snackbar is
short-lived and easy to miss.

Archive is a soft action (the `isArchived` flag; see
`Docs/prd/history-feed.md`), so the goal is to remove accidental triggers, not
to add a confirmation dialog to every archive.

## Common approaches for destructive/soft-destructive swipe gestures

Patterns used by mainstream Android apps (Gmail, Google Tasks, Messages,
Outlook) and the Material guidance, assessed for this feed:

| # | Approach | What it does | Prevents accidental archive? | Cost to the user | Recommendation |
|---|---|---|---|---|---|
| A1 | **Distance threshold as fraction of width** | Commit only after the card is dragged past a fixed share of its width (typically 40–50 %). Releasing earlier springs back. | High | None for intended swipes | **Adopt** |
| A2 | **No commit by fling alone** | A fast flick no longer commits unless the distance threshold is also met. | High — flicks are the main accidental trigger while scrolling | Intended swipes need a longer drag | **Adopt** |
| A3 | **Direction lock (axis lock)** | After touch slop, the gesture locks to the dominant axis. If vertical movement dominates, the row swipe is ignored until finger-up. | High — targets the reported scroll case directly | None | **Adopt** |
| A4 | **Disable swipe while the list scrolls** | Row swipe does not start while the list is flinging/scrolling. | High for scroll-then-touch cases | None | **Adopt** |
| A5 | **"Armed" feedback before commit** | Background reveals icon + label; when the threshold is crossed the background/icon change and a light haptic tick fires; moving back below the threshold disarms (second tick). | Medium — lets the user abort before release | None | **Adopt** |
| A6 | **Longer, accessible Undo** | Undo snackbar stays longer; indefinite under TalkBack. | Recovery, not prevention | None | **Adopt** |
| A7 | **Non-gesture alternative** | Archive available as an accessibility action and from a card menu, so the gesture is not the only path. | Indirect — users who prefer it can avoid swiping | None | **Adopt** (accessibility action + card long-press menu) |
| A8 | Reveal-then-tap (two-step) | Swipe reveals an Archive button; a tap on it commits (iOS-mail style). | Very high | Two steps per archive; less common on Android | Not by default; keep as fallback if A1–A5 are insufficient |
| A9 | Confirmation dialog | Dialog after every swipe. | Very high | High friction for a reversible action; contradicts Material guidance for undoable actions | Reject |
| A10 | Setting to disable swipe | User toggle in settings. | Total, opt-in | Settings complexity | Defer (see open questions) |

The recommended set is **A1 + A2 + A3 + A4 + A5 + A6 + A7**: it removes the
accidental triggers (flick, short drag, diagonal scroll) without slowing an
intended archive, and keeps Undo as the safety net.

## Goals / Non-goals

**Goals**

- A vertical scroll of the History feed, including fast flings and diagonal
  thumb movement, never archives an entry.
- An intended archive remains a single swipe.
- The user sees and feels when a release will archive, and can abort before
  releasing.
- An accidental archive is recoverable from a clearly visible Undo.
- Archive is reachable without the swipe gesture (accessibility).

**Non-goals**

- Changing what archiving does (flag, sync, Archive screen) — unchanged from
  `Docs/prd/history-feed.md`.
- Adding swipe actions elsewhere (Archive screen, measurements, workouts).
- A start-to-end (right) swipe action — it stays disabled.

## User stories

- As a user scrolling History, I want my scroll never to archive an entry, so
  that I do not lose entries from the feed without noticing.
- As a user archiving an entry, I want one deliberate swipe to still work, so
  that cleaning the feed stays fast.
- As a user mid-swipe, I want a clear signal that releasing will archive, so I
  can drag back and cancel.
- As a user who archived by mistake, I want enough time to tap Undo.
- As a TalkBack user or a user who prefers menus over swiping, I want an Archive
  action on the card (via long-press menu and accessibility action), so I do not
  need a swipe gesture.

## Functional requirements

- FR-1 (A1): The archive commits only when the card is released after being
  dragged end-to-start by at least **50 % of the card width**. Releasing below
  that distance animates the card back with no state change.
- FR-2 (A2): Fling velocity alone never commits an archive. A fling past the
  FR-1 distance commits; a fling below it springs back.
- FR-3 (A3): After the platform touch slop is exceeded, the gesture is treated
  as a horizontal swipe only if horizontal travel is at least **2×** vertical
  travel (≈ 27° from horizontal). Otherwise the feed scrolls and the row swipe
  stays inactive until the finger lifts.
- FR-4 (A4): A row swipe cannot start while the feed list is scrolling or
  flinging. A touch that stops a fling does not start a swipe.
- FR-5 (A5): While dragging, the background uses the semantic error container
  role (`errorContainer` / `onErrorContainer`) and reveals the archive icon and
  an "Archive" text label at the end edge. When the drag crosses the FR-1
  threshold the background enters an "armed" style (distinct color intensity
  and icon scale) and one light haptic tick fires; dragging back below the
  threshold returns to the unarmed style with one more tick. No archive occurs
  until release in the armed state.
- FR-6: On commit, the existing confirm haptic fires once, the entry is
  archived via the existing view-model action, and the card leaves the feed.
- FR-7 (A6): The "Entry archived" snackbar with Undo is shown for the
  long snackbar duration (≈ 10 s). When an accessibility service (TalkBack) is
  enabled, it stays until the user acts on or dismisses it.
- FR-8: Archiving a second entry while a snackbar is visible replaces the
  snackbar; Undo on the new snackbar restores only the most recent entry.
  (Current single-snackbar behavior is kept.)
- FR-9 (A7): Each non-archived feed card exposes both:
  1. a long-press context menu containing an "Archive" item, and
  2. a custom accessibility action "Archive",
  each performing the same archive + Undo snackbar flow as the swipe gesture.
- FR-10: The gesture and its visuals mirror correctly in right-to-left
  layouts (the archive direction is end-to-start in both).

## Non-functional requirements

- The swipe, armed-state change, and spring-back animate at the display
  refresh rate with no dropped frames on a mid-range device, and do not
  degrade vertical scroll performance of the feed.
- Haptics respect the system "touch feedback" setting.
- All new user-visible strings (label, accessibility action) are localized in
  every shipped locale (see `Docs/prd/russian-localization.md`).
- Colors come from the semantic theme roles (see
  `Docs/prd/medical-color-system.md`); no hard-coded colors.

## Acceptance criteria

- AC-1: Given an entry in the feed, when the user drags it end-to-start less
  than 50 % of the card width and releases, then the card springs back and the
  entry stays non-archived.
- AC-2: Given an entry, when the user drags it past 50 % and releases, then the
  entry is archived and the Undo snackbar appears.
- AC-3: Given an entry, when the user performs a fast end-to-start flick shorter
  than 50 % of the card width, then the entry stays non-archived.
- AC-4: Given the feed, when the user scrolls vertically with a gesture whose
  horizontal component is less than 2× its vertical component — including
  fast flings and diagonal drags starting on a card — then no entry is archived.
- AC-5: Given the feed is flinging, when the user touches and drags a card
  horizontally to stop the fling, then no swipe starts on that touch.
- AC-6: Given a drag in progress, when it crosses the threshold, then the armed
  style is shown and one haptic tick fires; when it moves back below, the
  unarmed style returns and one more tick fires.
- AC-7: Given an entry was archived, then the Undo snackbar remains visible for
  ≈ 10 s (indefinitely with TalkBack on) and Undo restores the entry to the
  feed.
- AC-8: Given a non-archived card in the feed, when the user long-presses the
  card and selects "Archive" from the menu (or invokes the "Archive" custom
  accessibility action under TalkBack), then the entry is archived and the Undo
  snackbar appears.
- AC-9: A start-to-end (right) swipe never archives and fires no haptic
  (unchanged from the history-feed P0 coverage).

## Out of scope

- Swipe-to-delete (permanent deletion stays in the Archive screen only).
- Batch archive from History.
- A user setting to turn the gesture off (A10) — deferred.

## Grooming decisions

| ID | Topic | Decision |
|---|---|---|
| G1 | Commit distance threshold | **Resolved:** 50 % of card width (`FR-1`, `AC-1`, `AC-2`). |
| G2 | Non-gesture Archive path | **Resolved:** Include both a card long-press menu item ("Archive") and a TalkBack custom accessibility action (`FR-9`, `AC-8`). |
| G3 | Swipe background color | **Resolved:** Keep destructive red (`errorContainer` / `onErrorContainer`) with unarmed/armed visual distinction (`FR-5`). |
| G4 | Consecutive archives & Undo | **Resolved:** Keep single-snackbar replacement restoring the most recent entry (`FR-8`). |
| G5 | Setting to disable swipe (A10) | **Resolved:** Deferred to out of scope. |
| V1 | "Too quick" (user report) | **Resolved:** Replaced by concrete thresholds in `FR-1` … `FR-4` (50 % width, no fling-only commit, 2× horizontal axis lock, disabled while scrolling). |

## Cross-references

- `Docs/prd/history-feed.md` — the feed and archive behavior this refines
  (FR-4 / AC-4).
- `Docs/psd/swipe-to-archive-safety.md` — technical specification.
- `Docs/tests/swipe-to-archive-safety.md` — test cases.
- `Docs/psd/history-feed.md` — parent specification.
- `Docs/tests/history-feed.md` — parent test cases.
- [[ui-layer]] — the History screen.

## Sources

- `app/src/main/java/com/example/healthjournal/domain/SwipeArchivePolicy.kt` — pure gesture decision policy and axis tracker.
- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — `SwipeToArchiveWrapper`, archive haptic and Undo snackbar.
- `app/src/main/java/com/example/healthjournal/ui/components/JournalEntryItem.kt` — feed card rendering (`onLongClick` hook, context menu, and semantics).
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — archive / restore actions.
- `Docs/psd/swipe-to-archive-safety.md` — technical specification.
- `Docs/tests/swipe-to-archive-safety.md` — test cases.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryFeedDeletionP0Test.kt` — existing History gesture guards (left archives, right springs back).
- `Docs/prd/history-feed.md` — parent requirements.
