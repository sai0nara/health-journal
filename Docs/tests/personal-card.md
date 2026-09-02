# Personal Card — Test Cases

> Verifies the draft-edit model, date-of-birth/height/weight validation, and
> unit conversion at the JVM unit level, plus the card screen and DAO behaviour
> at the instrumented level.

Last updated: 2026-09-02

## Automated coverage

| Stack | Test file | Scope |
|---|---|---|
| JVM unit | `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` | initial state, edit/save/cancel flow, field handlers, unit toggle, validation wiring |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/validation/DemographicsValidatorTest.kt` | orchestrator: empty/valid/invalid per field |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/validation/ValidateDateOfBirthUseCaseTest.kt` | blank, past, future, age bound, format |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/validation/ValidateHeightUseCaseTest.kt` | metric/imperial in-range and out-of-range |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/validation/ValidateWeightUseCaseTest.kt` | metric/imperial in-range and out-of-range |
| JVM unit | `app/src/test/java/com/example/healthjournal/domain/validation/ValidationResultTest.kt` | Valid/Invalid shape |
| JVM unit | `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` | conversion, formatting, input parse, sanitize |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/data/local/PersonalCardDaoTest.kt` | singleton upsert, reads, delete, sync status, dirty, complex-field round-trip |
| Instrumented | `app/src/androidTest/java/com/example/healthjournal/ui/screens/PersonalCardScreenTest.kt` | empty states, section display, edit/cancel/save, dialogs, unit toggle, save gating |

## Test cases

| ID | Criterion | Scenario | Preconditions | Expected |
|---|---|---|---|---|
| T-1 | AC-1 | Valid save | all fields valid | persists locally, exits edit, pending-sync |
| T-2 | AC-2 | Future DOB | DOB in future | field invalid; save disabled |
| T-3 | AC-2 | Age out of range | DOB older than bound | field invalid; save disabled |
| T-4 | AC-2 | Height/weight out of range | over metric/imperial cap | field invalid; save disabled |
| T-5 | AC-2 | Blank optional fields | DOB/height/weight blank | valid; save enabled |
| T-6 | AC-3 | Unit toggle | switch metric→imperial | height/weight reformat; stored value preserved |
| T-7 | AC-4 | Cancel edit | edits made then cancel | drafts reverted to saved |
| T-8 | AC-5 | Sync payload/merge | card changed + cloud | last-write-wins merge; payload round-trip |
| T-9 | AC-6 | Backup/restore | full backup + restore | card present in ZIP; restored locally |
| T-10 | AC-5 | Singleton guarantee | multiple inserts | always one `personal_card` row |

## Manual checks

- Date-picker DOB flow preserves the local calendar date.
- Decimal keyboard and auto-dash `yyyy-MM-dd` input with cursor preservation.
- Add/remove dialogs for allergies, medications, reactions, history, contacts.
- Light and dark rendering of the card and its dialogs.

## Cross-references

- `Docs/prd/personal-card.md` — requirements under test.
- `Docs/psd/personal-card.md` — design the cases verify.
- `Docs/prd/drive-sync.md` — sync cases.
- [[unit-tests]] / [[instrumented]] — test stacks.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt` — ViewModel.
- `app/src/test/java/com/example/healthjournal/domain/validation/DemographicsValidatorTest.kt` — validator.
- `app/src/test/java/com/example/healthjournal/domain/validation/ValidateDateOfBirthUseCaseTest.kt` — DOB.
- `app/src/test/java/com/example/healthjournal/domain/validation/ValidateHeightUseCaseTest.kt` — height.
- `app/src/test/java/com/example/healthjournal/domain/validation/ValidateWeightUseCaseTest.kt` — weight.
- `app/src/test/java/com/example/healthjournal/domain/validation/ValidationResultTest.kt` — result shape.
- `app/src/test/java/com/example/healthjournal/data/local/UnitConverterTest.kt` — units.
- `app/src/androidTest/java/com/example/healthjournal/data/local/PersonalCardDaoTest.kt` — DAO.
- `app/src/androidTest/java/com/example/healthjournal/ui/screens/PersonalCardScreenTest.kt` — screen.
- `Docs/prd/personal-card.md` — requirements.
- `Docs/psd/personal-card.md` — design.
