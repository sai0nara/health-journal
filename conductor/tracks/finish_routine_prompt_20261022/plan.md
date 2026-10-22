# Track finish_routine_prompt_20261022 — Implementation Plan

## Technical Notes
- Detection lives in `WorkoutViewModel.toggleSetCompleted`: after applying the toggle, fire the prompt only when `newCompleted` is true, the session is a preset routine (`isRoutine`), and every set in the matrix becomes complete.
- Prompt visibility is transient ViewModel state on `WorkoutUiState.Active`, defaulting to false — never persisted, never set on start/resume/recovery.
- The dialog renders in `WorkoutScreen` beside the existing recovery/error `AlertDialog`s; confirm calls `finishSession()`, dismiss calls a new `dismissFinishPrompt()`.
- No schema, sync, or restore changes.

## Phase 1: Failing tests (Red)

- [x] Task: JVM tests for prompt lifecycle
    - [x] Extend `WorkoutViewModelTest`: completing the last planned set exposes the prompt; partial completion does not
    - [x] Manual strength, HIIT, empty routine, and resumed/recovered sessions never expose it
    - [x] Dismiss leaves the session Active and rest timer intact; unchecking clears eligibility; re-completing shows it again
    - [x] Finish from the prompt produces Summary, a persisted `COMPLETED` session, and one linked Fitness entry
    - [x] Run `:app:testDebugUnitTest --tests '*WorkoutViewModelTest'`; confirm expected failures
- [ ] Task: Instrumented UI tests
    - [x] New `FinishRoutinePromptTest`: after the final set, dialog shows title “Finish routine?”, body, Finish and Keep going
    - [x] Keep going dismisses and leaves Active; Finish reaches Summary
    - [x] No dialog for partial sets, manual strength, or HIIT; manual Finish bypasses the prompt
    - [x] Run one class per invocation on SM-F936B; confirm Red
- [ ] Task: Conductor - User Manual Verification 'Phase 1: Failing tests (Red)' (Protocol in workflow.md)

## Phase 2: Implementation (Green + Refactor)

- [x] Task: Prompt state and lifecycle
    - [x] Add a transient prompt flag to `WorkoutUiState.Active`
    - [x] Set it in `toggleSetCompleted` only on the final set’s completion transition; clear it when any set is unchecked
    - [x] Add `dismissFinishPrompt()`; ensure manual Finish and recovery paths are unchanged
    - [x] Add EN/RU strings for title, body, and “Keep going”
- [x] Task: Compose dialog
    - [x] Render the `AlertDialog` in the Active state; Finish → `finishSession()`, Keep going/back/outside → dismiss only
    - [x] Re-run JVM tests green; refactor without behavior change
- [x] Task: Conductor - User Manual Verification 'Phase 2: Implementation (Green + Refactor)' (Protocol in workflow.md)

## Phase 3: UI tests, docs, verification

- [x] Task: Device UI verification (Blue)
    - [x] Run `FinishRoutinePromptTest`, `RoutineExecutionScreenTest`, and workout hub tests; fix selectors/flakiness
- [x] Task: Docs + wiki
    - [x] Update `Docs/prd/workout-presets.md` (FR/AC), `Docs/psd/workout-presets.md` (state/dialog), `Docs/tests/workout-presets.md` (cases + coverage)
    - [x] Update wiki pages cited by changed `WorkoutScreen`/`WorkoutViewModel` sources; bump dates
- [x] Task: Full verification
    - [x] JVM suite green, `./gradlew build` green, touched instrumented classes green, `python3 wiki/lint.py` exit 0
- [ ] Task: Conductor - User Manual Verification 'Phase 3: UI tests, docs, verification' (Protocol in workflow.md)
