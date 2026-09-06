# Demographics Validation for Personal Card

## About

This plan implements comprehensive demographics validation for the Personal Card feature of the Health Journal Android app. The Personal Card settles the existing "flat" validation (a single `parseMeasurement` method with magic max values) into an MVI/Clean Architecture form: dedicated `ValidateXUseCase` classes for each validated field, a `UnitSystem` abstraction (metric/imperial), a `DemographicsValidator` orchestrator, real-time validation wired through the `PersonalCardViewModel`, and a Material 3 DatePicker replacing the manual DOB text entry.

## Goals

- Domain validation use cases with unit tests
- Unit system support: metric (kg/cm) and imperial (lbs/in) with seamless switching
- Native Material 3 DatePicker for Date of Birth
- Real-time validation: error states with localized (string resource) messages
- Save button disabled until all fields pass validation
- SI unit storage in the database (kg, cm); conversions performed in the ViewModel
- TalkBack accessibility support with content descriptions
- Locale-aware date handling using `java.time`

## Non-Goals

- No synchronization/sync logic changes
- No changes to other cards (Medical Profile, Medical History, Emergency Contacts) validation
- No server-side validation

## Background

The `personal_card` table (DB v12) stores `Demographics` with `heightCm`, `weightKg`, and `dateOfBirth` as strings. The existing validation lives inside `PersonalCardViewModel` via a `parseMeasurement` helper and ad-hoc string checks. The spec requires this to become a set of pure domain use cases.

## Global Constraints

- Store all numeric values in SI units (kg, cm) in the database
- Perform metric-to-imperial conversions in the ViewModel using `BigDecimal` for precision
- Validation runs on the main thread with O(1) time complexity
- All fields must pass validation before Save is enabled
- Locale-aware date formatting using `java.time.format.DateTimeFormatter`
- TalkBack accessibility support with content descriptions

## Prerequisites

| # | Prerequisite | Must exist in plan | Produced by |
|---|--------------|--------------------|-------------|
| 1 | `ValidationResult` sealed interface | Task 1 | Task 1 |
| 2 | `UnitSystem` enum + `UnitConverter` object | Task 2 | Task 2 |
| 3 | `ValidateDateOfBirthUseCase` | Task 3 | Task 3 |
| 4 | `ValidateHeightUseCase` | Task 4 | Task 4 |
| 5 | `ValidateWeightUseCase` | Task 5 | Task 5 |
| 6 | `DemographicsValidator` + `DemographicsValidationResult` | Task 6 | Task 6 |
| 7 | Updated `PersonalCardViewModel` | Task 7 | Task 7 |
| 8 | Updated `PersonalCardScreen` | Task 8 | Task 8 |

## Risks

- Task 7 requires access to `UnitConverter` from the ViewModel package.
- The `Period` import in Task 3 needs `java.time.Period`.

## Tasks

### Task 1: Create Validation Result Sealed Interface

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/domain/validation/ValidationResult.kt`
- Test: `app/src/test/java/com/example/healthjournal/domain/validation/ValidationResultTest.kt`

**Interfaces:**
- Produces: `ValidationResult` sealed interface with `Valid` and `Invalid(errorResId: Int)` variants

Constants:
- `Valid` — represents a valid field value
- `Invalid(errorResId: Int)` — represents an invalid field value with a string resource id

### Task 2: Create Unit System Enum and Converter

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/data/local/UnitSystem.kt`
- Create: `app/src/main/java/com/example/healthjournal/viewmodel/UnitConverter.kt`
- Test: `app/src/test/java/com/example/healthjournal/viewmodel/UnitConverterTest.kt`

**Interfaces:**
- Produces: `UnitSystem` enum, `UnitConverter` object with conversion functions

Constants:
- `METRIC` and `IMPERIAL` enum values
- `CM_PER_INCH = 2.54`, `KG_PER_LB = 0.45359237`

