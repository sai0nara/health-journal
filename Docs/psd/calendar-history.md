# Calendar History (Single Day & Date Period Filtering) — Product Specification

> Technical architecture and component design for calendar-based history browsing and date filtering.

Last updated: 2026-11-10

## Architecture & Data Flow

Calendar History integrates directly into the existing `HistoryScreen` composable without duplicating feed management or database storage.

```
HistoryScreen (Compose UI)
  ├── TopAppBar (History title, ProfileSwitcher, Calendar toggle button, overflow)
  ├── SharedSearchBar (query)
  ├── HistoryCalendarPanel (collapsible month grid, presets, active filter chip)
  ├── TagSelectionRow (selectedTags)
  └── LazyColumn (reactive feed of JournalEntry cards)
         ▲
         │ StateFlow<List<JournalEntry>> (combines search, sort, tags, dateFilter, profileId)
         │
JournalViewModel
  ├── dateFilter: StateFlow<HistoryDateFilter> (All, SingleDay, Period)
  ├── visibleMonth: StateFlow<YearMonth>
  └── daysWithEntriesInVisibleMonth: StateFlow<Set<LocalDate>>
         ▲
         │ Room Queries
         │
JournalRepository / JournalDao
  ├── getActiveEntryTimestampsInDateRange(profileId, startMillis, endMillis)
  └── getAllEntriesInDateRange(profileId, startMillis, endMillis)
```

## Domain Models

- `HistoryDateFilter`: Sealed class with `All`, `SingleDay(date: LocalDate)`, and `Period(startDate: LocalDate, endDate: LocalDate)`.
- `EpochDateRange`: Value class representing `[startMillis, endMillis]` inclusive.
- `DatePreset`: Enum representing `TODAY`, `YESTERDAY`, `LAST_7_DAYS`, and `LAST_30_DAYS`.

## Timezone & DST Safety

- Local midnight boundaries are computed using `LocalDate.atStartOfDay(zone).toInstant().toEpochMilli()` and `plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L`.
- Safely handles negative/positive UTC offsets and 23-hour (spring forward) / 25-hour (fall back) DST transitions.

## Cross-References

- `Docs/prd/calendar-history.md` — requirements specification.
- `Docs/tests/calendar-history.md` — verification test suite.
- `app/src/main/java/com/example/healthjournal/domain/HistoryDateFilter.kt` — domain models.
- `app/src/main/java/com/example/healthjournal/ui/components/HistoryCalendarPanel.kt` — UI component.
