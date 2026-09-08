# Specification: Workout Tracking (v1)

## Overview
A Workout hub where users browse activity categories (Run, Fitness/Strength, Yoga), quick-start timed sessions, manually log past workouts, and save summaries to the Journal. v1 covers timed sessions without live GPS, crash recovery, and Health Connect workout sync. Entry point: the History screen (top-bar action/menu, alongside Export/Measurements).

## Functional Requirements
- **Discovery (Idle):** Category list (Run, Fitness, Yoga), recent workouts, and metrics overview.
- **Configuration:** Per-type setup — target distance/duration, timer, rest intervals — with validation.
- **Timed active session:** Start/pause/resume/stop with elapsed timer; haptic confirmation on start/stop/lap and interval completion. No GPS route drawing in v1.
- **Summary → Journal:** On finish, a summary screen saves the workout (type, duration, calories, notes) as a journal entry, which then syncs via the existing Drive sync.
- **Manual log:** Per workout type, a 'Log Past <Type>' dialog (opened from the configuration screen, header names the chosen activity) with duration, calories, date, and notes; appends to the Journal feed with validation. The date field copies the Personal Card Date of Birth entry and validation rules (digit masking as yyyy-MM-dd with calendar picker; blank allowed; invalid format, future date, and 130-year rules with identical messages).
- **Crash recovery:** Session state persisted to Room every few seconds; on launch, an unfinished session prompts 'Resume Workout' or 'Discard'.
- **Health Connect:** New EXERCISE read/write permissions through the existing permission flow; completed workouts are written as Health Connect exercise records.
- **Architecture:** MVVM with MVI-style sealed `WorkoutUiState` (`Idle`, `Configuring`, `Active`, `Paused`, `Summary`, `Error`) over `StateFlow`; repository over Room DAO + Health Connect data source; ViewModel built manually via a per-ViewModel `Factory` (no DI framework, per repo convention).

## Non-Functional Requirements
- **Offline-first:** Timers, session persistence, and manual logging work fully offline; Drive sync happens in the background when connected.
- **Timer correctness:** High-frequency timer updates must not cause race conditions, backpressure issues, or list recomposition storms (immutable state, keyed lazy lists, batched Room writes every few seconds — not every tick).
- **Permissions:** Runtime request for Health Connect EXERCISE scopes; no location permission in v1 (no GPS).
- **Design system:** Material 3 via `MaterialTheme.colorScheme` semantic tokens; no absolute colors.

## Acceptance Criteria
- Start/pause/finish a timed workout → correct summary → journal entry appears in feed.
- Manual past-workout log validates input and appends to the feed.
- Killing the app mid-session → relaunch offers Resume/Discard with correct restored state.
- Airplane-mode logging works; syncs afterwards via existing mechanisms.
- With permission granted, a completed workout appears as a Health Connect exercise record; denial fails gracefully with an actionable error.
- UI tests cover discovery, active session, manual log, and recovery; >80% coverage on new code; wiki lint exits 0.

## Out of Scope
- GPS route drawing, location permissions, foreground service with live notification, ambient/always-on mode.
- Live sensor telemetry (e.g., streaming heart rate) during sessions.
- Auto-scheduled/periodic workouts.
- Launcher/store assets.