Functions:
- `cmToInches(cm: Double): Double` uses `BigDecimal(cm / CM_PER_INCH)` scaled to 1 decimal
- `inchesToCm(inches: Double): Double` uses `BigDecimal(inches * CM_PER_INCH)` scaled to 1 decimal
- `kgToLbs(kg: Double): Double` uses `BigDecimal(kg / KG_PER_LB)` scaled to 1 decimal
- `lbsToKg(lbs: Double): Double` uses `BigDecimal(lbs * KG_PER_LB)` scaled to 2 decimals
- `formatForDisplay(value, unitSystem, isHeight): String`
- `parseInput(input, unitSystem, isHeight): Double?`

### Task 3: Create ValidateDateOfBirthUseCase

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/domain/validation/ValidateDateOfBirthUseCase.kt`
- Test: `app/src/test/java/com/example/healthjournal/domain/validation/ValidateDateOfBirthUseCaseTest.kt`

**Interfaces:**
- Consumes: `ValidationResult`
- Produces: `ValidateDateOfBirthUseCase` class with `invoke(dateString: String): ValidationResult`

Rules:
- Blank date returns `Valid`
- Non-`yyyy-MM-dd` format returns `Invalid(error_invalid_date_format)`
- Date in the future returns `Invalid(error_date_in_future)`
- Age > 130 years returns `Invalid(error_age_too_high)`; exactly 130 years is valid

String resources (add to `strings.xml`):
```xml
<string name="error_invalid_date_format">Invalid date format</string>
<string name="error_date_in_future">Date cannot be in the future</string>
<string name="error_age_too_high">Age cannot exceed 130 years</string>
<string name="error_height_out_of_range">Height must be between %1$s and %2$s</string>
<string name="error_weight_out_of_range">Weight must be between %1$s and %2$s</string>
<string name="error_invalid_number">Invalid number format</string>
```

### Task 4: Create ValidateHeightUseCase

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/domain/validation/ValidateHeightUseCase.kt`
- Test: `app/src/test/java/com/example/healthjournal/domain/validation/ValidateHeightUseCaseTest.kt`

**Interfaces:**
- Consumes: `UnitSystem`, `ValidationResult`
- Produces: `ValidateHeightUseCase` class with `invoke(heightCm: Double?, unitSystem: UnitSystem): ValidationResult`

Constants:
- `MIN_HEIGHT_CM = 20.0`, `MAX_HEIGHT_CM = 275.0`
- `MIN_HEIGHT_INCHES = 8.0`, `MAX_HEIGHT_INCHES = 108.0`

Rules:
- Null height returns `Valid`
- Convert to display units via `UnitConverter.cmToInches` for imperial
- Value outside range returns `Invalid(error_height_out_of_range)`

