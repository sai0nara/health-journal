# Unit Tests

> The JVM test stack under `app/src/test` — fast, framework-level tests that run without a device.

Last updated: 2026-09-01

## What this stack is

`app/src/test` is the **JVM unit test stack**. It uses JUnit 4 plus MockK for mocking
and kotlinx-coroutines-test for coroutine control, declared in the module build
file's `testImplementation` dependencies. These tests exercise ViewModels, domain
logic, sync merges/payloads, and export/restore internals without an emulator or
device.

This is one of **two test stacks** and must not be confused with the instrumented
stack described in [[instrumented]]. Roughly: logic that can run on the JVM lives
here; tests that need the Android framework, Room on-device, or Compose UI live in
`app/src/androidTest`.

## How it is selected

The JVM suite runs via Gradle's JVM test task for the `app` module. The instrumented
stack, by contrast, requires a connected device/emulator and the `connected*` tasks.
The two stacks are selected by which Gradle task you run, not by any in-file flag.

## Test data

There is no separate JVM fixture module; tests construct subject instances and
dependencies (often with MockK) inline. Sync tests build small payload/merge fixtures
in place.

## Cross-references

- [[instrumented]] — the other, on-device test stack.
- [[viewmodel-layer]], [[sync-engine]], [[export-restore]] — the code most heavily
  covered by this stack.

## Sources

- `app/src/test/java/com/example/healthjournal/viewmodel` — ViewModel unit tests live here.
- `app/src/test/java/com/example/healthjournal/export` — export/restore unit tests live here.
- `app/build.gradle.kts` — the JVM test dependencies.

Back to [[overview]]
