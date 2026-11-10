# Calendar History (Single Day & Date Period Filtering) — Product Requirements

> Browse and filter the journal history by a single calendar day or a custom date period using an interactive calendar with active-day indicators, composable with search, tag filters, and sort order.

Last updated: 2026-11-10

> **Status: PLANNED.** This PRD records the groomed requirements and architectural decisions for calendar-based history browsing and single-day / period filtering. The product specification (`Docs/psd/calendar-history.md`) and test cases (`Docs/tests/calendar-history.md`) will be added when the feature is implemented.

## Overview

The History screen (`app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt`) currently displays all non-archived journal entries for the active profile in a continuous chronological feed with text search (`SharedSearchBar`), tag filtering (`TagSelectionRow`), and ascending/descending sort (`Docs/prd/history-feed.md`). However, users cannot jump to a specific calendar date or isolate entries from a specific multi-day window (such as a week of symptom tracking, a medication cycle, or a recovery period) without manually scrolling through the entire feed.

Meanwhile, `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` already provides a range query (`getAllEntriesInDateRange`) used by PDF export, and `app/src/main/java/com/example/healthjournal/domain/UtcToLocalDate.kt` already resolves Material 3 UTC midnight timestamps into local calendar dates.

This feature enriches the History experience with interactive calendar navigation and date filtering supporting two modes — **Single Day** and **Period (Date Range)** — alongside visual day indicators showing which dates contain logged events.

## Grooming Decisions (G1–G8, V1–V3)

All grooming findings from the Definition-of-Ready review are recorded here with their working assumptions and cited across the requirements below:

- **G1 (Major): Surface & navigation entry point.** Calendar History is integrated directly into `HistoryScreen` as a collapsible calendar view and date-filter bar (toggled via a Calendar icon button in the `HistoryScreen` top app bar or filter row), rather than duplicating feed management on a disconnected screen. This preserves all existing card interactions in place: entry detail navigation, inline vitals editing (`VitalsEntrySheet`), swipe-to-archive with Undo (`SwipeArchivePolicy`), pull-to-refresh sync, search, and tag filtering.
- **G2 (Major): Definition of "events" (data sources).** An "event" is any non-archived `JournalEntry` row (`app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`) belonging to the active profile. Because `journal_entries` already unifies text journal notes, photo/file attachments, standalone vitals readings, and completed workout sessions, all of these appear automatically. Body measurements remain on `MeasurementsScreen` (`Docs/prd/body-measurements.md`).
- **G3 (Major): Single Day vs. Period selection mechanics and inclusive bounds.**
  - **Unfiltered (`All Dates`, default):** All non-archived entries for the active profile are shown.
  - **Single Day (`SingleDay`):** Selecting a date `D` filters entries to `[00:00:00.000, 23:59:59.999]` on `D` in the device's local timezone.
  - **Period (`DatePeriod`):** Selecting `startDate` and `endDate` (`startDate <= endDate`) filters entries to `[startOfDay(startDate), endOfDay(endDate)]` inclusive in the device's local timezone. Quick preset chips (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`, `Custom Range`) accelerate common selections.
- **G4 (Major): Filter composition (Date × Search × Tags × Sort).** Date filtering composes via logical `AND` with the existing substring search query (`searchQuery`) and selected category tags (`selectedTags`), while respecting the active sort direction (`isAscending` newest-first or oldest-first).
- **G5 (Minor): Calendar day-dot indicators.** In the calendar month grid, any calendar day that contains at least one non-archived `JournalEntry` for the active profile renders a semantic dot marker below the day number, independent of the currently active text/tag filter so users can see at a glance where activity exists across the month.
- **G6 (Minor): Multi-profile isolation.** All calendar day indicators and filtered entry lists strictly observe `ProfileRepository.activeProfileIdFlow` (`Docs/prd/profiles-for-siblings.md`). Switching profiles immediately updates the marked calendar dates and filtered feed for the newly selected profile.
- **G7 (Minor): Empty state & filter reset.** When a Single Day or Period filter is active, a dismissible filter chip displays the active date or range with a clear (`×`) button. If zero entries match the selected date/period, an empty state displays the selected date/period label and a `Show all dates` reset action.
- **G8 (Minor): Timezone & DST accuracy.** Day start (`00:00:00.000`) and day end (`23:59:59.999`, i.e., `nextDay.atStartOfDay(zone) - 1 ms`) are calculated using `java.time.LocalDate` and `ZoneId.systemDefault()`, ensuring 23-hour and 25-hour Daylight Saving Time transition days never shift entries into adjacent calendar dates.
- **V1: "calendar history"** replaced with: An interactive collapsible calendar panel and date-range filter bar on `HistoryScreen` backed by reactive `StateFlow` state in `JournalViewModel`.
- **V2: "events"** replaced with: Non-archived `JournalEntry` entities scoped to the active `profileId`.
- **V3: "from one day or period"** replaced with: Sealed `HistoryDateFilter` state supporting `All`, `SingleDay(date: LocalDate)`, and `Period(startDate: LocalDate, endDate: LocalDate)` evaluated as inclusive epoch-millisecond intervals `[startMillis, endMillis]` in `ZoneId.systemDefault()`.

