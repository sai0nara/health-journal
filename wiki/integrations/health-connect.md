# Health Connect Integration

> How this repo reads wearable health metrics (steps, heart rate, sleep) from
> Android Health Connect and writes completed workouts as exercise records.

Last updated: 2026-09-08

## Relationship to Android Health Connect

Health Connect is a separate Android platform service that aggregates health and
fitness data from multiple apps. For the vitals feature this repo is a
**consumer**: it reads metrics from Health Connect rather than owning them. For
workouts it is a **producer**: it writes each completed workout as an exercise
record. The authoritative API lives with Android, not here — link outward for the
canonical contract.

## What this repo holds

The `health` package contains the integration entry points: `HealthConnectManager`
(the client-side wrapper that reads vitals and owns the permission gate) and
`HealthConnectWorkoutDataSource` (which writes workout exercise records). The
dependency that pulls in the Health Connect client is declared in
`app/build.gradle.kts`.

## Direction of the dependency

Outbound to the Health Connect platform: the app both reads vitals and writes
workouts. This repo holds no client for any other health-data provider.

## Cross-references

- [[overview]] — where the app's metric features sit in the layers.
- [[data-layer]] — where imported metrics would be persisted locally.
- [[domain-media]] — where measurement formatting and validation live.
- [[ui-layer]] — where the workout hub lives.

## Sources

- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — the Health Connect client wrapper (reads + permission gate).
- `app/src/main/java/com/example/healthjournal/health/HealthConnectWorkoutDataSource.kt` — workout exercise record writes.

Back to [[overview]]
