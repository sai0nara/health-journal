# ViewModel Layer

> The MVVM state holders: each feature's ViewModel exposes `StateFlow` state and orchestrates calls into the data layer.

Last updated: 2026-10-29

## What lives here

The `viewmodel` package holds the architecture's business-logic layer. Each ViewModel
owns the UI state for a screen (or feature), exposes it as a `StateFlow`, and turns
user intents into calls against repositories in [[data-layer]] or helpers from
[[domain-media]] and the [[export-restore]] feature.

Per the project convention, ViewModels never render UI and screens never call the
data layer directly — ViewModels are the only path between the two.

## Key components

- `JournalViewModel` — the journal feed and entry editing flows, plus the
  Drive-sync state mapped from WorkManager progress: status text, the
  manual-sync spinner gate, and an auth-required flag the Settings screen
  turns into an in-place re-authorize action. It also owns manual-vitals writes
  (`updateVitals` and the create/update vitals fields), marking the entry
  pending sync while preserving its text, tags, media, and archive state.
- `BodyMeasurementViewModel` / `BodyAnalyticsViewModel` — measurement capture and
  the weight/trend analytics the measurements screens render.
- `PersonalCardViewModel` — the personal-card feature.
- `PresetViewModel` — the routine library/editor: draft validation, load/save/delete,
  and the optional warm-up fields (`warmupEnabled`, name, duration).
- `WorkoutViewModel` — the workout hub and session state machine: countdown, active
  timer, routine set matrix, finish prompt, crash recovery, journaling, and the
  optional routine warm-up. A warm-up is copied from the preset at start, runs
  after the countdown, and records planned/actual elapsed time plus a
  `COMPLETED`/`SKIPPED` outcome. Skip, Finish, and recovery mark it skipped; Pause
  is blocked while it runs. It is never a strength set, so it contributes no
  tonnage, PR input, finish-prompt trigger, or Health Connect record. The
  persisted history line is produced through an injected
  `WarmupHistoryFormatter`, so it follows the device locale at finish time.
- The export/restore feature adds its own ViewModels (export and restore), which the
  [[export-restore]] page describes.

## Cross-references

- [[ui-layer]] — the screens that consume this state.
- [[data-layer]] — the repositories these ViewModels drive.
- [[unit-tests]] — ViewModel logic is covered by the JVM unit stack.

## Sources

- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — journal feed state and intents.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — measurement capture state.
- `app/src/main/java/com/example/healthjournal/viewmodel/PresetViewModel.kt` — routine preset editor state.
- `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt` — workout session and warm-up state machine.
- `app/src/main/java/com/example/healthjournal/domain/WarmupHistoryFormatter.kt` — formatter seam for the persisted warm-up history line.

Back to [[overview]]
