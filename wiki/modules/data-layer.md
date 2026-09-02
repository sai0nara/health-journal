# Data Layer

> The Room persistence layer: entities, DAOs, converters, and the repositories that own local reads and writes.

Last updated: 2026-09-01

## What lives here

The `data` and `data/local` packages hold the local persistence stack. The Room
database, its entities, its DAOs, and the type converters all live in `data/local`;
the feature repositories that orchestrate reads/writes live directly under `data`.

Repositories are the boundary the [[viewmodel-layer]] talks to. When a feature needs
to persist or query data, it goes through a repository, never directly through a DAO
from the UI layer.

## Key components

- **Database and DAOs.** Room is configured via the database class in
  `data/local`. Each DAO covers a domain (journal entries, body measurements, goals,
  personal card, body-measurement DAO).
- **Entities and relations.** Entities such as `JournalEntry`, `BodyMeasurementEntry`,
  `GoalEntity`, and `PersonalCard` model rows; cross-reference tables like
  `EntryTagCrossRef` model many-to-many links (journal entry to tags).
- **Type converters.** `JournalTypeConverters` (and the `UnitConverter`) bridge
  non-primitive types into Room columns.
- **Repositories.** `JournalRepository`, `BodyMeasurementRepository`, `GoalsRepository`,
  and `PersonalCardRepository` expose feature-scoped operations.

## Sync coexistence

Local writes are not duplicated into a separate store; [[sync-engine]] builds on top
of this same repository layer and Room database rather than maintaining its own copy.

## Cross-references

- [[viewmodel-layer]] — the layer above that consumes repositories.
- [[sync-engine]] — the Drive sync that reads and writes the same database.
- [[unit-tests]] — repository and DAO behavior is covered on both test stacks.

## Sources

- `app/src/main/java/com/example/healthjournal/data/local/JournalDatabase.kt` — the Room database.
- `app/src/main/java/com/example/healthjournal/data/JournalRepository.kt` — local persistence orchestration.

Back to [[overview]]
