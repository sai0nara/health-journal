# ViewModel Layer

> The MVVM state holders: each feature's ViewModel exposes `StateFlow` state and orchestrates calls into the data layer.

Last updated: 2026-09-01

## What lives here

The `viewmodel` package holds the architecture's business-logic layer. Each ViewModel
owns the UI state for a screen (or feature), exposes it as a `StateFlow`, and turns
user intents into calls against repositories in [[data-layer]] or helpers from
[[domain-media]] and the [[export-restore]] feature.

Per the project convention, ViewModels never render UI and screens never call the
data layer directly — ViewModels are the only path between the two.

## Key components

- `JournalViewModel` — the journal feed and entry editing flows.
- `BodyMeasurementViewModel` / `BodyAnalyticsViewModel` — measurement capture and
  the weight/trend analytics the measurements screens render.
- `PersonalCardViewModel` — the personal-card feature.
- The export/restore feature adds its own ViewModels (export and restore), which the
  [[export-restore]] page describes.

## Cross-references

- [[ui-layer]] — the screens that consume this state.
- [[data-layer]] — the repositories these ViewModels drive.
- [[unit-tests]] — ViewModel logic is covered by the JVM unit stack.

## Sources

- `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt` — journal feed state and intents.
- `app/src/main/java/com/example/healthjournal/viewmodel/BodyMeasurementViewModel.kt` — measurement capture state.

Back to [[overview]]
