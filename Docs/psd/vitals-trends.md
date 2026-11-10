# Vitals & Trends — Product Specification

> Centralized vitals hub querying existing `journal_entries`, transforming multi-metric time series across configurable windows, and rendering auto-scaled Compose Canvas charts with reference bands, interactive touch scrubbing, and multi-profile isolation.

Last updated: 2026-11-09

## Overview

Health Journal records five clinical vitals as optional columns in the `journal_entries` table. This specification establishes a dedicated top-level dashboard (`route = "vitals"`) that aggregates, filters, decimates, and plots chronological trends for Blood Pressure (dual systolic/diastolic curves), Blood Glucose, Heart Rate, Body Temperature, and SpO2 without requiring database schema changes or secondary storage tables. Requirements are defined in `Docs/prd/vitals-trends.md`.

## Architecture

The feature follows Android MVVM architecture with unidirectional data flow (UDF):

1. **Persistence & Data Access**: `JournalDao` executes filtered Flow queries over `journal_entries` for records where vital columns are populated (`getVitalsEntries(profileId)`). `JournalRepository` wraps this DAO and guarantees profile isolation.
2. **ViewModel State Pipeline**: `VitalsViewModel` uses coroutines and `combine` to merge `JournalRepository.vitalsEntries`, `ActiveProfileProvider.activeProfileId`, and `unitSystemFlow` into a consolidated `StateFlow<VitalsUiState>`. It computes summary statistics (latest, period mean, min, max, clinical status badge) and applies calendar-day aggregation for long-range windows (`90D`, `1Y`, `ALL`).
3. **Canvas Trend Chart Component**: `VitalsTrendChart` performs hardware-accelerated drawing on Compose Canvas. It dynamically computes min/max bounds across data values and clinical reference thresholds, plots dual curves for Blood Pressure and single curves with gradient fills for other vitals, and tracks pointer drag gestures to render an interactive scrubber cursor with haptic feedback ticks.
4. **Quick Log & Deletion Lifecycle**: `VitalLogSheet` allows direct recording of vitals with inline validation and date/time selection. Deletion updates UI state immediately and fires a 5-second Snackbar offering an Undo restoration path.

## Data flow

1. **Launch**: User taps the "Vitals" tile on `MainScreen` (`app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt`); Jetpack Navigation transitions to `"vitals"`.
2. **Query**: `VitalsViewModel` (`app/src/main/java/com/example/healthjournal/viewmodel/VitalsViewModel.kt`) collects `journalRepository.vitalsEntries` filtered by the active profile from `ActiveProfileProvider`.
3. **State Derivation**: Entries are filtered by the selected `VitalsTimeWindow` (`7D`, `30D`, `90D`, `1Y`, `ALL`). If long-range, multiple readings on the same calendar day are averaged into single daily points. Values are converted to active display units (`UnitSystem.METRIC` vs `UnitSystem.IMPERIAL`).
4. **Rendering**: `VitalsScreen` (`app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt`) renders the metric tabs, time-range chips, summary card, `VitalsTrendChart`, and chronological history cards.
5. **Logging**: User taps the FAB, completes `VitalLogSheet` (`app/src/main/java/com/example/healthjournal/ui/components/VitalLogSheet.kt`), and taps Save. `ValidateVitals.toEntryVitals` validates and converts display text to canonical metric units. The ViewModel saves a new `JournalEntry` stamped with active profile ID and marks it pending sync.
6. **Deletion & Undo**: User taps the delete icon on a history card. The entry ID is immediately hidden from UI state, `JournalRepository.delete` soft-deletes the row, and an Undo Snackbar appears. If Undo is tapped, `JournalRepository.insert` restores the row.

## Components

| Component | File | Responsibility |
|---|---|---|
| `VitalsViewModel` | `app/src/main/java/com/example/healthjournal/viewmodel/VitalsViewModel.kt` | Orchestrates reactive state pipeline, decimation, statistics, and quick-log / undo logic. |
| `VitalsTrendChart` | `app/src/main/java/com/example/healthjournal/ui/components/VitalsTrendChart.kt` | Compose Canvas drawing of dual-curve BP, single-metric curves, reference bands, and scrubber. |
| `VitalLogSheet` | `app/src/main/java/com/example/healthjournal/ui/components/VitalLogSheet.kt` | Modal bottom sheet for logging vitals with date picker and future-date validation. |
| `VitalsScreen` | `app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt` | Assembles TopAppBar, `ProfileSwitcher`, tabs, time chips, summary card, chart, and history list. |
| `JournalDao` / `JournalRepository` | `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` | Room query flows for vitals entries filtered by profile ID. |
| `ValidateVitals` | `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` | Pure validation, boundary checks, and canonical unit conversions. |
| `MainScreen` | `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` | Hosts `"Vitals"` tile in the 2-column dashboard grid. |
| `MainActivity` | `app/src/main/java/com/example/healthjournal/MainActivity.kt` | Instantiates `VitalsViewModelFactory` and registers `"vitals"` destination in `NavHost`. |

## Edge cases & failure handling

| Edge case | Anticipated condition | Handling |
|---|---|---|
| Zero readings in window | Empty list from repository or time-filter | Displays empty state card with CTA to log first reading; chart renders graceful placeholder. |
| Single reading in window | List size == 1 | Chart renders centered circle marker without division-by-zero crash in coordinate interpolation. |
| Long-range same-day multiple readings | Multiple logs in 90D/1Y/ALL on same day | Aggregates entries into daily mean point with sample count and min/max extremes in scrubber tooltip. |
| Rapid profile switching | Active profile changes via TopAppBar | Query Flow re-emits immediately; pending deletes for prior profile are cleared; zero cross-profile leakage. |
| Future date selected in logger | Date picker set beyond current time | `ValidateVitals` / date check detects future timestamp, displays error alert, and disables Save button. |
| Unit system change in Settings | Metric ↔ Imperial toggled | State pipeline converts Glucose (mmol/L ↔ mg/dL) and Temp (°C ↔ °F) without altering canonical database values. |

## Dependencies

- Room persistence (`JournalDatabase`, `JournalDao`)
- Jetpack Compose Material 3 (`Scaffold`, `ScrollableTabRow`, `FilterChip`, `Card`, `ModalBottomSheet`, `DatePickerDialog`)
- Kotlin Coroutines & StateFlow (`combine`, `stateIn`, `SharingStarted.Eagerly`)
- Local Haptic Feedback (`LocalHapticFeedback`)

## Cross-references

- `Docs/prd/vitals-trends.md` — requirements this specification implements.
- `Docs/tests/vitals-trends.md` — test cases verifying this design.
- `wiki/modules/data-layer.md` — Room persistence and repository pattern.

## Sources

- `app/src/main/java/com/example/healthjournal/viewmodel/VitalsViewModel.kt` — ViewModel state pipeline and statistics computation.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalsTrendChart.kt` — Canvas trend chart component.
- `app/src/main/java/com/example/healthjournal/ui/components/VitalLogSheet.kt` — Modal bottom sheet for quick logging.
- `app/src/main/java/com/example/healthjournal/ui/screens/VitalsScreen.kt` — Vitals screen composable.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — Data layer query and mutation methods.
- `app/src/main/java/com/example/healthjournal/data/local/JournalDao.kt` — Room DAO query definitions.
- `Docs/prd/vitals-trends.md` — requirements this specification implements.
- `Docs/tests/vitals-trends.md` — test cases.
