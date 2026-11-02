# Data Layer

> The Room persistence layer: entities, DAOs, converters, and the repositories that own local reads and writes.

Last updated: 2026-10-29

## What lives here

The `data` and `data/local` packages hold the local persistence stack. The Room
database, its entities, its DAOs, and the type converters all live in `data/local`;
the feature repositories that orchestrate reads/writes live directly under `data`.

Repositories are the boundary the [[viewmodel-layer]] talks to. When a feature needs
to persist or query data, it goes through a repository, never directly through a DAO
from the UI layer.

## Workout session persistence

`WorkoutSession` rows carry the session lifecycle (status, timestamps, elapsed
seconds, calories) plus two structured payloads: the strength **set matrix** and the
HIIT **interval state**, both of which `JournalTypeConverters` serializes to JSON for
Room's Gson columns. Optional routine warm-up metadata is stored as nullable
columns: `warmupName`, `warmupDurationSeconds`, `warmupElapsedSeconds`, and
`warmupOutcome`. Schema changes ship as versioned migrations in `JournalDatabase.kt`
— consult that file for the current version and the migration list — and the
exported JSON schema lives under `app/schemas/`. These payloads are what the session
engine restores on crash recovery (see [[viewmodel-layer]]).

## Journal entry vitals

Journal entries carry their vitals on the entry row rather than in a separate
table: blood pressure (systolic/diastolic), heart rate, blood glucose, body
temperature, SpO2, and sleep. Values are stored in canonical metric units and
converted for display in the UI layer (see [[ui-layer]]). Health Connect imports
and manual edits write the same columns, so the latest write wins, and rows or
payloads created before the vitals columns existed read them as null after the
forward-only migration registered in `JournalDatabase.kt`.

## Key components

- **Database and DAOs.** Room is configured via the database class in
  `data/local`. Each DAO covers a domain (journal entries, body measurements, goals,
  personal card, workout sessions, and presets).
- **Entities and relations.** Entities such as `JournalEntry`, `BodyMeasurementEntry`,
  `GoalEntity`, `PersonalCard`, and `WorkoutSession` model rows; cross-reference
  tables like `EntryTagCrossRef` model many-to-many links (journal entry to tags).
- **Type converters.** `JournalTypeConverters` (and the `UnitConverter`) bridge
  non-primitive types into Room columns — media/attachment JSON, the workout set
  matrix, and the HIIT interval state.
- **Repositories.** `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`,
  `PersonalCardRepository`, `WorkoutRepository`, and `PresetRepository` expose
  feature-scoped operations.

## Sync coexistence

Local writes are not duplicated into a separate store; [[sync-engine]] builds on top
of this same repository layer and Room database rather than maintaining its own copy.

## Cross-references

- [[viewmodel-layer]] — the layer above that consumes repositories.
- [[sync-engine]] — the Drive sync that reads and writes the same database.
- [[unit-tests]] — repository and DAO behavior is covered on both test stacks.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` — the Room database and migrations.
- `app/src/main/java/com/example/healthjournal/data/local/JournalEntry.kt` — the journal entry entity including the vitals columns.
- `app/src/main/java/com/example/healthjournal/data/local/WorkoutPreset.kt` — preset entity including optional warm-up columns.
- `app/src/main/java/com/example/healthjournal/data/local/WorkoutSession.kt` — session entity including warm-up snapshot/outcome columns.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — local persistence orchestration.

Back to [[overview]]
