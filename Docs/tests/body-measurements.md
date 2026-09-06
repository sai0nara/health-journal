# Body Measurements — Test Cases

> Verifies capture validation, the canSave matrix, undo, and analytics series at
> the JVM unit level, plus the sheet and screen behaviors (including light/dark)
> at the instrumented level.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/ValidateMeasurementsTest.kt` | format/negative/bounds, partial, at-least-one |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/GoalValidatorTest.kt` | goal rules + caps + labels |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/MeasurementFormattersTest.kt` | summaries + trend projection |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` | form state, canSave, future-date, justSaved, undo |
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModelTest.kt` | tab/series/goal projection |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/BodyMeasurementRepositoryTest.kt` | persistence + tombstone ordering |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/GoalsRepositoryTest.kt` | goal upsert/clear |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/BodyMeasurementEntryTest.kt` | entity defaults/partial |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` | sheet render, partial save, errors, future-date, date picker |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` | empty/newest/chart/delete-undo/goals/tabs/light-dark |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/BodyMeasurementDaoTest.kt` | Room roundtrip/order/pending |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/GoalDaoTest.kt` | goal DAO |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` | `body_measurements`/`goals` table migrations |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-2 | Over-bound blocked | weight above cap | field error; save disabled |
| T-2 | AC-2 | Negative blocked | -5 kg | field error; save disabled |
| T-3 | AC-2 | Future date blocked | future timestamp | block + alert |
| T-4 | AC-2 | No values | all blank | save disabled |
| T-5 | AC-1 | Partial save | one field filled | persists pending-sync |
| T-6 | AC-3 | Delete then undo | row deleted | Undo re-inserts row (per-id) |
| T-7 | AC-4 | Goal on chart | goal set for weight | chart renders goal line + label; persists |
| T-8 | AC-4 | Goal clear/invalid | clear action / over-bound goal | cleared / validation error |
| T-9 | AC-5 | Sync payload/merge | measurement rows | LWW merge + tombstone payload round-trip |
| T-10 | AC-5 | Newest-first list | measurements inserted | list newest-first; cloud/local icons |

## Manual checks

- Decimal keyboard and `ImeAction.Next` across the seven fields.
- Light and dark rendering of the sheet, screen, and chart.
- Chart auto-scaling with and without a goal, and with a flat series.

## Cross-references

- `Docs/prd/body-measurements.md` — requirements under test.
- `Docs/psd/body-measurements.md` — design the cases verify.
- `Docs/prd/drive-sync.md` — sync cases.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/domain/ValidateMeasurementsTest.kt` — capture rules.
- `app/src/test/java/com/example/healthjournal/domain/GoalValidatorTest.kt` — goals.
- `app/src/test/java/com/example/healthjournal/domain/MeasurementFormattersTest.kt` — summaries/trends.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModelTest.kt` — state/canSave/undo.
- `app/src/test/java/com/example/healthjournal/viewmodel/BodyAnalyticsViewModelTest.kt` — analytics.
- `app/src/test/java/com/example/healthjournal/data/BodyMeasurementRepositoryTest.kt` — persistence.
- `app/src/test/java/com/example/healthjournal/data/GoalsRepositoryTest.kt` — goals repo.
- `app/src/test/java/com/example/healthjournal/data/local/BodyMeasurementEntryTest.kt` — entity.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementEntrySheetTest.kt` — sheet.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/MeasurementScreenTest.kt` — screen.
- `app/src/androidTest/java/com/example/healthjournal/data/local/BodyMeasurementDaoTest.kt` — DAO.
- `app/src/androidTest/java/com/example/healthjournal/data/local/GoalDaoTest.kt` — goal DAO.
- `app/src/androidTest/java/com/example/healthjournal/data/local/MigrationTest.kt` — migrations.
- `Docs/prd/body-measurements.md` — requirements.
- `Docs/psd/body-measurements.md` — design.