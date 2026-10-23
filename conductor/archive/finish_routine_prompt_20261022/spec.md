# Track finish_routine_prompt_20261022 — Specification

## Overview
A preset-driven routine currently ends with the user having to find and tap **Finish** after the last set. When the final planned set is checked, show a modal **“Finish routine?”** confirmation so the session can be closed deliberately. Manual strength sessions and HIIT are unchanged.

## Context
- Active routine UI and Finish button: `app/src/main/java/com/example/healthjournal/ui/screens/WorkoutScreen.kt` (`SessionContent`, `RoutineExecution`)
- Set completion and finish pipeline: `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` (`toggleSetCompleted`, `finishSession`)
- Active state: `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutUiState.kt`
- Strings: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-ru/strings.xml`

## Functional Requirements
- **FR1 Trigger:** in an Active preset routine (FITNESS with at least one planned exercise), checking the last pending set — the transition from “not all sets complete” to “all planned sets complete” — shows a modal confirmation.
- **FR2 Once per transition:** dismissing closes only the prompt. It reappears only if a set is unchecked and the routine is completed again.
- **FR3 Scope:** preset routines only. Manual FITNESS set-matrix sessions and HIIT never show it.
- **FR4 No trigger on load/recovery:** opening an Active or recovered session whose sets are already complete does not show the prompt.
- **FR5 Dialog:** title **“Finish routine?”**, body **“All exercises are complete.”**, confirm **“Finish”**, dismiss **“Keep going”**; localised with Russian parity. Back/outside dismissal behaves like Keep going.
- **FR6 Finish:** invokes the existing `finishSession()` flow (COMPLETED session, linked Fitness entry, Summary). No duplicate save.
- **FR7 Keep going:** session stays Active, rest timer continues, nothing is saved or finished.
- **FR8 Manual Finish unchanged:** the existing Finish button still finishes immediately and does not show this prompt.
- **FR9 Empty routine:** no planned sets/exercises means no prompt.

## Non-Functional Requirements
- State-only: no Room schema, sync payload, or restore manifest changes.
- Prompt state lives in the ViewModel so it survives recomposition/rotation; it is not persisted across process death and never replays from recovery.
- Offline, no new I/O.
- JVM state tests plus Compose instrumented tests; RU string parity; wiki lint exits 0.

## Acceptance Criteria
- **AC1:** completing the last pending set of a preset routine shows the dialog exactly once.
- **AC2:** Keep going leaves the session Active, saves nothing, and the rest timer keeps running.
- **AC3:** Finish reaches Summary, persists `COMPLETED`, and writes exactly one linked Fitness entry.
- **AC4:** no dialog for manual strength, HIIT, partially complete routines, empty routines, or a recovered session that is already complete.
- **AC5:** after Keep going, unchecking and re-checking the final set shows the dialog again.
- **AC6:** tapping Finish manually before or after full completion still finishes immediately without the prompt.
- **AC7:** Russian strings render; JVM and instrumented tests green; wiki lint exit 0.

## Out of Scope
- Auto-finishing without confirmation.
- Rest-timer, summary, linked-entry, or haptic changes.
- Prompts for manual strength, HIIT, early Finish, or recovery.
- Per-exercise completion prompts.
