# Vitals & Trends — Test Cases

> Maps the PRD's acceptance criteria (`Docs/prd/vitals-trends.md`) and PSD's technical architecture (`Docs/psd/vitals-trends.md`) to automated unit, instrumented UI, and architectural audit verification suites.

Last updated: 2026-11-09

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/VitalsTrendsP0Test.kt` | Validates bounds, numeric constraints, and canonical unit conversions for all 5 vitals. |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/VitalsViewModelTest.kt` | Verifies StateFlow pipeline, time window filtering, long-range decimation, stats computation, and undo lifecycle. |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` | Verifies `vitalsEntries` query flow filtering by profile ID. |
| JVM audit | `app/src/test/java/com/example/healthjournal/localization/HardcodedStringAuditTest.kt` | Verifies zero hardcoded user-facing strings across all vitals screens and components. |
| JVM audit | `app/src/test/java/com/example/healthjournal/ui/theme/HardcodedColorAuditTest.kt` | Enforces semantic token usage from `Color.kt` and `MaterialTheme.colorScheme`. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsTrendChartTest.kt` | Validates Canvas curve rendering, dual-series BP, reference bands, and scrubber haptics. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalLogSheetTest.kt` | Validates bottom sheet inputs, future-date blocking, inline errors, and save callbacks. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/screens/VitalsScreenTest.kt` | End-to-end screen testing for tab navigation, empty states, quick logging, and undo snackbar. |
| Instrumented UI | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` | Validates presence and routing of the Vitals tile on the main dashboard grid. |

## Test cases

| ID | Scenario | Preconditions | Steps | Expected |
|---|---|---|---|---|
| T-1 | Profile isolation | Multiple profiles exist in database | Switch active profile via `ProfileSwitcher` | Vitals screen updates instantly with entries matching only the active profile ID. |
| T-2 | Metric tab switching | Screen loaded with mixed vitals entries | Tap each of the 5 tabs (`BP`, `Glucose`, `Heart Rate`, `Temp`, `SpO2`) | Chart and stats dynamically switch to the active metric series. |
| T-3 | Time window filtering | Readings logged 2, 15, and 45 days ago | Switch filter between `7D`, `30D`, `90D`, `1Y`, and `All` | Readings older than window cutoff are excluded from calculations and rendering. |
| T-4 | Same-day aggregation | Multiple readings taken on the same day | Select `90D`, `1Y`, or `All` window | Multiple same-day points are aggregated into a single daily mean with sample count and range preserved. |
| T-5 | Unit system conversion | Glucose (5.5 mmol/L) and Temp (37.0 °C) logged | Switch unit system from Metric to Imperial in Settings | Glucose displays as ~99 mg/dL and Temp as 98.6 °F in stats, chart axis, and tooltips. |
| T-6 | Dual-series BP plotting | Systolic and diastolic values present | Select Blood Pressure tab | Both systolic (coral) and diastolic (teal) lines and circles are rendered simultaneously. |
| T-7 | Touch scrubber interaction | Chart displayed with data points | Drag or tap across chart canvas | Scrubber line aligns to nearest point, haptic tick fires, and tooltip card displays values. |
| T-8 | Future-date prevention | Open `VitalLogSheet` | Select future date in `DatePickerDialog` | Inline error `error_date_in_future` displays and Save button remains disabled. |
| T-9 | Deletion and Undo | History item present on VitalsScreen | Tap delete icon on history card | Item is immediately hidden, Snackbar appears with Undo; tapping Undo restores the record. |
| T-10 | Main dashboard tile | App launched on `MainScreen` | Tap "Vitals" tile | App navigates to route `"vitals"` and renders `VitalsScreen`. |

## Manual checks

- Verify physical haptic feedback response on physical devices with varied vibration actuators.
- Inspect contrast of reference guideline bands and curve colors under both Light ("Medical Standard") and Dark ("Eye-strain Reduction") themes in direct sunlight.

## Cross-references

- `Docs/prd/vitals-trends.md` — requirements under test.
- `Docs/psd/vitals-trends.md` — design the cases verify.
- `wiki/modules/data-layer.md` — Room persistence and repository pattern.

## Sources

- `app/src/test/java/com/example/healthjournal/domain/VitalsTrendsP0Test.kt` — P0 domain validation tests.
- `app/src/test/java/com/example/healthjournal/viewmodel/VitalsViewModelTest.kt` — ViewModel pipeline and statistics tests.
- `app/src/test/java/com/example/healthjournal/data/JournalRepositoryTest.kt` — Data layer query tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalsTrendChartTest.kt` — Chart UI component tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/components/VitalLogSheetTest.kt` — Log sheet UI component tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/VitalsScreenTest.kt` — Full screen UI tests.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MainScreenTest.kt` — Dashboard navigation tests.
- `Docs/prd/vitals-trends.md` — requirements under test.
- `Docs/psd/vitals-trends.md` — design the cases verify.