## Goals / Non-goals

**Goals**

- Allow users to filter the History feed to a **single calendar day** or a **multi-day period (date range)**.
- Provide an interactive month calendar view with **event-dot indicators** on days that have logged entries for the active profile.
- Provide quick period presets (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`) and a custom date-range picker.
- Compose date filtering seamlessly with existing substring search, tag filtering, sort order, and swipe-to-archive.
- Respect multi-profile isolation (`Docs/prd/profiles-for-siblings.md`) and full English/Russian localization (`Docs/prd/russian-localization.md`).

**Non-goals**

- Merging `BodyMeasurementEntry` records into the `HistoryScreen` feed (measurements have their own history and charts on `MeasurementsScreen`).
- External calendar sync (Google Calendar / iCal export or import).
- Time-of-day (hour/minute) sub-filtering within a single day.
- Filtering the Archive screen by calendar grid (Archive retains its existing search and tag filters).

## User Stories

- As a user reviewing a symptom flare-up, I want to tap a specific date on a calendar to see every entry, vital reading, and workout logged on that exact day.
- As a user preparing for a doctor's visit, I want to select a two-week period (e.g., from the start of a new medication to today) so I can review all events from that interval in chronological order.
- As a user browsing past months, I want to see visual dots on calendar days that contain entries so I know which days have logs without tapping empty dates one by one.
- As a user filtering by both a `#headache` tag and the last 30 days, I want the date period and tag filters to work together so I can see only headache entries within that period.
- As a parent managing sibling profiles, I want the calendar dots and filtered entries to reflect only the currently selected child's profile.

## Functional Requirements

- **FR-1 (Calendar Toggle & Filter Bar):** `HistoryScreen` provides a Calendar filter toggle action in the top app bar (and/or filter row) that expands or collapses the calendar selection controls, plus an active filter summary chip whenever a date or period filter is applied (`(assumes G1)`).
- **FR-2 (Filter Modes):** `JournalViewModel` exposes a date filter state (`HistoryDateFilter`) with three modes (`(assumes G3, V3)`):
  1. `All` (default — no timestamp bounds applied).
  2. `SingleDay(date: LocalDate)` — filters entries whose `timestamp` falls within `[startOfDay(date), endOfDay(date)]` in `ZoneId.systemDefault()`.
  3. `Period(startDate: LocalDate, endDate: LocalDate)` — requires `startDate <= endDate` and filters entries whose `timestamp` falls within `[startOfDay(startDate), endOfDay(endDate)]` in `ZoneId.systemDefault()`.
- **FR-3 (Interactive Month Calendar & Day Indicators):** When the calendar view is open, it displays a month grid with previous/next month navigation and highlights:
  - Today's date.
  - The currently selected single date or start-to-end date range.
  - A visual dot indicator on every date in the visible month that has ≥ 1 non-archived `JournalEntry` for the active profile (`(assumes G2, G5)`).
- **FR-4 (Single-Day & Period Selection Gestures):**
  - Users can switch between **Day** selection and **Period (Range)** selection (or tap once for a single day and tap a second date / use the range mode to define `[startDate, endDate]`).
  - If the user selects a second date earlier than the first date in range mode, the selection either resets the start date to the newly tapped date or normalizes the pair so `startDate <= endDate` is always preserved (`(assumes G3)`).
  - Quick-select preset chips (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`) allow one-tap filtering without manual calendar scrolling (`(assumes G3)`).
- **FR-5 (Composition with Search, Tags, and Sort):** When a `SingleDay` or `Period` filter is active, the displayed entry list is the intersection (`AND`) of:
  - Active profile (`profileId`),
  - Non-archived status (`isArchived = 0`),
  - Timestamp within `[startMillis, endMillis]`,
  - Substring search match (`searchQuery`), and
  - Selected tags (`selectedTags`),
  ordered by `timestamp ASC` or `DESC` per `isAscending` (`(assumes G4)`).
- **FR-6 (Contextual Add Entry on Selected Date):** When a `SingleDay` filter is active for a past date `D` and the user taps the `+` (Add Entry) FAB, the new entry flow defaults its date/timestamp to `D` (or current time if `D` is today) so users can backfill logs for the day they are inspecting.
- **FR-7 (Active Filter Chip & Reset):** When `HistoryDateFilter` is `SingleDay` or `Period`, a prominent dismissible filter chip shows the formatted local date or range (e.g., `10 Nov 2026` or `1 Nov – 10 Nov 2026`) with a Clear (`×`) button that resets the filter to `All` (`(assumes G7)`).
- **FR-8 (Empty State for Date/Period):** When no entries match the active `SingleDay` or `Period` filter (and current search/tag filters), `HistoryScreen` displays a contextual empty state message with a `Clear date filter` button (`(assumes G7)`).
- **FR-9 (Multi-Profile Scoping):** Switching profiles via `ProfileSwitcher` reactively updates both the calendar's active-day dot indicators and the filtered entry feed for the selected profile (`(assumes G6)`).
- **FR-10 (Localization):** All calendar mode labels, preset chips, empty-state messages, and TalkBack content descriptions are externalized in `values/strings.xml` and `values-ru/strings.xml`, and dates format according to the active locale (`Docs/prd/russian-localization.md`).

## Non-functional Requirements

- **Performance:** Querying and filtering entries by `[startMillis, endMillis]` leverages the existing composite SQLite index `Index(value = ["profileId", "timestamp"])` on `journal_entries` (`app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`). Month-view dot calculation and feed updates must complete within ≤ 16 ms per frame (60 fps) without main-thread blocking.
- **Timezone & DST Correctness:** All `LocalDate` to epoch-millisecond boundary conversions use `ZoneId.systemDefault()` (`startOfDay` = `date.atStartOfDay(zone).toInstant().toEpochMilli()`, `endOfDay` = `date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1`), guaranteeing zero off-by-one-day shifts across UTC offsets and DST transitions (`(assumes G8)`).
- **Offline-First:** All calendar indicators and date-range queries run 100% locally against Room with zero network dependency.
- **Accessibility:** Calendar day cells announce the full localized date, selection state ("selected", "in range"), and whether entries exist on that date ("has entries") to TalkBack; minimum touch target size is 48×48 dp for action controls.
- **Theme Compliance:** Selected day circles, period highlight bands, and event dots use semantic Material 3 color tokens from `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` in both Light and Dark themes.

## Acceptance Criteria

- **AC-1 (Single Day Filter):** Selecting a single date `D` on the calendar filters the History feed to show only non-archived entries for the active profile whose local timestamp falls on `D` (`00:00:00.000`–`23:59:59.999`).
- **AC-2 (Period / Date Range Filter):** Selecting a start date `S` and end date `E` (`S <= E`) filters the History feed to show only non-archived entries for the active profile whose local timestamp falls between `S 00:00:00.000` and `E 23:59:59.999` inclusive.
- **AC-3 (Quick Presets):** Tapping `Today`, `Yesterday`, `Last 7 Days`, or `Last 30 Days` immediately updates the active filter and feed to the corresponding local date or period.
- **AC-4 (Event-Dot Indicators):** Days in the visible calendar month that have ≥ 1 non-archived entry for the active profile display an event dot; days with 0 entries display no dot. Archiving the last entry on a date removes its dot reactively.
- **AC-5 (Filter Composition):** With a date or period filter active, entering a search query or selecting tag chips further narrows the list to entries matching the date/period AND the search query AND all selected tags; toggling sort order reverses the chronological order within that filtered set.
- **AC-6 (Clear Filter & Empty State):** Tapping the Clear (`×`) button on the active date chip or `Clear date filter` on the empty state resets the filter to `All` and restores the full feed.
- **AC-7 (Multi-Profile Isolation):** Switching from Profile A to Profile B updates the calendar dots and filtered entries to show only Profile B's data, with zero leakage from Profile A.
- **AC-8 (DST & Timezone Boundary Safety):** Entries logged at `00:00:00.000` and `23:59:59.999` local time on a selected date are included in that date's filter, while entries at `23:59:59.999` the previous day or `00:00:00.000` the next day are excluded.
- **AC-9 (Localization & Theme):** All new UI strings render in Russian when the device locale is `ru` and in English otherwise; no hardcoded colors or strings exist outside theme/resource files.

## Out of Scope

- Displaying `BodyMeasurementEntry` rows inside `HistoryScreen` (covered by `MeasurementsScreen`).
- Sub-day time-of-day filtering (e.g., filtering between 08:00 and 12:00).
- Two-way sync with Android CalendarProvider or Google Calendar.
- Calendar view inside `ArchiveScreen`.

## Cross-references

- `Docs/prd/history-feed.md` — baseline History feed, search, tag filter, sort, and swipe-to-archive requirements that this feature extends.
- `Docs/prd/entry-logging.md` — how journal entries and their timestamps are created and edited.
- `Docs/prd/profiles-for-siblings.md` — active profile scoping across all queries and UI surfaces.
- `Docs/prd/russian-localization.md` — localization parity contract (`values/` and `values-ru/`).
- `Docs/psd/calendar-history.md` — product specification (to be added when the feature is built).
- `Docs/tests/calendar-history.md` — test cases (to be added when the feature is built).
- [[ui-layer]] — the History screen and shared Compose UI components.
- [[data-layer]] — Room DAO queries and repository flows.

## Sources

- `app/src/main/java/com/example/healthjournal/ui/screens/HistoryScreen.kt` — the History feed screen where calendar navigation and date filtering will be hosted.
- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — owns the reactive entry feed, search, tag filter, and sort state pipeline.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — mediates entry queries between `JournalViewModel` and `JournalDao`.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — Room DAO owning entry queries and `getAllEntriesInDateRange`.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — `journal_entries` entity with `Index(value = ["profileId", "timestamp"])`.
- `app/src/main/java/com/example/healthjournal/domain/UtcToLocalDate.kt` — existing UTC-to-local calendar date conversion helper.
- `Docs/prd/history-feed.md` — parent History feed product requirements.
