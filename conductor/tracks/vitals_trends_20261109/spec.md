# Specification: Vitals & Trends

## Overview
Introduce a dedicated Vitals & Trends destination (`VitalsScreen` at route `"vitals"`) to monitor, visualize, and record 5 core clinical vital metrics:
1. Blood Pressure (systolic/diastolic dual curves in mmHg)
2. Blood Glucose (mmol/L or mg/dL per UnitSettings)
3. Resting Heart Rate (bpm)
4. Body Temperature (°C or °F per UnitSettings)
5. Oxygen Saturation (SpO2 in %)

The screen features an auto-scaled Compose Canvas trend chart with clinical reference guideline bands and touch scrubber tooltips, 5 selectable time windows (7D, 30D, 90D, 1Y, All), a summary statistics card (Latest, Mean, Min, Max), a quick log bottom sheet (`VitalLogSheet`), and a chronological history list with 5-second deletion undo.

Data is queried directly from `journal_entries` rows having non-null vitals, scoped strictly to the active profile ID, ensuring zero database migration risk and instant availability of all historical data.

## Functional Requirements
- **FR-1 (Main Navigation):** Add a `"Vitals"` tile to the main dashboard (`MainScreen.kt`) navigating to route `"vitals"`.
- **FR-2 (Top Bar & Profile Switcher):** Top app bar includes back navigation and reactive `ProfileSwitcher` dropdown scoping all data to the active profile.
- **FR-3 (Metric Tabs):** Scrollable tab row allows switching between `Blood Pressure`, `Blood Glucose`, `Heart Rate`, `Body Temperature`, and `SpO2`.
- **FR-4 (Time Range Filtering):** Filter row with chips `7D`, `30D` (default), `90D`, `1Y`, and `All`.
- **FR-5 (Summary Statistics):** Metric card computes Latest value + timestamp, Period Mean average, and Period Min/Max extremes. For BP, computes both systolic and diastolic in tandem.
- **FR-6 (Canvas Trend Chart):**
  - High-performance auto-scaled Canvas rendering.
  - Blood Pressure plots dual distinct colored lines: Systolic (coral/red) and Diastolic (teal/blue) with reference line at 120/80 mmHg.
  - Other metrics render primary curve with shaded area gradient and dashed reference bands.
  - For long-range windows (90D, 1Y, All), same-day multiple readings aggregate into daily mean points with range tooltips.
  - Interactive touch scrubber cursor with haptic feedback tick (`LocalHapticFeedback`).
- **FR-7 (Chronological History & Deletion Undo):** List of readings with formatted values and timestamps; item deletion removes reading from UI and triggers a 5-second Snackbar with "Undo" action.
- **FR-8 (Quick Log Bottom Sheet):** FAB `+ Log Vital` opens `VitalLogSheet` preselected to active tab, with fields for all 5 vitals and date/time picker (future dates blocked; systolic > diastolic enforced).
- **FR-9 (Unit Conversion):** Glucose and Temperature dynamically display in active unit system (Metric vs Imperial) while persisting canonically as metric in Room.
- **FR-10 (Empty State):** Zero readings in selected window renders clean illustration and "Log your first reading" button.

## Non-Functional Requirements
- **Performance:** Chart calculation and rendering must complete within 16 ms (60 fps) on mid-range devices.
- **Offline First:** Zero network calls required; all data reads and writes execute against local Room database.
- **Architecture:** Clean MVVM with `VitalsViewModel` and `VitalsUiState` StateFlow.

## Acceptance Criteria
- **AC-1:** Tapping "Vitals" tile on `MainScreen` navigates to `"vitals"` destination.
- **AC-2:** Switching tabs updates active metric, chart curves, summary stats, and history list.
- **AC-3:** Changing time filters (`7D`, `30D`, `90D`, `1Y`, `All`) recalculates cutoff date and summary statistics immediately.
- **AC-4:** Blood Pressure chart displays two distinct colored curves (systolic and diastolic) and dual average stats.
- **AC-5:** Switching unit system in Settings updates Glucose and Temperature chart bounds, values, and summary stats without precision loss.
- **AC-6:** Submitting from `VitalLogSheet` validates input, stores canonical values to Room, updates chart immediately, and marks entry pending sync.
- **AC-7:** Deleting an item shows a 5-second Undo Snackbar; tapping Undo restores item, while timeout commits deletion.
- **AC-8:** Switching profiles via `ProfileSwitcher` updates vitals view to target profile with zero data leakage.
- **AC-9:** Zero-data state renders empty graphic with "Log your first reading" CTA.

## Out of Scope
- Direct Bluetooth device streaming.
- Medical diagnostic interpretations.
- Dedicated standalone PDF report export (covered in general PDF/ZIP backup).
