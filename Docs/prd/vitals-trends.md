# Vitals & Trends — Product Requirements

> Dedicated vitals hub to monitor blood pressure, blood glucose, heart rate, body temperature, and oxygen saturation with interactive trend charts, time-range statistics, and clinical reference bands.

Last updated: 2026-11-09

## Overview

Health Journal currently records five editable vitals (systolic/diastolic blood pressure, heart rate, blood glucose, body temperature, and SpO2) as optional columns on journal entries (`app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt`). Users can log these vitals from the History card or the Add/Edit Entry screen (`Docs/prd/manual-vitals.md`). However, there is no centralized dashboard to visualize chronological trends, observe fluctuations across weeks or months, evaluate clinical ranges, or inspect summary statistics (averages, min/max).

This feature introduces a dedicated `"vitals"` destination accessible from the main page dashboard (`Docs/prd/main-page-tiles.md`). It aggregates historical vitals across all journal records for the active profile, plots auto-scaled trend charts across selectable time windows (7D, 30D, 90D, 1Y, All), displays key stats (latest reading, period mean, min/max range), and provides a bottom sheet to log vitals directly without requiring a text entry.

## Grooming Decisions (G1–G7, V1–V3)

All grooming findings from the Definition-of-Ready review are recorded here with their resolution and cited across the requirements below:

- **G1 (Major): Data source & persistence layer.** Rather than introducing a redundant Room table, the vitals hub queries existing `journal_entries` where vital columns are non-null (`(systolic IS NOT NULL AND diastolic IS NOT NULL) OR bloodGlucose IS NOT NULL OR heartRate IS NOT NULL OR bodyTemperature IS NOT NULL OR spo2 IS NOT NULL`). Logging from the vitals hub creates a journal entry stamped with the active profile ID, current timestamp, and null content text. This ensures instant availability of all historical data, zero database migration risk, and automatic cloud sync (`Docs/prd/drive-sync.md`) and backup compatibility (`Docs/prd/restore-from-backup.md`).
- **G2 (Major): Scope of vitals parameters.** Exactly five vitals parameters are charted:
  1. Blood Pressure (systolic / diastolic in mmHg)
  2. Blood Glucose (mmol/L or mg/dL per `Docs/prd/unit-conversion.md`)
  3. Heart Rate (bpm)
  4. Body Temperature (°C or °F per `Docs/prd/unit-conversion.md`)
  5. Oxygen Saturation (SpO2 in %)
  Sleep duration (hours) from Health Connect remains accessible as a supplemental read-only reference.
- **G3 (Major): Trend aggregation & point decimation.** For 7D and 30D windows, all individual readings are plotted at their exact timestamps. For 90D, 1Y, and All windows, multiple readings taken on the same calendar day are averaged into a daily mean point to preserve rendering performance, with the interactive scrubber tooltip revealing the day's min/max range and sample count.
- **G4 (Minor): Clinical reference bands.** Charts display standardized, subtle horizontal reference guideline bands based on medical consensus (e.g. Normal Blood Pressure <120/<80 mmHg, Normal Fasting Glucose 3.9–5.6 mmol/L / 70–99 mg/dL, Normal Resting HR 60–100 bpm, Normal SpO2 ≥95%, Normal Temperature 36.1–37.2 °C / 97.0–99.0 °F).
- **G5 (Minor): Navigation entry points.** A new `"Vitals"` tile is added to `MainScreen` (`route = "vitals"`). Additionally, tapping any vital chip on the History feed or the quick-add sheet provides a "View Trends" shortcut to the corresponding vital tab.
- **G6 (Minor): Multi-profile isolation.** All vitals queries, statistics, and charts strictly observe `ProfileRepository.activeProfileIdFlow`, instantly refreshing when switching sibling profiles (`Docs/prd/profiles-for-siblings.md`).
- **G7 (Minor): Deletion lifecycle & undo.** Individual vitals records can be deleted from the chronological list; deletion soft-deletes the underlying journal record or clears its vital columns, accompanied by a 5-second Snackbar with an Undo action.
- **V1: "Separate screen"** replaced with: Top-level composable `VitalsScreen` at route `"vitals"`, featuring `ProfileSwitcher`, metric tab selector, period chips (7D, 30D, 90D, 1Y, All), interactive Canvas trend chart, summary statistics card, and chronological log list.
- **V2: "Others vitals"** replaced with: The 5 discrete parameters validated in `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt`.
- **V3: "With trends"** replaced with: Custom Compose Canvas auto-scaled line/band chart with touch scrubber tooltips, period averages, min/max extremes, and clinical reference threshold lines.

