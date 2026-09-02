# Instrumented Tests

> The on-device test stack under `app/src/androidTest` — Compose UI tests plus Room DAO and migration tests that need the Android framework.

Last updated: 2026-09-01

## What this stack is

`app/src/androidTest` is the **instrumented test stack** that runs on a connected
device or emulator. It includes Compose UI tests (exercising screens under
`ui/screens` and `ui/components`), Room DAO tests, and database migration tests
under `data/local`. Its dependencies come from the module build file's
`androidTestImplementation` block.

This is the second of two test stacks. Unlike the [[unit-tests]] JVM stack, it
requires a running device and cannot run in a plain JVM context.

## The compose runner flag (important)

Compose instrumented tests set the Allure JUnit4 test listener as the instrumentation
runner by default, which can break Compose UI tests with a "no compose hierarchies
found" error. To run Compose tests, pass the project property that switches that
listener off. This is the single most frequent reason a Compose test "passes in my
head but fails on device."

The exact flag and its wiring to the runner are in the module build file's default
config block.

## How it is selected / executed

Instrumented tests run through the Gradle `connected*` tasks (which need a device and
a running `adb`). The Compose-runner test property must be supplied for the Compose
suites. The JVM suite is a different task and needs no device.

## Cross-references

- [[unit-tests]] — the sibling JVM stack.
- [[ui-layer]] — the screens these Compose tests exercise.
- [[data-layer]] — the DAO and migration tests.
- [[export-restore]] — restore UI and integration tests live on this stack.

## Sources

- `app/src/androidTest/java/com/example/healthjournal/ui/screens` — Compose screen tests live here.
- `app/src/androidTest/java/com/example/healthjournal/data/local` — DAO and migration tests live here.
- `app/build.gradle.kts` — the instrumented test dependencies and the compose-runner listener switch.

Back to [[overview]]
