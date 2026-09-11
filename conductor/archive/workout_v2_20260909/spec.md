# Specification: Workout Tracking v2

## Overview

A v2 increment on the Workout hub (v1 shipped on this branch): expand the
activity catalog from 3 to 10 types, add manual-interval HIIT sessions, a full
interactive set/reps matrix for strength training, and a 3-2-1 start countdown.
GPS route tracking, background service/notification, ambient mode, and
heart-rate sensors are explicitly deferred. Builds directly on the v1 work
(schema 12→13); adds schema 13→14.

## Functional Requirements

- **Activity catalog (10):** Run, Fitness, Yoga, HIIT, Walking/Hiking, Cycling,
  Stretching/Mobility, Pilates, Swimming, Calisthenics/Bodyweight. Each maps to
  a Health Connect EXERCISE type.
- **Per-type targets:** Run = distance (km); Fitness = duration OR set matrix;
  HIIT = manual intervals (duration); Walking/Cycling = duration or distance;
  Stretching/Pilates = duration; Swimming = duration + lap count;
  Calisthenics = duration + movement counts.
- **HIIT — manual intervals:** timed session with a phase/round indicator; user
  taps "Next interval" to advance (no fixed plan); a haptic cue fires on each
  advance; rounds/intervals recorded in the summary.
- **Strength set matrix (Fitness):** during an active session the user can add
  exercises, log sets (kg + reps), and a rest timer advances between sets;
  tonnage (Σ kg×reps) auto-calculated; exercises/sets persisted with the session
  and shown on the summary.
- **Start countdown:** 3–2–1 before the session timer begins.
- **Manual log dialog** supports all 10 types with duration, calories, date,
  notes + per-type extras (laps for Swimming, movement counts for Calisthenics);
  reuses v1 validation (duration ≤1440 min, calories ≤50000 kcal, strict
  calendar dates).
- **Calorie estimation** extended to new types via MET values.
- **Health Connect write** maps all 10 types to `ExerciseSessionRecord`; session
  notes carried through.

## Non-Functional Requirements

- Extend the existing MVI sealed `WorkoutUiState` machine ("Active" now carries
  interval-phase and set-matrix state); no DI framework (manual per-ViewModel
  Factory).
- Crash recovery persists the full session (incl. sets/intervals) every few
  seconds; Resume/Discard restored on relaunch.
- Immutable state + keyed lazy lists; timer ticks must not cause recomposition
  storms.
- >80% coverage on new code; UI test per new user-facing flow.
- Wiki lint exits 0; affected vault pages updated with the code.

## Acceptance Criteria

- Start/track/finish sessions for all 10 types → correct summary → journal entry
  + Health Connect record.
- HIIT: advance intervals manually, haptic cue each advance, round count
  reflected in summary.
- Fitness: build a set matrix in-session, rest between sets, tonnage shown in
  summary and persisted.
- 3–2–1 countdown shows before the timer starts.
- Manual log validates all new types identically to v1.
- Kill mid-session (incl. mid-set / mid-interval) → resume restores state.

## Out of Scope

- GPS/location, routes, Kalman filtering; foreground service + persistent
  notification; ambient/wake mode; heart-rate/sensor integration; SQLCipher
  encryption; preset HIIT plans (fixed work/rest/rounds); Swimming SWOLF/stroke
  detection.