## Goals / Non-goals

**Goals**

- Provide a dedicated Vitals dashboard reachable in one tap from the main page dashboard (`Docs/prd/main-page-tiles.md`).
- Render auto-scaled, high-performance Canvas trend charts for Blood Pressure (dual systolic/diastolic curves), Blood Glucose, Heart Rate, Body Temperature, and SpO2.
- Support selectable time windows: 7 Days (`7D`), 30 Days (`30D`), 90 Days (`90D`), 1 Year (`1Y`), and All Time (`All`).
- Display a summary statistics card per metric showing latest reading, period average, min/max values, and clinical category status.
- Allow direct logging of vitals via a bottom sheet without requiring note text or photo attachments.
- Full unit conversion support for glucose (mmol/L ↔ mg/dL) and temperature (°C ↔ °F) per `Docs/prd/unit-conversion.md`.
- Strict multi-profile separation per `Docs/prd/profiles-for-siblings.md`.

**Non-goals**

- Continuous CGM / Bluetooth medical device streaming (all manual or Health Connect pull).
- AI diagnostic interpretations or medical advice warnings.
- Custom user-defined medical threshold lines (v1 uses standard clinical reference bands).
- Modifying Room database tables or sync payload schemas (v1 reuses `journal_entries`).

## User Stories

- As a user monitoring hypertension, I want a dedicated screen to view my systolic and diastolic blood pressure over the past 30 and 90 days so I can see if lifestyle changes are lowering my pressure.
- As a diabetic user, I want to track blood glucose trends in mg/dL or mmol/L with clear indicators of fasting normal ranges so I can share my readings with my endocrinologist.
- As a user managing asthma or recovery, I want to review my resting heart rate and SpO2 trends over time.
- As a parent caring for sibling profiles, I want to switch profiles and immediately inspect my child's temperature and vitals history independently of my own.
- As a user on the main dashboard, I want a "Vitals" tile so I can jump directly to my vitals without scrolling through the journal feed.

## Functional Requirements

- **FR-1:** The main dashboard (`MainScreen`) displays a `"Vitals"` tile that navigates to the `"vitals"` route (`(assumes G5)`).
- **FR-2:** The top bar of `VitalsScreen` includes the screen title, back navigation arrow, and the standard `ProfileSwitcher` dropdown (`(assumes G6)`).
- **FR-3:** A scrollable tab row allows switching between the five vital parameters: `Blood Pressure`, `Blood Glucose`, `Heart Rate`, `Body Temperature`, and `SpO2` (`(assumes G2)`).
- **FR-4:** A time-range filter row provides five toggle chips: `7D`, `30D`, `90D`, `1Y`, and `All` (`(assumes G3)`). The default selection is `30D`.
- **FR-5:** Above the chart, a summary metrics card displays:
  - Latest reading value with relative/formatted timestamp and clinical status badge.
  - Period average (mean of all readings in the selected window).
  - Minimum and Maximum readings within the window.
- **FR-6:** The trend chart is rendered via Compose Canvas:
  - For `Blood Pressure`, two distinct colored lines are plotted: Systolic (coral/red) and Diastolic (teal/blue).
  - For single-value vitals, a primary brand curve with filled area gradient or datapoint dots is rendered.
  - Horizontal dashed reference lines/bands represent normal clinical healthy thresholds (`(assumes G4)`).
  - Dragging or tapping along the chart activates a vertical cursor with a tooltip popup showing exact timestamp and recorded value(s), accompanied by haptic feedback.
