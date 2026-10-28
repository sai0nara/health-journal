# Track warm_up_20261023 — Specification

## Overview

Preset routines currently jump from the 3-2-1 countdown straight into work sets. This track adds an optional, single timed warm-up to each routine preset: a free-form name plus a duration. During execution it appears before the work sets as a skippable countdown. It is not a strength set and does not affect tonnage, PRs, analytics, the finish prompt, or Health Connect. The session history records the warm-up name, planned/actual duration, and whether it was completed or skipped.

## Context

- `WorkoutPreset` stores preset metadata and exercises; presets are persisted as Room entities with exercise JSON (`app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt`).
- `startPreset` builds the planned `StrengthExercise` matrix and enters the countdown (`app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt`).
- Routine history text is built by `describe()` from the finished session; tonnage is calculated only from `setMatrix` (`WorkoutViewModel.kt`, `domain/StrengthExercise.kt`).
- Current Room schema version is 19; a nullable-column migration is required (`data/local/JournalDatabase.kt`).

## Functional Requirements

- **FR1 — Preset data:** A preset may store one optional warm-up with a non-blank name (max 60 chars) and a duration of 1–60 minutes. Existing presets remain valid with no warm-up.
- **FR2 — Editor:** The create/edit preset form gains an optional Warm-up section: enable/disable toggle, name field, and duration input. Defaults when enabled: 5 minutes. Disabling clears the warm-up on save. Validation is inline and blocks saving invalid values.
- **FR3 — Library:** Preset cards show a compact warm-up summary when configured, e.g. “Warm-up · 5 min”.
- **FR4 — Execution:** After the 3-2-1 countdown, an active preset with a warm-up shows a dedicated warm-up panel with the name, remaining time, and a Skip action. The timer starts automatically and counts down using the existing session clock.
- **FR5 — Completion:** At zero the warm-up is marked completed and the work-set UI appears. Skipping marks it skipped and advances immediately. Both save the outcome.
- **FR6 — Pause and finish:** Pause is disabled while the warm-up is running; Skip is the only early exit. Manual Finish during the warm-up marks it skipped and runs the existing finish flow.
- **FR7 — Recovery:** If the app is killed during the warm-up, recovery starts at the work sets and records the warm-up as skipped; the warm-up is never replayed.
- **FR8 — Session detail:** The finished session records the warm-up name, planned duration, actual elapsed duration, and outcome. The history description includes one warm-up line; the Summary may show the same. The warm-up does not create a `StrengthSet` and is excluded from tonnage, PRs, analytics, and the finish-prompt trigger.
- **FR9 — Health Connect:** The existing session-level Health Connect record is unchanged; no separate warm-up record is written.
- **FR10 — Persistence and compatibility:** Add nullable warm-up columns to `workout_presets` and `workout_sessions`; migrate schema 19→20 with `ALTER TABLE`. Old backups and presets restore with no warm-up. Full backup/restore round-trips the new fields; no sync payload change.
- **FR11 — Localization:** New labels, hints, validation messages, Skip, and history text have EN and RU strings.
- **FR12 — Manual/HIIT:** Manual strength and HIIT sessions are unchanged and never show a warm-up panel.

## Non-Functional Requirements

- No new libraries or tech-stack changes.
- Migration is transactional and covered by a real 19→20 migration test.
- Timer updates remain within the existing one-clock `advanceTime` path; no per-tick database writes beyond the existing cadence.
- Warm-up state is immutable UI state; screen rotation preserves remaining time via the ViewModel.
- Offline-first; no network dependency.
- New code targets >80% coverage; JVM and Compose UI tests cover the user-facing flow.
- Update `Docs/prd|psd|tests/workout-presets.md` and cited wiki pages; `python3 wiki/lint.py` exits 0.

## Acceptance Criteria

- AC1: Create a preset with a 5-minute warm-up, save, reopen, and see the values preserved; edit and remove it successfully.
- AC2: Invalid name/duration shows inline errors and prevents saving.
- AC3: Starting a configured routine shows the warm-up countdown after the countdown; waiting to zero opens the work sets and records “completed”.
- AC4: Skip advances immediately and records “skipped” with the elapsed time.
- AC5: Old presets show no warm-up and start exactly as before.
- AC6: Warm-up is absent from tonnage, PR/analytics inputs, finish-prompt triggering, and Health Connect; finishing during warm-up still completes the session.
- AC7: History shows the warm-up name, duration, and outcome; backup/restore round-trips it.
- AC8: JVM suite, `./gradlew build`, touched instrumented suites, and wiki lint are green.

## Out of Scope

- Multiple warm-up exercises or a warm-up library.
- Logging warm-up sets, reps, weight, RPE, or rest timers.
- Guided mobility routines, heart-rate/GPS warm-up targets, or warm-up progression.
- Changes to manual strength, HIIT, or the existing finish-prompt behavior for work sets.
- Syncing workout sessions or changing the sync payload format.
