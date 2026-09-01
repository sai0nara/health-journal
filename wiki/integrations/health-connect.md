# Health Connect Integration

> How this repo reads wearable health metrics (steps, heart rate, sleep) from Android Health Connect.

Last updated: 2026-09-01

## Relationship to Android Health Connect

Health Connect is a separate Android platform service that aggregates health and
fitness data from multiple apps. This repo is a **consumer**: it reads metrics from
Health Connect rather than owning them. The authoritative API lives with Android,
not here — link outward for the canonical contract.

## What this repo holds

The `health` package contains the integration entry point (`HealthConnectManager`),
the client-side wrapper around the Health Connect API. The dependency that pulls in
the Health Connect client is declared in `app/build.gradle.kts`.

## Direction of the dependency

Outbound to the Health Connect platform: the app is the consumer that requests and
reads health data. This repo holds no client for any other health-data provider.

## Cross-references

- [[overview]] — where the app's metric features sit in the layers.
- [[data-layer]] — where imported metrics would be persisted locally.
- [[domain-media]] — where measurement formatting and validation live.

## Sources

- `app/src/main/java/com/example/healthjournal/health/HealthConnectManager.kt` — the Health Connect client wrapper.

Back to [[overview]]