- **FR-7:** Below the chart, a chronological history list displays all readings within the selected window with timestamp, formatted values with active units, and an overflow menu or swipe action to edit or delete (`(assumes G7)`).
- **FR-8:** A Floating Action Button (`+ Log Vital`) opens `VitalLogSheet` allowing the user to record any or all vitals with date/time picker (past dates allowed, future dates blocked per `ValidateVitals`).
- **FR-9:** Saving from `VitalLogSheet` persists the record to `JournalRepository` with the active profile ID and marks it pending sync (`(assumes G1)`).
- **FR-10:** Blood glucose and body temperature display in the active unit system selected in `Settings` (`mmol/L` vs `mg/dL`, `°C` vs `°F`), with storage strictly canonical metric per `Docs/prd/unit-conversion.md`.
- **FR-11:** If no vitals are logged in the selected window, the screen renders an empty state illustration with an action button `Log your first reading`.

## Non-functional Requirements

- **Performance:** Chart rendering and time-window filtering must calculate on the Default coroutine dispatcher and render within 16 ms (60 fps) without UI jank.
- **Offline First:** All queries execute against local Room database; zero network calls required for rendering or logging.
- **Data Integrity:** Pure validation via `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` prevents malformed inputs before database insertion.
- **Accessibility:** All tabs, filter chips, chart summary metrics, and history items must have complete TalkBack semantics and content descriptions.
- **Theme Consistency:** Colors for blood pressure, glucose, and chart lines must use semantic tokens from `app/src/main/java/com/example/healthjournal/ui/theme/Color.kt` supporting both Light and Dark themes.

## Acceptance Criteria

- **AC-1:** Tapping the "Vitals" tile on `MainScreen` navigates to `"vitals"`, displaying the active profile's vitals.
- **AC-2:** Switching between metric tabs (`Blood Pressure`, `Blood Glucose`, etc.) smoothly updates the chart, summary card, and history list for the selected metric.
- **AC-3:** Changing the time filter (`7D`, `30D`, `90D`, `1Y`, `All`) filters readings and recalculates period average, min, and max accurately.
- **AC-4:** For Blood Pressure, both systolic and diastolic values appear on the chart with distinct colors, and the summary card displays both averages (e.g. `118/76 mmHg`).
- **AC-5:** Switching unit system in Settings between Metric and Imperial updates Glucose (`mmol/L` ↔ `mg/dL`) and Temperature (`°C` ↔ `°F`) values, bounds, chart axis labels, and statistics without loss of precision.
- **AC-6:** Tapping `+ Log Vital` opens the sheet preselected to the active vital; submitting a valid reading adds it to the database, updates the chart immediately, and marks the entry pending cloud sync.
- **AC-7:** Deleting a reading from the history list removes it from the chart and list, with an Undo snackbar restoring the record if tapped within 5 seconds.
- **AC-8:** Switching profiles via `ProfileSwitcher` updates the vitals view to the newly selected profile without leaking data from other profiles.
- **AC-9:** An empty state with call-to-action is displayed when no data exists for the selected metric and window.

## Out of Scope

- Direct Bluetooth pairing with pulse oximeters, blood pressure cuffs, or glucometers.
- Automated medical diagnostic alerts or clinical condition classification.
- Exporting dedicated standalone vitals CSV/PDF reports (covered in general PDF/backup export).
- Custom user-defined goal lines on vitals charts (deferred to a follow-up feature).

## Cross-references

- `Docs/prd/manual-vitals.md` — requirements for the underlying vitals data model and validation.
- `Docs/prd/unit-conversion.md` — unit system conversions for glucose and temperature.
- `Docs/prd/profiles-for-siblings.md` — profile-scoping requirements.
- `Docs/prd/main-page-tiles.md` — main screen tile dashboard.
- `Docs/prd/drive-sync.md` — cloud synchronization of entries.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — database entity storing vitals columns.
- `app/src/main/java/com/example/healthjournal/domain/ValidateVitals.kt` — validation rules and bounds for vitals inputs.
- `app/src/main/java/com/example/healthjournal/data/local/UnitConverter.kt` — metric/imperial conversion logic.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — repository providing journal and vitals data access.
- `app/src/main/java/com/example/healthjournal/ui/screens/MainScreen.kt` — dashboard hosting the navigation tile.
- `Docs/prd/manual-vitals.md` — baseline requirements for manual vitals capture.
