# Track move_add_measurement_button_20261016 — Implementation Plan

## Phase 1: Failing tests (Red)

- [x] Task: History single-FAB failing tests (ca09619)
    - [x] Extend `HistoryScreenTest` with a failing assertion: exactly one FAB (add entry) is composed and no measurement action or sheet trigger exists
    - [x] Run the new History assertions and confirm they fail (`Red`) with `JAVA_HOME` on JDK 21
- [x] Task: Measurements FAB failing tests (ca09619)
    - [x] Extend `MeasurementScreenTest` / `MeasurementEntrySheetTest` with failing assertions: FAB visible in empty and populated states and opens the entry sheet on click
    - [x] Run the new Measurements assertions and confirm they fail (`Red`) with `JAVA_HOME` on JDK 21
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Failing tests (Red)' (Protocol in workflow.md)

## Phase 2: Implementation (Green + Refactor)

- [x] Task: Remove measurement entry point from History (2dfb792)
    - [x] Delete the measurement `SmallFloatingActionButton`, `showMeasurementSheet` state, `MeasurementEntrySheet` host, measurement ViewModel instance, `measurementViewModelFactory` parameter, and the `onUnitSystemChanged` sync block from `HistoryScreen.kt`
    - [x] Update all callers of `HistoryScreen` for the removed parameter and confirm the single add-entry FAB plus the top-bar measurements navigation action remain
- [x] Task: Add measurement FAB to Measurements screen (0b157da)
    - [x] Add a regular `FloatingActionButton` (Straighten icon, existing measurement content description) to `MeasurementsScreen.kt`, visible in empty and populated states
    - [x] Host `MeasurementEntrySheet` behind a local `showSheet` boolean reusing the screen's existing `BodyMeasurementViewModel`; no ViewModel or sheet logic changes
    - [x] Re-run the Phase 1 tests and confirm green; refactor for clarity/duplication and re-run to stay green
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md)

## Phase 3: Docs + verification (Blue + gates)

- [x] Task: Ship PSD and test-case docs (fceba64)
    - [x] Write `Docs/psd/move-add-measurement-button.md` from the confirmed spec
    - [x] Write `Docs/tests/move-add-measurement-button.md` covering AC-1 through AC-5
    - [x] Drop the `— planned` mark on the PRD line in `Docs/index.md`
- [x] Task: Full verification
    - [x] Run `./gradlew :app:testDebugUnitTest` green with `CI=true` and `JAVA_HOME` on JDK 21
    - [x] Write/run UI tests for both touched surfaces until green on an emulator (`connectedAndroidTest` for the touched screens, then `adb emu kill`)
    - [x] Verify coverage >80% on new/changed code and run `python3 wiki/lint.py` to exit 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: Docs + verification (Blue + gates)' (Protocol in workflow.md)

## Review
- Applied review fix: removed 5 duplicated unchecked sub-task lines (d0c7aab)
