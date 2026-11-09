# Swipe-to-Archive Safety — Test Cases

> Verification suite for gesture thresholds, axis locking, scroll guards, pre-commit armed feedback, long-press context menu, and accessibility actions.

Last updated: 2026-11-08

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/SwipeArchivePolicyTest.kt` | 50 % threshold math, fling suppression under 50 %, axis locking ratio, scroll-in-progress guard, RTL normalization, armed transition tick detection |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` | Drag < 50 % spring-back, diagonal drag scroll lock, full swipe archive + undo |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` | Non-archived long-press menu, custom accessibility action, absence of archive actions on archived cards |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryFeedDeletionP0Test.kt` | P0 swipe left archives, swipe right springs back, no deletion or tombstones |
| JVM unit | `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` | English and Russian resource parity for archive label and undo strings |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Short drag < 50 % width | Non-archived entry in History | Drag card left 30 % width, release | Card springs back to 0; entry remains in feed; no snackbar |
| T-2 | Fast flick < 50 % width | Non-archived entry in History | Flick card left with high velocity < 50 % distance | Fling alone does not commit; card springs back |
| T-3 | Full drag >= 50 % width | Non-archived entry in History | Drag card left past 50 %, release | Card animates off-screen; confirm haptic fires; entry archived; Undo snackbar shown |
| T-4 | Diagonal scroll attempt | Multiple entries in History | Drag diagonally with `|dx| < 2 * |dy|` | Gesture locks to vertical list scroll; card does not swipe or archive |
| T-5 | Touch during fling | Feed is actively scrolling/flinging | Touch card during scroll | Swipe is blocked; touch stops scroll without swiping card |
| T-6 | Armed transition feedback | Non-archived entry in History | Drag across 50 % threshold and back | Armed visual appears and tick fires on cross; disarms and tick fires on uncross |
| T-7 | Card long-press context menu | Non-archived entry in History | Long press card | DropdownMenu appears with "Archive"; clicking archives entry and shows Undo |
| T-8 | TalkBack custom action | Non-archived entry in History | Activate "Archive" custom accessibility action | Entry is archived; Undo snackbar shown |
| T-9 | Archived card safety | Archived entry in Archive screen | Long press card / inspect semantics | No Archive menu item; no Archive accessibility action |
| T-10 | Start-to-end swipe | Non-archived entry in History | Swipe right on card | Drag clamped to 0; no archive commits |

## Manual checks

- Verify physical device feel with one-handed thumb swipes on various screen sizes.
- Verify TalkBack announcement of the "Archive" custom action on cards.
- Verify system haptic feedback toggle muting vibration when disabled.

## Cross-references

- `Docs/prd/swipe-to-archive-safety.md` — requirements under test.
- `Docs/psd/swipe-to-archive-safety.md` — technical specification.
- [[ui-layer]] — History UI architecture.

## Sources

- `app/src/test/java/com/example/healthjournal/domain/SwipeArchivePolicyTest.kt` — unit tests for gesture decision model.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenTest.kt` — compose UI tests for swipe thresholds.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/JournalEntryItemTest.kt` — compose UI tests for card context menu and semantics.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryFeedDeletionP0Test.kt` — P0 regression tests for feed deletion/archive safety.
- `app/src/test/java/com/example/healthjournal/localization/LocalizationParityTest.kt` — localization parity tests.
- `Docs/prd/swipe-to-archive-safety.md` — requirements under test.
- `Docs/psd/swipe-to-archive-safety.md` — technical specification under test.
