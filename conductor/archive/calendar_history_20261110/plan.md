# Implementation Plan: Calendar History (Single Day & Date Period Filtering)

## Phase 1: Domain Models, Date Range Bounds & Repository Range Query (TDD)
- [x] Task: Implement `HistoryDateFilter` domain model & date range epoch calculation
    - [x] Write failing JVM unit tests in `CalendarHistoryP0Test.kt` (`TC-001`, `TC-002`, `TC-003`, `TC-005`, `TC-007`, `TC-021`, `TC-022`, `TC-023`, `TC-030`)
    - [x] Implement sealed class `HistoryDateFilter` (`All`, `SingleDay`, `Period`) with `toEpochRange(ZoneId)` and inverted range normalization
    - [x] Implement quick preset resolver (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`)
    - [x] Confirm tests pass and refactor
- [x] Task: Ensure DAO & Repository support reactive date range query & days with entries
    - [x] Write unit tests in `JournalRepositoryTest.kt` verifying `getEntriesInDateRange` and `getDatesWithEntriesInMonth` flow scoping by profile ID (`TC-019`, `TC-032`)
    - [x] Add reactive Flow query to `JournalDao.kt` / `JournalRepository.kt` for `getEntriesInDateRange(profileId, startMillis, endMillis)` and active entry dates for month grid dots
    - [x] Confirm tests pass and refactor
- [x] Task: Conductor - User Manual Verification 'Phase 1: Domain Models, Date Range Bounds & Repository Range Query' (Protocol in workflow.md)

## Phase 2: ViewModel Pipeline & Filter Composition (TDD)
- [x] Task: Integrate `HistoryDateFilter` into `JournalViewModel`
    - [x] Write failing unit tests in `JournalViewModelTest.kt` for date filtering, preset selection, compound `AND` with search and tags (`TC-015`, `TC-016`, `TC-017`), sort direction (`TC-018`), and clear action (`TC-026`)
    - [x] Add `dateFilter: StateFlow<HistoryDateFilter>` and `visibleMonth: StateFlow<YearMonth>` to `JournalViewModel`
    - [x] Update `filteredEntries` pipeline to combine `dateFilter`, `searchQuery`, `selectedTags`, `isAscending`, and active profile
    - [x] Add `daysWithEntriesInVisibleMonth: StateFlow<Set<LocalDate>>` for calendar dots (`TC-013`, `TC-014`, `TC-025`)
    - [x] Confirm tests pass and refactor
- [x] Task: Conductor - User Manual Verification 'Phase 2: ViewModel Pipeline & Filter Composition' (Protocol in workflow.md)

## Phase 3: Compose UI Components (Calendar View & Filter Chips)
- [x] Task: Implement `HistoryCalendarView` month grid Composable
    - [x] Write Compose UI tests for month navigation, single-day selection, period selection, and event dots (`TC-012`, `TC-013`)
    - [x] Implement `HistoryCalendarView.kt` with header (prev/next month, month name), weekday row, day cells with event dots, and period range selection highlights
    - [x] Add TalkBack accessibility descriptions and Material 3 theme styling
    - [x] Confirm tests pass
- [x] Task: Implement `DateFilterPresetRow` and `ActiveDateFilterChip`
    - [x] Implement preset chips (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`) and active filter chip with Clear (`×`) action
    - [x] Write Compose UI tests for chip clicks and reset action (`TC-026`, `TC-031`)
    - [x] Confirm tests pass
- [x] Task: Conductor - User Manual Verification 'Phase 3: Compose UI Components' (Protocol in workflow.md)

## Phase 4: HistoryScreen Integration, Contextual FAB & Empty State
- [x] Task: Assemble Calendar History on `HistoryScreen`
    - [x] Add Calendar toggle button in top bar or filter row (`TC-024`)
    - [x] Add collapsible calendar panel and active filter chip to `HistoryScreen.kt`
    - [x] Implement contextual empty state with `Clear date filter` CTA (`TC-027`)
    - [x] Update Add Entry FAB navigation when `SingleDay(pastDate)` is active to prefill date (`TC-028`, `TC-029`)
- [x] Task: Comprehensive Compose UI and End-to-End Tests
    - [x] Write end-to-end Compose UI tests in `HistoryScreenCalendarTest.kt` (`TC-001`, `TC-004`, `TC-019`, `TC-025`, `TC-027`, `TC-028`)
    - [x] Run localization and hardcoded strings/colors audit (`TC-040`)
    - [x] Confirm all tests pass on connected device / emulator
- [x] Task: Conductor - User Manual Verification 'Phase 4: HistoryScreen Integration, Contextual FAB & Empty State' (Protocol in workflow.md)

## Phase 5: Product Documentation & Final Verification
- [x] Task: Author product documentation
    - [x] Author `Docs/psd/calendar-history.md`
    - [x] Author `Docs/tests/calendar-history.md`
    - [x] Update `Docs/index.md` removing `— planned` marker
- [x] Task: Execute full verification suite
    - [x] Run full JVM test suite (`./gradlew testDebugUnitTest`)
    - [x] Run wiki lint (`python3 wiki/lint.py`)
- [x] Task: Conductor - User Manual Verification 'Phase 5: Product Documentation & Final Verification' (Protocol in workflow.md)

## Phase: Review Fixes
- [x] Task: Apply review suggestions 4186d8c