### Task 5: Create ValidateWeightUseCase

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/domain/validation/ValidateWeightUseCase.kt`
- Test: `app/src/test/java/com/example/healthjournal/domain/validation/ValidateWeightUseCaseTest.kt`

**Interfaces:**
- Consumes: `UnitSystem`, `ValidationResult`
- Produces: `ValidateWeightUseCase` class with `invoke(weightKg: Double?, unitSystem: UnitSystem): ValidationResult`

Constants:
- `MIN_WEIGHT_KG = 0.5`, `MAX_WEIGHT_KG = 650.0`
- `MIN_WEIGHT_LBS = 1.1`, `MAX_WEIGHT_LBS = 1430.0`

Rules:
- Null weight returns `Valid`
- Convert to display units via `UnitConverter.kgToLbs` for imperial
- Value outside range returns `Invalid(error_weight_out_of_range)`

### Task 6: Create DemographicsValidator Orchestrator

**Files:**
- Create: `app/src/main/java/com/example/healthjournal/domain/validation/DemographicsValidator.kt`
- Test: `app/src/test/java/com/example/healthjournal/domain/validation/DemographicsValidatorTest.kt`

**Interfaces:**
- Consumes: `ValidateDateOfBirthUseCase`, `ValidateHeightUseCase`, `ValidateWeightUseCase`, `Demographics`, `UnitSystem`
- Produces: `DemographicsValidator` class, `DemographicsValidationResult` data class

`DemographicsValidationResult`:
```kotlin
data class DemographicsValidationResult(
    val dateOfBirth: ValidationResult = ValidationResult.Valid,
    val height: ValidationResult = ValidationResult.Valid,
    val weight: ValidationResult = ValidationResult.Valid
) {
    val isValid: Boolean
        get() = dateOfBirth is ValidationResult.Valid &&
                height is ValidationResult.Valid &&
                weight is ValidationResult.Valid
}
```

### Task 7: Update PersonalCardViewModel with Validation

**Files:**
- Modify: `app/src/main/java/com/example/healthjournal/viewmodel/PersonalCardViewModel.kt`
- Test: `app/src/test/java/com/example/healthjournal/viewmodel/PersonalCardViewModelTest.kt`

**Interfaces:**
- Consumes: `DemographicsValidator`, `UnitSystem`, `UnitConverter`
- Produces: Updated `PersonalCardUiState` with validation state and unit system

`PersonalCardUiState` gains:
```kotlin
val unitSystem: UnitSystem = UnitSystem.METRIC,
val validation: DemographicsValidationResult = DemographicsValidationResult()
```

`PersonalCardViewModel` gains:
- `private val demographicsValidator = DemographicsValidator()`
- `onUnitSystemChanged(unitSystem: UnitSystem)`
- `validateDraft()` private method invoked from `onDateOfBirthChanged`, `onHeightChanged`, `onWeightChanged`, `onUnitSystemChanged`
- `onHeightChanged`/`onWeightChanged` parse via `UnitConverter.parseInput` instead of `parseMeasurement` (removed)

### Task 8: Update PersonalCardScreen with Error States and Date Picker

**Files:**
- Modify: `app/src/main/java/com/example/healthjournal/ui/screens/PersonalCardScreen.kt`
- Test: `app/src/androidTest/java/com/example/healthjournal/ui/screens/PersonalCardScreenTest.kt`

**Interfaces:**
- Consumes: `PersonalCardUiState`, `ValidationResult`, `UnitSystem`
- Produces: Updated UI with error states, unit toggle, date picker

`DemographicsEditCard` updates:
- Unit System `ExposedDropdownMenuBox` with Metric/Imperial items
- DOB field with `isError`/`supportingText`, `KeyboardType.Number`, `DateRange` trailing icon launching `DatePickerDialog` with Material 3 `DatePicker`
- Height field with unit label, `isError`/`supportingText`, `KeyboardType.Decimal`
- Weight field with unit label, `isError`/`supportingText`, `KeyboardType.Decimal`
- Save button `enabled = uiState.validation.isValid`
- TalkBack `contentDescription` on form fields

### Task 9: Final Integration Testing

**Files:**
- Test: Full integration test across all components

**Interfaces:**
- Consumes: All previous tasks
- Produces: Verified integration

Steps:
1. Run all unit tests (`./gradlew testDebugUnitTest`)
2. Run all instrumentation tests (`./gradlew connectedDebugAndroidTest`)
3. Manual testing checklist:
   1. Open Personal Card screen
   2. Tap Edit icon
   3. Toggle between Metric and Imperial
   4. Enter date via calendar picker - verify format YYYY-MM-DD
   5. Enter invalid date (future) - verify error message
   6. Enter height in cm - verify validation
   7. Switch to Imperial - verify height converts to inches
   8. Enter weight in kg - verify validation
   9. Verify Save button disabled when validation fails
   10. Verify Save button enabled when all fields valid
   11. Save and verify data persists correctly
4. Commit final changes

## Verification

Run the full test suite:

```bash
./gradlew testDebugUnitTest
```

Commit checkpoint after tasks with green tests.

## Summary

This plan implements comprehensive demographics validation with:

1. **Domain Validation Use Cases**: Separate, testable classes for each field
2. **Unit System Support**: Seamless metric/imperial switching
3. **Native Date Picker**: Material 3 DatePicker for Date of Birth
4. **Real-time Validation**: Error states with localized messages
5. **Save Button Control**: Disabled until all fields pass validation
6. **SI Unit Storage**: Database stores kg/cm, conversions happen in ViewModel
7. **Accessibility**: TalkBack support with content descriptions
8. **Locale-aware Dates**: Uses java.time for proper formatting