# Specification: Calendar History (Single Day & Date Period Filtering)

## Overview
Enable interactive calendar-based history browsing and date filtering on `HistoryScreen` supporting two modes:
1. **Single Day (`SingleDay(date: LocalDate)`)**: Filters non-archived `JournalEntry` rows to `[00:00:00.000, 23:59:59.999]` in `ZoneId.systemDefault()`.
2. **Period (`Period(startDate: LocalDate, endDate: LocalDate)`)**: Filters non-archived entries across `[startOfDay(startDate), endOfDay(endDate)]` inclusive, with quick period presets (`Today`, `Yesterday`, `Last 7 Days`, `Last 30 Days`).

The feature adds a collapsible interactive month grid calendar with event-dot indicators on days containing logged entries for the active profile, composes via logical `AND` with substring search and category tag filters, preserves sort direction (`isAscending`), displays an active filter chip with Clear (`×`), supports contextual date prefill when creating a new entry on past dates, and strictly enforces multi-profile isolation.

## Functional Requirements
- **FR-1 (Calendar Toggle):** Tap Calendar icon button in `HistoryScreen` top bar or filter row to expand/collapse month calendar panel.
- **FR-2 (Single Day & Period Filter):** Select single date or date range to filter feed via Room range queries.
- **FR-3 (Event Dots):** Calendar days with $\ge 1$ active `JournalEntry` render event dots (unaffected by search/tags). Archiving sole entry removes dot; Undo restores it.
- **FR-4 (Quick Presets):** Chips for `Today`, `Yesterday`, `Last 7 Days`, and `Last 30 Days`.
- **FR-5 (Compound Filter & Sort):** Composes with `searchQuery` and `selectedTags`, sorted by `isAscending`.
- **FR-6 (Contextual Add Entry Prefill):** Tapping `+` FAB with past `SingleDay(D)` active prefills entry date to `D`.
- **FR-7 (Active Filter Chip & Reset):** Filter chip with Clear (`×`) button resets filter to `All`.
- **FR-8 (Contextual Empty State):** Empty state with `Clear date filter` CTA when 0 entries match.
- **FR-9 (Multi-Profile Isolation):** Reactive scoping to `ProfileRepository.activeProfileIdFlow`.
- **FR-10 (Localization & Theme):** Full EN/RU parity and Material 3 semantic color tokens.

## Acceptance Criteria
- **AC-1:** Single-day filter includes exact `startOfDay` and `endOfDay` local boundaries.
- **AC-2:** Period filter includes all entries within `[startDate, endDate]` and normalizes inverted taps (`endDate < startDate`).
- **AC-3:** Quick presets resolve correctly relative to `LocalDate.now(zone)`.
- **AC-4:** Event dots accurately reflect non-archived entries in visible month.
- **AC-5:** Date filtering composes via `AND` with search and tags.
- **AC-6:** Filter chip and contextual empty state clear actions reset filter to `All`.
- **AC-7:** Profile switching updates dots and feed with zero cross-profile leakage.
- **AC-8:** Local midnight epoch bounds calculated safely across UTC offsets and 23h/25h DST days.
- **AC-9:** Zero hardcoded strings or colors.

## Out of Scope
- Merging `BodyMeasurementEntry` rows into `HistoryScreen`.
- Sub-day time-of-day (hour/minute) filtering within a single date.
- External calendar sync (Google Calendar / iCal).
- Calendar month grid inside `ArchiveScreen`.
