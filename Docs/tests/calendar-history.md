# Calendar History (Single Day & Date Period Filtering) — Test Cases

> Maps the PRD's acceptance criteria (`Docs/prd/calendar-history.md`) and PSD's architecture (`Docs/psd/calendar-history.md`) to automated unit and instrumented UI test suites.

Last updated: 2026-11-10

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/CalendarHistoryP0Test.kt` | Validates epoch boundaries, ±1 ms inclusivity, DST transitions, inverted range normalization, and quick presets. |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/CalendarHistoryP1P2Test.kt` | Validates cross-year periods, presets, pairwise configurations, sort toggles, and SQL/wildcard safety (P1/P2). |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` | Verifies ViewModel state pipeline, compound search/tag filtering with date ranges, and clear actions. |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | Verifies repository delegation and profile scoping for active entry timestamps. |
| JVM audit | `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` | Verifies zero hardcoded strings across calendar components and resources. |
| JVM audit | `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` | Enforces semantic token usage from `Color.kt`. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/components/HistoryCalendarPanelTest.kt` | Validates calendar month grid, event dots, preset chip clicks, and active filter chip. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenCalendarTest.kt` | End-to-end screen testing for calendar toggle, contextual empty state, and filter chip clear action. |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Single-day bounds | Entries at start and end of day | Select `SingleDay(D)` | Exact `00:00:00.000` to `23:59:59.999` entries included; previous/next day excluded. |
| T-2 | Inverted range normalization | Period mode active | Tap `endDate < startDate` | Range normalizes to `startDate <= endDate` preserving query safety. |
| T-3 | Event-dot indicators | Entries exist on specific dates | View month grid | Event dots appear only on active non-archived dates for active profile. |
| T-4 | Compound filter composition | Search query and tags active | Apply date filter | All conditions compose via logical `AND` respecting sort order. |
| T-5 | Multi-profile isolation | Sibling profile active | Switch profile | Dots and feed update with zero cross-profile leakage. |
| T-6 | Contextual empty state | Zero entries on selected date | Select empty date | Contextual empty state displays with `Clear date filter` button. |

## Sources

- `Docs/prd/calendar-history.md` — requirements under test.
- `Docs/psd/calendar-history.md` — design the cases verify.
- `app/src/test/java/com/example/healthjournal/domain/CalendarHistoryP0Test.kt` — P0 domain tests.
- `app/src/test/java/com/example/healthjournal/domain/CalendarHistoryP1P2Test.kt` — P1/P2 domain tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/JournalViewModelTest.kt` — ViewModel tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/HistoryCalendarPanelTest.kt` — UI tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/HistoryScreenCalendarTest.kt` — Screen UI tests.
