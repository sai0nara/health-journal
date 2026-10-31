# Health Connect Integration

> How this repo reads wearable health metrics (steps, heart rate, sleep) from
> Android Health Connect and writes completed workouts as exercise records.

Last updated: 2026-10-29

## Opt-in gate

Every Health Connect call in this repo is gated behind a user-facing Settings
toggle that defaults to off: when disabled the app performs no reads, no
writes, and requests no health permissions, while previously synced metric
values stay untouched in the database. The toggle is persisted in
`app/src/main/java/com/example/healthjournal/data/local/HealthSyncSettings.kt`
and enforced in `app/src/main/java/com/example/healthjournal/viewmodel/JournalViewModel.kt`
(entry vitals) and `app/src/main/java/com/example/healthjournal/viewmodel/WorkoutViewModel.kt`
(exercise writes) plus the entry UI.

## Manual vitals alongside imports

Entry vitals are not a separate Health Connect-owned store: the entry UI keeps one
draft seeded from the stored entry, and an import overwrites only blood pressure,
heart rate, and sleep. Manually typed blood glucose, body temperature, and SpO2
survive the import, and a later manual save replaces the imported BP/HR values in
the same columns (see [[ui-layer]] and [[data-layer]]).

## Relationship to Android Health Connect

Health Connect is a separate Android platform service that aggregates health and
fitness data from multiple apps. For the vitals feature this repo is a
**consumer**: it reads metrics from Health Connect rather than owning them. For
workouts it is a **producer**: it writes each completed workout as an exercise
record. The authoritative API lives with Android, not here — link outward for the
canonical contract.

## Workout exercise-record writes

Completed workouts are written as `ExerciseSessionRecord`s. The platform-agnostic
mapping lives on the `WorkoutHealthDataSource` seam ([JVM-tested](../tests/unit-tests)):

- `WorkoutHealthMapper` converts a `WorkoutSession` into a `WorkoutHealthRecord`
  — each of the 10 workout types maps to its exercise counterpart
  (`RUN`→running, `FITNESS`→strength training, `HIIT`→high-intensity interval
  training, etc.), with unknown types degrading to `UNKNOWN` instead of throwing.
- `HealthConnectWorkoutDataSource` implements that seam against the actual Health
  Connect client: it inserts exercise records with the session's start/end
  offsets, requests exercise read/write permissions, and gracefully reports
  failure when permission is missing or the write throws (airplane mode).
- The viewmodel-layer always completes the journal write and summary regardless
  of Health Connect availability, so denial or offline never blocks logging.
- A routine warm-up shares the session's single exercise record; it does not
  create an extra record or change the mapped exercise type.

## What this repo holds

The `health` package contains the integration entry points: `HealthConnectManager`
(the client-side wrapper that reads vitals and owns the permission gate),
`HealthConnectWorkoutDataSource` (which writes workout exercise records), and the
`WorkoutHealthDataSource` seam plus `WorkoutHealthMapper` that keep that write
JVM-testable. The dependency that pulls in the Health Connect client is declared
in `app/build.gradle.kts`.

## Direction of the dependency

Outbound to the Health Connect platform: the app both reads vitals and writes
workouts. This repo holds no client for any other health-data provider.

## Cross-references

- [[overview]] — where the app's metric features sit in the layers.
- [[data-layer]] — where imported metrics would be persisted locally.
- [[domain-media]] — where measurement formatting and validation live.
- [[ui-layer]] — where the workout hub lives.
- [[viewmodel-layer]] — the finish flow that triggers the exercise write.

## Sources

- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — the Health Connect client wrapper (reads + permission gate).
- `app/src/main/java/com/example/healthjournal/health/WorkoutHealthDataSource.kt` — the platform-agnostic workout-record seam.
- `app/src/main/java/com/example/healthjournal/health/WorkoutHealthMapper.kt` — the session → record mapping (JVM-tested).
- `app/src/main/java/com/example/healthjournal/health/HealthConnectWorkoutDataSource.kt` — workout exercise record writes.

Back to [[overview]]
