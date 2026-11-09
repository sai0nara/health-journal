# Implementation Plan: Vitals & Trends

## Phase 1: Domain, Data Access & ViewModel (TDD)
- [ ] Task: Implement `JournalDao` / `JournalRepository` vitals query flow filtered by active profile
    - [ ] Write failing unit tests for vitals queries in `JournalRepositoryTest.kt`
    - [ ] Implement `getVitalsEntriesByProfile(profileId)` in `JournalDao.kt` and `JournalRepository.kt`
    - [ ] Confirm tests pass and refactor
- [ ] Task: Implement `VitalsViewModel` state pipeline & statistics computation
    - [ ] Write failing unit tests in `VitalsViewModelTest.kt` (tab switching, 7D/30D/90D/1Y/All filtering, mean/min/max statistics, same-day aggregation decimation)
    - [ ] Implement `VitalsViewModel.kt` combining profile, unit system, and repository flows into `StateFlow<VitalsUiState>`
    - [ ] Confirm tests pass and refactor
- [ ] Task: Implement quick-log save & deletion/undo logic in ViewModel
    - [ ] Write failing unit tests for `logVital(entry)` and `deleteVital(id)` with undo restoration
    - [ ] Implement quick-log save and undo lifecycle in `VitalsViewModel.kt`
    - [ ] Confirm tests pass and refactor
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Domain, Data Access & ViewModel' (Protocol in workflow.md)

## Phase 2: Canvas Trend Chart Component
- [ ] Task: Implement `VitalsTrendChart` Composable with Compose Canvas
    - [ ] Write Compose UI tests for chart rendering, curve auto-scaling, and reference bands
    - [ ] Implement `VitalsTrendChart.kt` with dual-curve Blood Pressure (systolic/diastolic) and single-curve metrics
    - [ ] Implement interactive touch scrubber cursor with haptic feedback tick and tooltip popup
    - [ ] Confirm UI tests pass on Compose test runner
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Canvas Trend Chart Component' (Protocol in workflow.md)

## Phase 3: Screen Assembly, Navigation & Quick Log Sheet
- [ ] Task: Implement `VitalLogSheet` Composable
    - [ ] Write Compose UI test for `VitalLogSheetTest.kt` (prefill, range validation, future date block, Save/Cancel)
    - [ ] Implement `VitalLogSheet.kt` with numeric inputs, inline validation messages, and date/time picker
    - [ ] Confirm tests pass
- [ ] Task: Assemble `VitalsScreen` Composable
    - [ ] Implement `VitalsScreen.kt` with TopAppBar, `ProfileSwitcher`, metric tab row, time-range chips, summary card, chart, history list, and Undo snackbar
    - [ ] Add empty state illustration and CTA when zero readings exist in selected window
- [ ] Task: Wire `"vitals"` destination into `MainActivity.kt` and `MainScreen.kt`
    - [ ] Add "Vitals" tile to `MainScreen.kt` tile grid
    - [ ] Register `composable("vitals")` route with `VitalsViewModelFactory` in `MainActivity.kt`
- [ ] Task: End-to-end Compose UI test suite for `VitalsScreen`
    - [ ] Write comprehensive UI tests in `VitalsScreenTest.kt` (navigation from MainScreen, tab switching, quick log flow, deletion undo, profile switching)
    - [ ] Execute UI tests and confirm passing
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Screen Assembly, Navigation & Quick Log Sheet' (Protocol in workflow.md)

## Phase 4: Product Documentation & Final Verification
- [ ] Task: Create product specification and test documentation
    - [ ] Author `Docs/psd/vitals-trends.md` detailing architecture, composables, and data flow
    - [ ] Author `Docs/tests/vitals-trends.md` mapping test cases to files
    - [ ] Update `Docs/index.md` removing `— planned` marker
- [ ] Task: Execute full verification suite
    - [ ] Run full JVM unit test suite (`./gradlew :app:testDebugUnitTest`)
    - [ ] Run wiki lint (`python3 wiki/lint.py`)
- [ ] Task: Conductor - User Manual Verification 'Phase 4: Product Documentation & Final Verification' (Protocol in workflow.md)
