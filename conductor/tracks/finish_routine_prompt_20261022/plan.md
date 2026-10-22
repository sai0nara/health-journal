# Track finish_routine_prompt_20261022 — Implementation Plan

## Technical Notes
- Detection lives in `WorkoutViewModel.toggleSetCompleted`: after applying the toggle, fire the prompt only when `newCompleted` is true, the session is a preset routine (`isRoutine`), and every set in the matrix becomes complete.
- Prompt visibility is transient ViewModel state on `WorkoutUiState.Active`, defaulting to false — never persisted, never set on start/resume/recovery.
- The dialog renders in `WorkoutScreen` beside the existing recovery/error `AlertDialog`s; confirm calls `finishSession()`, dismiss calls a new `dismissFinishPrompt()`.
- No schema, sync, or restore changes.

## Phase 1: Failing tests (Red)

- [ ] Task: JVM tests for prompt lifecycle
    - [ ] Extend `WorkoutViewModelTest`: completing the last planned set exposes the prompt; partial completion does not
    - [ ] Manual strength, HIIT, empty routine, and resumed/recovered sessions never expose it
    - [ ] Dismiss leaves the session Active and rest timer intact; unchecking clears eligibility; re-completing shows it again
    - [ ] Finish from the prompt produces Summary, a persisted `COMPLETED` session, and one linked Fitness entry
    - [ ] Run `:app:testDebugUnitTest --tests '*WorkoutViewModelTest'`; confirm expected failures
- [ ] Task: Instrumented UI tests
    - [ ] New `FinishRoutinePromptTest`: after the final set, dialog shows title “Finish routine?”, body, Finish and Keep going
    - [ ] Keep going dismisses and leaves Active; Finish reaches Summary
    - [ ] No dialog for partial sets, manual strength, or HIIT; manual Finish bypasses the prompt
    - [ ] Run one class per invocation on SM-F936B; confirm Red
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Failing tests (Red)' (Protocol in workflow.md)

## Phase 2: Implementation (Green + Refactor)

- [ ] Task: Prompt state and lifecycle
    - [ ] Add a transient prompt flag to `WorkoutUiState.Active`
    - [ ] Set it in `toggleSetCompleted` only on the final set’s completion transition; clear it when any set is unchecked
    - [ ] Add `dismissFinishPrompt()`; ensure manual Finish and recovery paths are unchanged
    - [ ] Add EN/RU strings for title, body, and “Keep going”
- [ ] Task: Compose dialog
    - [ ] Render the `AlertDialog` in the Active state; Finish → `finishSession()`, Keep going/back/outside → dismiss only
    - [ ] Re-run JVM tests green; refactor without behavior change
- [ ] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md)

## Phase 3: UI tests, docs, verification

- [ ] Task: Device UI verification (Blue)
    - [ ] Run `FinishRoutinePromptTest`, `RoutineExecutionScreenTest`, and workout hub tests; fix selectors/flakiness
- [ ] Task: Docs + wiki
    - [ ] Update `Docs/prd/workout-presets.md` (FR/AC), `Docs/psd/workout-presets.md` (state/dialog), `Docs/tests/workout-presets.md` (cases + coverage)
    - [ ] Update wiki pages cited by changed `WorkoutScreen`/`WorkoutViewModel` sources; bump dates
- [ ] Task: Full verification
    - [ ] JVM suite green, `./gradlew build` green, touched instrumented classes green, `python3 wiki/lint.py` exit 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI tests, docs, verification' (Protocol in workflow.md)
